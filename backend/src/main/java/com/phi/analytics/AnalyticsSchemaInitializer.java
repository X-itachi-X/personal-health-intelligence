package com.phi.analytics;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsSchemaInitializer {

    private final JdbcTemplate analyticsJdbc;

    public AnalyticsSchemaInitializer(@Qualifier("analyticsJdbcTemplate") JdbcTemplate analyticsJdbcTemplate) {
        this.analyticsJdbc = analyticsJdbcTemplate;
    }

    public void ensureSchema() {
        analyticsJdbc.execute("""
                CREATE TABLE IF NOT EXISTS dim_person (
                    person_id BIGINT PRIMARY KEY,
                    family_id VARCHAR,
                    display_name VARCHAR,
                    sex VARCHAR,
                    date_of_birth DATE
                )
                """);

        analyticsJdbc.execute("""
                CREATE TABLE IF NOT EXISTS fact_biomarker (
                    biomarker_id BIGINT PRIMARY KEY,
                    person_id BIGINT,
                    family_id VARCHAR,
                    lab_report_id BIGINT,
                    report_date DATE,
                    canonical_name VARCHAR,
                    numeric_value DOUBLE,
                    text_value VARCHAR,
                    unit VARCHAR,
                    reference_range VARCHAR,
                    uploaded_at TIMESTAMP
                )
                """);

        analyticsJdbc.execute("""
                CREATE INDEX IF NOT EXISTS idx_fact_person_canonical
                ON fact_biomarker (person_id, canonical_name, report_date)
                """);
    }
}
