package consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import rmq.RmqConnectionHandler;
import utils.UC;
import java.nio.charset.StandardCharsets;

public abstract class AbstractSwipeConsumer {

    private String exchangeName;
    private String queueName;
    private RmqConnectionHandler connectionHandler;
    private Thread[] threadPool;
    private Gson gson;

    public AbstractSwipeConsumer(String exchangeName, String queueName) {
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.connectionHandler = RmqConnectionHandler.createConnectionHandler(UC.NUM_THREADS, UC.RMQ_HOST_ADDRESS);
        connectionHandler.declareQueue(queueName, true);
        connectionHandler.bindQueue(queueName, exchangeName, "");
        this.gson = new Gson();
        // Initialize the thread pool
        this.threadPool = new Thread[UC.NUM_THREADS];
        for (int i = 0; i < UC.NUM_THREADS; i++) {
            threadPool[i] = new Thread(new Consumer());
        }
    }

    public void consume() {
        int numThreads = threadPool.length;
        for (int i = 0; i < numThreads; i++) {
            threadPool[i].start();
        }
    }

    public abstract void updateStats(int swiperId, int swipeeId, boolean liked);


    // ------------------------------ Consumer ------------------------------
    public class Consumer implements Runnable {

        @Override
        public void run() {
            // Borrow a channel from the exchange
            Channel channel = connectionHandler.borrowChannel();
            try {
                // Set this channel to receive only one message between ack
                int prefetchCount = 1;
                channel.basicQos(prefetchCount);

                // Callback lambda used when message delivered
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    // Get the msg json body
                    String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    SwipeMessageJson json = gson.fromJson(msg, SwipeMessageJson.class);

                    // Update the stats
                    updateStats(json.swiper, json.swipee, json.like);
                };

                // Consume any messages that arrive and manually ack them
                boolean autoAck = true;
                channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> { });
            } catch (Exception e) {
                System.err.println("Consumer thread error");
                e.printStackTrace();
            } finally {
                connectionHandler.returnChannel(channel);
            }
        }
    }

    // ------------------------------ SwipeMessageJson ------------------------------
    public static class SwipeMessageJson {
        public int swiper;
        public int swipee;
        public boolean like;
    }

    // ------------------------------ UserSwipeStats ------------------------------
    public static abstract class UserSwipeStats {
        public abstract String toJson();
    }
}
