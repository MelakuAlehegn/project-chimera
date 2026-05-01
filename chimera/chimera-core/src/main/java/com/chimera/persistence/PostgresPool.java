package com.chimera.persistence;

import com.chimera.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Factory for the application's PostgreSQL connection pool.
 *
 * Connection pools amortize the ~50ms cost of opening a TCP connection to
 * Postgres across many requests. The pool is created once at startup and
 * shared by everything that touches the database.
 */
public final class PostgresPool {

    private PostgresPool() {
    }

    public static HikariDataSource create(Config config) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(config.databaseUrl());
        hc.setUsername(config.databaseUser());
        hc.setPassword(config.databasePassword());
        hc.setMaximumPoolSize(10);
        hc.setMinimumIdle(2);
        hc.setPoolName("chimera-pg");
        return new HikariDataSource(hc);
    }
}
