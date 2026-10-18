package com.hrgenius.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Stores a {@link LocalTime} as "HH:mm:ss" text.
 * Oracle has no TIME data type, so time-of-day columns are VARCHAR2 in the Flyway migrations.
 */
@Converter
public class LocalTimeStringConverter implements AttributeConverter<LocalTime, String> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalTime attribute) {
        return attribute == null ? null : attribute.format(FORMAT);
    }

    @Override
    public LocalTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        // LocalTime.parse accepts both "HH:mm" and "HH:mm:ss"
        return LocalTime.parse(dbData.trim());
    }
}
