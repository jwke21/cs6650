package consumer;

import utils.UC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class LikedUsersTrackerConsumer extends AbstractSwipeConsumer {

    ConcurrentHashMap<Integer, LikedUsersStats> bySwiperId;

    public LikedUsersTrackerConsumer() {
        super(UC.RMQ_EXCHANGE_NAME, UC.RMQ_LIKED_USERS_QUEUE_NAME);
        bySwiperId = new ConcurrentHashMap<>(UC.MAX_SWIPER_ID);
        // Initialize the map
        for (int i = 0; i < UC.MAX_SWIPER_ID; i++) {
            bySwiperId.put(i, new LikedUsersStats());
        }
        System.out.println("Consumer for queue '" + UC.RMQ_LIKED_USERS_QUEUE_NAME + "' successfully launched. " +
                "Press CTRL+C to shut down.");
    }

    @Override
    public void updateStats(int swiperId, int swipeeId, boolean liked) {
        // Atomically update the list of liked users
        bySwiperId.computeIfPresent(swiperId, (k, v) -> {
            v.likedUsers.add(swipeeId);
            return v;
        });
    }

    public static void main(String[] argv) {
        LikedUsersTrackerConsumer consumer = new LikedUsersTrackerConsumer();
        consumer.consume();
    }

    // ------------------------------ LikedUsers ------------------------------
    public static class LikedUsersStats extends AbstractSwipeConsumer.UserSwipeStats {
        ArrayList<Integer> likedUsers;
        int total;

        public LikedUsersStats() {
            likedUsers = new ArrayList<>(100);
        }

        @Override
        public String toJson() {
            // "Given a user id, return a list of 100 users maximum"
            List<Integer> ret = likedUsers.size() <= 100 ? likedUsers : likedUsers.subList(0, 100);
            return new String("{likedUsers" + ret + "}");
        }
    }
}
