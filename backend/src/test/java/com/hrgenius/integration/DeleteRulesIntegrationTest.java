package com.hrgenius.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.entity.Department;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.User;
import com.hrgenius.repository.DepartmentRepository;
import com.hrgenius.repository.EmployeeRepository;
import com.hrgenius.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deleting employees, departments and designations: history is kept, and records in use explain why
 * they can't be removed instead of failing with a 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class DeleteRulesIntegrationTest {

    private static final String STRONG = "Str0ng!Pass";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("Deleting an employee hides them, blocks their login and keeps the record")
    void deleteEmployeeKeepsHistory() throws Exception {
        String email = newSignup("10.3.0.1");
        Employee employee = employeeRepository.findByEmail(email).orElseThrow();
        String admin = token("admin", "Admin@123", "10.3.0.2");

        mockMvc.perform(delete("/employees/" + employee.getId()).header("Authorization", "Bearer " + admin))
                .andExpect(status().is2xxSuccessful());

        Employee after = employeeRepository.findById(employee.getId()).orElseThrow();
        assertThat(after.isDeleted()).isTrue();
        assertThat(after.getStatus()).isEqualTo(Employee.EmployeeStatus.EXITED);
        User login = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(login.isEnabled()).isFalse();

        mockMvc.perform(get("/employees").param("name", after.getEmployeeCode()).param("size", "50")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(post("/auth/login").with(ip("10.3.0.1")).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", email, "password", STRONG))))
                .andExpect(status().is4xxClientError());
        // A second delete is a clean 404
        mockMvc.perform(delete("/employees/" + employee.getId()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("You can't delete your own employee record")
    void cannotDeleteYourself() throws Exception {
        Long adminEmployeeId = employeeRepository.findByEmployeeCode("EMP001").orElseThrow().getId();
        mockMvc.perform(delete("/employees/" + adminEmployeeId)
                        .header("Authorization", "Bearer " + token("admin", "Admin@123", "10.3.0.3")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("your own")));
    }

    @Test
    @DisplayName("A department with employees explains why it can't be deleted; an empty one is deleted")
    void departmentDeleteRules() throws Exception {
        String admin = token("admin", "Admin@123", "10.3.0.4");
        Department engineering = departmentRepository.findByCode("ENG").orElseThrow();
        mockMvc.perform(delete("/departments/" + engineering.getId()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Engineering still has")));

        // Only a deleted employee left in it: the department can go, the employee's history stays
        Department temp = new Department();
        temp.setName("Temp " + UUID.randomUUID().toString().substring(0, 6));
        temp.setCode("T" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        temp = departmentRepository.save(temp);
        Employee leaver = employeeRepository.findByEmail(newSignup("10.3.0.5")).orElseThrow();
        leaver.setDepartment(temp);
        employeeRepository.save(leaver);

        mockMvc.perform(delete("/departments/" + temp.getId()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/employees/" + leaver.getId()).header("Authorization", "Bearer " + admin))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(delete("/departments/" + temp.getId()).header("Authorization", "Bearer " + admin))
                .andExpect(status().is2xxSuccessful());

        assertThat(departmentRepository.findById(temp.getId())).isEmpty();
        assertThat(employeeRepository.findById(leaver.getId())).isPresent();
    }

    @Test
    @DisplayName("A designation in use explains why it can't be deleted")
    void designationInUse() throws Exception {
        Long designationId = employeeRepository.findByEmployeeCode("EMP004").orElseThrow().getDesignation().getId();
        mockMvc.perform(delete("/designations/" + designationId)
                        .header("Authorization", "Bearer " + token("admin", "Admin@123", "10.3.0.6")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Change their designation first")));
    }

    private String newSignup(String clientIp) throws Exception {
        String email = "leaver." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        mockMvc.perform(post("/auth/register").with(ip(clientIp)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Lea", "lastName", "Ver", "email", email, "password", STRONG))))
                .andExpect(status().isCreated());
        return email;
    }

    private String token(String user, String password, String clientIp) throws Exception {
        String body = mockMvc.perform(post("/auth/login").with(ip(clientIp)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", user, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private static RequestPostProcessor ip(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
