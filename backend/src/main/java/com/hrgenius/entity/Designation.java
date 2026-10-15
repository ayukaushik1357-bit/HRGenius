package com.hrgenius.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "designations")
@Getter
@Setter
@NoArgsConstructor
public class Designation extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String title;

    @Column(name = "designation_level")
    private Integer level;

    @Column(length = 500)
    private String description;
}
