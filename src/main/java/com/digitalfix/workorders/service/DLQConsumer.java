package com.digitalfix.workorders.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class DLQConsumer {

    @RabbitListener(queues = "${rabbitmq.queue.dlq}")
    public void procesarMensajeFallido(String mensaje) {
        // Registramos el error fatal en la consola/logs
        System.err.println(" [DLQ-ALERTA] Mensaje irrecuperable capturado en la cola muerta: " + mensaje);
    }
}