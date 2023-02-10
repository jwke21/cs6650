import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class SwipeClient {

  public static int completedRequests = 0;
  public static final int MAX_SWIPER_ID = 5000;
  public static final int MAX_SWIPEE_ID = 1000000;
  public static final int COMMENT_LEN = 256;
  public static final int TARGET_NUM_REQUESTS = 500_000;
//  public static final int REQUEST_BATCH_SIZE = 50_000;
  public static final int NUM_THREADS = 100;
  public static CountDownLatch remainingRequests;

  public static String URL = "http://10.0.0.58:8080/a1_server_war_exploded/swipe/";

  public static synchronized void incCompletedRequests() { completedRequests++; }

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

//    // Send requests in batches of 50000 to prevent outbound throttling
//    int numBatches = 0;
//    while (completedRequests < TARGET_NUM_REQUESTS) {
//      numBatches++;
//      int batchSize =
//          TARGET_NUM_REQUESTS - completedRequests > REQUEST_BATCH_SIZE ?
//              REQUEST_BATCH_SIZE : TARGET_NUM_REQUESTS;
//      // Initialize latch for batch
//      remainingRequests = new CountDownLatch(NUM_THREADS);
//      // Create threads
//      int reqPerThread = batchSize / NUM_THREADS;
//      for (int i = 0; i < NUM_THREADS; i++) {
//        new Thread(new Requester(reqPerThread)).start();
//      }
//      System.out.println("Request batch size: " + batchSize);
//      System.out.println("Requests per thread: " + reqPerThread);
//      remainingRequests.await();
//      System.out.println("Completed batch " + numBatches);
//    }
    System.out.println("Completed requests: " + completedRequests);
  }

  public static void main(String[] args) throws InterruptedException {
    SwipeClient swipeClient = new SwipeClient();
    swipeClient.sendRequests();
  }

  // ------------------------------ Requester ------------------------------
  public class Requester implements Runnable {

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

    @Override
    public void run() {
      // Create HttpClient
      CloseableHttpClient client = HttpClients.createDefault();
      for (int i = 0; i < numRequestsToSend; i++) {
        // Set random request values
        setRequestValues();
        // Create PostMethod
        HttpPost httpPost = new HttpPost(URL + swipe);
        // Create json payload
        String jsonPayload = "{ swiper: " + swiper + ",swipee: " + swipee + ",comment: " + comment + " }";
        // Add json payload to PostMethod
        httpPost.setEntity(new StringEntity(jsonPayload));
        try {
          // Execute the method
          CloseableHttpResponse response = client.execute(httpPost);
          int statusCode = response.getCode();
          // Get the response body
          HttpEntity responseEntity = response.getEntity();
          // If the request was rejected, print response error info by reading response body
          if (responseEntity != null && statusCode != HttpStatus.SC_OK) {
            BufferedReader bufReader =
                new BufferedReader(new InputStreamReader(responseEntity.getContent()));
            String line;
            StringBuilder responseBodyBuilder = new StringBuilder();
            while ((line = bufReader.readLine()) != null) {
              responseBodyBuilder.append(line);
            }
            String responseBody = responseBodyBuilder.toString();
            System.err.println("Method failed: " + responseBody);
          }
          // Close response
          response.close();
        } catch (Exception e) {
          System.err.println("POST request error");
          System.out.println(e);
        } finally {
          incCompletedRequests();
        }
      }
      // Close client
      try {
        client.close();
      } catch (Exception e) {}
      remainingRequests.countDown();
    }
  }
}
