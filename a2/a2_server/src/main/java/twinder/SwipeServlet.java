package twinder;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
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
    private static final Pattern validPostUrls[] = {
            // url = "/swipe/{leftorright}/"
            // leftorright - Like or dislike user. String
            Pattern.compile("/(left|right)"),
    };

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
            return;
        }
        try {
            String requestBody = readRequestBody(request);
            // Gson instance that will handle json serialization and de-serialization
            // Ref: https://github.com/google/gson/blob/master/UserGuide.md
            Gson gson = new Gson();
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
            boolean durable = true; // Persist messages to disk
            channel.queueDeclare(UC.SWIPE_QUEUE_NAME, durable, false, false, null);
            // Publish the JSON to the queue
            channel.basicPublish("", UC.SWIPE_QUEUE_NAME,
                                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                                    requestBody.getBytes(StandardCharsets.UTF_8));
            // Return the channel to the channel pool
            connectionHandler.returnChannel(channel);
            // Send response to client
            response.setStatus(HttpServletResponse.SC_OK); // HTTP 200
            response.getWriter().write("Write successful");
        } catch (Exception e) {
            // There was an issue parsing the JSON payload
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
        return json.swiper != null && json.swipee != null && json.comment != null && // null fields
                !json.swiper.isEmpty() && !json.swipee.isEmpty() && !json.comment.isEmpty() && // Empty fields
                json.swiper.length() <= UC.MAX_SWIPER_ID && json.swipee.length() <= UC.MAX_SWIPEE_ID; // Field lengths
    }

    // ------------------------------ PostRequestJson ------------------------------
    private static class PostRequestJson {
        public String swiper;
        public String swipee;
        public String comment;
    }
}
