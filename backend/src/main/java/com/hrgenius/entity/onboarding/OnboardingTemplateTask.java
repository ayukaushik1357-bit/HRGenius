package com.hrgenius.entity.onboarding;

import com.hrgenius.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "onboarding_template_tasks")
@Getter
@Setter
@NoArgsConstructor
public class OnboardingTemplateTask extends BaseEntity {

    public enum TaskCategory {
        DOCUMENTS, IT_SETUP, INDUCTION, TRAINING
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private OnboardingTemplate template;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskCategory category = TaskCategory.DOCUMENTS;

    @Column(name = "default_days_to_complete", nullable = false)
    private Integer defaultDaysToComplete = 3;
}
