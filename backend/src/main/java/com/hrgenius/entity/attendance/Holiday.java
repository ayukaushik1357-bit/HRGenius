package com.hrgenius.entity.attendance;

import com.hrgenius.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "holidays")
@Getter
@Setter
@NoArgsConstructor
public class Holiday extends BaseEntity {

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @jakarta.persistence.Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    @Column(name = "is_optional", nullable = false)
    private boolean optional = false;
}
