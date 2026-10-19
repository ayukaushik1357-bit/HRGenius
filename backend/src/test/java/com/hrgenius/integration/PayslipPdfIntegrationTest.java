package com.hrgenius.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrgenius.entity.payroll.Payslip;
import com.hrgenius.repository.PayslipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Payslip PDF download, which reads lazy employee data outside the web request's session. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class PayslipPdfIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PayslipRepository payslipRepository;

    @Test
    @DisplayName("HR can download any payslip as a PDF")
    void downloadPayslipPdf() throws Exception {
        Payslip payslip = payslipRepository.findAll().stream().findFirst().orElseThrow();
        String body = mockMvc.perform(post("/auth/login").with(r -> { r.setRemoteAddr("10.4.0.1"); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "hrmanager", "password", "Hr@123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("accessToken").asText();

        byte[] pdf = mockMvc.perform(get("/payroll/payslips/" + payslip.getId() + "/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
