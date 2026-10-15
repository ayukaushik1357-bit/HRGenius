package com.hrgenius.entity.onboarding;

import com.hrgenius.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "onboarding_templates")
@Getter
@Setter
@NoArgsConstructor
public class OnboardingTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OnboardingTemplateTask> tasks = new ArrayList<>();

    public void addTask(OnboardingTemplateTask task) {
        tasks.add(task);
        task.setTemplate(this);
    }
}
