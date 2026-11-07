package org.acme.vehicle.health;

import com.mongodb.client.MongoClient;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.sql.Connection;
import java.sql.Statement;

@Readiness
@ApplicationScoped
public class DatabaseReadinessCheck implements HealthCheck {

    @Inject
    AgroalDataSource dataSource;

    @Inject
    MongoClient mongoClient;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Database readiness check");
        boolean pgOk = false;
        boolean mongoOk = false;

        // Check PostgreSQL
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            pgOk = true;
            builder.withData("postgres", "UP");
        } catch (Exception e) {
            builder.withData("postgres", "DOWN: " + e.getMessage());
        }

        // Check MongoDB
        try {
            mongoClient.getDatabase("admin").runCommand(new Document("ping", 1));
            mongoOk = true;
            builder.withData("mongodb", "UP");
        } catch (Exception e) {
            builder.withData("mongodb", "DOWN: " + e.getMessage());
        }

        if (pgOk && mongoOk) {
            return builder.up().build();
        } else {
            return builder.down().build();
        }
    }
}

