package com.hrgenius.repository;

import com.hrgenius.entity.attendance.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    Optional<Holiday> findByHolidayDate(LocalDate holidayDate);
    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate startDate, LocalDate endDate);
    List<Holiday> findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(LocalDate holidayDate);
    boolean existsByHolidayDate(LocalDate holidayDate);
}
