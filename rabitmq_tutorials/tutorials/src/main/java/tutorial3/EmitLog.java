package tutorial3;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class EmitLog {

    private static final String EXCHANGE_NAME = "logs";
    private static final String EXCHANGE_TYPE = "fanout";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, EXCHANGE_TYPE);

            String msg = argv.length < 1 ? "info: Hello World!" : String.join(" ", argv);

            channel.basicPublish(EXCHANGE_NAME, "", null, msg.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + msg + "'");
        }
    }

}
