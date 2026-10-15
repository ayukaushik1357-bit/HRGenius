import { Component, OnInit, AfterViewInit, ElementRef, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AnalyticsService } from '../../core/services/analytics.service';
import { DepartmentService } from '../../core/services/department.service';
import { AnalyticsDashboard } from '../../core/models/analytics.model';
import { Department } from '../../core/models/department.model';
import { Chart, registerables } from 'chart.js/auto';

Chart.register(...registerables);

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatSelectModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.scss']
})
export class AnalyticsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('headcountCanvas') headcountCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('trendCanvas') trendCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('genderCanvas') genderCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('funnelCanvas') funnelCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('ratingCanvas') ratingCanvas!: ElementRef<HTMLCanvasElement>;

  dashboardData: AnalyticsDashboard | null = null;
  departments: Department[] = [];
  selectedRole: string = 'HR_MANAGER';
  selectedDepartmentId?: number;
  loading: boolean = true;
  exportingExcel: boolean = false;
  exportingPdf: boolean = false;

  private charts: Chart[] = [];

  constructor(
    private analyticsService: AnalyticsService,
    private departmentService: DepartmentService
  ) {}

  ngOnInit(): void {
    this.loadDepartments();
    this.loadDashboard();
  }

  ngAfterViewInit(): void {
    if (this.dashboardData) {
      this.initCharts();
    }
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  loadDepartments(): void {
    this.departmentService.getAllDepartments().subscribe({
      next: (depts: Department[]) => (this.departments = depts),
      error: (err: any) => console.error('Failed to load departments', err)
    });
  }

  loadDashboard(): void {
    this.loading = true;
    this.destroyCharts();
    this.analyticsService.getDashboard(this.selectedRole, this.selectedDepartmentId).subscribe({
      next: (data: AnalyticsDashboard) => {
        this.dashboardData = data;
        this.loading = false;
        setTimeout(() => this.initCharts(), 100);
      },
      error: (err: any) => {
        console.error('Failed to load analytics dashboard', err);
        this.loading = false;
      }
    });
  }

  onFilterChange(): void {
    this.loadDashboard();
  }

  private destroyCharts(): void {
    this.charts.forEach((c) => c.destroy());
    this.charts = [];
  }

  private initCharts(): void {
    if (!this.dashboardData) return;
    this.destroyCharts();

    // 1. Headcount by Department (Bar)
    if (this.headcountCanvas?.nativeElement) {
      const labels = this.dashboardData.headcountByDepartment.map((c) => c.category);
      const data = this.dashboardData.headcountByDepartment.map((c) => c.count);
      const chart = new Chart(this.headcountCanvas.nativeElement, {
        type: 'bar',
        data: {
          labels,
          datasets: [{
            label: 'Headcount',
            data,
            backgroundColor: 'rgba(59, 130, 246, 0.75)',
            borderColor: '#3b82f6',
            borderWidth: 1.5,
            borderRadius: 6
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
        }
      });
      this.charts.push(chart);
    }

    // 2. Hiring vs Exit Trend (Line)
    if (this.trendCanvas?.nativeElement) {
      const labels = this.dashboardData.hiringVsExitTrend.map((t) => t.month);
      const joinings = this.dashboardData.hiringVsExitTrend.map((t) => t.joinings);
      const exits = this.dashboardData.hiringVsExitTrend.map((t) => t.exits);
      const chart = new Chart(this.trendCanvas.nativeElement, {
        type: 'line',
        data: {
          labels,
          datasets: [
            {
              label: 'New Joinings',
              data: joinings,
              borderColor: '#10b981',
              backgroundColor: 'rgba(16, 185, 129, 0.15)',
              tension: 0.35,
              fill: true
            },
            {
              label: 'Exits',
              data: exits,
              borderColor: '#ef4444',
              backgroundColor: 'rgba(239, 68, 68, 0.15)',
              tension: 0.35,
              fill: true
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
        }
      });
      this.charts.push(chart);
    }

    // 3. Gender Ratio (Doughnut)
    if (this.genderCanvas?.nativeElement) {
      const labels = this.dashboardData.genderDistribution.map((g) => g.category);
      const data = this.dashboardData.genderDistribution.map((g) => g.count);
      const chart = new Chart(this.genderCanvas.nativeElement, {
        type: 'doughnut',
        data: {
          labels,
          datasets: [{
            data,
            backgroundColor: ['#3b82f6', '#ec4899', '#8b5cf6'],
            borderWidth: 2
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { position: 'bottom' } }
        }
      });
      this.charts.push(chart);
    }

    // 4. Recruitment Funnel (Horizontal Bar)
    if (this.funnelCanvas?.nativeElement) {
      const labels = this.dashboardData.recruitmentFunnel.map((f) => f.category);
      const data = this.dashboardData.recruitmentFunnel.map((f) => f.count);
      const chart = new Chart(this.funnelCanvas.nativeElement, {
        type: 'bar',
        data: {
          labels,
          datasets: [{
            label: 'Candidates',
            data,
            backgroundColor: 'rgba(245, 158, 11, 0.75)',
            borderColor: '#f59e0b',
            borderWidth: 1.5,
            borderRadius: 6
          }]
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: { x: { beginAtZero: true, ticks: { precision: 0 } } }
        }
      });
      this.charts.push(chart);
    }

    // 5. Rating Distribution (Bar)
    if (this.ratingCanvas?.nativeElement) {
      const labels = this.dashboardData.performanceRatingDistribution.map((r) => r.category);
      const data = this.dashboardData.performanceRatingDistribution.map((r) => r.count);
      const chart = new Chart(this.ratingCanvas.nativeElement, {
        type: 'bar',
        data: {
          labels,
          datasets: [{
            label: 'Reviews Count',
            data,
            backgroundColor: 'rgba(139, 92, 246, 0.75)',
            borderColor: '#8b5cf6',
            borderWidth: 1.5,
            borderRadius: 6
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
        }
      });
      this.charts.push(chart);
    }
  }

  exportExcel(): void {
    this.exportingExcel = true;
    this.analyticsService.exportExcel().subscribe({
      next: (blob: Blob) => {
        this.downloadBlob(blob, `HRGenius_Analytics_${new Date().toISOString().slice(0, 10)}.xlsx`);
        this.exportingExcel = false;
      },
      error: () => (this.exportingExcel = false)
    });
  }

  exportPdf(): void {
    this.exportingPdf = true;
    this.analyticsService.exportPdf().subscribe({
      next: (blob: Blob) => {
        this.downloadBlob(blob, `HRGenius_Analytics_${new Date().toISOString().slice(0, 10)}.pdf`);
        this.exportingPdf = false;
      },
      error: () => (this.exportingPdf = false)
    });
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }
}
