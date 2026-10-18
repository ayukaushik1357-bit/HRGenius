package com.hrgenius.service;

import com.hrgenius.dto.performance.*;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.notification.AppNotification;
import com.hrgenius.entity.performance.*;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.PerformanceMapper;
import com.hrgenius.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final ReviewCycleRepository reviewCycleRepository;
    private final GoalRepository goalRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final PeerFeedbackRepository peerFeedbackRepository;
    private final EmployeeRepository employeeRepository;
    private final PerformanceMapper performanceMapper;
    private final NotificationService notificationService;

    // --- Review Cycles ---
    @Transactional(readOnly = true)
    public List<ReviewCycleResponse> getAllCycles() {
        return reviewCycleRepository.findAll().stream()
                .map(performanceMapper::toCycleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewCycleResponse createCycle(ReviewCycleRequest request) {
        ReviewCycle cycle = new ReviewCycle();
        cycle.setName(request.getName());
        cycle.setStartDate(request.getStartDate());
        cycle.setEndDate(request.getEndDate());
        cycle.setStatus(ReviewCycle.CycleStatus.valueOf(request.getStatus().toUpperCase()));
        ReviewCycle saved = reviewCycleRepository.save(cycle);

        // Auto-initialize PerformanceReview records for all active employees
        List<Employee> activeEmps = employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == Employee.EmployeeStatus.ACTIVE)
                .collect(Collectors.toList());

        for (Employee emp : activeEmps) {
            PerformanceReview review = new PerformanceReview();
            review.setReviewCycle(saved);
            review.setEmployee(emp);
            review.setManager(emp.getReportingManager());
            review.setStatus(PerformanceReview.ReviewStatus.NOT_STARTED);
            performanceReviewRepository.save(review);
        }

        return performanceMapper.toCycleResponse(saved);
    }

    // --- Goals / KRAs ---
    @Transactional(readOnly = true)
    public List<GoalResponse> getEmployeeGoals(Long employeeId, Long cycleId) {
        List<Goal> goals = (cycleId != null) ?
                goalRepository.findByEmployeeIdAndReviewCycleId(employeeId, cycleId) :
                goalRepository.findByEmployeeId(employeeId);
        return goals.stream().map(performanceMapper::toGoalResponse).collect(Collectors.toList());
    }

    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));
        ReviewCycle cycle = reviewCycleRepository.findById(request.getReviewCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", "id", request.getReviewCycleId()));

        Goal goal = new Goal();
        goal.setEmployee(employee);
        goal.setReviewCycle(cycle);
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setWeightage(request.getWeightage());
        goal.setProgressPercentage(request.getProgressPercentage() != null ? request.getProgressPercentage() : 0.0);

        Goal saved = goalRepository.save(goal);
        return performanceMapper.toGoalResponse(saved);
    }

    @Transactional
    public GoalResponse updateGoalProgress(Long goalId, Double progressPercentage) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId));
        goal.setProgressPercentage(Math.max(0.0, Math.min(100.0, progressPercentage)));
        Goal saved = goalRepository.save(goal);
        return performanceMapper.toGoalResponse(saved);
    }

    // --- Performance Reviews ---
    @Transactional(readOnly = true)
    public PerformanceReviewResponse getReview(Long cycleId, Long employeeId) {
        PerformanceReview review = performanceReviewRepository.findByReviewCycleIdAndEmployeeId(cycleId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("PerformanceReview", "employeeId", employeeId));
        return performanceMapper.toReviewResponse(review);
    }

    @Transactional(readOnly = true)
    public List<PerformanceReviewResponse> getReviewsForManager(Long managerId) {
        return performanceReviewRepository.findByManagerId(managerId).stream()
                .map(performanceMapper::toReviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PerformanceReviewResponse> getReviewsForCycle(Long cycleId) {
        return performanceReviewRepository.findByReviewCycleId(cycleId).stream()
                .map(performanceMapper::toReviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PerformanceReviewResponse submitSelfReview(Long reviewId, Double selfRating, String comments) {
        PerformanceReview review = performanceReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("PerformanceReview", "id", reviewId));

        review.setSelfRating(selfRating);
        review.setSelfComments(comments);
        review.setStatus(PerformanceReview.ReviewStatus.MANAGER_REVIEW);
        review.setSubmittedAt(LocalDateTime.now());

        PerformanceReview saved = performanceReviewRepository.save(review);

        // Notify manager
        if (review.getManager() != null && review.getManager().getUser() != null) {
            notificationService.notifyUser(
                    review.getManager().getUser(),
                    "Self-Review Submitted",
                    review.getEmployee().getFirstName() + " " + review.getEmployee().getLastName() +
                            " has submitted self-review for cycle " + review.getReviewCycle().getName(),
                    AppNotification.NotificationType.GENERAL,
                    "/performance/reviews"
            );
        }

        return performanceMapper.toReviewResponse(saved);
    }

    @Transactional
    public PerformanceReviewResponse submitManagerReview(Long reviewId, Double managerRating, String comments) {
        PerformanceReview review = performanceReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("PerformanceReview", "id", reviewId));

        review.setManagerRating(managerRating);
        review.setManagerComments(comments);

        // Calculate final rating based on manager evaluation and weighted goal completion
        List<Goal> goals = goalRepository.findByEmployeeIdAndReviewCycleId(
                review.getEmployee().getId(), review.getReviewCycle().getId());

        double totalWeight = goals.stream().mapToDouble(Goal::getWeightage).sum();
        double weightedGoalScore = 0.0;
        if (totalWeight > 0) {
            weightedGoalScore = goals.stream()
                    .mapToDouble(g -> (g.getProgressPercentage() / 100.0) * (g.getWeightage() / totalWeight) * 5.0)
                    .sum();
        }

        double finalScore = (managerRating != null) ?
                (totalWeight > 0 ? (managerRating * 0.6) + (weightedGoalScore * 0.4) : managerRating) :
                (totalWeight > 0 ? weightedGoalScore : 3.0);

        review.setFinalRating(Math.round(finalScore * 100.0) / 100.0);
        review.setStatus(PerformanceReview.ReviewStatus.COMPLETED);
        review.setReviewedAt(LocalDateTime.now());

        PerformanceReview saved = performanceReviewRepository.save(review);

        // Notify employee
        if (review.getEmployee().getUser() != null) {
            notificationService.notifyUser(
                    review.getEmployee().getUser(),
                    "Performance Review Completed",
                    "Your performance appraisal for " + review.getReviewCycle().getName() +
                            " is finalized with rating: " + review.getFinalRating() + "/5.0",
                    AppNotification.NotificationType.GENERAL,
                    "/performance/goals"
            );
        }

        return performanceMapper.toReviewResponse(saved);
    }

    // --- Peer Feedback 360 ---
    @Transactional
    public PeerFeedbackResponse submitPeerFeedback(Long reviewerId, PeerFeedbackRequest request) {
        PerformanceReview review = performanceReviewRepository.findById(request.getPerformanceReviewId())
                .orElseThrow(() -> new ResourceNotFoundException("PerformanceReview", "id", request.getPerformanceReviewId()));
        if (review.getEmployee() != null && review.getEmployee().getId().equals(reviewerId)) {
            throw new BadRequestException("Employees cannot submit peer feedback for themselves");
        }
        Employee reviewer = employeeRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee (Reviewer)", "id", reviewerId));

        PeerFeedback feedback = new PeerFeedback();
        feedback.setPerformanceReview(review);
        feedback.setReviewer(reviewer);
        feedback.setFeedback(request.getFeedback());
        feedback.setRating(request.getRating());
        feedback.setSubmittedAt(LocalDateTime.now());

        PeerFeedback saved = peerFeedbackRepository.save(feedback);
        return performanceMapper.toPeerFeedbackResponse(saved);
    }
}
