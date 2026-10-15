package com.hrgenius.repository;

import com.hrgenius.entity.performance.ReviewCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, Long> {
    Optional<ReviewCycle> findByName(String name);
}
