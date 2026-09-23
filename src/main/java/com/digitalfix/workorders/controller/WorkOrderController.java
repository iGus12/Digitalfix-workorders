package com.digitalfix.workorders.controller;

import com.digitalfix.workorders.entity.OrderStatus;
import com.digitalfix.workorders.entity.WorkOrder;
import com.digitalfix.workorders.repository.WorkOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workorders")
public class WorkOrderController {

    @Autowired
    private WorkOrderRepository repository;

    @GetMapping
    public List<WorkOrder> getAllOrders() {
        return repository.findAll();
    }

    @PostMapping
    public WorkOrder createOrder(@RequestBody WorkOrder order) {
        if (order.getStatus() == null) {
        order.setStatus(OrderStatus.CREADA);
        }
        return repository.save(order);
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
            return ResponseEntity.ok(repository.save(order));
        }).orElse(ResponseEntity.notFound().build());
    }
}