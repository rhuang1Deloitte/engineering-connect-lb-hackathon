package com.hackathon.lb.model;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single backend target server.
 * Tracks runtime state including health status and connection count.
 */
public class Target {
    
    private final String url;
    private final int weight;
    private final AtomicBoolean healthy;
    private final AtomicInteger activeConnections;
    private final AtomicInteger consecutiveSuccesses;
    private final AtomicInteger consecutiveFailures;
    private final AtomicLong lastHealthCheckTime;
    
    /**
     * Initializes a target with the provided URL and weight.
     */
    public Target(String url, int weight) {
        this.url = url;
        this.weight = weight;
        this.healthy = new AtomicBoolean(true); // Assume healthy initially
        this.activeConnections = new AtomicInteger(0);
        this.consecutiveSuccesses = new AtomicInteger(0);
        this.consecutiveFailures = new AtomicInteger(0);
        this.lastHealthCheckTime = new AtomicLong(0);
    }
    
    /**
     * Returns the upstream URL for this target.
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Returns the load balancing weight assigned to this target.
     */
    public int getWeight() {
        return weight;
    }
    
    /**
     * Checks whether this target is considered healthy.
     */
    public boolean isHealthy() {
        return healthy.get();
    }
    
    /**
     * Marks the target healthy and resets the failure counter.
     */
    public void markHealthy() {
        healthy.set(true);
        consecutiveSuccesses.incrementAndGet();
        consecutiveFailures.set(0);
    }
    
    /**
     * Marks the target unhealthy and increments the failure counter.
     */
    public void markUnhealthy() {
        healthy.set(false);
        consecutiveFailures.incrementAndGet();
        consecutiveSuccesses.set(0);
    }
    
    /**
     * Returns the current number of open connections to this target.
     */
    public int getActiveConnections() {
        return activeConnections.get();
    }
    
    /**
     * Increments the counter of active connections.
     */
    public void incrementConnections() {
        activeConnections.incrementAndGet();
    }
    
    /**
     * Decrements the counter of active connections.
     */
    public void decrementConnections() {
        activeConnections.decrementAndGet();
    }
    
    /**
     * Returns the number of consecutive successful health checks.
     */
    public int getConsecutiveSuccesses() {
        return consecutiveSuccesses.get();
    }
    
    /**
     * Increments the counter of consecutive successful health checks.
     */
    public void incrementSuccesses() {
        consecutiveSuccesses.incrementAndGet();
        consecutiveFailures.set(0);
    }
    
    /**
     * Returns the number of consecutive failed health checks.
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }
    
    /**
     * Increments the counter of consecutive failed health checks.
     */
    public void incrementFailures() {
        consecutiveFailures.incrementAndGet();
        consecutiveSuccesses.set(0);
    }
    
    /**
     * Returns the last timestamp (epoch millis) when health was checked.
     */
    public long getLastHealthCheckTime() {
        return lastHealthCheckTime.get();
    }
    
    /**
     * Updates the timestamp of the most recent health check.
     */
    public void setLastHealthCheckTime(long time) {
        lastHealthCheckTime.set(time);
    }
    
    /**
     * Returns a short string useful for logging the target state.
     */
    @Override
    public String toString() {
        return String.format("Target{url='%s', weight=%d, healthy=%s, activeConnections=%d}", 
            url, weight, healthy.get(), activeConnections.get());
    }
}
