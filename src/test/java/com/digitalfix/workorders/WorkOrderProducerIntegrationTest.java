
package com.digitalfix.workorders;

import com.digitalfix.workorders.service.WorkOrderProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WorkOrderProducerIntegrationTest {

    @Autowired
    private WorkOrderProducer workOrderProducer;

    @Test
    void enviarMensajeANotificaciones() {

        String mensajeJson = """
            {
              "para": "prueba@ejemplo.com",
              "asunto": "Integracion DigitalFix",
              "mensaje": "Workorders envio una notificacion mediante RabbitMQ.",
              "ordenId": "15"
            }
            """;

        workOrderProducer.enviarNotificacionGmail(mensajeJson);
    }
}
