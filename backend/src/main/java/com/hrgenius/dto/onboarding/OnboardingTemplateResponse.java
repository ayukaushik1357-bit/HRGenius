package com.hrgenius.dto.onboarding;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OnboardingTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private List<TemplateTaskDto> tasks;

    @Getter
    @Builder
    public static class TemplateTaskDto {
        private Long id;
        private String title;
        private String description;
        private String category;
        private Integer defaultDaysToComplete;
    }
}
