package tutorial6;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class RPCServer {

    private static final String RPC_QUEUE_NAME = "rpc_queue";

    private static int fib(int n) {
        if (n <= 0) { return 0; }
        if (n == 1) { return 1; }
        return fib(n - 1) + fib(n - 2);
    }

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
        channel.queuePurge(RPC_QUEUE_NAME);

        channel.basicQos(1);

        System.out.println(" [x] Awaiting RPC requests");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            AMQP.BasicProperties replyProperties = new AMQP.BasicProperties()
                                                            .builder()
                                                            .correlationId(delivery.getProperties().getCorrelationId())
                                                            .build();

            String resp = "";
            try {
                String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
                int n = Integer.parseInt(msg);

                System.out.println(" [.] fib(" + msg + ")");
                resp += fib(n);
            } catch (RuntimeException e) {
                System.out.println(" [.] " + e);
            } finally {
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProperties, resp.getBytes(StandardCharsets.UTF_8));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };

        channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    }
}
