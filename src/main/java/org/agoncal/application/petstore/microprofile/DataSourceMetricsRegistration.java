package org.agoncal.application.petstore.microprofile;

import org.apache.openejb.resource.jdbc.managed.local.ManagedDataSource;
import org.apache.tomee.jdbc.TomEEDataSourceCreator;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.sql.DataSource;

import static org.eclipse.microprofile.metrics.MetricRegistry.Type.APPLICATION;

@Dependent
public class DataSourceMetricsRegistration {
    @Resource(name = "db")
    private DataSource dataSource;

    public void onStart(@Observes @Initialized(ApplicationScoped.class) final Object onStart,
                        @RegistryType(type = APPLICATION) final MetricRegistry registry) {
        final TomEEDataSourceCreator.TomEEDataSource tds = TomEEDataSourceCreator.TomEEDataSource.class.cast(ManagedDataSource.class.cast(dataSource).getDelegate());
        registry.gauge("petstore.datasource.active", tds::getActive);
        registry.gauge("petstore.datasource.maxActive", tds::getMaxActive);
        registry.gauge("petstore.datasource.created", tds::getCreatedCount);
        registry.gauge("petstore.datasource.idle", tds::getIdle);
        registry.gauge("petstore.datasource.maxIdle", tds::getMaxIdle);
    }
}
