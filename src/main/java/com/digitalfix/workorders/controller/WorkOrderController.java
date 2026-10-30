
package com.digitalfix.workorders.controller;

import com.digitalfix.workorders.entity.OrderStatus;
import com.digitalfix.workorders.entity.WorkOrder;
import com.digitalfix.workorders.repository.WorkOrderRepository;
import com.digitalfix.workorders.service.WorkOrderProducer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workorders")
public class WorkOrderController {

    @Autowired
    private WorkOrderRepository repository;

    @Autowired
    private WorkOrderProducer workOrderProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public List<WorkOrder> getAllOrders() {
        return repository.findAll();
    }

    @PostMapping
    public WorkOrder createOrder(
            @RequestBody WorkOrder order,
            @AuthenticationPrincipal Jwt jwt) {

        String usuarioId = jwt.getSubject();

        System.out.println(
            "[Workorders] Orden solicitada por usuario: " + usuarioId
        );

        // Comprobar los datos disponibles en el token de Microsoft
        // No se muestra el correo ni el token completo
        System.out.println(
            "[Workorders] Tiene preferred_username: "
            + (jwt.getClaimAsString("preferred_username") != null)
        );

        System.out.println(
            "[Workorders] Tiene email: "
            + (jwt.getClaimAsString("email") != null)
        );

        System.out.println(
            "[Workorders] Tiene upn: "
            + (jwt.getClaimAsString("upn") != null)
        );

        // 1. Establecer el estado inicial de la orden
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.CREADA);
        }

        // 2. Guardar la orden en Oracle
        WorkOrder savedOrder = repository.save(order);

        // 3. Mantener el evento original de RabbitMQ
        workOrderProducer.notificarNuevaOrden(
            "Nueva orden creada ID: " + savedOrder.getId()
        );

        // 4. Enviar la notificacion interna a la campanita
        Map<String, Object> datosNotificacion = new HashMap<>();

        datosNotificacion.put("usuarioId", usuarioId);
        datosNotificacion.put("titulo", "Nueva orden de trabajo");
        datosNotificacion.put(
            "mensaje",
            "Se ha creado correctamente la orden de trabajo N.º "
            + savedOrder.getId()
        );
        datosNotificacion.put("ordenId", savedOrder.getId());

        try {
            String mensajeNotificacion =
                    objectMapper.writeValueAsString(datosNotificacion);

            workOrderProducer.enviarNotificacionInterna(
                mensajeNotificacion
            );

        } catch (JsonProcessingException error) {
            System.err.println(
                "[Workorders] Error al preparar la notificacion interna: "
                + error.getMessage()
            );
        }

        // 5. Obtener el correo del usuario autenticado
        String correoUsuario =
                jwt.getClaimAsString("preferred_username");

        // 6. Publicar solicitud de correo mediante RabbitMQ
        if (correoUsuario != null
                && correoUsuario.matches(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                )) {

            Map<String, Object> datosCorreo = new HashMap<>();

            datosCorreo.put("para", correoUsuario);
            datosCorreo.put(
                "asunto",
                "Nueva orden de trabajo - DigitalFix"
            );
            datosCorreo.put(
                "mensaje",
                "Se ha creado correctamente la orden de trabajo N.º "
                + savedOrder.getId()
                + " en DigitalFix."
            );
            datosCorreo.put(
                "ordenId",
                String.valueOf(savedOrder.getId())
            );

            try {
                String mensajeCorreo =
                        objectMapper.writeValueAsString(datosCorreo);

                workOrderProducer.enviarNotificacionGmail(
                    mensajeCorreo
                );

                System.out.println(
                    "[Workorders] Solicitud de correo publicada para orden: "
                    + savedOrder.getId()
                );

            } catch (JsonProcessingException error) {
                System.err.println(
                    "[Workorders] Error al preparar el correo: "
                    + error.getMessage()
                );
            }

        } else {
            System.out.println(
                "[Workorders] No se publico correo: "
                + "el token no contiene una direccion valida."
            );
        }

        return savedOrder;
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrder> getOrderById(
            @PathVariable Long id) {

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody WorkOrder statusUpdate) {

        return repository.findById(id).map(order -> {

            if (statusUpdate.getStatus() == OrderStatus.EN_EJECUCION
                    && order.getTechnician() == null) {

                return ResponseEntity.badRequest().body(
                    "Error: No se puede pasar a EN_EJECUCION "
                    + "sin asignar un técnico antes."
                );
            }

            order.setStatus(statusUpdate.getStatus());

            if (statusUpdate.getTechnician() != null) {
                order.setTechnician(statusUpdate.getTechnician());
            }

            // Guardar la actualizacion en Oracle
            WorkOrder updatedOrder = repository.save(order);

            // Mantener el evento original de RabbitMQ
            workOrderProducer.notificarNuevaOrden(
                "Orden ID: " + updatedOrder.getId()
                + " cambió a estado: " + updatedOrder.getStatus()
            );

            return ResponseEntity.ok(updatedOrder);

        }).orElse(ResponseEntity.notFound().build());
    }
}
