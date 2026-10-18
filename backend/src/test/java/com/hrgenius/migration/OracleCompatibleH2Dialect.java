package com.hrgenius.migration;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.jdbc.NumericJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

/**
 * H2Dialect extension for testing Flyway migrations on H2 in MODE=Oracle.
 * In Oracle & H2 MODE=Oracle:
 * - Columns defined as DATE are treated as TIMESTAMP(0) to emulate Oracle DATE.
 * - Columns defined as NUMBER(p,s) for Double/Float are treated as NUMERIC.
 */
public class OracleCompatibleH2Dialect extends H2Dialect {

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
        jdbcTypeRegistry.addDescriptor(Types.DATE, TimestampJdbcType.INSTANCE);
        jdbcTypeRegistry.addDescriptor(Types.DOUBLE, NumericJdbcType.INSTANCE);
        jdbcTypeRegistry.addDescriptor(Types.FLOAT, NumericJdbcType.INSTANCE);
    }
}
