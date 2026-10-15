package com.hrgenius.service;

import com.hrgenius.dto.attendance.HolidayRequest;
import com.hrgenius.dto.attendance.HolidayResponse;
import com.hrgenius.entity.attendance.Holiday;
import com.hrgenius.exception.DuplicateResourceException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.AttendanceMapper;
import com.hrgenius.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final AttendanceMapper attendanceMapper;

    @Transactional(readOnly = true)
    public List<HolidayResponse> getHolidaysForYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end).stream()
                .map(attendanceMapper::toHolidayResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public HolidayResponse createHoliday(HolidayRequest request) {
        if (holidayRepository.findByHolidayDate(request.getHolidayDate()).isPresent()) {
            throw new DuplicateResourceException("A holiday is already scheduled on " + request.getHolidayDate());
        }
        Holiday holiday = attendanceMapper.toHolidayEntity(request);
        Holiday saved = holidayRepository.save(holiday);
        return attendanceMapper.toHolidayResponse(saved);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", "id", id));
        holidayRepository.delete(holiday);
    }
}
