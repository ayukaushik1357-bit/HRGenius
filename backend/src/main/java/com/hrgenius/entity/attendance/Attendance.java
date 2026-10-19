package com.hrgenius.entity.attendance;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.converter.LocalTimeStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employee_id", "attendance_date"})
})
@Getter
@Setter
@NoArgsConstructor
public class Attendance extends BaseEntity {

    public enum AttendanceStatus {
        PRESENT, HALF_DAY, ABSENT
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate date;

    @Column(name = "check_in_time", length = 10)
    @Convert(converter = LocalTimeStringConverter.class)
    private LocalTime checkInTime;

    @Column(name = "check_out_time", length = 10)
    @Convert(converter = LocalTimeStringConverter.class)
    private LocalTime checkOutTime;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "working_hours", precision = 5, scale = 2)
    private Double workingHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(length = 255)
    private String notes;
}
