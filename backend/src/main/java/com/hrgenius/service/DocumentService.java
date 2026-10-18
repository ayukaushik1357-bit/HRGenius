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

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB
    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(
            ".pdf", ".png", ".jpg", ".jpeg", ".doc", ".docx"
    );
    private static final java.util.Set<String> ALLOWED_CONTENT_TYPES = java.util.Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/octet-stream"
    );

    @Transactional
    public EmployeeResponse.DocumentDto uploadDocument(
            Long employeeId,
            Document.DocumentType documentType,
            MultipartFile file) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 10MB");
        }

        String rawOriginalFileName = file.getOriginalFilename();
        if (rawOriginalFileName == null || rawOriginalFileName.isBlank()) {
            throw new BadRequestException("File must have a valid name");
        }

        // Clean and sanitize original filename (never trust path separators or traversal characters)
        String cleanOriginalFileName = StringUtils.cleanPath(rawOriginalFileName);
        int lastSep = Math.max(cleanOriginalFileName.lastIndexOf('/'), cleanOriginalFileName.lastIndexOf('\\'));
        if (lastSep >= 0) {
            cleanOriginalFileName = cleanOriginalFileName.substring(lastSep + 1);
        }
        cleanOriginalFileName = cleanOriginalFileName.replace("..", "").trim();

        // Validate extension
        int dotIndex = cleanOriginalFileName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new BadRequestException("File must have a valid extension (.pdf, .png, .jpg, .jpeg, .doc, .docx)");
        }
        String fileExtension = cleanOriginalFileName.substring(dotIndex).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new BadRequestException("Invalid file extension: " + fileExtension + ". Allowed extensions: PDF, PNG, JPG, JPEG, DOC, DOCX");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file content type: " + contentType);
        }

        if (cleanOriginalFileName.isBlank()) {
            cleanOriginalFileName = "document" + fileExtension;
        }

        // Generate safe unique file name on disk - never trust client name for storage path
        String storedFileName = UUID.randomUUID() + fileExtension;

        Path uploadPath = fileStorageConfig.getUploadPath().normalize().toAbsolutePath();
        Path targetLocation = uploadPath.resolve(storedFileName).normalize().toAbsolutePath();

        if (!targetLocation.startsWith(uploadPath)) {
            throw new BadRequestException("Invalid file storage path / path traversal detected");
        }

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + cleanOriginalFileName + ". Please try again!", ex);
        }

        Document document = new Document();
        document.setFileName(storedFileName);
        document.setOriginalFileName(cleanOriginalFileName);
        document.setContentType(contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream");
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
            Path uploadPath = fileStorageConfig.getUploadPath().normalize().toAbsolutePath();
            Path filePath = uploadPath.resolve(document.getFileName()).normalize().toAbsolutePath();
            if (!filePath.startsWith(uploadPath)) {
                throw new BadRequestException("Invalid file path / path traversal detected");
            }
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
            Path uploadPath = fileStorageConfig.getUploadPath().normalize().toAbsolutePath();
            Path filePath = uploadPath.resolve(document.getFileName()).normalize().toAbsolutePath();
            if (filePath.startsWith(uploadPath)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ignored) {
            // log error if needed
        }

        documentRepository.delete(document);
    }
}
