package com.hrgenius.mapper;

import com.hrgenius.dto.performance.*;
import com.hrgenius.entity.performance.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class PerformanceMapper {

    public ReviewCycleResponse toCycleResponse(ReviewCycle entity) {
        if (entity == null) return null;
        return ReviewCycleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public GoalResponse toGoalResponse(Goal entity) {
        if (entity == null) return null;
        return GoalResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .employeeName(entity.getEmployee() != null ? entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName() : null)
                .reviewCycleId(entity.getReviewCycle() != null ? entity.getReviewCycle().getId() : null)
                .reviewCycleName(entity.getReviewCycle() != null ? entity.getReviewCycle().getName() : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .weightage(entity.getWeightage())
                .progressPercentage(entity.getProgressPercentage())
                .build();
    }

    public PerformanceReviewResponse toReviewResponse(PerformanceReview entity) {
        if (entity == null) return null;
        return PerformanceReviewResponse.builder()
                .id(entity.getId())
                .reviewCycleId(entity.getReviewCycle() != null ? entity.getReviewCycle().getId() : null)
                .reviewCycleName(entity.getReviewCycle() != null ? entity.getReviewCycle().getName() : null)
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .employeeName(entity.getEmployee() != null ? entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName() : null)
                .employeeCode(entity.getEmployee() != null ? entity.getEmployee().getEmployeeCode() : null)
                .managerId(entity.getManager() != null ? entity.getManager().getId() : null)
                .managerName(entity.getManager() != null ? entity.getManager().getFirstName() + " " + entity.getManager().getLastName() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .selfRating(entity.getSelfRating())
                .selfComments(entity.getSelfComments())
                .managerRating(entity.getManagerRating())
                .managerComments(entity.getManagerComments())
                .finalRating(entity.getFinalRating())
                .submittedAt(entity.getSubmittedAt())
                .reviewedAt(entity.getReviewedAt())
                .peerFeedbacks(entity.getPeerFeedbacks() == null ? Collections.emptyList() :
                        entity.getPeerFeedbacks().stream().map(this::toPeerFeedbackResponse).collect(Collectors.toList()))
                .build();
    }

    public PeerFeedbackResponse toPeerFeedbackResponse(PeerFeedback entity) {
        if (entity == null) return null;
        return PeerFeedbackResponse.builder()
                .id(entity.getId())
                .performanceReviewId(entity.getPerformanceReview() != null ? entity.getPerformanceReview().getId() : null)
                .reviewerId(entity.getReviewer() != null ? entity.getReviewer().getId() : null)
                .reviewerName(entity.getReviewer() != null ? entity.getReviewer().getFirstName() + " " + entity.getReviewer().getLastName() : null)
                .feedback(entity.getFeedback())
                .rating(entity.getRating())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }
}
