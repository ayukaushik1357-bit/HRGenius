package com.hrgenius.repository;

import com.hrgenius.entity.leave.LeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    List<LeaveApplication> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<LeaveApplication> findByStatusOrderByCreatedAtDesc(LeaveApplication.LeaveStatus status);

    @Query("SELECT l FROM LeaveApplication l WHERE l.employee.reportingManager.id = :managerId AND l.status = 'PENDING'")
    List<LeaveApplication> findPendingByManagerId(@Param("managerId") Long managerId);

    @Query("SELECT COUNT(l) FROM LeaveApplication l " +
           "WHERE l.employee.id = :empId " +
           "AND l.status <> 'REJECTED' " +
           "AND l.startDate <= :endDate " +
           "AND l.endDate >= :startDate " +
           "AND (:excludeId IS NULL OR l.id <> :excludeId)")
    long countOverlappingLeaves(
            @Param("empId") Long empId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COALESCE(SUM(l.numberOfDays), 0) FROM LeaveApplication l " +
           "WHERE l.employee.id = :empId " +
           "AND l.status = 'APPROVED' " +
           "AND l.leaveType = 'UNPAID' " +
           "AND l.startDate <= :endDate " +
           "AND l.endDate >= :startDate")
    Double sumUnpaidLeaveDays(
            @Param("empId") Long empId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
