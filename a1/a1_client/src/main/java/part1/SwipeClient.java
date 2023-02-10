package part1;

import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import utils.UC;

public class SwipeClient {

  public static int successfulRequests = 0;
  public static int unsuccessfulRequests = 0;
  public static CountDownLatch remainingRequests;

  public static synchronized void incSuccessfulRequests() { successfulRequests++; }
  public static synchronized void incUnSuccessfulRequests() { unsuccessfulRequests++; }

  private void sendAllRequests() throws InterruptedException {
    successfulRequests = 0;
    unsuccessfulRequests = 0;
    int reqPerThread = UC.TARGET_NUM_REQUESTS / UC.NUM_THREADS;
    remainingRequests = new CountDownLatch(UC.NUM_THREADS);

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
    remainingRequests.await();

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

  private void singleThreadedTest() throws InterruptedException {
    System.out.println("Running single threaded test...");
    int requestsToSend = 10_000;
    remainingRequests = new CountDownLatch(1);
    long start = System.currentTimeMillis();
    new Thread(new Requester(requestsToSend)).start();
    remainingRequests.await();
    long end = System.currentTimeMillis();
    System.out.println("Calculating Little's law statistics...");
    // Wall time (total time taken in seconds)
    double wall = (end - start) * UC.MSEC_TO_SECONDS_CONV;
    System.out.println("Single threaded wall time for 10,000 requests: " + wall + " sec");
    // Throughput = requests / wall time
    double w = wall / requestsToSend;
    System.out.println("Avg. Response Time (W): " + w + " sec");
    // λ = N / W
    System.out.println("Est. Throughput (λ) for 50 threads (N=50): " + (50 / w) + " req/sec");
    System.out.println("Est. Throughput (λ) for 100 threads (N=100): " + (100 / w) + " req/sec");
    System.out.println("Est. Throughput (λ) for 150 threads (N=150): " + (150 / w) + " req/sec");
    System.out.println("Est. Throughput (λ) for 200 threads (N=200): " + (200 / w) + " req/sec");
  }

  public static void main(String[] args) {
    System.out.println("------------------------------ Client Part 1 ------------------------------");
    try {
      SwipeClient swipeClient = new SwipeClient();
      // Run single threaded test
      swipeClient.singleThreadedTest();
      // Run multithreaded test
      swipeClient.sendAllRequests();
    } catch (Exception e) {
      System.err.println("error: " + e);
      e.printStackTrace();
    }
  }

  // ------------------------------ Requester ------------------------------
  public static class Requester implements Runnable {

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
      HttpPost ret = new HttpPost(UC.URL + swipe);
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
        for (int attempts = 0; attempts < UC.MAX_REQUEST_ATTEMPTS; attempts++) {
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
            incUnSuccessfulRequests();
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
