package tutorial4;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class EmitLogDirect {

    private static final String EXCHANGE_NAME = "direct_logs";
    private static final String EXCHANGE_TYPE = "direct";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, EXCHANGE_TYPE);

            String severity = "info";
            String msg = "test 'info' severity message";
            channel.basicPublish(EXCHANGE_NAME, severity, null, msg.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + severity + "': '" + msg + "'");

            severity = "warning";
            msg = "test 'warning' severity message";
            channel.basicPublish(EXCHANGE_NAME, severity, null, msg.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + severity + "': '" + msg + "'");

            severity = "error";
            msg = "test 'error' severity message";
            channel.basicPublish(EXCHANGE_NAME, severity, null, msg.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + severity + "': '" + msg + "'");
        }
    }
}
