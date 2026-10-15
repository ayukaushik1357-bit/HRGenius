package com.hrgenius.service;

import com.hrgenius.dto.performance.GoalRequest;
import com.hrgenius.dto.performance.GoalResponse;
import com.hrgenius.dto.performance.PerformanceReviewResponse;
import com.hrgenius.dto.performance.ReviewCycleRequest;
import com.hrgenius.dto.performance.ReviewCycleResponse;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.performance.Goal;
import com.hrgenius.entity.performance.PerformanceReview;
import com.hrgenius.entity.performance.ReviewCycle;
import com.hrgenius.mapper.PerformanceMapper;
import com.hrgenius.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    private ReviewCycleRepository reviewCycleRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private PerformanceReviewRepository performanceReviewRepository;
    @Mock
    private PeerFeedbackRepository peerFeedbackRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PerformanceMapper performanceMapper;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PerformanceService performanceService;

    private Employee employee;
    private Employee manager;
    private ReviewCycle reviewCycle;
    private PerformanceReview review;

    @BeforeEach
    void setUp() {
        manager = new Employee();
        manager.setId(10L);
        manager.setFirstName("Vikram");
        manager.setLastName("Manager");

        employee = new Employee();
        employee.setId(20L);
        employee.setFirstName("Rohan");
        employee.setLastName("Developer");
        employee.setReportingManager(manager);

        reviewCycle = new ReviewCycle();
        reviewCycle.setId(1L);
        reviewCycle.setName("Annual Cycle 2025");
        reviewCycle.setStatus(ReviewCycle.CycleStatus.ACTIVE);

        review = new PerformanceReview();
        review.setId(100L);
        review.setEmployee(employee);
        review.setManager(manager);
        review.setReviewCycle(reviewCycle);
        review.setStatus(PerformanceReview.ReviewStatus.NOT_STARTED);
    }

    @Test
    void testCreateCycle() {
        ReviewCycleRequest req = new ReviewCycleRequest();
        req.setName("Q1 2025");
        req.setStartDate(LocalDate.of(2025, 1, 1));
        req.setEndDate(LocalDate.of(2025, 3, 31));
        req.setStatus("ACTIVE");

        when(reviewCycleRepository.save(any(ReviewCycle.class))).thenReturn(reviewCycle);
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(performanceMapper.toCycleResponse(any(ReviewCycle.class)))
                .thenReturn(ReviewCycleResponse.builder().id(1L).name("Q1 2025").build());

        ReviewCycleResponse res = performanceService.createCycle(req);
        assertNotNull(res);
        assertEquals(1L, res.getId());
        verify(performanceReviewRepository, times(1)).save(any(PerformanceReview.class));
    }

    @Test
    void testUpdateGoalProgress() {
        Goal goal = new Goal();
        goal.setId(50L);
        goal.setProgressPercentage(40.0);

        when(goalRepository.findById(50L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenReturn(goal);
        when(performanceMapper.toGoalResponse(any(Goal.class)))
                .thenReturn(GoalResponse.builder().id(50L).progressPercentage(80.0).build());

        GoalResponse res = performanceService.updateGoalProgress(50L, 80.0);
        assertNotNull(res);
        assertEquals(80.0, res.getProgressPercentage());
    }

    @Test
    void testSubmitManagerReviewCalculatesWeightedScore() {
        review.setStatus(PerformanceReview.ReviewStatus.MANAGER_REVIEW);
        review.setSelfRating(4.0);

        Goal g1 = new Goal();
        g1.setWeightage(50.0);
        g1.setProgressPercentage(100.0); // full 5.0 score

        Goal g2 = new Goal();
        g2.setWeightage(50.0);
        g2.setProgressPercentage(50.0); // 2.5 score
        // Goal average score = (5.0 * 0.5) + (2.5 * 0.5) = 3.75

        when(performanceReviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(goalRepository.findByEmployeeIdAndReviewCycleId(20L, 1L)).thenReturn(List.of(g1, g2));
        when(performanceReviewRepository.save(any(PerformanceReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(performanceMapper.toReviewResponse(any(PerformanceReview.class))).thenAnswer(inv -> {
            PerformanceReview pr = inv.getArgument(0);
            return PerformanceReviewResponse.builder()
                    .id(pr.getId())
                    .finalRating(pr.getFinalRating())
                    .status(pr.getStatus().name())
                    .build();
        });

        // Manager rating = 4.0
        // Weighted = 4.0 * 0.6 + 3.75 * 0.4 = 2.4 + 1.5 = 3.9
        PerformanceReviewResponse res = performanceService.submitManagerReview(100L, 4.0, "Great contribution");

        assertNotNull(res);
        assertEquals("COMPLETED", res.getStatus());
        assertEquals(3.9, res.getFinalRating());
    }
}
