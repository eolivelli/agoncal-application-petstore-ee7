package org.agoncal.application.petstore.herddb;

import org.agoncal.application.petstore.service.CategoryService;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// enable to have some figures on the *startup* time (on a cold jvm since it is what we use the most today)
//
// findAll:
// -> Database: H2 1.4.200 (2019-10-14): findall: 132ms
// -> Database: HerdDB 0.20.0-SNAPSHOT:  findall: 237ms
//
public final class HerdDBFigures {
    private HerdDBFigures() {
        // no-op
    }

    public static void main(final String... args) throws NamingException, SQLException {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
        System.setProperty("openejb.log.factory", "slf4j");
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s%6$s%n");
        System.setProperty("javax.persistence.sql-load-script-source", "init_db.sql");
        System.setProperty("db", "new://Resource?type=DataSource");
        if ("herddb".equalsIgnoreCase(System.getProperty("type"))) {
            System.setProperty("db.JdbcDriver", "herddb.jdbc.Driver");
            System.setProperty("db.JdbcUrl", "jdbc:herddb:local");
            System.setProperty("db.UserName", "sa");
            System.setProperty("db.Password", "hdb");
        } else {
            System.setProperty("db.JdbcDriver", "org.h2.Driver");
            System.setProperty("db.JdbcUrl", "jdbc:h2:mem:db");
            System.setProperty("db.UserName", "sa");
            System.setProperty("db.Password", "");
        }

        final Map<String, Object> config = new HashMap<>();
        config.put("javax.ejb.embeddable.provider", "tomee-embedded");
        config.put("disabled", "true"); // cxf-rs
        try (final EJBContainer container = EJBContainer.createEJBContainer(config)) {
            final CategoryService lookup = CategoryService.class.cast(container.getContext().lookup("global/agoncal-application-petstore-ee7/CategoryService"));
            final DataSource ds = DataSource.class.cast(container.getContext().lookup("openejb:Resource/db"));
            final long start = System.nanoTime();
            if (lookup.listAll().size() != 5) {
                throw new IllegalStateException("should be 5");
            }
            final long end = System.nanoTime();
            try (final Connection c = ds.getConnection()) {
                final DatabaseMetaData databaseMetaData = c.getMetaData();
                System.out.println("Database: " + databaseMetaData.getDatabaseProductName() + " " + databaseMetaData.getDatabaseProductVersion());
            }
            System.out.println("findall: " + TimeUnit.NANOSECONDS.toMillis(end - start) + "ms");
        }
    }
}
