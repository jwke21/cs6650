package tutorial6;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class RPCClient implements AutoCloseable {

    private Connection connection;
    private Channel channel;
    private String requestQueueName = "rpc_queue";

    public RPCClient() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] argv) {
        try (RPCClient fibonacciRpc = new RPCClient()) {
            for (int i = 0; i < 32; i++) {
                String i_str = Integer.toString(i);
                System.out.println(" [.] Requesting fib(" + i_str + ")");
                String resp = fibonacciRpc.call(i_str);
                System.out.println(" [.] Got '" + resp + "'");
            }
        } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    // Makes the actual RPC request
    public String call(String msg) throws IOException, InterruptedException, ExecutionException {
        // Generate unique correlationId
        final String correlationId = UUID.randomUUID().toString();

        // Create exclusive queue for reply
        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties properties = new AMQP.BasicProperties()
                                                    .builder()
                                                    .correlationId(correlationId)
                                                    .replyTo(replyQueueName)
                                                    .build();

        channel.basicPublish("", requestQueueName, properties, msg.getBytes(StandardCharsets.UTF_8));

        final CompletableFuture<String> response = new CompletableFuture<>();

        // Consumer checks if correlationId matches and completes the future if so
        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                response.complete(new String(delivery.getBody(), StandardCharsets.UTF_8));
            }
        }, consumerTag -> { });

        String res = response.get();
        channel.basicCancel(ctag);
        return res;
    }

    public void close() throws IOException {
        connection.close();
    }
}
