import java.io.BufferedReader;
import java.util.regex.Pattern;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet(name = "SkierServlet", value = "/SkierServlet")
public class SkierServlet extends HttpServlet {
  private final Pattern validGetUrls[] = {
      // url = "/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}"
      // resortID - ID of the resort the skier is at. int32
      // seasonID - ID of the ski season. string
      // dayID - ID number of ski day in the ski season. string: min=1, max=366
      // skierID - ID of the skier riding the lift. int32
      Pattern.compile("/[-]*(\\d{1,10})/seasons/[12](\\d{3})/days"
          + "/([1-9]|1\\d{2}|2\\d{2}|(3[0-5]\\d|36[0-6]))/skiers/[-]*(\\d{1,10})"),
      // url = "/skiers/{skierID}/vertical"
      // skierID - ID of the skier to retrieve data for
      Pattern.compile("/[-]*(\\d{1,10})/vertical"),
  };
  private final Pattern validPostUrls[] = {
      // url = "/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}"
      // resortID - ID of the resort the skier is at. int32
      // seasonID - ID of the ski season. string
      // dayID - ID number of ski day in the ski season. string: min=1, max=366
      // skierID - ID of the skier riding the lift. int32
      Pattern.compile("/[-]*(\\d{1,10})/seasons/[12](\\d{3})/days"
          + "/([1-9]|1\\d{2}|2\\d{2}|(3[0-5]\\d|36[0-6]))/skiers/[-]*(\\d{1,10})"),
  };

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");
    String urlPath = request.getPathInfo();
    // null check
    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("missing GET parameters");
      return;
    }

    // Validate GET Path
    if (!isValidGetUrl(urlPath)) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("invalid GET path format");
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    String[] urlParts = urlPath.split("/");

    // TODO: Implement logic handling GET requests to the API

    // url = "/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}"
    if (validGetUrls[0].matcher(urlPath).matches()) {
      // TODO: parse urlParts and do something with it
    }
    // url = "/skiers/{skierID}/vertical"
    if (validGetUrls[1].matcher(urlPath).matches()) {
      // TODO: parse urlParts and do something with it
    }

    response.getWriter().write("It works!");
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");
    String urlPath = request.getPathInfo();
    // null check
    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("missing POST parameters");
      return;
    }
    // Validate POST path
    if (!isValidPostUrl(urlPath)) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("invalid POST path format");
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    String[] urlParts = urlPath.split("/");

    // TODO: Implement logic handling POST requests to API

    // TODO: Handle request bodies containing JSON with BufferedReader
    BufferedReader requestBody = request.getReader();
    StringBuilder jsonPayload = new StringBuilder();
    String line;
    while ( (line = requestBody.readLine()) != null ) {
      jsonPayload.append(line);
    }

    // url = "/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}"
    if (validPostUrls[0].matcher(urlPath).matches()) {
      // TODO: parse urlParts and do something with it
    }

    response.getWriter().write("It works!");
  }

  private boolean isValidGetUrl(String urlPath) {
    // Check if given urlPath matches any valid GET url patterns
    for (int i = 0; i < validGetUrls.length; i++) {
      if (validGetUrls[i].matcher(urlPath).matches()) {
        return true;
      }
    }
    return false;
  }

  private boolean isValidPostUrl(String urlPath) {
    // Check if given urlPath matches any valid POST url patterns
    for (int i = 0; i < validPostUrls.length; i++) {
      if (validPostUrls[i].matcher(urlPath).matches()) {
        return true;
      }
    }
    return false;
  }
}
