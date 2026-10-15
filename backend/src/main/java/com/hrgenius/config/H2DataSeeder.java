package com.hrgenius.config;

import com.hrgenius.entity.*;
import com.hrgenius.entity.attendance.Holiday;
import com.hrgenius.entity.leave.LeaveBalance;
import com.hrgenius.entity.onboarding.OnboardingTemplate;
import com.hrgenius.entity.onboarding.OnboardingTemplateTask;
import com.hrgenius.entity.payroll.SalaryStructure;
import com.hrgenius.entity.recruitment.JobPosting;
import com.hrgenius.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Seeds initial data when running with the H2 profile.
 */
@Component
@Profile("h2")
@RequiredArgsConstructor
@Slf4j
public class H2DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeRepository employeeRepository;
    private final JobPostingRepository jobPostingRepository;
    private final OnboardingTemplateRepository onboardingTemplateRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Seeding H2 database...");

        // Roles
        Role adminRole = createRole(Role.RoleName.ADMIN);
        Role hrRole = createRole(Role.RoleName.HR_MANAGER);
        createRole(Role.RoleName.MANAGER);
        createRole(Role.RoleName.EMPLOYEE);

        // Admin user
        User admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setEmail("admin@hrgenius.com");
            u.setPassword(passwordEncoder.encode("Admin@123"));
            u.setEnabled(true);
            u.setRoles(Set.of(adminRole, hrRole));
            return userRepository.save(u);
        });

        // Sample departments
        Department engDept = createDepartment("Engineering", "ENG", "Software Engineering");
        Department hrDept = createDepartment("Human Resources", "HR", "Human Resources Department");
        createDepartment("Finance", "FIN", "Finance and Accounting");
        createDepartment("Marketing", "MKT", "Marketing Department");

        // Sample designations
        Designation sseDesig = createDesignation("Senior Software Engineer", 4, "Senior IC");
        createDesignation("Software Engineer", 3, "Individual contributor");
        createDesignation("Engineering Manager", 5, "People manager");
        createDesignation("HR Executive", 3, "HR operations");
        createDesignation("Director", 7, "Department director");

        // Admin employee profile
        Employee adminEmp = employeeRepository.findByEmail(admin.getEmail()).orElseGet(() -> {
            Employee e = new Employee();
            e.setEmployeeCode("EMP001");
            e.setFirstName("Alex");
            e.setLastName("Admin");
            e.setEmail(admin.getEmail());
            e.setPhone("+91 9876543210");
            e.setJoiningDate(LocalDate.of(2025, 1, 1));
            e.setDepartment(hrDept);
            e.setDesignation(sseDesig);
            e.setUser(admin);
            e.setSalaryCTC(BigDecimal.valueOf(1800000.00));
            return employeeRepository.save(e);
        });

        // Sample Salary Structure for EMP001
        if (salaryStructureRepository.findByEmployeeId(adminEmp.getId()).isEmpty()) {
            SalaryStructure ss = new SalaryStructure();
            ss.setEmployee(adminEmp);
            ss.setBasicSalary(BigDecimal.valueOf(75000.00));
            ss.setHra(BigDecimal.valueOf(37500.00));
            ss.setSpecialAllowance(BigDecimal.valueOf(30000.00));
            ss.setOtherAllowances(BigDecimal.valueOf(7500.00));
            ss.setEffectiveFrom(LocalDate.of(2025, 1, 1));
            salaryStructureRepository.save(ss);
        }

        // Leave Balance for EMP001
        int currentYear = LocalDate.now().getYear();
        if (leaveBalanceRepository.findByEmployeeIdAndYear(adminEmp.getId(), currentYear).isEmpty()) {
            LeaveBalance lb = new LeaveBalance();
            lb.setEmployee(adminEmp);
            lb.setYear(currentYear);
            lb.setCasualQuota(12.0);
            lb.setCasualUsed(1.0);
            lb.setSickQuota(10.0);
            lb.setSickUsed(0.0);
            lb.setEarnedQuota(15.0);
            lb.setEarnedUsed(2.0);
            leaveBalanceRepository.save(lb);
        }

        // Sample Job Posting
        if (jobPostingRepository.count() == 0) {
            JobPosting jp = new JobPosting();
            jp.setTitle("Senior Full Stack Java & Angular Engineer");
            jp.setDescription("Leading development on enterprise human capital management solutions.");
            jp.setRequirements("5+ years of Java Spring Boot, Hibernate, Angular 17+, Oracle SQL.");
            jp.setLocation("Bengaluru, India (Hybrid)");
            jp.setMinExperienceYears(5);
            jp.setMaxExperienceYears(10);
            jp.setOpenings(3);
            jp.setStatus(JobPosting.JobPostingStatus.OPEN);
            jp.setDepartment(engDept);
            jp.setDesignation(sseDesig);
            jobPostingRepository.save(jp);
        }

        // Sample Onboarding Template
        if (onboardingTemplateRepository.count() == 0) {
            OnboardingTemplate t = new OnboardingTemplate();
            t.setName("Standard Technical Onboarding");
            t.setDescription("Standard induction and setup for engineering joiners.");

            OnboardingTemplateTask task1 = new OnboardingTemplateTask();
            task1.setTitle("Submit Identity & Education Documents");
            task1.setDescription("Upload Aadhaar/PAN, degree certificates, and previous experience letters.");
            task1.setCategory(OnboardingTemplateTask.TaskCategory.DOCUMENTS);
            task1.setDefaultDaysToComplete(2);
            t.addTask(task1);

            OnboardingTemplateTask task2 = new OnboardingTemplateTask();
            task2.setTitle("Laptop & Dev Environment Setup");
            task2.setDescription("Provision MacBook Pro, configure VPN, git credentials, and IDE.");
            task2.setCategory(OnboardingTemplateTask.TaskCategory.IT_SETUP);
            task2.setDefaultDaysToComplete(3);
            t.addTask(task2);

            OnboardingTemplateTask task3 = new OnboardingTemplateTask();
            task3.setTitle("Engineering Induction & Architecture Walkthrough");
            task3.setDescription("Overview of HRGenius services, database schemas, and git workflows.");
            task3.setCategory(OnboardingTemplateTask.TaskCategory.INDUCTION);
            task3.setDefaultDaysToComplete(5);
            t.addTask(task3);

            onboardingTemplateRepository.save(t);
        }

        // Sample Holidays
        if (holidayRepository.count() == 0) {
            Holiday h1 = new Holiday();
            h1.setHolidayDate(LocalDate.of(currentYear, 1, 26));
            h1.setName("Republic Day");
            h1.setDescription("National Holiday");
            holidayRepository.save(h1);

            Holiday h2 = new Holiday();
            h2.setHolidayDate(LocalDate.of(currentYear, 8, 15));
            h2.setName("Independence Day");
            h2.setDescription("National Holiday");
            holidayRepository.save(h2);

            Holiday h3 = new Holiday();
            h3.setHolidayDate(LocalDate.of(currentYear, 10, 2));
            h3.setName("Gandhi Jayanti");
            h3.setDescription("National Holiday");
            holidayRepository.save(h3);
        }

        log.info("H2 seed data loaded successfully with Phase 2 samples.");
    }

    private Role createRole(Role.RoleName name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private Department createDepartment(String name, String code, String desc) {
        return departmentRepository.findByCode(code).orElseGet(() -> {
            Department d = new Department();
            d.setName(name);
            d.setCode(code);
            d.setDescription(desc);
            return departmentRepository.save(d);
        });
    }

    private Designation createDesignation(String title, int level, String desc) {
        return designationRepository.findByTitle(title).orElseGet(() -> {
            Designation d = new Designation();
            d.setTitle(title);
            d.setLevel(level);
            d.setDescription(desc);
            return designationRepository.save(d);
        });
    }
}
