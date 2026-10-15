package com.hrgenius.entity.performance;

import com.hrgenius.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "review_cycles")
@Getter
@Setter
@NoArgsConstructor
public class ReviewCycle extends BaseEntity {

    public enum CycleStatus {
        ACTIVE, CLOSED
    }

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CycleStatus status = CycleStatus.ACTIVE;
}
