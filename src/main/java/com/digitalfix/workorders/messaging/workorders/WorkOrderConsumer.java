
package com.digitalfix.workorders.messaging.workorders;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class WorkOrderConsumer {

    private static final int MAX_INTENTOS = 3;

    @RabbitListener(queues = "${rabbitmq.queue.workorders}")
    public void recibirMensaje(
            String mensaje,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag)
            throws IOException {

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {

            try {
                System.out.println(
                    "[RabbitMQ] Procesando mensaje. Intento "
                    + intento + ": " + mensaje
                );

                // Simulación de error no recuperable
                if (mensaje.contains("ErrorFalso")) {
                    throw new IllegalArgumentException(
                        "Error no recuperable simulado"
                    );
                }

                // Simulación de error temporal
                if (mensaje.contains("ErrorTemporal")) {
                    throw new RuntimeException(
                        "Error temporal simulado"
                    );
                }

                // Procesamiento exitoso
                System.out.println(
                    "[RabbitMQ] Mensaje procesado correctamente"
                );

                channel.basicAck(tag, false);
                return;

            } catch (IllegalArgumentException e) {

                // Error definitivo: no reintentar
                System.err.println(
                    "[RabbitMQ] Error no recuperable: "
                    + e.getMessage()
                );

                channel.basicNack(tag, false, false);
                return;

            } catch (Exception e) {

                System.err.println(
                    "[RabbitMQ] Error en intento "
                    + intento + ": " + e.getMessage()
                );

                if (intento == MAX_INTENTOS) {

                    System.err.println(
                        "[RabbitMQ] Reintentos agotados. "
                        + "Enviando mensaje a DLQ"
                    );

                    channel.basicNack(tag, false, false);
                    return;
                }
            }
        }
    }
}
