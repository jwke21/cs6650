package consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import rmq.RmqConnectionHandler;
import utils.UC;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SwipeConsumer {

    private String exchangeName;
    private String queueName;
    private RmqConnectionHandler connectionHandler;
    private Thread[] threadPool;
    private static Gson gson;

    public SwipeConsumer(String exchangeName, String queueName) {
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.connectionHandler = RmqConnectionHandler.createConnectionHandler(UC.NUM_THREADS, UC.RMQ_HOST_NAME);
        this.threadPool = new Thread[UC.NUM_THREADS];
        this.gson = new Gson();
    }

    public void consume() {
        int numThreads = threadPool.length;
        for (int i = 0; i < numThreads; i++) {
            threadPool[i].start();
        }
    }

    public abstract void updateStats(int swiperId, int swipeeId, boolean liked);


    // ------------------------------ LikeDislikeTrackerConsumer ------------------------------
    public static class LikeDislikeTrackerConsumer extends SwipeConsumer {

        ConcurrentHashMap<Integer, LikeDislikeStats> bySwiperId;

        public LikeDislikeTrackerConsumer() {
            super(UC.RMQ_EXCHANGE_NAME, UC.RMQ_LIKE_DISLIKE_QUEUE_NAME);
            bySwiperId = new ConcurrentHashMap<>(UC.MAX_SWIPER_ID);
            // Initialize the map
            for (int i = 0; i < UC.MAX_SWIPER_ID; i++) {
                bySwiperId.put(i, new LikeDislikeStats());
            }
        }

        public static void main(String[] argv) {
            LikeDislikeTrackerConsumer consumer = new LikeDislikeTrackerConsumer();
            consumer.consume();
        }

        @Override
        public void updateStats(int swiperId, int swipeeId, boolean liked) {
            bySwiperId.computeIfPresent(swipeeId, (k, v) -> {
                if (liked) {
                    v.numLikes++;
                } else {
                    v.numDislikes++;
                }
                return v;
            });
        }

        // ------------------------------ LikeStats ------------------------------
        public static class LikeDislikeStats {
            int numLikes;
            int numDislikes;
        }
    }

    // ------------------------------ LikedUsersTrackerConsumer ------------------------------
    public static class LikedUsersTrackerConsumer extends SwipeConsumer {

        ConcurrentHashMap<Integer, LikedUsersStats> bySwiperId;

        public LikedUsersTrackerConsumer() {
            super(UC.RMQ_EXCHANGE_NAME, UC.RMQ_POTENTIAL_MATCHES_QUEUE_NAME);
            bySwiperId = new ConcurrentHashMap<>(UC.MAX_SWIPER_ID);
            // Initialize the map
            for (int i = 0; i < UC.MAX_SWIPER_ID; i++) {
                bySwiperId.put(i, new LikedUsersStats());
            }
        }

        public static void main(String[] argv) {
            LikedUsersTrackerConsumer consumer = new LikedUsersTrackerConsumer();
            consumer.consume();
        }

        @Override
        public void updateStats(int swiperId, int swipeeId, boolean liked) {
            bySwiperId.computeIfPresent(swiperId, (k, v) -> {
                if (v.total < 100) {
                    v.likedUsers[v.total] = swipeeId;
                }
                v.total++;
                return v;
            });
        }

        // ------------------------------ LikedUsers ------------------------------
        public static class LikedUsersStats {
            int[] likedUsers;
            int total;

            public LikedUsersStats() {
                // "Given a user id, return a list of 100 users maximum"
                likedUsers = new int[100];
            }
        }
    }

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

                // Declare a non-durable queue
                boolean durableQueue = false;
                channel.queueDeclare(queueName, durableQueue, false, false, null);
                // Declare a non-durable exchange
                boolean durableExchange = false;
                channel.exchangeDeclare(UC.RMQ_EXCHANGE_NAME, UC.RMQ_EXCHANGE_TYPE, durableExchange);
                // Bind the queue to the exchange (without a routing key)
                channel.queueBind(queueName, exchangeName, "");

                // Callback lambda used when message delivered
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    // Get the msg json body
                    String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    SwipeMessageJson json = gson.fromJson(msg, SwipeMessageJson.class);

                    // Update the stats
                    updateStats(json.swiper, json.swipee, json.like);
                };

                // Consume any messages that arrive and manually ack them
                boolean autoAck = false;
                channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> { });
            } catch (Exception e) {
                System.err.println("Consumer thread error");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    // ------------------------------ SwipeMessageJson ------------------------------
    public static class SwipeMessageJson {
        public int swiper;
        public int swipee;
        public boolean like;
    }
}
