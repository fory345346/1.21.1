package com.example.webcontrol.performance;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring system for WebControl mod
 * Tracks FPS impact, memory usage, and operation timing
 */
public class PerformanceMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-performance");
    private static final ConcurrentHashMap<String, AtomicLong> operationTimes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    
    private static long lastMemoryCheck = 0;
    private static long lastFpsCheck = 0;
    private static final long CHECK_INTERVAL = 5000; // 5 seconds
    
    private static boolean monitoringEnabled = false;
    
    public static void enable() {
        monitoringEnabled = true;
        LOGGER.info("Performance monitoring enabled");
    }
    
    public static void disable() {
        monitoringEnabled = false;
        LOGGER.info("Performance monitoring disabled");
    }
    
    public static boolean isEnabled() {
        return monitoringEnabled;
    }
    
    /**
     * Time an operation and track its performance
     */
    public static void timeOperation(String operationName, Runnable operation) {
        if (!monitoringEnabled) {
            operation.run();
            return;
        }
        
        long startTime = System.nanoTime();
        try {
            operation.run();
        } finally {
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            
            operationTimes.computeIfAbsent(operationName, k -> new AtomicLong(0)).addAndGet(duration);
            operationCounts.computeIfAbsent(operationName, k -> new AtomicLong(0)).incrementAndGet();
            
            // Log if operation takes too long (>1ms)
            if (duration > 1_000_000) {
                LOGGER.warn("Slow operation '{}' took {:.2f}ms", operationName, duration / 1_000_000.0);
            }
        }
    }
    
    /**
     * Check system performance periodically
     */
    public static void checkPerformance() {
        if (!monitoringEnabled) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Check memory usage
        if (currentTime - lastMemoryCheck > CHECK_INTERVAL) {
            checkMemoryUsage();
            lastMemoryCheck = currentTime;
        }
        
        // Check FPS impact
        if (currentTime - lastFpsCheck > CHECK_INTERVAL) {
            checkFpsImpact();
            lastFpsCheck = currentTime;
        }
    }
    
    private static void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        if (memoryUsagePercent > 80) {
            LOGGER.warn("High memory usage: {:.1f}% ({} MB / {} MB)", 
                       memoryUsagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
        }
    }
    
    private static void checkFpsImpact() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentFps() < 30) {
            LOGGER.warn("Low FPS detected: {} FPS", client.getCurrentFps());
        }
    }
    
    /**
     * Get performance statistics
     */
    public static String getPerformanceReport() {
        if (!monitoringEnabled) {
            return "Performance monitoring is disabled";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("=== WebControl Performance Report ===\n");
        
        // Operation timing statistics
        report.append("\nOperation Timing:\n");
        operationTimes.forEach((operation, totalTime) -> {
            long count = operationCounts.get(operation).get();
            double avgTime = totalTime.get() / (double) count / 1_000_000.0; // Convert to ms
            report.append(String.format("  %s: %.2fms avg (%d calls)\n", operation, avgTime, count));
        });
        
        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        report.append(String.format("\nMemory Usage: %d MB / %d MB (%.1f%%)\n", 
                                   usedMemory, maxMemory, (double) usedMemory / maxMemory * 100));
        
        // FPS
        MinecraftClient client = MinecraftClient.getInstance();
        report.append(String.format("Current FPS: %d\n", client.getCurrentFps()));
        
        return report.toString();
    }
    
    /**
     * Reset performance statistics
     */
    public static void resetStats() {
        operationTimes.clear();
        operationCounts.clear();
        LOGGER.info("Performance statistics reset");
    }
}
