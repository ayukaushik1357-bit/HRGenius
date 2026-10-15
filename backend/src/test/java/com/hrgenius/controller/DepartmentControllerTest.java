package com.hrgenius.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.dto.department.DepartmentRequest;
import com.hrgenius.dto.department.DepartmentResponse;
import com.hrgenius.security.CustomUserDetailsService;
import com.hrgenius.security.JwtAuthenticationFilter;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentService departmentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser
    void testGetAllDepartments() throws Exception {
        DepartmentResponse dept = DepartmentResponse.builder()
                .id(1L)
                .name("Engineering")
                .code("ENG")
                .description("Software Engineering")
                .build();

        when(departmentService.getAllDepartments()).thenReturn(List.of(dept));

        mockMvc.perform(get("/departments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Engineering"))
                .andExpect(jsonPath("$[0].code").value("ENG"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateDepartment() throws Exception {
        DepartmentRequest request = new DepartmentRequest();
        request.setName("Human Resources");
        request.setCode("HR");
        request.setDescription("HR Dept");

        DepartmentResponse response = DepartmentResponse.builder()
                .id(2L)
                .name("Human Resources")
                .code("HR")
                .description("HR Dept")
                .build();

        when(departmentService.createDepartment(any(DepartmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Human Resources"));
    }
}
