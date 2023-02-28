package rmq;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import utils.UC;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RmqConnectionHandler {

    private int maxSize;
    private Connection connection;
    private BlockingQueue<Channel> pool;

    public RmqConnectionHandler(int maxSize, Connection connection) {
        this.maxSize = maxSize;
        this.connection = connection;
        pool = new LinkedBlockingQueue<>(maxSize);
        for (int i = 0; i < maxSize; i++) {
            Channel newChannel;
            try {
                newChannel = connection.createChannel();
                pool.put(newChannel);
            } catch(Exception e) {
                System.out.println("Unable to create new channel number " + (i + 1) + " due to exception: " + e);
                e.printStackTrace();
            }
        }
    }

    public Channel borrowChannel() {
        try {
            return pool.take();
        } catch (Exception e) {
            System.out.println("Unable to borrow channel due to exception: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void returnChannel(Channel channel) {
        if (channel != null) {
            pool.add(channel);
        }
    }

    // Factory method for creating RmqConnectionHandlers
    public static RmqConnectionHandler createConnectionHandler(int maxChannels, String rmqHostName) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setHost(rmqHostName);
            connectionFactory.setUsername(UC.RMQ_USERNAME);
            connectionFactory.setPassword(UC.RMQ_PASSWORD);
            connectionFactory.setVirtualHost(UC.RMQ_VHOST);
            Connection connection = connectionFactory.newConnection();
            return new RmqConnectionHandler(maxChannels, connection);
        } catch (Exception e) {
            System.out.println("Unable to create new connection to RMQ host at " + rmqHostName + " due to exception: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
