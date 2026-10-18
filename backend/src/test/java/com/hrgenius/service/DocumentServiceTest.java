package com.hrgenius.service;

import com.hrgenius.config.FileStorageConfig;
import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.entity.Document;
import com.hrgenius.entity.Employee;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.repository.DocumentRepository;
import com.hrgenius.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private FileStorageConfig fileStorageConfig;

    @TempDir
    Path tempDir;

    private DocumentService documentService;
    private Employee employee;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, employeeRepository, fileStorageConfig);
        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("EMP001");
    }

    @Test
    void testUploadDocument_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(fileStorageConfig.getUploadPath()).thenReturn(tempDir);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(10L);
            return doc;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "Sample PDF content".getBytes()
        );

        EmployeeResponse.DocumentDto response = documentService.uploadDocument(1L, Document.DocumentType.RESUME, file);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("resume.pdf", response.getOriginalFileName());
        assertEquals("application/pdf", response.getContentType());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void testUploadDocument_RejectsInvalidExtension() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "executable binary".getBytes()
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                documentService.uploadDocument(1L, Document.DocumentType.OTHER, file)
        );
        assertTrue(ex.getMessage().contains("Invalid file extension"));
        verifyNoInteractions(fileStorageConfig, documentRepository);
    }

    @Test
    void testUploadDocument_RejectsEmptyFile() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                documentService.uploadDocument(1L, Document.DocumentType.OTHER, file)
        );
        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    @Test
    void testUploadDocument_RejectsOversizedFile() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeBytes
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                documentService.uploadDocument(1L, Document.DocumentType.OTHER, file)
        );
        assertTrue(ex.getMessage().contains("File size exceeds maximum allowed limit"));
    }

    @Test
    void testUploadDocument_PathTraversalSanitization() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(fileStorageConfig.getUploadPath()).thenReturn(tempDir);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(20L);
            return doc;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../../../evil_resume.pdf",
                "application/pdf",
                "Sample content".getBytes()
        );

        EmployeeResponse.DocumentDto response = documentService.uploadDocument(1L, Document.DocumentType.RESUME, file);

        assertNotNull(response);
        assertEquals("evil_resume.pdf", response.getOriginalFileName());
        assertFalse(response.getOriginalFileName().contains(".."));
    }

    @Test
    void testGetDocumentsByEmployee_Success() {
        when(employeeRepository.existsById(1L)).thenReturn(true);
        Document doc = new Document();
        doc.setId(10L);
        doc.setOriginalFileName("id_proof.png");
        doc.setContentType("image/png");
        doc.setFileSize(1024L);
        doc.setDocumentType(Document.DocumentType.ID_PROOF);

        when(documentRepository.findByEmployeeId(1L)).thenReturn(List.of(doc));

        List<EmployeeResponse.DocumentDto> list = documentService.getDocumentsByEmployee(1L);
        assertEquals(1, list.size());
        assertEquals("id_proof.png", list.get(0).getOriginalFileName());
    }

    @Test
    void testGetDocumentsByEmployee_NotFound() {
        when(employeeRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                documentService.getDocumentsByEmployee(999L)
        );
    }
}
