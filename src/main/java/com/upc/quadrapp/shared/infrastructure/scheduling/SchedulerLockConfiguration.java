package com.upc.quadrapp.shared.infrastructure.scheduling;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * The backend runs as several replicas, so every {@code @Scheduled} job must also carry
 * {@code @SchedulerLock}: the replica that takes the lock in {@code shared.shedlock} runs the
 * job and the others skip that execution. Lock times use the database clock, so replicas with
 * drifting clocks still agree.
 */
@Configuration(proxyBeanMethods = false)
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class SchedulerLockConfiguration {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName("shared.shedlock")
                .usingDbTime()
                .build());
    }
}
