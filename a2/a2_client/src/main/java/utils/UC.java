package utils;

public class UC {

    public static final int MAX_SWIPER_ID = 5_000;
    public static final int MAX_SWIPEE_ID = 1_000_000;
    public static final int COMMENT_LEN = 256;
    public static final int NUM_THREADS = 100;
    public static final int MAX_REQUEST_ATTEMPTS = 5;
    public static final Integer TARGET_NUM_REQUESTS = 500_000;
    public static final double MSEC_TO_SECONDS_CONV = 0.001;
//    public static final String SWIPE_SERVLET_ADDR = "http://swipe-servlets-network-lb-d09d279ec4e2f5af.elb.us-west-2.amazonaws.com/"; // Network LB Hostname
//    public static final String LB_HOSTNAME = "http://swipe-servlet-application-lb-304876359.us-west-2.elb.amazonaws.com"; // Application LB Hostname
//    public static final String SWIPE_SERVLET_ADDR = "http://52.12.59.9:80/a2_swipe_servlet/swipe/"; // Network LB IP
//    public static final String SWIPE_SERVLET_ADDR = "http://35.155.34.195/a2_swipe_servlet/swipe/"; // Application LB IP
    public static final String SWIPE_SERVLET_ADDR = "http://54.185.208.4:8080/a2_swipe_servlet/swipe/";
//    public static final String SWIPE_SERVLET_ADDR = "http://35.160.135.31:8080/a2_swipe_servlet/swipe/";
//    public static final String SWIPE_SERVLET_ADDR = "http://localhost:8080/a2_server_war_exploded/swipe/";
}
