package com.digitalfix.workorders.controller;

import com.digitalfix.workorders.entity.OrderStatus;
import com.digitalfix.workorders.entity.WorkOrder;
import com.digitalfix.workorders.repository.WorkOrderRepository;
import com.digitalfix.workorders.service.WorkOrderProducer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public WorkOrder createOrder(@RequestBody WorkOrder order) {
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.CREADA);
        }
        
        // 1. Guardar en la base de datos Oracle
        WorkOrder savedOrder = repository.save(order);
        
        // 2. Disparar el evento asíncrono a RabbitMQ
        workOrderProducer.notificarNuevaOrden("Nueva orden creada ID: " + savedOrder.getId());
        
        return savedOrder;
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrder> getOrderById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody WorkOrder statusUpdate) {
        return repository.findById(id).map(order -> {
            
            if (statusUpdate.getStatus() == OrderStatus.EN_EJECUCION && order.getTechnician() == null) {
                return ResponseEntity.badRequest().body("Error: No se puede pasar a EN_EJECUCION sin asignar un técnico antes.");
            }

            order.setStatus(statusUpdate.getStatus());
            if (statusUpdate.getTechnician() != null) {
                order.setTechnician(statusUpdate.getTechnician());
            }
            
            // 1. Guardar la actualización en Oracle
            WorkOrder updatedOrder = repository.save(order);
            
            // 2. Disparar notificación de actualización a RabbitMQ
            workOrderProducer.notificarNuevaOrden("Orden ID: " + updatedOrder.getId() + " cambió a estado: " + updatedOrder.getStatus());
            
            return ResponseEntity.ok(updatedOrder);
        }).orElse(ResponseEntity.notFound().build());
    }
}