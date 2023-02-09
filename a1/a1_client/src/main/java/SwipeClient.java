import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.hc.client5.http.classic.HttpClient;
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
  public static final int TARGET_NUM_REQUESTS = 50000;
  public static final int NUM_THREADS = 200;
  public static CountDownLatch remainingRequests = new CountDownLatch(NUM_THREADS);
  public static String URL = "http://10.0.0.58:8080/a1_server_war_exploded/swipe/";

  public static synchronized void incCompletedRequests() { completedRequests++; }

  private void sendRequests() throws InterruptedException {
    int reqPerThread = TARGET_NUM_REQUESTS / NUM_THREADS;
    System.out.println("Requests to make: " + TARGET_NUM_REQUESTS);
    // Create threads
    for (int i = 0; i < NUM_THREADS; i++) {
      new Thread(new Requester(reqPerThread)).start();
    }
    System.out.println("Threads spawned: " + NUM_THREADS);
    System.out.println("Requests per thread: " + reqPerThread);
    remainingRequests.await();
    System.out.println("Completed requests: " + completedRequests);
  }

  public static void main(String[] args) throws InterruptedException {
    SwipeClient swipeClient = new SwipeClient();
    swipeClient.sendRequests();
  }

  // ------------------------------ Requester ------------------------------
  public class Requester implements Runnable {

    int numRequestsToSend;
    String swipe;
    int swiper;
    int swipee;
    String comment;

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
      for (int i = 0; i < numRequestsToSend; i++) {
        // Create HttpClient
        CloseableHttpClient client = HttpClients.createDefault();
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
          // Read the response body
          HttpEntity responseEntity = response.getEntity();
          // If the request was rejected, print response error info
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
          // If response was successful, do nothing
        } catch (IOException e) {
          System.err.println("Fatal transport error: " + e.getMessage());
          e.printStackTrace();
        } finally {
          incCompletedRequests();
        }
      }
      remainingRequests.countDown();
    }
  }
}
