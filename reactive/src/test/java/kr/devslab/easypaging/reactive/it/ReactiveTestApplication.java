package kr.devslab.easypaging.reactive.it;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * Spring Boot test application that wires up R2DBC + WebFlux. Used by the
 * integration tests in this package to exercise the reactive module's
 * argument resolver, R2DBC paging helpers, and auto-configurations.
 */
@SpringBootApplication
@EnableR2dbcRepositories
public class ReactiveTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveTestApplication.class, args);
    }

    /**
     * Runs schema.sql + data.sql against the R2DBC H2 connection at startup.
     * Spring Boot's R2DBC autoconfig handles this automatically when the SQL
     * files are on the classpath, but doing it explicitly here documents the
     * test fixture and stays robust against config changes.
     */
    @Bean
    public ConnectionFactoryInitializer connectionFactoryInitializer(ConnectionFactory factory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(factory);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"),
                new ClassPathResource("data.sql"));
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
