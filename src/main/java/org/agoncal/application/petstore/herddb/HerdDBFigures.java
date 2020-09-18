package org.agoncal.application.petstore.herddb;

import herddb.jdbc.Driver;
import org.agoncal.application.petstore.model.Category;
import org.agoncal.application.petstore.service.CategoryService;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

// enable to have some figures on the *startup* time (on a cold jvm since it is what we use the most today)
//
// findAll - hot = 10000 calls:
// -> Database: H2 1.4.200 (2019-10-14): findall: 154ms, hot findall: 1ms (1008 ms in total)
// -> Database: HerdDB 0.20.0-SNAPSHOT: findall: 187ms, hot findall: 1ms (1098 ms in total) [embedded]
// -> Database: MySQL 8.0.21-0ubuntu0.20.04.4: findall: 176ms, hot findall: 5ms (5503 ms in total) [localhost]
// -> Database: HerdDB 0.20.0-SNAPSHOT: findall: 196ms, hot findall: 2ms (2939 ms in total) [remote, requirefsync=true]
// -> Database: HerdDB 0.20.0-SNAPSHOT: findall: 169ms, hot findall: 2ms (2839 ms in total) [remote, requirefsync=false]
//
public final class HerdDBFigures {
    private HerdDBFigures() {
        // no-op
    }

    public static void main(final String... args) throws NamingException, SQLException {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
        System.setProperty("org.agoncal.application.petstore.util.Loggable.skip", "true");
        System.setProperty("openejb.log.factory", "slf4j");
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s%6$s%n");
        System.setProperty("db", "new://Resource?type=DataSource");
        switch (System.getProperty("type")) {
            case "herddb":
                // System.setProperty("herddb.tablemanager.enableStreamingDataScanner", "false");
                // System.setProperty("server.planner.type", "none");

                System.setProperty("db.JdbcDriver", "herddb.jdbc.Driver");
                System.setProperty("db.JdbcUrl", "jdbc:herddb:local");
                System.setProperty("db.UserName", "sa");
                System.setProperty("db.Password", "hdb");
                System.setProperty("javax.persistence.sql-load-script-source", "init_db.sql");
                break;
            case "herddb-remote":
                System.setProperty("db.JdbcDriver", "herddb.jdbc.Driver");
                System.setProperty("db.JdbcUrl", "jdbc:herddb:server:localhost:7000?");
                System.setProperty("db.UserName", "sa");
                System.setProperty("db.Password", "hdb");
                System.setProperty("javax.persistence.sql-load-script-source", "init_db.sql");
                break;
            case "mysql":
                System.setProperty("db.JdbcDriver", "com.mysql.cj.jdbc.Driver");
                System.setProperty("db.JdbcUrl", "jdbc:mysql://localhost:3306/herddb_perf_comparison?serverTimezone=GMT");
                System.setProperty("db.UserName", "root");
                System.setProperty("db.Password", "rootpwd");
                System.setProperty("javax.persistence.sql-load-script-source", "init_db_mysql.sql");
                break;
            default:
                System.setProperty("db.JdbcDriver", "org.h2.Driver");
                System.setProperty("db.JdbcUrl", "jdbc:h2:mem:db");
                System.setProperty("db.UserName", "sa");
                System.setProperty("db.Password", "");
                System.setProperty("javax.persistence.sql-load-script-source", "init_db.sql");
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
                System.out.println("// -> Database URL: " + databaseMetaData.getURL());
                System.out.print("// -> Database: " + databaseMetaData.getDatabaseProductName() + " " + databaseMetaData.getDatabaseProductVersion() + ": ");
            }
            System.out.print("findall: " + TimeUnit.NANOSECONDS.toMillis(end - start) + "ms, ");

            // now do the same on a hot JVM
            IntStream.range(0, 5000).forEach(idx -> it(lookup));
            final long start2 = System.nanoTime();
            IntStream.range(0, 10000).forEach(idx -> it(lookup));
            final long end2 = System.nanoTime();
            System.out.println("hot findall: " + TimeUnit.NANOSECONDS.toMillis(end2 - start2) / 1000 + "ms (" + TimeUnit.NANOSECONDS.toMillis(end2 - start2) + " ms in total)");
        }

        new Driver().close(); // not yet elegant
    }

    private static void it(final CategoryService lookup) {
        final List<Category> categories = lookup.listAll();
        if (categories.size() != 5) {
            throw new IllegalStateException("should be 5: " + categories);
        }
    }
}
