package com.fleetops.simulator;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitLeaderLease {
    private static final String QUEUE = "fleetops.simulator.leader";
    private final String addresses;
    private final String username;
    private final String password;
    private Connection connection;
    private Channel channel;

    public RabbitLeaderLease(@Value("${spring.rabbitmq.addresses}") String addresses,
                             @Value("${spring.rabbitmq.username}") String username,
                             @Value("${spring.rabbitmq.password}") String password) {
        this.addresses = addresses;
        this.username = username;
        this.password = password;
    }

    public synchronized boolean acquire() {
        if (isLeader()) return true;
        release();
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setConnectionTimeout(3000);
            factory.setRequestedHeartbeat(10);
            factory.setAutomaticRecoveryEnabled(false);
            connection = factory.newConnection(Arrays.asList(Address.parseAddresses(addresses)));
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE, false, true, true, null);
            return true;
        } catch (Exception exception) {
            release();
            return false;
        }
    }

    public synchronized boolean isLeader() {
        if (connection == null || channel == null || !connection.isOpen() || !channel.isOpen()) {
            return false;
        }
        try {
            channel.queueDeclarePassive(QUEUE);
            return true;
        } catch (IOException | RuntimeException exception) {
            release();
            return false;
        }
    }

    @PreDestroy
    public synchronized void release() {
        try { if (channel != null && channel.isOpen()) channel.close(); }
        catch (Exception ignored) { }
        try { if (connection != null && connection.isOpen()) connection.close(); }
        catch (Exception ignored) { }
        channel = null;
        connection = null;
    }
}
