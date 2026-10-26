package com.digitalfix.workorders.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Lectura de variables centralizadas
    @Value("${rabbitmq.exchange.main}")
    private String exchangeMain;
    
    @Value("${rabbitmq.exchange.dlx}")
    private String exchangeDlx;

    @Value("${rabbitmq.queue.workorders}")
    private String queueWorkorders;

    @Value("${rabbitmq.routing.workorders}")
    private String routingWorkorders;

    @Value("${rabbitmq.queue.dlq}")
    private String queueDlq;

    @Value("${rabbitmq.routing.dlq}")
    private String routingDlq;

    // 1. EXCHANGES
    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(exchangeMain);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(exchangeDlx);
    }

    // 2. QUEUES (Cola de trabajo conectada a la DLQ para errores)
    @Bean
    public Queue workordersQueue() {
        return QueueBuilder.durable(queueWorkorders)
                .withArgument("x-dead-letter-exchange", exchangeDlx)
                .withArgument("x-dead-letter-routing-key", routingDlq)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(queueDlq).build();
    }

    // 3. BINDINGS (Reglas de enrutamiento)
    @Bean
    public Binding bindingWorkorders() {
        return BindingBuilder.bind(workordersQueue()).to(mainExchange()).with(routingWorkorders);
    }

    @Bean
    public Binding bindingDlq() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(routingDlq);
    }
}