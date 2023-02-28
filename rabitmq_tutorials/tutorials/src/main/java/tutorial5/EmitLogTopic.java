package tutorial5;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class EmitLogTopic {

    private static final String EXCHANGE_NAME = "topic_logs";
    private static final String EXCHANGE_TYPE = "topic";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, EXCHANGE_TYPE);

            String routingKey = "kern.critical";
            String msg = "A critical kernel error";

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, msg.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + routingKey + ": '" + msg + "'");
        }
    }
}
