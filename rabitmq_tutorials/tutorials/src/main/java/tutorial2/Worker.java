package tutorial2;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.nio.charset.StandardCharsets;
public class Worker {

  private final static String QUEUE_NAME = "task_queue";

  private static void doWork(String task) {
    for (char ch : task.toCharArray()) {
      if (ch == '.') {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException _ignored) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    boolean durable = true; // Whether or not queue will survive RabbitMQ restart
    channel.queueDeclare(QUEUE_NAME, durable, false, false, null);

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    // How many messages can be given to a single worker at a time (i.e. in between Acks)
    int prefetchCount = 1;
    channel.basicQos(prefetchCount);

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);

      System.out.println(" [x] Received '" + msg + "'");

      try {
        doWork(msg);
      } finally {
        System.out.println(" [x] Done");
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false); // Consumer acks
      }
    };

    boolean autoAck = false; // Whether the consumer auto acks or manually acks
    channel.basicConsume(QUEUE_NAME, autoAck, deliverCallback, consumerTag -> { });
  }

}
