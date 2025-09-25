package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
@PropertySource("classpath:database.properties")
public class DatabaseConfig {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Bean
    public DataSource dataSource() {
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return DriverManager.getConnection(dbUrl, username, password);
            }

            @Override
            public java.io.PrintWriter getLogWriter() throws SQLException {
                return null;
            }

            @Override
            public void setLogWriter(java.io.PrintWriter out) throws SQLException {
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;
            }

            @Override
            public java.util.logging.Logger getParentLogger() {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }
        };
    }
}
