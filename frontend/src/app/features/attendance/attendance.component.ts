import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { AttendanceService } from '../../core/services/attendance.service';
import { NotificationService } from '../../core/services/notification.service';
import { AttendanceRecord, Holiday } from '../../core/models/attendance.model';

@Component({
  selector: 'app-attendance',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule
  ],
  templateUrl: './attendance.component.html',
  styleUrls: ['./attendance.component.scss']
})
export class AttendanceComponent implements OnInit {
  todayAttendance = signal<AttendanceRecord | null>(null);
  monthlyRecords = signal<AttendanceRecord[]>([]);
  holidays = signal<Holiday[]>([]);

  selectedYear = new Date().getFullYear();
  selectedMonth = new Date().getMonth() + 1;
  currentTime = new Date();

  displayedColumns = ['date', 'checkInTime', 'checkOutTime', 'workingHours', 'status'];

  constructor(
    private attendanceService: AttendanceService,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    setInterval(() => this.currentTime = new Date(), 1000);
    this.loadTodayStatus();
    this.loadMonthlyAttendance();
    this.loadHolidays();
  }

  loadTodayStatus(): void {
    this.attendanceService.getTodayAttendance().subscribe(rec => {
      this.todayAttendance.set(rec);
    });
  }

  loadMonthlyAttendance(): void {
    this.attendanceService.getMonthlyAttendance(this.selectedYear, this.selectedMonth).subscribe(records => {
      this.monthlyRecords.set(records);
    });
  }

  loadHolidays(): void {
    this.attendanceService.getHolidays(this.selectedYear).subscribe(h => {
      this.holidays.set(h);
    });
  }

  punchCheckIn(): void {
    this.attendanceService.checkIn().subscribe({
      next: rec => {
        this.todayAttendance.set(rec);
        this.notify.success('Check-in recorded successfully!');
        this.loadMonthlyAttendance();
      },
      error: (err) => this.notify.error(err.error?.message || 'Check-in failed')
    });
  }

  punchCheckOut(): void {
    this.attendanceService.checkOut().subscribe({
      next: rec => {
        this.todayAttendance.set(rec);
        this.notify.success(`Check-out recorded! Total hours: ${rec.workingHours || 0} hrs`);
        this.loadMonthlyAttendance();
      },
      error: (err) => this.notify.error(err.error?.message || 'Check-out failed')
    });
  }
}
