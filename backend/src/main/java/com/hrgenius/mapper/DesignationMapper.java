package com.hrgenius.mapper;

import com.hrgenius.dto.designation.DesignationRequest;
import com.hrgenius.dto.designation.DesignationResponse;
import com.hrgenius.entity.Designation;
import org.springframework.stereotype.Component;

@Component
public class DesignationMapper {

    public DesignationResponse toResponse(Designation entity) {
        return DesignationResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .level(entity.getLevel())
                .description(entity.getDescription())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public Designation toEntity(DesignationRequest request) {
        Designation d = new Designation();
        d.setTitle(request.getTitle());
        d.setLevel(request.getLevel());
        d.setDescription(request.getDescription());
        return d;
    }

    public void updateEntity(Designation entity, DesignationRequest request) {
        entity.setTitle(request.getTitle());
        entity.setLevel(request.getLevel());
        entity.setDescription(request.getDescription());
    }
}
