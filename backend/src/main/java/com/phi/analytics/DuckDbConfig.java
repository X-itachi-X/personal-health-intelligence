package com.phi.analytics;

import com.phi.config.PhiProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class DuckDbConfig {

    @Bean(name = "analyticsJdbcTemplate")
    JdbcTemplate analyticsJdbcTemplate(PhiProperties properties) throws Exception {
        Path dbPath = Path.of(properties.analytics().duckdbPath());
        Files.createDirectories(dbPath.getParent());

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.duckdb.DuckDBDriver");
        dataSource.setUrl("jdbc:duckdb:" + dbPath.toAbsolutePath());

        return new JdbcTemplate(dataSource);
    }
}
