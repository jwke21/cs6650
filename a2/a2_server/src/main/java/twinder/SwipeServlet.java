package twinder;

import com.google.gson.Gson;
import rmq.RmqConnectionHandler;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

@WebServlet(name = "SwipeServlet", value = "/swipe")
public class SwipeServlet extends HttpServlet {

    private static final Pattern validPostUrls[] = {
            // url = "/swipe/{leftorright}/"
            // leftorright - Like or dislike user. String
            Pattern.compile("/(left|right)"),
    };
    private static final int MAX_SWIPER_ID = 5000;
    private static final int MAX_SWIPEE_ID = 1000000;
    private static RmqConnectionHandler connectionHandler;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // Establish connection with RabbitMQ server
        connectionHandler = RmqConnectionHandler.createConnectionHandler(10,"localhost");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();
        // null check
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND); // HTTP 404
            response.getWriter().write("Missing POST parameters");
            return;
        }
        // Validate POST path
        if (!isValidPostUrl(urlPath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND); // HTTP 404
            response.getWriter().write("Invalid POST path format");
            return;
        }
        try {
            // Read JSON payload into BufferedReader
            BufferedReader requestBody = request.getReader();
            StringBuilder jsonBuffer = new StringBuilder();
            String line;
            while ((line = requestBody.readLine()) != null) {
                jsonBuffer.append(line);
            }
            // Gson instance that will handle json serialization and de-serialization
            // Ref: https://github.com/google/gson/blob/master/UserGuide.md
            Gson gson = new Gson();
            // Parse JSON into a PostRequestJson Object
            PostRequestJson jsonPayload = gson.fromJson(jsonBuffer.toString(), SwipeServlet.PostRequestJson.class);
            // Check for null json fields
            if (jsonPayload.swiper == null || jsonPayload.swipee == null || jsonPayload.comment == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // HTTP 400
                response.getWriter().write("Missing POST request JSON field");
                return;
            }
            // Check for invalid json fields
            if (jsonPayload.swiper.isEmpty() || jsonPayload.swipee.isEmpty() ||
                    jsonPayload.swiper.length() > MAX_SWIPER_ID ||
                    jsonPayload.swipee.length() > MAX_SWIPEE_ID) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // HTTP 400
                response.getWriter().write("Invalid POST request JSON field");
                return;
            }
            // TODO: Format Swipe data and send it to remote queue

            // Send response to client
            response.setStatus(HttpServletResponse.SC_OK); // HTTP 200
            response.getWriter().write("Write successful");
        } catch (Exception e) {
            // There was an issue parsing the JSON payload
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // HTTP 400
            response.getWriter().write("Invalid JSON payload");
            e.printStackTrace();
        }
    }

    private boolean isValidPostUrl(String urlPath) {
        // Check if given urlPath matches any valid POST url patterns
        for (int i=0; i < validPostUrls.length; i++) {
            if (validPostUrls[i].matcher(urlPath).matches()) return true;
        }
        return false;
    }

    // ------------------------------ PostResponse ------------------------------
    private static class PostRequestJson {
        private String swiper;
        private String swipee;
        private String comment;
    }

}
