package com.example.synctube.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Railway: resolve JDBC URL from MYSQL_URL or validate MYSQL* before Spring opens the pool.
 */
public class RailwayDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String JDBC_PARAMS =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!shouldConfigureRailwayDatabase(environment)) {
            return;
        }

        String jdbcUrl = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"));

        String user = null;
        String password = null;

        if (jdbcUrl == null) {
            MysqlConnection mysqlConnection = fromMysqlUrl(environment.getProperty("MYSQL_URL"));
            if (mysqlConnection != null) {
                jdbcUrl = mysqlConnection.jdbcUrl();
                user = mysqlConnection.username();
                password = mysqlConnection.password();
            }
        }

        if (jdbcUrl != null) {
            applyDatasource(environment, jdbcUrl, user, password);
            return;
        }

        String host = environment.getProperty("MYSQLHOST");
        String port = environment.getProperty("MYSQLPORT");
        String database = environment.getProperty("MYSQLDATABASE");
        user = environment.getProperty("MYSQLUSER");
        password = environment.getProperty("MYSQLPASSWORD");

        if (!hasText(host) || !hasText(port) || !hasText(database)) {
            throw new IllegalStateException(
                    """
                    MySQL chưa được cấu hình trên Railway.
                    Service backend → Variables → Add Reference từ MySQL:
                    MYSQLHOST, MYSQLPORT, MYSQLUSER, MYSQLPASSWORD, MYSQLDATABASE
                    (hoặc thêm reference MYSQL_URL)
                    Đồng thời set SPRING_PROFILES_ACTIVE=oauth,prod
                    """);
        }

        applyDatasource(
                environment,
                "jdbc:mysql://" + host + ":" + port + "/" + database + JDBC_PARAMS,
                user,
                password);
    }

    private static boolean shouldConfigureRailwayDatabase(ConfigurableEnvironment environment) {
        if (containsProfile(environment.getProperty("SPRING_PROFILES_ACTIVE"), "prod")
                || Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return true;
        }
        return hasText(environment.getProperty("MYSQLHOST"))
                || hasText(environment.getProperty("MYSQL_URL"))
                || hasText(System.getenv("PORT"));
    }

    private static boolean containsProfile(String profiles, String profile) {
        if (!hasText(profiles)) {
            return false;
        }
        return Arrays.stream(profiles.split(","))
                .map(String::trim)
                .anyMatch(value -> value.equalsIgnoreCase(profile));
    }

    private static void applyDatasource(
            ConfigurableEnvironment environment, String url, String username, String password) {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", url);
        if (hasText(username)) {
            props.put("spring.datasource.username", username);
        }
        if (hasText(password)) {
            props.put("spring.datasource.password", password);
        }
        environment.getPropertySources().addFirst(new MapPropertySource("railwayDatabase", props));
    }

    static MysqlConnection fromMysqlUrl(String mysqlUrl) {
        if (!hasText(mysqlUrl)) {
            return null;
        }
        if (mysqlUrl.startsWith("jdbc:")) {
            return new MysqlConnection(mysqlUrl, null, null);
        }
        if (!mysqlUrl.startsWith("mysql://")) {
            return null;
        }

        String remainder = mysqlUrl.substring("mysql://".length());
        int at = remainder.lastIndexOf('@');
        if (at < 0) {
            return null;
        }

        String credentials = remainder.substring(0, at);
        String hostAndDb = remainder.substring(at + 1);
        int slash = hostAndDb.indexOf('/');
        if (slash < 0) {
            return null;
        }

        String hostPort = hostAndDb.substring(0, slash);
        String database = hostAndDb.substring(slash + 1);
        int colon = credentials.indexOf(':');
        if (colon < 0) {
            return null;
        }

        String username = credentials.substring(0, colon);
        String password = credentials.substring(colon + 1);

        int portSep = hostPort.lastIndexOf(':');
        String host = portSep >= 0 ? hostPort.substring(0, portSep) : hostPort;
        String port = portSep >= 0 ? hostPort.substring(portSep + 1) : "3306";

        return new MysqlConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database + JDBC_PARAMS, username, password);
    }

    record MysqlConnection(String jdbcUrl, String username, String password) {}

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
