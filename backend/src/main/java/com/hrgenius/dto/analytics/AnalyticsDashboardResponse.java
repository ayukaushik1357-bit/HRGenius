package com.hrgenius.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AnalyticsDashboardResponse {
    // Top KPI cards
    private long totalHeadcount;
    private long activeEmployees;
    private double attritionRate;
    private double averageTenureYears;
    private BigDecimal monthlyPayrollTotal;
    private long openRequisitions;
    private double averagePerformanceRating;
    private double leaveUtilisationPercentage;

    // Charts data
    private List<CategoryCount> headcountByDepartment;
    private List<CategoryCount> genderDistribution;
    private List<CategoryCount> recruitmentFunnel;
    private List<CategoryAmount> payrollCostByDepartment;
    private List<CategoryCount> performanceRatingDistribution;
    private List<MonthlyTrend> hiringVsExitTrend;

    @Getter
    @Builder
    public static class CategoryCount {
        private String category;
        private long count;
    }

    @Getter
    @Builder
    public static class CategoryAmount {
        private String category;
        private BigDecimal amount;
    }

    @Getter
    @Builder
    public static class MonthlyTrend {
        private String month;
        private long joinings;
        private long exits;
    }
}
