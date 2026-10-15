import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { AuditService } from '../../core/services/audit.service';
import { AuditLog, AuditLogPage } from '../../core/models/audit.model';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatChipsModule
  ],
  templateUrl: './audit.component.html',
  styleUrls: ['./audit.component.scss']
})
export class AuditComponent implements OnInit {
  logs: AuditLog[] = [];
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  loading = true;

  displayedColumns = ['changedAt', 'entity', 'action', 'changedBy', 'values', 'ipAddress'];

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading = true;
    this.auditService.getAuditLogs(this.currentPage, this.pageSize).subscribe({
      next: (page: AuditLogPage) => {
        this.logs = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadLogs();
  }
}
