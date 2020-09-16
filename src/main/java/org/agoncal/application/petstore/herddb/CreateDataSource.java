package org.agoncal.application.petstore.herddb;

import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.resource.jdbc.DataSourceFactory;
import org.apache.openejb.resource.jdbc.pool.DataSourceCreator;
import org.apache.openejb.util.Duration;
import org.apache.tomee.jdbc.TomEEDataSourceCreator;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.concurrent.TimeUnit;

// enable to have some figures on the *startup* time (on a cold jvm since it is what we use the most today)
//
// createDataSource - ds, no table:
// -> Database: H2 1.4.200 (2019-10-14): createDataSource: 533ms
// -> Database: HerdDB 0.20.0-SNAPSHOT: createDataSource: 1005ms
//
public final class CreateDataSource {
    private static final Duration DURATION;
    static {
        DURATION = Duration.parse("30 seconds");
        try {
            SystemInstance.init(System.getProperties());
            SystemInstance.get().setComponent(DataSourceCreator.class, new TomEEDataSourceCreator());
            SystemInstance.get().setComponent(TransactionManager.class, new GeronimoTransactionManager());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private CreateDataSource() {
        // no-op
    }

    public static void main(final String... args) throws Exception {
        final Class<?> driver;
        final String definition;
        if ("herddb".equalsIgnoreCase(System.getProperty("type"))) {
            driver = herddb.jdbc.Driver.class;
            definition = "" +
                    "JdbcDriver=" + driver.getName() + "\n" +
                    "JdbcUrl=jdbc:herddb:local\n" +
                    "UserName=sa\n" +
                    "Password=hdb\n" +
                    "";
        } else {
            driver = org.h2.Driver.class;
            definition = "" +
                    "JdbcDriver=" + driver.getName() + "\n" +
                    "JdbcUrl=jdbc:h2:mem:db\n" +
                    "UserName=sa\n" +
                    "Password=\n" +
                    "";
        }
        final long start = System.nanoTime();
        final DataSource dataSource = DataSource.class.cast(DataSourceFactory.create("test", true, driver, definition, DURATION, DURATION, DURATION, false));
        final long end = System.nanoTime();
        try (final Connection c = dataSource.getConnection()) {
            final DatabaseMetaData databaseMetaData = c.getMetaData();
            System.out.print("Database: " + databaseMetaData.getDatabaseProductName() + " " + databaseMetaData.getDatabaseProductVersion() + ": ");
        }
        System.out.println("createDataSource: " + TimeUnit.NANOSECONDS.toMillis(end - start) + "ms");
        if (Closeable.class.isInstance(dataSource)) {
            Closeable.class.cast(dataSource);
        }
    }
}
