package part2;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
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
  public static Record record;
  public static long startTime;

  public static synchronized void incSuccessfulRequests() { successfulRequests++; }
  public static synchronized void incUnSuccessfulRequests() { unsuccessfulRequests++; }

  public static synchronized void updateRecord(RequestStats stats) {
    record.addRequestStatsToRecord(stats);
  }

  private void sendRequests() throws InterruptedException, IOException {
    int reqPerThread = UC.TARGET_NUM_REQUESTS / UC.NUM_THREADS;
    remainingRequests = new CountDownLatch(UC.NUM_THREADS);
    // Record for tracking request latencies
    record = new Record("request_record.csv");

    System.out.println("Running multithreaded test...");
    System.out.println("Requests to make: " + UC.TARGET_NUM_REQUESTS);
    System.out.println("Threads used: " + UC.NUM_THREADS);
    System.out.println("Requests per thread: " + reqPerThread);

    // Start time
    startTime = System.currentTimeMillis();
    // Spawn all threads
    for (int i = 0; i < UC.NUM_THREADS; i++) {
      new Thread(new SwipeClient.Requester(reqPerThread)).start();
    }
    // Wait for all threads to terminate
    remainingRequests.await();

    // Save the record (i.e. close the open CSVWriter)
    record.save();

    // End time
    long end = System.currentTimeMillis();
    // Wall time (total time taken in seconds)
    double wall = (end - startTime) * UC.MSEC_TO_SECONDS_CONV;
    // Throughput
    double throughput = UC.TARGET_NUM_REQUESTS.doubleValue() / wall;

    System.out.println("Successful requests: " + successfulRequests);
    System.out.println("Unsuccessful requests: " + unsuccessfulRequests);
    System.out.println("Wall time for " + UC.TARGET_NUM_REQUESTS + " requests: " + wall + " sec");
    System.out.println("Throughput: " + throughput + " req/sec");
  }

  public static void main(String[] args) {
    System.out.println("------------------------------ Client Part 2 ------------------------------");
    try {
      SwipeClient swipeClient = new SwipeClient();
      // Run multithreaded test
      swipeClient.sendRequests();
      // Calculate the overall statistics
      record.calculateRecordStatistics();
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
            // Get start time
            long start = System.currentTimeMillis();
            // Execute the method
            CloseableHttpResponse response = client.execute(httpPost);
            // Get end time
            long end = System.currentTimeMillis();
            // Get response code
            int statusCode = response.getCode();
            // Update record with request stats
            updateRecord(new RequestStats(start, end, statusCode));
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

  // ------------------------------ Record ------------------------------
  public static class Record {

    public static final int BUF_SIZE = 5000;
    public static final String[] headers =
        {"start_time (ms)", "request_type (str)", "latency (ms)", "response_code (int)"};
    // Name of record, used as file name when written to disk
    public String name;
    // Buffer for tracking the latencies of each request
    public RequestStats[] requestsBuffer = new RequestStats[BUF_SIZE];
    // Number of RequestStats in buffer
    public int count = 0;
    // Total number of RequestStats in record
    public int numRows = 0;
    private CSVWriter csvWriter;
    private DescriptiveStatistics latencyStatistics;

    public Record(String name) {
      this.name = name;
      // Initialize the csv and write the column headers
      try {
        csvWriter = new CSVWriter(new FileWriter(name));
        csvWriter.writeNext(headers);
      } catch (IOException e) {
        System.err.println("Failed to initialize CSVWriter: " + e);
        throw new RuntimeException(e);
      }
    }

    public void save() throws IOException {
      // Write remaining data in buffer to disk
      writeRecordToDisk();
      csvWriter.close();
      System.out.println("Saved " + name);
    }

    public void addRequestStatsToRecord(RequestStats requestStats) {
      // If buffer is full write it out to disk
      if (count == BUF_SIZE) {
        writeRecordToDisk();
        count = 0;
      }
      requestsBuffer[count] = requestStats;
      count++;
      numRows++;
    }

    public void writeRecordToDisk() {
      // Add the new lines to the csv
      for (int i = 0; i < count; i++) {
        csvWriter.writeNext(requestsBuffer[i].toRow());
      }
    }

    public void calculateRecordStatistics() {
      try {
        CSVReader csvReader = new CSVReader(new FileReader(name));
        // Read the header
        csvReader.readNext();
        // Read the lines one by one
        String[] line;
        double sumLatency = 0L;
        double[] latencyList = new double[numRows];
        for (int i = 0; i < numRows; i++) {
          line = csvReader.readNext();
          // Read the request latency and add it to the running total
          double latency = Double.valueOf(line[2]); // Latency is 3rd column in csv
          sumLatency += latency;
          latencyList[i] = latency;
        }
        // Close the file stream
        csvReader.close();
        // Init statistics for latency
        latencyStatistics = new DescriptiveStatistics(latencyList);
        // Get the mean latency
        double meanRespTime = latencyStatistics.getMean();
        // Get the median (50th percentile)
        double medianRespTime = latencyStatistics.getPercentile(50);
        // Get 99th percentile
        double nineNineP = latencyStatistics.getPercentile(99);

        System.out.println("Mean request latency: " + meanRespTime + " ms");
        System.out.println("Median request latency: " + medianRespTime + " ms");
        System.out.println("99th percentile request latency: " + nineNineP + " ms");
      } catch (Exception e) {
        System.err.println("Read from disk error: " + e);
        throw new RuntimeException(e);
      }
    }
  }

  // ------------------------------ RequestStats ------------------------------
  public static class RequestStats {

    public long startTime;
    public String requestType = "POST";
    public long latency;
    public int respCode;

    public RequestStats(long start, long end, int respCode) {
      // Start time from system init
      startTime = start - SwipeClient.startTime;
      latency = end - start;
      this.respCode = respCode;
    }

    public String[] toRow() {
      String[] ret = {
          String.valueOf(startTime),
          requestType,
          String.valueOf(latency),
          String.valueOf(respCode)
      };
      return ret;
    }
  }
}
