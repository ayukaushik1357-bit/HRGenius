package com.hrgenius.service;

import com.hrgenius.config.FileStorageConfig;
import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.entity.Document;
import com.hrgenius.entity.Employee;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.repository.DocumentRepository;
import com.hrgenius.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final FileStorageConfig fileStorageConfig;

    @Transactional
    public EmployeeResponse.DocumentDto uploadDocument(
            Long employeeId,
            Document.DocumentType documentType,
            MultipartFile file) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");

        // Generate unique file name on disk
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            fileExtension = originalFileName.substring(dotIndex);
        }
        String storedFileName = UUID.randomUUID() + fileExtension;

        Path targetLocation = fileStorageConfig.getUploadPath().resolve(storedFileName);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }

        Document document = new Document();
        document.setFileName(storedFileName);
        document.setOriginalFileName(originalFileName);
        document.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        document.setFileSize(file.getSize());
        document.setFilePath(targetLocation.toString());
        document.setDocumentType(documentType);
        document.setEmployee(employee);

        Document saved = documentRepository.save(document);

        return EmployeeResponse.DocumentDto.builder()
                .id(saved.getId())
                .originalFileName(saved.getOriginalFileName())
                .contentType(saved.getContentType())
                .fileSize(saved.getFileSize())
                .documentType(saved.getDocumentType().name())
                .uploadedAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse.DocumentDto> getDocumentsByEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", "id", employeeId);
        }

        return documentRepository.findByEmployeeId(employeeId).stream()
                .map(d -> EmployeeResponse.DocumentDto.builder()
                        .id(d.getId())
                        .originalFileName(d.getOriginalFileName())
                        .contentType(d.getContentType())
                        .fileSize(d.getFileSize())
                        .documentType(d.getDocumentType().name())
                        .uploadedAt(d.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public Document getDocumentMetadata(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
    }

    public Resource loadFileAsResource(Document document) {
        try {
            Path filePath = fileStorageConfig.getUploadPath().resolve(document.getFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on disk: " + document.getOriginalFileName());
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File path error: " + document.getOriginalFileName());
        }
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        try {
            Path filePath = fileStorageConfig.getUploadPath().resolve(document.getFileName()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // log error if needed
        }

        documentRepository.delete(document);
    }
}
