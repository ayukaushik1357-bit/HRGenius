package com.hrgenius.security;

import com.hrgenius.entity.Employee;
import com.hrgenius.repository.EmployeeRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Table-driven authorization tests covering every controller, using real access tokens for the
 * seeded demo users (h2 profile seeds them via H2DataSeeder):
 *
 *   admin     -> ADMIN + HR_MANAGER, employee EMP001
 *   hrmanager -> HR_MANAGER,         employee EMP002
 *   manager   -> MANAGER,            employee EMP003 (direct reports: EMP004, EMP005)
 *   employee  -> EMPLOYEE,           employee EMP004 (reports to EMP003)
 *   EMP006 reports to EMP001, so it is NOT in the manager's team.
 *
 * Paths may contain {EMPxxx} placeholders, replaced by that employee's database id.
 *
 * "ALLOW" asserts the request passed authorization (not 401/403); functional behaviour of each
 * endpoint is covered by the service/controller tests. "DENY" asserts exactly 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SecurityRulesParameterizedTest {

    private static final Pattern EMP_PLACEHOLDER = Pattern.compile("\\{(EMP\\d{3})}");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EmployeeRepository employeeRepository;

    static Stream<Arguments> rules() {
        return Stream.of(
                // ---- Analytics ----
                Arguments.of("GET", "/analytics/dashboard", "manager", "ALLOW"),
                Arguments.of("GET", "/analytics/dashboard", "employee", "DENY"),
                Arguments.of("GET", "/analytics/export/excel", "manager", "DENY"),

                // ---- Audit ----
                Arguments.of("GET", "/audit/logs", "hrmanager", "ALLOW"),
                Arguments.of("GET", "/audit/logs", "manager", "DENY"),

                // ---- Auth ----
                Arguments.of("GET", "/auth/me", "employee", "ALLOW"),

                // ---- Users (ADMIN only) ----
                Arguments.of("GET", "/users", "admin", "ALLOW"),
                Arguments.of("GET", "/users", "hrmanager", "DENY"),

                // ---- Departments / Designations ----
                Arguments.of("GET", "/departments", "employee", "ALLOW"),
                Arguments.of("DELETE", "/departments/999999", "hrmanager", "DENY"),
                Arguments.of("GET", "/designations", "employee", "ALLOW"),
                Arguments.of("DELETE", "/designations/999999", "hrmanager", "DENY"),

                // ---- Employees (role + self + team) ----
                Arguments.of("GET", "/employees", "manager", "ALLOW"),
                Arguments.of("GET", "/employees", "employee", "DENY"),
                Arguments.of("GET", "/employees/{EMP004}", "employee", "ALLOW"),   // self
                Arguments.of("GET", "/employees/{EMP005}", "employee", "DENY"),    // IDOR: colleague
                Arguments.of("GET", "/employees/{EMP004}", "manager", "ALLOW"),    // direct report
                Arguments.of("GET", "/employees/{EMP006}", "manager", "DENY"),     // not in team
                Arguments.of("DELETE", "/employees/{EMP006}", "hrmanager", "DENY"),

                // ---- Documents ----
                Arguments.of("GET", "/employees/{EMP004}/documents", "employee", "ALLOW"),
                Arguments.of("GET", "/employees/{EMP005}/documents", "employee", "DENY"),
                Arguments.of("GET", "/employees/{EMP004}/documents", "manager", "DENY"),

                // ---- Attendance ----
                Arguments.of("GET", "/attendance/monthly", "employee", "ALLOW"),
                Arguments.of("GET", "/attendance/monthly?employeeId={EMP005}", "employee", "DENY"),
                Arguments.of("GET", "/attendance/monthly?employeeId={EMP004}", "manager", "ALLOW"),
                Arguments.of("GET", "/attendance/monthly?employeeId={EMP006}", "manager", "DENY"),
                Arguments.of("GET", "/attendance/daily", "employee", "DENY"),
                Arguments.of("GET", "/attendance/holidays", "employee", "ALLOW"),

                // ---- Leaves ----
                Arguments.of("GET", "/leaves/my", "employee", "ALLOW"),
                Arguments.of("GET", "/leaves/balance", "employee", "ALLOW"),
                Arguments.of("GET", "/leaves/balance?employeeId={EMP005}", "employee", "DENY"),
                Arguments.of("GET", "/leaves/balance?employeeId={EMP004}", "manager", "ALLOW"),
                Arguments.of("GET", "/leaves/balance?employeeId={EMP006}", "manager", "DENY"),
                Arguments.of("GET", "/leaves", "hrmanager", "ALLOW"),
                Arguments.of("GET", "/leaves", "manager", "DENY"),
                Arguments.of("GET", "/leaves/pending-approvals", "employee", "DENY"),

                // ---- Notifications ----
                Arguments.of("GET", "/notifications", "employee", "ALLOW"),
                Arguments.of("PATCH", "/notifications/999999/read", "employee", "DENY"),

                // ---- Offboarding ----
                Arguments.of("GET", "/offboarding/my-requests", "employee", "ALLOW"),
                Arguments.of("GET", "/offboarding/requests", "manager", "ALLOW"),
                Arguments.of("GET", "/offboarding/requests", "employee", "DENY"),

                // ---- Onboarding ----
                Arguments.of("GET", "/onboarding/templates", "hrmanager", "ALLOW"),
                Arguments.of("GET", "/onboarding/templates", "manager", "DENY"),
                Arguments.of("GET", "/onboarding/employees/{EMP005}/progress", "employee", "DENY"),
                Arguments.of("GET", "/onboarding/employees/{EMP006}/progress", "manager", "DENY"),

                // ---- Payroll ----
                Arguments.of("GET", "/payroll/runs", "hrmanager", "ALLOW"),
                Arguments.of("GET", "/payroll/runs", "employee", "DENY"),
                Arguments.of("GET", "/payroll/my-payslips", "employee", "ALLOW"),
                Arguments.of("GET", "/payroll/employee/{EMP004}/payslips", "manager", "DENY"),
                Arguments.of("GET", "/payroll/salary-structure/{EMP004}", "employee", "DENY"),

                // ---- Performance ----
                Arguments.of("GET", "/performance/cycles", "employee", "ALLOW"),
                Arguments.of("GET", "/performance/reviews/team", "manager", "ALLOW"),
                Arguments.of("GET", "/performance/reviews/team", "employee", "DENY"),
                Arguments.of("GET", "/performance/reviews/cycle/1", "manager", "DENY"),

                // ---- Recruitment ----
                Arguments.of("GET", "/recruitment/jobs", "employee", "ALLOW"),
                Arguments.of("GET", "/recruitment/candidates", "hrmanager", "ALLOW"),
                Arguments.of("GET", "/recruitment/candidates", "employee", "DENY"),
                Arguments.of("GET", "/recruitment/candidates", "manager", "DENY"),

                // ---- Self-service portal ----
                Arguments.of("GET", "/portal/dashboard", "employee", "ALLOW"),
                Arguments.of("GET", "/portal/profile-updates", "hrmanager", "ALLOW"),
                Arguments.of("GET", "/portal/profile-updates", "employee", "DENY")
        );
    }

    @ParameterizedTest(name = "[{index}] {2} {0} {1} -> {3}")
    @MethodSource("rules")
    void enforcesRule(String method, String pathTemplate, String username, String expectation) throws Exception {
        String path = resolveEmployeeIds(pathTemplate);
        String token = jwtTokenProvider.generateAccessTokenFromUsername(username, List.of());

        MvcResult result = mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        int status = result.getResponse().getStatus();

        if ("DENY".equals(expectation)) {
            assertThat(status)
                    .as("%s %s as %s must be forbidden", method, path, username)
                    .isEqualTo(403);
        } else {
            assertThat(status)
                    .as("%s %s as %s must pass authorization (body: %s)",
                            method, path, username, result.getResponse().getContentAsString())
                    .isNotIn(401, 403);
        }
    }

    private String resolveEmployeeIds(String template) {
        Matcher m = EMP_PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Long id = employeeRepository.findByEmployeeCode(m.group(1))
                    .map(Employee::getId)
                    .orElseThrow(() -> new IllegalStateException("Seeded employee missing: " + m.group(1)));
            m.appendReplacement(sb, String.valueOf(id));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
