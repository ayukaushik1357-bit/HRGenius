package com.hrgenius.dto.attendance;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class HolidayResponse {
    private Long id;
    private LocalDate holidayDate;
    private String name;
    private String description;
    private boolean optional;
}
