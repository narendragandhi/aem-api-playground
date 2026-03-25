package com.aemtools.aem.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent audit logger using SQLite.
 * Stores API call history, user actions, and system events.
 */
public class AuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);
    private static AuditLogger instance;
    private static final String DB_FILE = System.getProperty("user.home") + "/.aem-api/audit.db";
    private Connection connection;

    private AuditLogger() {
        initDatabase();
    }

    public static synchronized AuditLogger getInstance() {
        if (instance == null) {
            instance = new AuditLogger();
        }
        return instance;
    }

    private void initDatabase() {
        try {
            File dbFile = new File(DB_FILE);
            dbFile.getParentFile().mkdirs();

            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);

            // Create tables if not exist
            try (Statement stmt = connection.createStatement()) {
                // API calls table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS api_calls (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        timestamp TEXT NOT NULL,
                        method TEXT NOT NULL,
                        path TEXT NOT NULL,
                        status_code INTEGER,
                        duration_ms INTEGER,
                        environment TEXT,
                        user_id TEXT,
                        request_size INTEGER,
                        response_size INTEGER,
                        error_message TEXT
                    )
                """);

                // User actions table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS user_actions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        timestamp TEXT NOT NULL,
                        action TEXT NOT NULL,
                        target TEXT,
                        details TEXT,
                        result TEXT
                    )
                """);

                // System events table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS system_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        timestamp TEXT NOT NULL,
                        event_type TEXT NOT NULL,
                        message TEXT,
                        severity TEXT
                    )
                """);

                // Create indexes for faster queries
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_api_calls_timestamp ON api_calls(timestamp)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_api_calls_method ON api_calls(method)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_actions_timestamp ON user_actions(timestamp)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_system_events_timestamp ON system_events(timestamp)");
            }

            logger.info("Audit database initialized at: {}", DB_FILE);
        } catch (SQLException e) {
            logger.error("Failed to initialize audit database: {}", e.getMessage());
        }
    }

    /**
     * Logs an API call.
     */
    public void logApiCall(String method, String path, int statusCode, long durationMs,
                           String environment, String userId, Integer requestSize,
                           Integer responseSize, String errorMessage) {
        if (connection == null) return;

        String sql = """
            INSERT INTO api_calls (timestamp, method, path, status_code, duration_ms,
                                   environment, user_id, request_size, response_size, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, Instant.now().toString());
            pstmt.setString(2, method);
            pstmt.setString(3, path);
            pstmt.setInt(4, statusCode);
            pstmt.setLong(5, durationMs);
            pstmt.setString(6, environment);
            pstmt.setString(7, userId);
            pstmt.setObject(8, requestSize);
            pstmt.setObject(9, responseSize);
            pstmt.setString(10, errorMessage);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.warn("Failed to log API call: {}", e.getMessage());
        }
    }

    /**
     * Logs a user action.
     */
    public void logUserAction(String action, String target, String details, String result) {
        if (connection == null) return;

        String sql = "INSERT INTO user_actions (timestamp, action, target, details, result) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, Instant.now().toString());
            pstmt.setString(2, action);
            pstmt.setString(3, target);
            pstmt.setString(4, details);
            pstmt.setString(5, result);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.warn("Failed to log user action: {}", e.getMessage());
        }
    }

    /**
     * Logs a system event.
     */
    public void logSystemEvent(String eventType, String message, Severity severity) {
        if (connection == null) return;

        String sql = "INSERT INTO system_events (timestamp, event_type, message, severity) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, Instant.now().toString());
            pstmt.setString(2, eventType);
            pstmt.setString(3, message);
            pstmt.setString(4, severity.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.warn("Failed to log system event: {}", e.getMessage());
        }
    }

    /**
     * Retrieves recent API calls.
     */
    public List<ApiCallRecord> getRecentApiCalls(int limit) {
        List<ApiCallRecord> records = new ArrayList<>();
        if (connection == null) return records;

        String sql = "SELECT * FROM api_calls ORDER BY timestamp DESC LIMIT ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new ApiCallRecord(
                        rs.getLong("id"),
                        rs.getString("timestamp"),
                        rs.getString("method"),
                        rs.getString("path"),
                        rs.getInt("status_code"),
                        rs.getLong("duration_ms"),
                        rs.getString("environment"),
                        rs.getString("user_id"),
                        rs.getObject("request_size") != null ? rs.getInt("request_size") : null,
                        rs.getObject("response_size") != null ? rs.getInt("response_size") : null,
                        rs.getString("error_message")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to retrieve API calls: {}", e.getMessage());
        }

        return records;
    }

    /**
     * Retrieves API calls within a time range.
     */
    public List<ApiCallRecord> getApiCallsInRange(Instant start, Instant end) {
        List<ApiCallRecord> records = new ArrayList<>();
        if (connection == null) return records;

        String sql = "SELECT * FROM api_calls WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new ApiCallRecord(
                        rs.getLong("id"),
                        rs.getString("timestamp"),
                        rs.getString("method"),
                        rs.getString("path"),
                        rs.getInt("status_code"),
                        rs.getLong("duration_ms"),
                        rs.getString("environment"),
                        rs.getString("user_id"),
                        rs.getObject("request_size") != null ? rs.getInt("request_size") : null,
                        rs.getObject("response_size") != null ? rs.getInt("response_size") : null,
                        rs.getString("error_message")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to retrieve API calls: {}", e.getMessage());
        }

        return records;
    }

    /**
     * Retrieves recent user actions.
     */
    public List<UserActionRecord> getRecentUserActions(int limit) {
        List<UserActionRecord> records = new ArrayList<>();
        if (connection == null) return records;

        String sql = "SELECT * FROM user_actions ORDER BY timestamp DESC LIMIT ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new UserActionRecord(
                        rs.getLong("id"),
                        rs.getString("timestamp"),
                        rs.getString("action"),
                        rs.getString("target"),
                        rs.getString("details"),
                        rs.getString("result")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to retrieve user actions: {}", e.getMessage());
        }

        return records;
    }

    /**
     * Retrieves recent system events.
     */
    public List<SystemEventRecord> getRecentSystemEvents(int limit) {
        List<SystemEventRecord> records = new ArrayList<>();
        if (connection == null) return records;

        String sql = "SELECT * FROM system_events ORDER BY timestamp DESC LIMIT ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new SystemEventRecord(
                        rs.getLong("id"),
                        rs.getString("timestamp"),
                        rs.getString("event_type"),
                        rs.getString("message"),
                        Severity.valueOf(rs.getString("severity"))
                    ));
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to retrieve system events: {}", e.getMessage());
        }

        return records;
    }

    /**
     * Gets audit statistics.
     */
    public AuditStats getStats() {
        AuditStats stats = new AuditStats();
        if (connection == null) return stats;

        try (Statement stmt = connection.createStatement()) {
            // Total API calls
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM api_calls")) {
                if (rs.next()) stats.setTotalApiCalls(rs.getLong(1));
            }

            // API calls today
            String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM api_calls WHERE timestamp LIKE '" + today + "%'")) {
                if (rs.next()) stats.setApiCallsToday(rs.getLong(1));
            }

            // Error count
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM api_calls WHERE status_code >= 400")) {
                if (rs.next()) stats.setErrorCount(rs.getLong(1));
            }

            // Average response time
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT AVG(duration_ms) FROM api_calls WHERE duration_ms IS NOT NULL")) {
                if (rs.next()) stats.setAvgResponseTimeMs(rs.getDouble(1));
            }

            // Total user actions
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_actions")) {
                if (rs.next()) stats.setTotalUserActions(rs.getLong(1));
            }

            // Total system events
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM system_events")) {
                if (rs.next()) stats.setTotalSystemEvents(rs.getLong(1));
            }

            // Most used endpoints (top 5)
            try (ResultSet rs = stmt.executeQuery("""
                SELECT method || ' ' || path as endpoint, COUNT(*) as count
                FROM api_calls
                GROUP BY method, path
                ORDER BY count DESC
                LIMIT 5
            """)) {
                List<String> topEndpoints = new ArrayList<>();
                while (rs.next()) {
                    topEndpoints.add(rs.getString("endpoint") + " (" + rs.getInt("count") + ")");
                }
                stats.setTopEndpoints(topEndpoints);
            }

        } catch (SQLException e) {
            logger.warn("Failed to get audit stats: {}", e.getMessage());
        }

        return stats;
    }

    /**
     * Purges audit records older than the specified number of days.
     */
    public int purgeOldRecords(int olderThanDays) {
        if (connection == null) return 0;

        Instant cutoff = Instant.now().minusSeconds(olderThanDays * 24L * 60 * 60);
        String cutoffStr = cutoff.toString();
        int totalDeleted = 0;

        try (Statement stmt = connection.createStatement()) {
            totalDeleted += stmt.executeUpdate(
                "DELETE FROM api_calls WHERE timestamp < '" + cutoffStr + "'");
            totalDeleted += stmt.executeUpdate(
                "DELETE FROM user_actions WHERE timestamp < '" + cutoffStr + "'");
            totalDeleted += stmt.executeUpdate(
                "DELETE FROM system_events WHERE timestamp < '" + cutoffStr + "'");

            // Vacuum to reclaim space
            stmt.execute("VACUUM");

            logger.info("Purged {} audit records older than {} days", totalDeleted, olderThanDays);
        } catch (SQLException e) {
            logger.warn("Failed to purge old records: {}", e.getMessage());
        }

        return totalDeleted;
    }

    /**
     * Exports audit data to a file.
     */
    public void exportToFile(String filePath, ExportFormat format) {
        // Implementation for CSV/JSON export
        logSystemEvent("EXPORT", "Audit export requested to: " + filePath, Severity.INFO);
    }

    /**
     * Closes the database connection.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Audit database connection closed");
            } catch (SQLException e) {
                logger.warn("Error closing audit database: {}", e.getMessage());
            }
        }
    }

    // Enums and Records

    public enum Severity {
        DEBUG, INFO, WARN, ERROR, CRITICAL
    }

    public enum ExportFormat {
        CSV, JSON
    }

    public record ApiCallRecord(
        long id,
        String timestamp,
        String method,
        String path,
        int statusCode,
        long durationMs,
        String environment,
        String userId,
        Integer requestSize,
        Integer responseSize,
        String errorMessage
    ) {
        @Override
        public String toString() {
            return String.format("[%s] %s %s -> %d (%dms)",
                timestamp, method, path, statusCode, durationMs);
        }
    }

    public record UserActionRecord(
        long id,
        String timestamp,
        String action,
        String target,
        String details,
        String result
    ) {
        @Override
        public String toString() {
            return String.format("[%s] %s: %s -> %s", timestamp, action, target, result);
        }
    }

    public record SystemEventRecord(
        long id,
        String timestamp,
        String eventType,
        String message,
        Severity severity
    ) {
        @Override
        public String toString() {
            return String.format("[%s] [%s] %s: %s", timestamp, severity, eventType, message);
        }
    }

    public static class AuditStats {
        private long totalApiCalls;
        private long apiCallsToday;
        private long errorCount;
        private double avgResponseTimeMs;
        private long totalUserActions;
        private long totalSystemEvents;
        private List<String> topEndpoints = new ArrayList<>();

        public long getTotalApiCalls() { return totalApiCalls; }
        public void setTotalApiCalls(long totalApiCalls) { this.totalApiCalls = totalApiCalls; }
        public long getApiCallsToday() { return apiCallsToday; }
        public void setApiCallsToday(long apiCallsToday) { this.apiCallsToday = apiCallsToday; }
        public long getErrorCount() { return errorCount; }
        public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
        public double getAvgResponseTimeMs() { return avgResponseTimeMs; }
        public void setAvgResponseTimeMs(double avgResponseTimeMs) { this.avgResponseTimeMs = avgResponseTimeMs; }
        public long getTotalUserActions() { return totalUserActions; }
        public void setTotalUserActions(long totalUserActions) { this.totalUserActions = totalUserActions; }
        public long getTotalSystemEvents() { return totalSystemEvents; }
        public void setTotalSystemEvents(long totalSystemEvents) { this.totalSystemEvents = totalSystemEvents; }
        public List<String> getTopEndpoints() { return topEndpoints; }
        public void setTopEndpoints(List<String> topEndpoints) { this.topEndpoints = topEndpoints; }

        @Override
        public String toString() {
            return String.format(
                "Audit Stats:\n" +
                "  Total API Calls: %d (today: %d)\n" +
                "  Errors: %d\n" +
                "  Avg Response Time: %.1fms\n" +
                "  User Actions: %d\n" +
                "  System Events: %d\n" +
                "  Top Endpoints: %s",
                totalApiCalls, apiCallsToday, errorCount, avgResponseTimeMs,
                totalUserActions, totalSystemEvents, String.join(", ", topEndpoints)
            );
        }
    }
}
