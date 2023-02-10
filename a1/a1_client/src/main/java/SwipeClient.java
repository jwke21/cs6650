import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class SwipeClient {

  public static int successfulRequests = 0;
  public static final int MAX_SWIPER_ID = 5000;
  public static final int MAX_SWIPEE_ID = 1000000;
  public static final int COMMENT_LEN = 256;
  public static final int TARGET_NUM_REQUESTS = 500_000;
  public static final int NUM_THREADS = 100;
  public static CountDownLatch remainingRequests;

  public static String URL = "http://54.212.204.134/a1_server_war/swipe/";

  public static synchronized void incSuccessfulRequests() { successfulRequests++; }

  private void sendRequests() throws InterruptedException {
    int reqPerThread = TARGET_NUM_REQUESTS / NUM_THREADS;
    remainingRequests = new CountDownLatch(NUM_THREADS);

    System.out.println("Requests to make: " + TARGET_NUM_REQUESTS);
    System.out.println("Threads used: " + NUM_THREADS);
    System.out.println("Requests per thread: " + reqPerThread);

    for (int i = 0; i < NUM_THREADS; i++) {
      new Thread(new Requester(reqPerThread)).start();
    }
    remainingRequests.await();

    System.out.println("Completed requests: " + successfulRequests);
  }

  public static void main(String[] args) throws InterruptedException {
    SwipeClient swipeClient = new SwipeClient();
    swipeClient.sendRequests();
  }

  // ------------------------------ Requester ------------------------------
  public class Requester implements Runnable {

    private static final int MAX_REQUEST_ATTEMPTS = 5;
    public int numRequestsToSend;
    public String swipe;
    public int swiper;
    public int swipee;
    public String comment;

    public Requester(int count) {
      numRequestsToSend = count;
    }

    private void setRequestValues() {
      swipe = RandomUtils.nextInt(1, 3) == 1 ? "left" : "right";
      swiper = RandomUtils.nextInt(1, MAX_SWIPER_ID);
      swipee = RandomUtils.nextInt(1, MAX_SWIPEE_ID);
      comment = RandomStringUtils.random(COMMENT_LEN, true, false);
    }

    public HttpPost buildPostRequest() {
      // Set random request values
      setRequestValues();
      // Initialize a new HttpPost
      HttpPost ret = new HttpPost(URL + swipe);
      // Create json payload
      String jsonPayload = "{ swiper: " + swiper + ",swipee: " + swipee + ",comment: " + comment + " }";
      // Add json payload to PostMethod
      ret.setEntity(new StringEntity(jsonPayload));
      return ret;
    }

    @Override
    public void run() {
      // Create HttpClient
      CloseableHttpClient client = HttpClients.createDefault();
      // Send the given number of requests
      for (int i = 0; i < numRequestsToSend; i++) {
        // Create PostMethod
        HttpPost httpPost = buildPostRequest();
        // Attempt request 5 times
        for (int attempts = 0; attempts < MAX_REQUEST_ATTEMPTS; attempts++) {
          try {
            // Execute the method
            CloseableHttpResponse response = client.execute(httpPost);
            // Get response code
            int statusCode = response.getCode();
            // Close response object
            response.close();
            // If the request was successful break out of loop
            if (statusCode == HttpStatus.SC_OK) {
              incSuccessfulRequests();
              break;
            }
            // If request was unsuccessful, try again
          } catch (Exception e) {
            // If there was an exception print it and try again
            System.err.println("POST request error");
            System.out.println(e);
          }
        }
      }
      // Close HttpClient
      try {
        client.close();
      } catch (Exception e) {
        System.err.println("Client close error");
        System.out.println(e);
      }
      // Count down latch at thread termination
      remainingRequests.countDown();
    }
  }
}
