package com.digitalfix.workorders.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Leemos el exchange y la ruta desde el properties
    @Value("${rabbitmq.exchange.main}")
    private String exchangeMain;

    @Value("${rabbitmq.routing.workorders}")
    private String routingWorkorders;

    // Método que enviará el mensaje
    public void notificarNuevaOrden(String mensaje) {
        rabbitTemplate.convertAndSend(exchangeMain, routingWorkorders, mensaje);
        System.out.println(" [x] Mensaje enviado a RabbitMQ: " + mensaje);
    }
}