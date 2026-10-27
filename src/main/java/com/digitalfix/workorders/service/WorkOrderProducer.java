
package com.digitalfix.workorders.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Configuración existente de Workorders
    @Value("${rabbitmq.exchange.main}")
    private String exchangeMain;

    @Value("${rabbitmq.routing.workorders}")
    private String routingWorkorders;

    // Configuración del microservicio de Notificaciones
    @Value("${rabbitmq.notificaciones.exchange}")
    private String exchangeNotificaciones;

    @Value("${rabbitmq.notificaciones.routing.gmail}")
    private String routingGmail;

    // Método original: se conserva
    public void notificarNuevaOrden(String mensaje) {
        rabbitTemplate.convertAndSend(
                exchangeMain,
                routingWorkorders,
                mensaje
        );

        System.out.println(
                "[RabbitMQ] Mensaje enviado a Workorders: " + mensaje
        );
    }

    // Nuevo método para enviar solicitudes a Notificaciones
    public void enviarNotificacionGmail(String mensajeJson) {

        rabbitTemplate.convertAndSend(
                exchangeNotificaciones,
                routingGmail,
                mensajeJson
        );

        System.out.println(
                "[RabbitMQ] Solicitud enviada a Notificaciones: "
                + mensajeJson
        );
    }
}
