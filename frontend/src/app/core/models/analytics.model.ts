export interface CategoryCount {
  category: string;
  count: number;
}

export interface CategoryAmount {
  category: string;
  amount: number;
}

export interface MonthlyTrend {
  month: string;
  joinings: number;
  exits: number;
}

export interface AnalyticsDashboard {
  totalHeadcount: number;
  activeEmployees: number;
  attritionRate: number;
  averageTenureYears: number;
  monthlyPayrollTotal: number;
  openRequisitions: number;
  averagePerformanceRating: number;
  leaveUtilisationPercentage: number;
  headcountByDepartment: CategoryCount[];
  genderDistribution: CategoryCount[];
  recruitmentFunnel: CategoryCount[];
  payrollCostByDepartment: CategoryAmount[];
  performanceRatingDistribution: CategoryCount[];
  hiringVsExitTrend: MonthlyTrend[];
}
