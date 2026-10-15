package com.hrgenius.repository;

import com.hrgenius.entity.selfservice.ProfileUpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfileUpdateRequestRepository extends JpaRepository<ProfileUpdateRequest, Long> {
    List<ProfileUpdateRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<ProfileUpdateRequest> findByStatusOrderByCreatedAtDesc(ProfileUpdateRequest.RequestStatus status);
}
