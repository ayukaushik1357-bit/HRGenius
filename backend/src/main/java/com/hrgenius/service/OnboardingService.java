package com.hrgenius.service;

import com.hrgenius.dto.onboarding.*;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.notification.AppNotification;
import com.hrgenius.entity.onboarding.OnboardingTask;
import com.hrgenius.entity.onboarding.OnboardingTemplate;
import com.hrgenius.entity.onboarding.OnboardingTemplateTask;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.OnboardingMapper;
import com.hrgenius.repository.EmployeeRepository;
import com.hrgenius.repository.OnboardingTaskRepository;
import com.hrgenius.repository.OnboardingTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingTemplateRepository templateRepository;
    private final OnboardingTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final OnboardingMapper onboardingMapper;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<OnboardingTemplateResponse> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(onboardingMapper::toTemplateResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OnboardingTemplateResponse getTemplateById(Long id) {
        OnboardingTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingTemplate", "id", id));
        return onboardingMapper.toTemplateResponse(template);
    }

    @Transactional
    public OnboardingTemplateResponse createTemplate(OnboardingTemplateRequest request) {
        OnboardingTemplate template = new OnboardingTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());

        if (request.getTasks() != null) {
            for (OnboardingTemplateTaskRequest tr : request.getTasks()) {
                OnboardingTemplateTask task = new OnboardingTemplateTask();
                task.setTitle(tr.getTitle());
                task.setDescription(tr.getDescription());
                if (tr.getCategory() != null) {
                    task.setCategory(OnboardingTemplateTask.TaskCategory.valueOf(tr.getCategory().toUpperCase()));
                }
                task.setDefaultDaysToComplete(tr.getDefaultDaysToComplete() != null ? tr.getDefaultDaysToComplete() : 3);
                template.addTask(task);
            }
        }

        OnboardingTemplate saved = templateRepository.save(template);
        return onboardingMapper.toTemplateResponse(saved);
    }

    @Transactional
    public void assignChecklistToEmployee(Long employeeId, Long templateId, Long ownerId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        OnboardingTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingTemplate", "id", templateId));
        Employee owner = ownerId != null ? employeeRepository.findById(ownerId).orElse(null) : employee.getReportingManager();

        List<OnboardingTask> tasks = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (OnboardingTemplateTask tt : template.getTasks()) {
            OnboardingTask task = new OnboardingTask();
            task.setEmployee(employee);
            task.setTitle(tt.getTitle());
            task.setDescription(tt.getDescription());
            task.setCategory(tt.getCategory());
            task.setStatus(OnboardingTask.TaskStatus.PENDING);
            task.setOwner(owner);
            task.setDueDate(today.plusDays(tt.getDefaultDaysToComplete() != null ? tt.getDefaultDaysToComplete() : 3));
            tasks.add(task);
        }

        taskRepository.saveAll(tasks);

        if (employee.getUser() != null) {
            notificationService.notifyUser(
                    employee.getUser(),
                    "Onboarding Checklist Assigned",
                    "Welcome! Your onboarding checklist (" + template.getName() + ") with " + tasks.size() + " tasks has been assigned.",
                    AppNotification.NotificationType.ONBOARDING,
                    "/onboarding/progress"
            );
        }
    }

    @Transactional(readOnly = true)
    public OnboardingProgressResponse getEmployeeProgress(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        List<OnboardingTask> tasks = taskRepository.findByEmployeeId(employeeId);
        return onboardingMapper.toProgressResponse(employee, tasks);
    }

    @Transactional
    public OnboardingTaskResponse updateTaskStatus(Long taskId, String statusStr) {
        OnboardingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingTask", "id", taskId));

        OnboardingTask.TaskStatus newStatus = OnboardingTask.TaskStatus.valueOf(statusStr.toUpperCase());
        task.setStatus(newStatus);
        if (newStatus == OnboardingTask.TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }

        OnboardingTask saved = taskRepository.save(task);
        return onboardingMapper.toTaskResponse(saved);
    }

    @Transactional
    public OnboardingTaskResponse addTaskToEmployee(Long employeeId, OnboardingTaskRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        OnboardingTask task = new OnboardingTask();
        task.setEmployee(employee);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getCategory() != null) {
            task.setCategory(OnboardingTemplateTask.TaskCategory.valueOf(request.getCategory().toUpperCase()));
        }
        if (request.getStatus() != null) {
            task.setStatus(OnboardingTask.TaskStatus.valueOf(request.getStatus().toUpperCase()));
        }
        if (request.getOwnerId() != null) {
            task.setOwner(employeeRepository.findById(request.getOwnerId()).orElse(null));
        }
        task.setDueDate(request.getDueDate() != null ? request.getDueDate() : LocalDate.now().plusDays(3));

        OnboardingTask saved = taskRepository.save(task);
        return onboardingMapper.toTaskResponse(saved);
    }
}
