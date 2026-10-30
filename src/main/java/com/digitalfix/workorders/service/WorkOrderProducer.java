
package com.digitalfix.workorders.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Configuracion existente de Workorders
    @Value("${rabbitmq.exchange.main}")
    private String exchangeMain;

    @Value("${rabbitmq.routing.workorders}")
    private String routingWorkorders;

    // Configuracion del microservicio de Notificaciones
    @Value("${rabbitmq.notificaciones.exchange}")
    private String exchangeNotificaciones;

    @Value("${rabbitmq.notificaciones.routing.gmail}")
    private String routingGmail;

    @Value("${rabbitmq.notificaciones.routing.interna}")
    private String routingInterna;

    // Metodo original de Workorders
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

    // Metodo existente para solicitudes de correo
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

    // Nuevo metodo para notificaciones internas
    public void enviarNotificacionInterna(String mensajeJson) {
        rabbitTemplate.convertAndSend(
                exchangeNotificaciones,
                routingInterna,
                mensajeJson
        );

        System.out.println(
                "[RabbitMQ] Notificacion interna publicada: "
                + mensajeJson
        );
    }
}
