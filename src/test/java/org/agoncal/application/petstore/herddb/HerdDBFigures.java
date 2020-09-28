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
import java.util.stream.IntStream;
import org.junit.Test;

// enable to have some figures on the *startup* time (on a cold jvm since it is what we use the most today)
//
// findAll - hot = 1000 calls:
// -> Database: H2 1.4.200 (2019-10-14): findall: 125ms, hot findall: 0ms (527 ms in total)
// -> Database: HerdDB 0.20.0-SNAPSHOT:  findall: 177ms, hot findall: 0ms (779 ms in total)
//
// findAll - hot = 10000 calls:
// -> Database: H2 1.4.200 (2019-10-14): findall: 118ms, hot findall: 1ms (1207 ms in total)
// -> Database: HerdDB 0.20.0-SNAPSHOT: findall: 218ms, hot findall: 2ms (2166 ms in total)
//
public final class HerdDBFigures {
   

    @Test
    public void test() throws NamingException, SQLException {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
        System.setProperty("org.agoncal.application.petstore.util.Loggable.skip", "true");
        System.setProperty("openejb.log.factory", "slf4j");
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s%6$s%n");
        System.setProperty("javax.persistence.sql-load-script-source", "init_db.sql");
        System.setProperty("db", "new://Resource?type=DataSource");
        if ("herddb".equalsIgnoreCase(System.getProperty("type")) || true) {
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
            it(lookup);
            final long end = System.nanoTime();
            try (final Connection c = ds.getConnection()) {
                final DatabaseMetaData databaseMetaData = c.getMetaData();
                System.out.print("Database: " + databaseMetaData.getDatabaseProductName() + " " + databaseMetaData.getDatabaseProductVersion() + ": ");
            }
            System.out.print("findall: " + TimeUnit.NANOSECONDS.toMillis(end - start) + "ms, ");

            // now do the same on a hot JVM
            IntStream.range(0, 5000).forEach(idx -> it(lookup));
            final long start2 = System.nanoTime();
            IntStream.range(0, 10000).forEach(idx -> it(lookup));
            final long end2 = System.nanoTime();
            System.out.println("hot findall: " + TimeUnit.NANOSECONDS.toMillis(end2 - start2) / 1000 + "ms (" + TimeUnit.NANOSECONDS.toMillis(end2 - start2) + " ms in total)");
        }
    }

    private static void it(final CategoryService lookup) {
        if (lookup.listAll().size() != 5) {
            throw new IllegalStateException("should be 5");
        }
    }
}
