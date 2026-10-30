
package com.digitalfix.workorders.controller;

import com.digitalfix.workorders.entity.OrderStatus;
import com.digitalfix.workorders.entity.WorkOrder;
import com.digitalfix.workorders.repository.WorkOrderRepository;
import com.digitalfix.workorders.service.WorkOrderProducer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@RestController
@RequestMapping("/api/workorders")
public class WorkOrderController {

    @Autowired
    private WorkOrderRepository repository;

    @Autowired
    private WorkOrderProducer workOrderProducer;

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

        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.CREADA);
        }

        // 1. Guardar la orden en Oracle
        WorkOrder savedOrder = repository.save(order);

        // 2. Mantener el evento original de RabbitMQ
        workOrderProducer.notificarNuevaOrden(
            "Nueva orden creada ID: " + savedOrder.getId()
        );

        // 3. Enviar la notificacion interna al usuario autenticado
        String mensajeNotificacion = String.format(
            "{\"usuarioId\":\"%s\",\"titulo\":\"Nueva orden de trabajo\",\"mensaje\":\"Se ha creado correctamente la orden de trabajo N.º %d\",\"ordenId\":%d}",
            usuarioId,
            savedOrder.getId(),
            savedOrder.getId()
        );

        workOrderProducer.enviarNotificacionInterna(mensajeNotificacion);

        return savedOrder;
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrder> getOrderById(@PathVariable Long id) {
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
                    "Error: No se puede pasar a EN_EJECUCION sin asignar un técnico antes."
                );
            }

            order.setStatus(statusUpdate.getStatus());

            if (statusUpdate.getTechnician() != null) {
                order.setTechnician(statusUpdate.getTechnician());
            }

            // 1. Guardar la actualizacion en Oracle
            WorkOrder updatedOrder = repository.save(order);

            // 2. Mantener el evento original de RabbitMQ
            workOrderProducer.notificarNuevaOrden(
                "Orden ID: " + updatedOrder.getId()
                + " cambió a estado: " + updatedOrder.getStatus()
            );

            return ResponseEntity.ok(updatedOrder);

        }).orElse(ResponseEntity.notFound().build());
    }
}
