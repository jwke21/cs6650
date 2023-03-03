package consumer;

import utils.UC;

import java.util.concurrent.ConcurrentHashMap;

public class LikeDislikeTrackerConsumer extends AbstractSwipeConsumer {

    ConcurrentHashMap<Integer, LikeDislikeStats> bySwiperId;

    public LikeDislikeTrackerConsumer() {
        super(UC.RMQ_EXCHANGE_NAME, UC.RMQ_LIKE_DISLIKE_QUEUE_NAME);
        bySwiperId = new ConcurrentHashMap<>(UC.MAX_SWIPER_ID);
        // Initialize the map
        for (int i = 0; i < UC.MAX_SWIPER_ID; i++) {
            bySwiperId.put(i, new LikeDislikeStats());
        }
        System.out.println("Consumer for queue '" + UC.RMQ_LIKE_DISLIKE_QUEUE_NAME + "' successfully launched. " +
                "Press CTRL+C to shut down.");
    }

    @Override
    public void updateStats(int swiperId, int swipeeId, boolean liked) {
        // Atomically update the like/dislike count
        bySwiperId.computeIfPresent(swipeeId, (k, v) -> {
            if (liked) {
                v.numLikes++;
            } else {
                v.numDislikes++;
            }
            return v;
        });
    }

    public static void main(String[] argv) {
        LikeDislikeTrackerConsumer consumer = new LikeDislikeTrackerConsumer();
        consumer.consume();
    }

    // ------------------------------ LikeStats ------------------------------
    public static class LikeDislikeStats extends AbstractSwipeConsumer.UserSwipeStats {
        int numLikes;
        int numDislikes;

        @Override
        public String toJson() {
            return new String("{likes:" + numLikes + ",dislikes:" + numDislikes + "}");
        }
    }
}