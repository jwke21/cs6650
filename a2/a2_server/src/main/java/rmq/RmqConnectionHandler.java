package rmq;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RmqConnectionHandler {

    private int maxSize;
    private Connection connection;
    private BlockingQueue<Channel> pool;
    private ChannelFactory channelFactory;
    private static final int DEFAULT_MAX_SIZE = 10;

    public RmqConnectionHandler(int maxSize, Connection connection) {
        this.maxSize = maxSize;
        this.connection = connection;
        pool = new LinkedBlockingQueue<>(maxSize);
        this.channelFactory = new ChannelFactory(connection);
        for (int i = 0; i < maxSize; i++) {
            Channel newChannel;
            try {
                newChannel = channelFactory.create();
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
            Connection connection = connectionFactory.newConnection();
            return new RmqConnectionHandler(maxChannels, connection);
        } catch (Exception e) {
            System.out.println("Unable to create new connection to RMQ host at " + rmqHostName + " due to exception: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // ------------------------------ ChannelFactory ------------------------------
    public static class ChannelFactory extends BasePooledObjectFactory<Channel> {

        private Connection connection;
        private int numChannels;

        public ChannelFactory(Connection connection) {
            this.connection = connection;
        }

        @Override
        synchronized public Channel create() throws Exception {
            Channel ret = connection.createChannel();
            numChannels++;
            System.out.println("Channel created");
            System.out.println("Total channels: " + numChannels);
            return ret;
        }

        @Override
        public PooledObject<Channel> wrap(Channel channel) {
            return new DefaultPooledObject<>(channel);
        }

        public int getNumChannels() {
            return numChannels;
        }
    }
}
