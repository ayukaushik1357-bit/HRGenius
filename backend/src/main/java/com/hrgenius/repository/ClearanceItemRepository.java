package com.hrgenius.repository;

import com.hrgenius.entity.offboarding.ClearanceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClearanceItemRepository extends JpaRepository<ClearanceItem, Long> {
    List<ClearanceItem> findByExitRequestId(Long exitRequestId);
}
