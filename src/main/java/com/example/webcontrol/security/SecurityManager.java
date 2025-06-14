package com.example.webcontrol.security;

import com.example.webcontrol.WebControlConfig;
import com.example.webcontrol.notifications.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced security system for WebControl mod
 * Handles authentication, rate limiting, and security monitoring
 */
public class SecurityManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-security");
    private static final SecureRandom random = new SecureRandom();
    
    // Rate limiting
    private static final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long RATE_LIMIT_WINDOW = 60000; // 1 minute
    
    // Authentication
    private static String currentApiKey = null;
    private static final ConcurrentHashMap<String, Long> validTokens = new ConcurrentHashMap<>();
    private static final long TOKEN_EXPIRY = 3600000; // 1 hour
    
    // Security monitoring
    private static final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 300000; // 5 minutes
    private static final ConcurrentHashMap<String, Long> lockedIPs = new ConcurrentHashMap<>();
    
    public static void initialize() {
        generateApiKey();
        LOGGER.info("Security Manager initialized");
    }
    
    /**
     * Generate a new API key
     */
    public static String generateApiKey() {
        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);
        currentApiKey = Base64.getEncoder().encodeToString(keyBytes);
        
        WebControlConfig config = WebControlConfig.getInstance();
        config.apiKey = currentApiKey;
        config.save();
        
        LOGGER.info("New API key generated");
        NotificationManager.info("Security", "New API key generated");
        
        return currentApiKey;
    }
    
    /**
     * Validate API key
     */
    public static boolean validateApiKey(String providedKey) {
        WebControlConfig config = WebControlConfig.getInstance();
        
        if (!config.requireAuthentication) {
            return true;
        }
        
        if (currentApiKey == null) {
            currentApiKey = config.apiKey;
        }
        
        return currentApiKey != null && currentApiKey.equals(providedKey);
    }
    
    /**
     * Check rate limiting for an IP address
     */
    public static boolean checkRateLimit(String ipAddress) {
        long currentTime = System.currentTimeMillis();
        
        // Check if IP is locked out
        Long lockoutTime = lockedIPs.get(ipAddress);
        if (lockoutTime != null && currentTime - lockoutTime < LOCKOUT_DURATION) {
            LOGGER.warn("Request from locked IP: {}", ipAddress);
            return false;
        }
        
        // Remove expired lockouts
        if (lockoutTime != null && currentTime - lockoutTime >= LOCKOUT_DURATION) {
            lockedIPs.remove(ipAddress);
            failedAttempts.remove(ipAddress);
        }
        
        // Check rate limit
        AtomicInteger count = requestCounts.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
        Long lastRequest = lastRequestTime.get(ipAddress);
        
        if (lastRequest == null || currentTime - lastRequest > RATE_LIMIT_WINDOW) {
            // Reset counter for new window
            count.set(1);
            lastRequestTime.put(ipAddress, currentTime);
            return true;
        }
        
        int currentCount = count.incrementAndGet();
        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            LOGGER.warn("Rate limit exceeded for IP: {} ({} requests)", ipAddress, currentCount);
            recordFailedAttempt(ipAddress);
            return false;
        }
        
        return true;
    }
    
    /**
     * Record a failed authentication attempt
     */
    public static void recordFailedAttempt(String ipAddress) {
        int attempts = failedAttempts.computeIfAbsent(ipAddress, k -> 0) + 1;
        failedAttempts.put(ipAddress, attempts);
        
        LOGGER.warn("Failed attempt #{} from IP: {}", attempts, ipAddress);
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockedIPs.put(ipAddress, System.currentTimeMillis());
            LOGGER.error("IP {} locked out after {} failed attempts", ipAddress, attempts);
            NotificationManager.security("Security Alert", 
                "IP " + ipAddress + " locked out after " + attempts + " failed attempts");
        }
    }
    
    /**
     * Generate a temporary access token
     */
    public static String generateAccessToken() {
        byte[] tokenBytes = new byte[16];
        random.nextBytes(tokenBytes);
        String token = Base64.getEncoder().encodeToString(tokenBytes);
        
        validTokens.put(token, System.currentTimeMillis() + TOKEN_EXPIRY);
        
        LOGGER.info("Access token generated");
        return token;
    }
    
    /**
     * Validate access token
     */
    public static boolean validateAccessToken(String token) {
        Long expiry = validTokens.get(token);
        if (expiry == null) {
            return false;
        }
        
        if (System.currentTimeMillis() > expiry) {
            validTokens.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Hash a password securely
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to hash password", e);
            return null;
        }
    }
    
    /**
     * Generate a random salt
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Get security status report
     */
    public static String getSecurityReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== WebControl Security Report ===\n");
        
        WebControlConfig config = WebControlConfig.getInstance();
        report.append("Authentication Required: ").append(config.requireAuthentication).append("\n");
        report.append("API Key Set: ").append(currentApiKey != null && !currentApiKey.isEmpty()).append("\n");
        report.append("Active Tokens: ").append(validTokens.size()).append("\n");
        report.append("Locked IPs: ").append(lockedIPs.size()).append("\n");
        report.append("Failed Attempts: ").append(failedAttempts.size()).append("\n");
        
        if (!lockedIPs.isEmpty()) {
            report.append("\nLocked IP Addresses:\n");
            lockedIPs.forEach((ip, time) -> {
                long remaining = (time + LOCKOUT_DURATION - System.currentTimeMillis()) / 1000;
                report.append("  ").append(ip).append(" (").append(Math.max(0, remaining)).append("s remaining)\n");
            });
        }
        
        return report.toString();
    }
    
    /**
     * Clear all security data
     */
    public static void clearSecurityData() {
        requestCounts.clear();
        lastRequestTime.clear();
        validTokens.clear();
        failedAttempts.clear();
        lockedIPs.clear();
        
        LOGGER.info("Security data cleared");
        NotificationManager.info("Security", "All security data cleared");
    }
    
    /**
     * Check if an IP is currently locked
     */
    public static boolean isIPLocked(String ipAddress) {
        Long lockoutTime = lockedIPs.get(ipAddress);
        if (lockoutTime == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        return currentTime - lockoutTime < LOCKOUT_DURATION;
    }
}
