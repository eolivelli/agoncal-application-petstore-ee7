package org.agoncal.application.petstore.microprofile;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Readiness
@ApplicationScoped
public class DataSourceHealth implements HealthCheck {
    @Resource(name = "db")
    private DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        final HealthCheckResponseBuilder base = HealthCheckResponse.builder()
                .name("petstore-datasource");
        try (final Connection connection = dataSource.getConnection()) {
            if (connection.isValid(30000)) {
                base.up();
            } else {
                base.down();
                base.withData("invalid", true);
                base.withData("timeout", 30000);
            }
        } catch (final SQLException throwables) {
            base.down();
            base.withData("invalid", true);
            base.withData("exception", throwables.getMessage());
            base.withData("errorcode", throwables.getErrorCode());
            base.withData("sqlstate", throwables.getSQLState());
        }
        return base.build();
    }
}
