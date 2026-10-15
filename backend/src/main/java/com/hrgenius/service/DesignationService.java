package com.hrgenius.service;

import com.hrgenius.dto.designation.DesignationRequest;
import com.hrgenius.dto.designation.DesignationResponse;
import com.hrgenius.entity.Designation;
import com.hrgenius.exception.DuplicateResourceException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.DesignationMapper;
import com.hrgenius.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignationService {

    private final DesignationRepository designationRepository;
    private final DesignationMapper designationMapper;

    @Transactional(readOnly = true)
    public List<DesignationResponse> getAllDesignations() {
        return designationRepository.findAll().stream()
                .map(designationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DesignationResponse getDesignationById(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));
        return designationMapper.toResponse(designation);
    }

    @Transactional
    public DesignationResponse createDesignation(DesignationRequest request) {
        if (designationRepository.existsByTitle(request.getTitle())) {
            throw new DuplicateResourceException("Designation with title '" + request.getTitle() + "' already exists");
        }

        Designation designation = designationMapper.toEntity(request);
        Designation saved = designationRepository.save(designation);
        return designationMapper.toResponse(saved);
    }

    @Transactional
    public DesignationResponse updateDesignation(Long id, DesignationRequest request) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));

        designationRepository.findByTitle(request.getTitle()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Designation with title '" + request.getTitle() + "' already exists");
            }
        });

        designationMapper.updateEntity(designation, request);
        return designationMapper.toResponse(designation);
    }

    @Transactional
    public void deleteDesignation(Long id) {
        if (!designationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Designation", "id", id);
        }
        designationRepository.deleteById(id);
    }
}
