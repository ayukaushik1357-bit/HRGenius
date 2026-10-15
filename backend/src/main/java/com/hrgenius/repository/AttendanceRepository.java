package com.hrgenius.repository;

import com.hrgenius.entity.attendance.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
    List<Attendance> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByDate(LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.id = :empId AND a.date >= :start AND a.date <= :end AND a.status = 'PRESENT'")
    long countPresentDays(@Param("empId") Long empId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.id = :empId AND a.date >= :start AND a.date <= :end AND a.status = 'HALF_DAY'")
    long countHalfDays(@Param("empId") Long empId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
