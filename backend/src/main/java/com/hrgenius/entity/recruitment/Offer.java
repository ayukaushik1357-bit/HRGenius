package com.hrgenius.entity.recruitment;

import com.hrgenius.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "job_offers")
@Getter
@Setter
@NoArgsConstructor
public class Offer extends BaseEntity {

    public enum OfferStatus {
        PENDING, ACCEPTED, REJECTED
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(name = "offer_date", nullable = false)
    private LocalDate offerDate;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "offered_salary_ctc", nullable = false, precision = 15, scale = 2)
    private BigDecimal offeredSalaryCTC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status = OfferStatus.PENDING;

    @Column(length = 1000)
    private String remarks;
}
