package com.digitalfix.workorders.service;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class WorkOrderConsumer {

    @RabbitListener(queues = "${rabbitmq.queue.workorders}")
    public void recibirMensaje(String mensaje, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            System.out.println(" [x] Procesando notificación: " + mensaje);
            
            if (mensaje.contains("ErrorFalso")) {
                throw new RuntimeException("Falla simulada en procesamiento");
            }
            
            channel.basicAck(tag, false);
            
        } catch (Exception e) {
            System.err.println(" [!] Error procesando orden. Enviando a DLQ: " + e.getMessage());
            
            channel.basicNack(tag, false, false);
        }
    }
}