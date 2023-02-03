import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class HelloWorldClient {

    public static final int NUM_THREADS = 100;
    public int threadCount = 0;
    private static CountDownLatch completed = new CountDownLatch(NUM_THREADS);
    // URL of HelloWorldServlet on EC2 instance
    private static String url = "http://54.188.33.195/lab3_servlet_war/hello_world";
    // URL of HelloWorldServlet on local machine
//    private static String url = "http://localhost:8080/lab3_servlet_war_exploded/hello_world";

    public HelloWorldClient() {}

    synchronized void incThreads() { threadCount++; }
    synchronized void decLatch() { completed.countDown(); }
    public HelloWorldClient.ServletClient createClient() { return new ServletClient(); }

    public static void main(String[] args) throws InterruptedException {

        HelloWorldClient clients = new HelloWorldClient();

        // Begin timer
        long start = System.currentTimeMillis();

        // Spawn the threads
        for (int i=0; i<NUM_THREADS; i++) {
            new Thread(clients.createClient()).start();
        }

        // Await all threads to finish
        completed.await();

        // End timer
        long end = System.currentTimeMillis();

        // Print total time taken to execute 100 requests
        System.out.println("Time Taken: " + (end - start) + "ms");
        System.out.println("Value should be equal to " + NUM_THREADS + " It is: " + clients.threadCount);
    }

    // ---------------------------- ServletClient ----------------------------
    public class ServletClient implements Runnable {

        public ServletClient() {}

        @Override
        public void run() {
            // Increment counter
            incThreads();

            // Create an instance of HttpClient
            HttpClient client = new HttpClient();

            // Create a GetMethod instance
            GetMethod method = new GetMethod(url);

            // Set recovery handler
            client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());

            try {
                // Exceute the method
                int statusCode = client.executeMethod(method);

                if (statusCode != HttpStatus.SC_OK) {
                    System.err.println("Method failed: " + method.getStatusLine());
                }

                // Read the response body
                byte[] responseBody = method.getResponseBody();

//                // Deal with the response
//                // (Must ensure correct character encoding and not binary data)
//                System.out.println(statusCode);
//                System.out.println(new String(responseBody));
            } catch (HttpException e) {
                System.err.println("Fatal protocol violation: " + e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("Fatal transport error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Release the connection
                method.releaseConnection();

                // Decrement the latch
                decLatch();
            }
        }
    }
}
