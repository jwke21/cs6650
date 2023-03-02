package twinder;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import rmq.RmqConnectionHandler;
import utils.UC;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@WebServlet(name = "SwipeServlet", value = "/swipe")
public class SwipeServlet extends HttpServlet {

    private RmqConnectionHandler connectionHandler;
    private static Gson gson = new Gson();

    private static final Pattern validPostUrls[] = {
            // url = "/swipe/{leftorright}/"
            // leftorright - Like or dislike user. String
            Pattern.compile("/(left|right)"),
    };
    // Gson instance that will handle json serialization and de-serialization
    // Ref: https://github.com/google/gson/blob/master/UserGuide.md

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // Establish connection with RabbitMQ server
        connectionHandler = RmqConnectionHandler.createConnectionHandler(UC.NUM_CHANNELS, UC.RMQ_HOST_NAME);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();
        // Validate path
        if (!isValidPostUrl(urlPath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND); // HTTP 404
            response.getWriter().write("Invalid POST path");
            System.out.println("Invalid post request");
            return;
        }
        try {
            String requestBody = readRequestBody(request);
            // Parse request's JSON into a PostRequestJson Object
            PostRequestJson jsonPayload = gson.fromJson(requestBody, SwipeServlet.PostRequestJson.class);
            // Validate post request JSON
            if (!isValidPostRequestJson(jsonPayload)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // HTTP 400
                response.getWriter().write("Invalid POST request JSON");
                return;
            }
            // Borrow a channel from the channel pool
            Channel channel = connectionHandler.borrowChannel();
            // Declare the non-durable fanout exchange
            boolean durableExchange = false;
            channel.exchangeDeclare(UC.RMQ_SWIPE_EXCHANGE_NAME, UC.RMQ_SWIPE_EXCHANGE_TYPE, durableExchange);

            // Build the message to be sent to the RMQ broker
            boolean liked = false;
            if (urlPath.equals("/right")) {
                liked = true;
            }
            String msg = "{swiper:" + jsonPayload.swiper + ",swipee:" + jsonPayload.swipee + ",like:" + liked + "}";
            // Publish the JSON message to the fanout exchange
            channel.basicPublish(UC.RMQ_SWIPE_EXCHANGE_NAME,
                                 "", // routingKey
                                 null, // Message properties
                                 msg.getBytes(StandardCharsets.UTF_8));
            // Return the channel to the channel pool
            connectionHandler.returnChannel(channel);
            // Send response to client
            response.setStatus(HttpServletResponse.SC_OK); // HTTP 200
            response.getWriter().write("Write successful");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // HTTP 400
            response.getWriter().write("Issue handling JSON payload");
            e.printStackTrace();
        }
    }

    private boolean isValidPostUrl(String urlPath) {
        // null check
        if (urlPath == null || urlPath.isEmpty()) {
            return false;
        }
        // Check if given urlPath matches any valid POST url patterns
        for (int i=0; i < validPostUrls.length; i++) {
            if (validPostUrls[i].matcher(urlPath).matches()) return true;
        }
        return false;
    }

    private String readRequestBody(HttpServletRequest request) throws IOException {
        // Read JSON payload into BufferedReader
        BufferedReader requestBody = request.getReader();
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = requestBody.readLine()) != null) {
            body.append(line);
        }
        return body.toString();
    }

    private boolean isValidPostRequestJson(PostRequestJson json) {
        return json.swiper >= 1 && json.swiper <= UC.MAX_SWIPER_ID && // "swiper - between 1 and 5000"
                json.swipee >= 1 && json.swipee <= UC.MAX_SWIPEE_ID && // "swipee - between 1 and 1,000,000"
                json.comment != null && json.comment.length() == UC.COMMENT_LENGTH; // "comment - random string of 256 characters"
    }

    // ------------------------------ PostRequestJson ------------------------------
    private static class PostRequestJson {
        public int swiper;
        public int swipee;
        public String comment;
    }

    // ------------------------------ SwipeMessageJson ------------------------------
    public static class SwipeMessageJson {
        public int swiper;
        public int swipee;
        public boolean like;
    }
}
