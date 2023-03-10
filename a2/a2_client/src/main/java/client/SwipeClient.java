package client;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import utils.UC;
import java.util.concurrent.CountDownLatch;

public class SwipeClient {

  public int successfulRequests = 0;
  public int unsuccessfulRequests = 0;
  public CountDownLatch remainingThreads;

  public synchronized void incSuccessfulRequests() { successfulRequests++; }
  public synchronized void incUnsuccessfulRequests() { unsuccessfulRequests++; }

  private void sendAllRequests() throws InterruptedException {
    successfulRequests = 0;
    unsuccessfulRequests = 0;
    int reqPerThread = UC.TARGET_NUM_REQUESTS / UC.NUM_THREADS;
    remainingThreads = new CountDownLatch(UC.NUM_THREADS);

    System.out.println("Running multithreaded test...");
    System.out.println("Requests to make: " + UC.TARGET_NUM_REQUESTS);
    System.out.println("Threads used: " + UC.NUM_THREADS);
    System.out.println("Requests per thread: " + reqPerThread);

    // Start time
    long start = System.currentTimeMillis();
    // Spawn all threads
    for (int i = 0; i < UC.NUM_THREADS; i++) {
      new Thread(new Requester(reqPerThread)).start();
    }
    // Wait for all threads to terminate
    remainingThreads.await();

    // End time
    long end = System.currentTimeMillis();
    // Wall time (total time taken in seconds)
    double wall = (end - start) * UC.MSEC_TO_SECONDS_CONV;
    // Throughput
    double throughput = UC.TARGET_NUM_REQUESTS.doubleValue() / wall;

    System.out.println("Successful requests: " + successfulRequests);
    System.out.println("Unsuccessful requests: " + unsuccessfulRequests);
    System.out.println("Wall time for " + UC.TARGET_NUM_REQUESTS + " requests: " + wall + " sec");
    System.out.println("Throughput: " + throughput + " req/sec");
  }

  public static void main(String[] args) {
    try {
      SwipeClient swipeClient = new SwipeClient();
      // Run multithreaded test
      swipeClient.sendAllRequests();
    } catch (Exception e) {
      System.err.println("error: " + e);
      e.printStackTrace();
    }
  }

  // ------------------------------ Requester ------------------------------
  public class Requester implements Runnable {

    public int numRequestsToSend;
    public String swipe;
    public int swiper;
    public int swipee;
    public String comment;

    public Requester(int count) { numRequestsToSend = count; }

    private void setRequestValues() {
      swipe = RandomUtils.nextInt(1, 3) == 1 ? "left" : "right";
      swiper = RandomUtils.nextInt(1, UC.MAX_SWIPER_ID);
      swipee = RandomUtils.nextInt(1, UC.MAX_SWIPEE_ID);
      comment = RandomStringUtils.random(UC.COMMENT_LEN, true, false);
    }

    public HttpPost buildPostRequest() {
      // Set random request values
      setRequestValues();
      // Initialize a new HttpPost
      HttpPost ret = new HttpPost(UC.SWIPE_SERVLET_ADDR + swipe);
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
      // Track whether each request was successful
      boolean reqSuccess;
      // Send the given number of requests
      for (int i = 0; i < numRequestsToSend; i++) {
        // Create PostMethod
        HttpPost httpPost = buildPostRequest();
        // Attempt request 5 times
        for (int attempts = 0; attempts < UC.MAX_REQUEST_ATTEMPTS; attempts++) {
          try {
            // Execute the method
            reqSuccess = client.execute(httpPost, response -> {
              // Return if the response was a success
              return response.getCode() == HttpStatus.SC_OK;
            });
            // If request was successful break out of loop
            if (reqSuccess) {
              incSuccessfulRequests();
              break;
            }
            // If request was unsuccessful, try again
          } catch (Exception e) {
            incUnsuccessfulRequests();
            // If there was an exception
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
      remainingThreads.countDown();
    }
  }

}
