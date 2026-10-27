
package com.digitalfix.workorders.messaging.deadletter;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DLQConsumer {

    @RabbitListener(queues = "${rabbitmq.queue.dlq}")
    public void procesarMensajeFallido(
            String mensaje,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag)
            throws IOException {

        System.err.println(
            "[DLQ-ALERTA] Mensaje fallido recibido: " + mensaje
        );

        // Confirmar que el mensaje fue registrado en el log.
        channel.basicAck(tag, false);
    }
}
