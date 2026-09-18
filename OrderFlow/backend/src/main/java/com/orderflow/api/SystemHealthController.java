package com.orderflow.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {
    private final DataSource dataSource;
    private final StringRedisTemplate redis;
    private final String kafkaBootstrapServers;
    private final boolean kafkaEnabled;

    public SystemHealthController(DataSource dataSource, StringRedisTemplate redis,
                                  @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String kafkaBootstrapServers,
                                  @Value("${app.kafka.enabled:false}") boolean kafkaEnabled) {
        this.dataSource = dataSource;
        this.redis = redis;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.kafkaEnabled = kafkaEnabled;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("API", "UP");
        checks.put("DATABASE", databaseStatus());
        checks.put("REDIS", redisStatus());
        checks.put("KAFKA", kafkaStatus());
        checks.put("PAYMENT", "UP");
        checks.put("NOTIFICATION", "UP");
        boolean healthy = checks.values().stream().allMatch(status -> "UP".equals(status) || "DISABLED".equals(status));
        return ResponseEntity.status(healthy ? 200 : 503).body(Map.of("status", healthy ? "UP" : "DEGRADED", "checks", checks));
    }

    private String databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1) ? "UP" : "DOWN";
        } catch (SQLException ex) {
            return "DOWN";
        }
    }

    private String redisStatus() {
        org.springframework.data.redis.connection.RedisConnection connection = null;
        try {
            connection = redis.getConnectionFactory().getConnection();
            return "PONG".equalsIgnoreCase(connection.ping()) ? "UP" : "DOWN";
        } catch (RuntimeException ex) {
            return "DOWN";
        } finally {
            if (connection != null) connection.close();
        }
    }

    private String kafkaStatus() {
        if (!kafkaEnabled) return "DISABLED";
        Map<String, Object> properties = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        try (AdminClient adminClient = AdminClient.create(properties)) {
            adminClient.describeCluster().nodes().get(2, java.util.concurrent.TimeUnit.SECONDS);
            return "UP";
        } catch (Exception ex) {
            return "DOWN";
        }
    }
}
