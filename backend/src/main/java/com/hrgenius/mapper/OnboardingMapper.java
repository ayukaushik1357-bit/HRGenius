package com.hrgenius.mapper;

import com.hrgenius.dto.onboarding.*;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.onboarding.OnboardingTask;
import com.hrgenius.entity.onboarding.OnboardingTemplate;
import com.hrgenius.entity.onboarding.OnboardingTemplateTask;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OnboardingMapper {

    public OnboardingTemplateResponse toTemplateResponse(OnboardingTemplate entity) {
        if (entity == null) return null;
        List<OnboardingTemplateResponse.TemplateTaskDto> taskDtos = entity.getTasks() == null ? Collections.emptyList() :
                entity.getTasks().stream().map(t -> OnboardingTemplateResponse.TemplateTaskDto.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .category(t.getCategory() != null ? t.getCategory().name() : null)
                        .defaultDaysToComplete(t.getDefaultDaysToComplete())
                        .build()
                ).collect(Collectors.toList());

        return OnboardingTemplateResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .tasks(taskDtos)
                .build();
    }

    public OnboardingTaskResponse toTaskResponse(OnboardingTask entity) {
        if (entity == null) return null;
        String employeeName = null;
        Long employeeId = null;
        if (entity.getEmployee() != null) {
            employeeId = entity.getEmployee().getId();
            employeeName = entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName();
        }

        String ownerName = null;
        Long ownerId = null;
        if (entity.getOwner() != null) {
            ownerId = entity.getOwner().getId();
            ownerName = entity.getOwner().getFirstName() + " " + entity.getOwner().getLastName();
        }

        return OnboardingTaskResponse.builder()
                .id(entity.getId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .category(entity.getCategory() != null ? entity.getCategory().name() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .ownerId(ownerId)
                .ownerName(ownerName)
                .dueDate(entity.getDueDate())
                .completedAt(entity.getCompletedAt())
                .build();
    }

    public OnboardingProgressResponse toProgressResponse(Employee employee, List<OnboardingTask> tasks) {
        if (employee == null) return null;
        long total = tasks != null ? tasks.size() : 0;
        long completed = tasks != null ? tasks.stream().filter(t -> t.getStatus() == OnboardingTask.TaskStatus.COMPLETED).count() : 0;
        double pct = total > 0 ? ((double) completed / total) * 100.0 : 0.0;

        List<OnboardingTaskResponse> taskResponses = tasks == null ? Collections.emptyList() :
                tasks.stream().map(this::toTaskResponse).collect(Collectors.toList());

        return OnboardingProgressResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .employeeCode(employee.getEmployeeCode())
                .totalTasks(total)
                .completedTasks(completed)
                .progressPercentage(Math.round(pct * 10.0) / 10.0)
                .tasks(taskResponses)
                .build();
    }
}
