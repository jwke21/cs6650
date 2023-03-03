package utils;

public class UC {

    public static final int MAX_SWIPER_ID = 5_000;
    public static final int MAX_SWIPEE_ID = 1_000_000;
    public static final int COMMENT_LEN = 256;
    public static final int NUM_THREADS = 200;
    public static final int MAX_REQUEST_ATTEMPTS = 5;
    public static final Integer TARGET_NUM_REQUESTS = 500_000;
    public static final double MSEC_TO_SECONDS_CONV = 0.001;
    public static String SWIPE_SERVLET_ADDR = "http://swipe-servlet-application-lb-304876359.us-west-2.elb.amazonaws.com/a2_swipe_servlet/swipe/";
//    public static String SWIPE_SERVLET_ADDR = "http://54.185.208.4/a2_swipe_servlet/swipe/";
//    public static String SWIPE_SERVLET_ADDR = "http://localhost:8080/a2_server_war_exploded/swipe/";
}
