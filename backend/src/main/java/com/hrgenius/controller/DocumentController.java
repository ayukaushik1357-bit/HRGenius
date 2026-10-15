package com.hrgenius.controller;

import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.entity.Document;
import com.hrgenius.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Employee document upload and download APIs")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/employees/{employeeId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Upload document for an employee (ADMIN or HR_MANAGER)")
    public ResponseEntity<EmployeeResponse.DocumentDto> uploadDocument(
            @PathVariable Long employeeId,
            @RequestParam("documentType") Document.DocumentType documentType,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(documentService.uploadDocument(employeeId, documentType, file));
    }

    @GetMapping("/employees/{employeeId}/documents")
    @Operation(summary = "Get all documents metadata for an employee")
    public ResponseEntity<List<EmployeeResponse.DocumentDto>> getEmployeeDocuments(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(documentService.getDocumentsByEmployee(employeeId));
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "Download an uploaded document file")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        Document document = documentService.getDocumentMetadata(documentId);
        Resource resource = documentService.loadFileAsResource(document);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Delete an uploaded document (ADMIN or HR_MANAGER)")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }
}
