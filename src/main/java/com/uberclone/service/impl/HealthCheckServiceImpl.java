package com.uberclone.service.impl;

import com.uberclone.service.HealthCheckService;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthCheckServiceImpl(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Map<String, String> checkHealth() {
        Map<String, String> health = new HashMap<>();
        health.put("app", "UP");
        health.put("db", checkDb() ? "UP" : "DOWN");
        health.put("redis", checkRedis() ? "UP" : "DOWN");
        return health;
    }

    private boolean checkDb() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(1);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            return redisConnectionFactory.getConnection().ping() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
