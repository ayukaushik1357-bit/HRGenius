package com.hrgenius.entity.performance;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
public class Goal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_cycle_id", nullable = false)
    private ReviewCycle reviewCycle;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(nullable = false, precision = 5, scale = 2)
    private Double weightage = 0.0; // e.g. 25.0 %

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "progress_percentage", nullable = false, precision = 5, scale = 2)
    private Double progressPercentage = 0.0; // 0 to 100%
}
