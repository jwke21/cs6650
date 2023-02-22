package tutorial2;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.nio.charset.StandardCharsets;

public class NewTask {

  private final static String QUEUE_NAME = "task_queue";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
          Channel channel = connection.createChannel()) {

      boolean durable = true;
      channel.queueDeclare(QUEUE_NAME, durable, false, false, null);

      String msg = String.join(" ", argv);

      channel.basicPublish("", QUEUE_NAME,
                            MessageProperties.PERSISTENT_TEXT_PLAIN, // Marks messages as persistent
                            msg.getBytes(StandardCharsets.UTF_8));
      System.out.println(" [x] Sent '" + msg + "'");
    }
  }
}
