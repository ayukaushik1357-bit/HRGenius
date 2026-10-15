package com.hrgenius.config;

import com.hrgenius.entity.*;
import com.hrgenius.entity.attendance.Holiday;
import com.hrgenius.entity.audit.AuditLog;
import com.hrgenius.entity.leave.LeaveBalance;
import com.hrgenius.entity.offboarding.ExitRequest;
import com.hrgenius.entity.onboarding.OnboardingTemplate;
import com.hrgenius.entity.onboarding.OnboardingTemplateTask;
import com.hrgenius.entity.payroll.PayrollRun;
import com.hrgenius.entity.payroll.Payslip;
import com.hrgenius.entity.payroll.SalaryStructure;
import com.hrgenius.entity.performance.Goal;
import com.hrgenius.entity.performance.PerformanceReview;
import com.hrgenius.entity.performance.ReviewCycle;
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
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeds comprehensive realistic demo data (50 employees, payroll, review cycles, KRAs, audit logs).
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
    private final ReviewCycleRepository reviewCycleRepository;
    private final GoalRepository goalRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final AuditLogRepository auditLogRepository;
    private final ExitRequestRepository exitRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Seeding realistic demo data (Phase 3: 50 employees, performance cycles, payroll, audit logs)...");

        // 1. Roles
        Role adminRole = createRole(Role.RoleName.ADMIN);
        Role hrRole = createRole(Role.RoleName.HR_MANAGER);
        Role mgrRole = createRole(Role.RoleName.MANAGER);
        Role empRole = createRole(Role.RoleName.EMPLOYEE);

        // 2. Standard Demo Users
        User adminUser = createUser("admin", "admin@hrgenius.com", "Admin@123", Set.of(adminRole, hrRole));
        User hrUser = createUser("hrmanager", "hr@hrgenius.com", "Hr@123", Set.of(hrRole));
        User mgrUser = createUser("manager", "manager@hrgenius.com", "Manager@123", Set.of(mgrRole));
        User empUser = createUser("employee", "employee@hrgenius.com", "Employee@123", Set.of(empRole));

        // 3. Departments
        Department engDept = createDepartment("Engineering", "ENG", "Software Engineering & Architecture");
        Department hrDept = createDepartment("Human Resources", "HR", "People Operations & Talent");
        Department finDept = createDepartment("Finance", "FIN", "Finance, Tax & Accounting");
        Department mktDept = createDepartment("Marketing", "MKT", "Product Marketing & Growth");
        Department opsDept = createDepartment("Operations", "OPS", "Customer Support & Business Operations");

        List<Department> depts = List.of(engDept, hrDept, finDept, mktDept, opsDept);

        // 4. Designations
        Designation sseDesig = createDesignation("Senior Software Engineer", 4, "Senior IC");
        Designation seDesig = createDesignation("Software Engineer", 3, "Individual Contributor");
        Designation emDesig = createDesignation("Engineering Manager", 5, "Engineering Lead");
        Designation hrLeadDesig = createDesignation("Lead HR Business Partner", 5, "HR Leadership");
        Designation hrExecDesig = createDesignation("HR Executive", 3, "HR Operations");
        Designation finAnalystDesig = createDesignation("Senior Financial Analyst", 4, "Finance & Tax");
        Designation mktLeadDesig = createDesignation("Growth Marketing Lead", 4, "Digital Growth");
        Designation opsSpecialistDesig = createDesignation("Operations Specialist", 3, "Operations & SLA");

        // 5. Seed Core Role Employees
        Employee adminEmp = createOrGetEmployee("EMP001", "Alex", "Admin", adminUser.getEmail(), "+91 9876543210",
                LocalDate.of(2021, 1, 10), hrDept, hrLeadDesig, null, adminUser, BigDecimal.valueOf(2400000.00), Employee.EmployeeStatus.ACTIVE, Employee.Gender.MALE);

        Employee hrEmp = createOrGetEmployee("EMP002", "Priya", "Sharma", hrUser.getEmail(), "+91 9876543211",
                LocalDate.of(2022, 3, 15), hrDept, hrExecDesig, adminEmp, hrUser, BigDecimal.valueOf(1400000.00), Employee.EmployeeStatus.ACTIVE, Employee.Gender.FEMALE);

        Employee mgrEmp = createOrGetEmployee("EMP003", "Vikram", "Malhotra", mgrUser.getEmail(), "+91 9876543212",
                LocalDate.of(2021, 6, 1), engDept, emDesig, adminEmp, mgrUser, BigDecimal.valueOf(2800000.00), Employee.EmployeeStatus.ACTIVE, Employee.Gender.MALE);

        Employee devEmp = createOrGetEmployee("EMP004", "Rohan", "Gupta", empUser.getEmail(), "+91 9876543213",
                LocalDate.of(2023, 2, 20), engDept, sseDesig, mgrEmp, empUser, BigDecimal.valueOf(1800000.00), Employee.EmployeeStatus.ACTIVE, Employee.Gender.MALE);

        // 6. Seed Remaining 46 Employees (making 50 total)
        String[] firstNames = {
                "Aarav", "Ananya", "Aditi", "Arjun", "Deepak", "Divya", "Gaurav", "Isha", "Kavita", "Karan",
                "Manish", "Meera", "Neha", "Nikhil", "Pooja", "Pranav", "Rahul", "Riya", "Sameer", "Sneha",
                "Suresh", "Tanvi", "Varun", "Zoya", "Amit", "Bhavna", "Chetan", "Geeta", "Harish", "Jyoti",
                "Kunal", "Lata", "Mohit", "Nandini", "Omkar", "Payal", "Rajesh", "Shweta", "Tarun", "Uma",
                "Vivek", "Yash", "Ankit", "Chitra", "Dinesh", "Preeti"
        };
        String[] lastNames = {
                "Patel", "Verma", "Reddy", "Nair", "Iyer", "Chopra", "Joshi", "Bose", "Menon", "Kapoor",
                "Deshmukh", "Singhania", "Bhatia", "Saxena", "Kulkarni", "Ghosh", "Agarwal", "Bansal", "Mehta", "Rao",
                "Pillai", "Mishra", "Pandey", "Trivedi", "Sengupta", "Das", "Mukherjee", "Chatterjee", "Dutta", "Roy",
                "Gowda", "Shetty", "Nambiar", "Chauhan", "Yadav", "Thakur", "Solanki", "Bhardwaj", "Tandon", "Kaur",
                "Gill", "Dhillon", "Sethi", "Kohli", "Grover", "Bahl"
        };

        Designation[] poolDesigs = {seDesig, sseDesig, hrExecDesig, finAnalystDesig, mktLeadDesig, opsSpecialistDesig};

        List<Employee> allEmployees = new ArrayList<>(List.of(adminEmp, hrEmp, mgrEmp, devEmp));

        if (employeeRepository.count() < 50) {
            for (int i = 0; i < 46; i++) {
                String code = String.format("EMP%03d", i + 5);
                String fn = firstNames[i % firstNames.length];
                String ln = lastNames[i % lastNames.length];
                String email = (fn + "." + ln + (i + 5) + "@hrgenius.com").toLowerCase();
                Department dept = depts.get(i % depts.size());
                Designation desig = poolDesigs[i % poolDesigs.length];
                Employee manager = (i % 3 == 0) ? mgrEmp : adminEmp;

                LocalDate joiningDate = LocalDate.of(2021 + (i % 5), (i % 12) + 1, ((i * 3) % 25) + 1);
                Employee.Gender gender = (i % 2 == 0) ? Employee.Gender.FEMALE : Employee.Gender.MALE;
                Employee.EmployeeStatus status = (i == 10 || i == 25) ? Employee.EmployeeStatus.ON_NOTICE :
                        (i == 15 || i == 35) ? Employee.EmployeeStatus.EXITED : Employee.EmployeeStatus.ACTIVE;

                BigDecimal salary = BigDecimal.valueOf(800000.00 + (i * 45000.00));

                Employee emp = createOrGetEmployee(code, fn, ln, email, "+91 98" + String.format("%08d", 10000000 + i),
                        joiningDate, dept, desig, manager, null, salary, status, gender);

                allEmployees.add(emp);
            }
        }

        // 7. Seed Salary Structures and Leave Balances for all
        int currentYear = LocalDate.now().getYear();
        for (Employee e : allEmployees) {
            if (salaryStructureRepository.findByEmployeeId(e.getId()).isEmpty()) {
                SalaryStructure ss = new SalaryStructure();
                ss.setEmployee(e);
                BigDecimal ctc = e.getSalaryCTC() != null ? e.getSalaryCTC() : BigDecimal.valueOf(1200000.00);
                BigDecimal monthly = ctc.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
                ss.setBasicSalary(monthly.multiply(BigDecimal.valueOf(0.50)).setScale(2, java.math.RoundingMode.HALF_UP));
                ss.setHra(monthly.multiply(BigDecimal.valueOf(0.25)).setScale(2, java.math.RoundingMode.HALF_UP));
                ss.setSpecialAllowance(monthly.multiply(BigDecimal.valueOf(0.15)).setScale(2, java.math.RoundingMode.HALF_UP));
                ss.setOtherAllowances(monthly.multiply(BigDecimal.valueOf(0.10)).setScale(2, java.math.RoundingMode.HALF_UP));
                ss.setEffectiveFrom(e.getJoiningDate() != null ? e.getJoiningDate() : LocalDate.of(currentYear, 1, 1));
                salaryStructureRepository.save(ss);
            }

            if (leaveBalanceRepository.findByEmployeeIdAndYear(e.getId(), currentYear).isEmpty()) {
                LeaveBalance lb = new LeaveBalance();
                lb.setEmployee(e);
                lb.setYear(currentYear);
                lb.setCasualQuota(12.0);
                lb.setCasualUsed(2.0 + (e.getId() % 4));
                lb.setSickQuota(10.0);
                lb.setSickUsed(1.0 + (e.getId() % 3));
                lb.setEarnedQuota(15.0);
                lb.setEarnedUsed(3.0 + (e.getId() % 5));
                leaveBalanceRepository.save(lb);
            }
        }

        // 8. Performance Review Cycle & Sample Goals & Appraisals
        if (reviewCycleRepository.count() == 0) {
            ReviewCycle cycle = new ReviewCycle();
            cycle.setName("Annual Appraisal Cycle " + currentYear);
            cycle.setStartDate(LocalDate.of(currentYear, 1, 1));
            cycle.setEndDate(LocalDate.of(currentYear, 12, 31));
            cycle.setStatus(ReviewCycle.CycleStatus.ACTIVE);
            ReviewCycle savedCycle = reviewCycleRepository.save(cycle);

            // Goals for EMP004 (Rohan Gupta)
            Goal g1 = new Goal();
            g1.setEmployee(devEmp);
            g1.setReviewCycle(savedCycle);
            g1.setTitle("Deliver Spring Boot 3 & Hibernate Microservices");
            g1.setDescription("Architect performant, secure backend endpoints with zero latency bottlenecks.");
            g1.setWeightage(40.0);
            g1.setProgressPercentage(90.0);
            goalRepository.save(g1);

            Goal g2 = new Goal();
            g2.setEmployee(devEmp);
            g2.setReviewCycle(savedCycle);
            g2.setTitle("Build Responsive Angular Material UI & Charts");
            g2.setDescription("Implement interactive dashboards, responsive views, and export functionality.");
            g2.setWeightage(35.0);
            g2.setProgressPercentage(85.0);
            goalRepository.save(g2);

            Goal g3 = new Goal();
            g3.setEmployee(devEmp);
            g3.setReviewCycle(savedCycle);
            g3.setTitle("Unit and E2E Test Automation");
            g3.setDescription("Maintain >80% test coverage across core domain logic.");
            g3.setWeightage(25.0);
            g3.setProgressPercentage(95.0);
            goalRepository.save(g3);

            // Completed Reviews with ratings across employees
            double[] ratings = {4.5, 4.2, 4.8, 3.9, 4.0, 4.6, 3.8, 4.4, 4.1, 4.7};
            for (int i = 0; i < Math.min(10, allEmployees.size()); i++) {
                Employee emp = allEmployees.get(i);
                PerformanceReview pr = new PerformanceReview();
                pr.setReviewCycle(savedCycle);
                pr.setEmployee(emp);
                pr.setManager(emp.getReportingManager() != null ? emp.getReportingManager() : mgrEmp);
                pr.setStatus(PerformanceReview.ReviewStatus.COMPLETED);
                pr.setSelfRating(ratings[i % ratings.length]);
                pr.setSelfComments("Delivered all sprint goals on time with high code quality.");
                pr.setManagerRating(ratings[i % ratings.length]);
                pr.setManagerComments("Strong individual performance and positive team contributions.");
                pr.setFinalRating(ratings[i % ratings.length]);
                pr.setSubmittedAt(LocalDateTime.now().minusDays(15));
                pr.setReviewedAt(LocalDateTime.now().minusDays(10));
                performanceReviewRepository.save(pr);
            }
        }

        // 9. Sample Payroll Run & Payslips for Current Month
        int currentMonth = LocalDate.now().getMonthValue();
        if (payrollRunRepository.findByYearAndMonth(currentYear, currentMonth).isEmpty()) {
            PayrollRun pr = new PayrollRun();
            pr.setYear(currentYear);
            pr.setMonth(currentMonth);
            pr.setTotalEmployees(allEmployees.size());
            pr.setStatus(PayrollRun.PayrollStatus.PROCESSED);
            pr.setProcessedAt(LocalDateTime.now().minusDays(2));

            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;

            PayrollRun savedPr = payrollRunRepository.save(pr);

            for (Employee e : allEmployees) {
                SalaryStructure ss = salaryStructureRepository.findByEmployeeId(e.getId()).orElse(null);
                if (ss != null) {
                    BigDecimal gross = ss.getMonthlyGross();
                    BigDecimal pf = gross.multiply(BigDecimal.valueOf(0.12)).setScale(2, java.math.RoundingMode.HALF_UP);
                    BigDecimal net = gross.subtract(pf);

                    totalGross = totalGross.add(gross);
                    totalNet = totalNet.add(net);

                    Payslip ps = new Payslip();
                    ps.setPayrollRun(savedPr);
                    ps.setEmployee(e);
                    ps.setYear(currentYear);
                    ps.setMonth(currentMonth);
                    ps.setTotalWorkingDays(30);
                    ps.setPresentDays(30.0);
                    ps.setBasicSalary(ss.getBasicSalary());
                    ps.setHra(ss.getHra());
                    ps.setSpecialAllowance(ss.getSpecialAllowance());
                    ps.setOtherAllowances(ss.getOtherAllowances());
                    ps.setGrossSalary(gross);
                    ps.setPfDeduction(pf);
                    ps.setTotalDeductions(pf);
                    ps.setNetSalary(net);
                    ps.setStatus(PayrollRun.PayrollStatus.PAID);
                    payslipRepository.save(ps);
                }
            }

            savedPr.setTotalGrossPay(totalGross);
            savedPr.setTotalNetPay(totalNet);
            payrollRunRepository.save(savedPr);
        }

        // 10. Sample Audit Logs
        if (auditLogRepository.count() == 0) {
            createAuditLog("Employee", devEmp.getId(), "SALARY_REVISION", "1500000", "1800000", "admin");
            createAuditLog("Employee", hrEmp.getId(), "DESIGNATION_UPDATE", "HR Officer", "HR Executive", "admin");
            createAuditLog("User", adminUser.getId(), "ROLE_ASSIGNMENT", "HR_MANAGER", "ADMIN, HR_MANAGER", "SYSTEM");
        }

        // 11. Sample Job Postings
        if (jobPostingRepository.count() == 0) {
            JobPosting jp1 = new JobPosting();
            jp1.setTitle("Senior Full Stack Java & Angular Engineer");
            jp1.setDescription("Leading development on enterprise human capital management solutions.");
            jp1.setRequirements("5+ years of Java Spring Boot, Hibernate, Angular 17+, Oracle SQL.");
            jp1.setLocation("Bengaluru, India (Hybrid)");
            jp1.setMinExperienceYears(5);
            jp1.setMaxExperienceYears(10);
            jp1.setOpenings(4);
            jp1.setStatus(JobPosting.JobPostingStatus.OPEN);
            jp1.setDepartment(engDept);
            jp1.setDesignation(sseDesig);
            jobPostingRepository.save(jp1);

            JobPosting jp2 = new JobPosting();
            jp2.setTitle("Senior Financial Analyst");
            jp2.setDescription("Manage quarterly financial modeling, statutory audits, and payroll forecasts.");
            jp2.setRequirements("Chartered Accountant or MBA Finance with 4+ years corporate experience.");
            jp2.setLocation("Mumbai, India (On-site)");
            jp2.setMinExperienceYears(4);
            jp2.setMaxExperienceYears(8);
            jp2.setOpenings(2);
            jp2.setStatus(JobPosting.JobPostingStatus.OPEN);
            jp2.setDepartment(finDept);
            jp2.setDesignation(finAnalystDesig);
            jobPostingRepository.save(jp2);
        }

        // 12. Sample Holidays
        if (holidayRepository.count() == 0) {
            createHoliday(LocalDate.of(currentYear, 1, 26), "Republic Day", "National Holiday");
            createHoliday(LocalDate.of(currentYear, 3, 14), "Holi", "Festival of Colors");
            createHoliday(LocalDate.of(currentYear, 8, 15), "Independence Day", "National Holiday");
            createHoliday(LocalDate.of(currentYear, 10, 2), "Gandhi Jayanti", "National Holiday");
            createHoliday(LocalDate.of(currentYear, 10, 20), "Diwali", "Festival of Lights");
            createHoliday(LocalDate.of(currentYear, 12, 25), "Christmas", "National Holiday");
        }

        // 13. Sample Onboarding Template
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

        log.info("H2 database seeded successfully: 50 realistic employees across 5 departments, payroll runs, review cycles, and audit trails.");
    }

    private User createUser(String username, String email, String password, Set<Role> roles) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode(password));
            u.setEnabled(true);
            u.setRoles(roles);
            return userRepository.save(u);
        });
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

    private Employee createOrGetEmployee(String code, String fn, String ln, String email, String phone,
                                         LocalDate joining, Department dept, Designation desig,
                                         Employee manager, User user, BigDecimal ctc,
                                         Employee.EmployeeStatus status, Employee.Gender gender) {
        return employeeRepository.findByEmail(email).orElseGet(() -> {
            Employee e = new Employee();
            e.setEmployeeCode(code);
            e.setFirstName(fn);
            e.setLastName(ln);
            e.setEmail(email);
            e.setPhone(phone);
            e.setDateOfBirth(LocalDate.of(1992, 5, 12));
            e.setGender(gender);
            e.setJoiningDate(joining);
            e.setDepartment(dept);
            e.setDesignation(desig);
            e.setReportingManager(manager);
            e.setUser(user);
            e.setSalaryCTC(ctc);
            e.setStatus(status);
            e.setPanNumber("ABCDE" + (1000 + (code.hashCode() % 8000)) + "F");
            e.setAadhaarNumber("5489" + String.format("%08d", Math.abs(code.hashCode() % 90000000)));
            e.setBankAccountNumber("9182" + String.format("%08d", Math.abs(code.hashCode() % 80000000)));
            e.setBankIfscCode("HDFC0001234");
            e.setBankName("HDFC Bank");
            return employeeRepository.save(e);
        });
    }

    private void createHoliday(LocalDate date, String name, String desc) {
        Holiday h = new Holiday();
        h.setHolidayDate(date);
        h.setName(name);
        h.setDescription(desc);
        holidayRepository.save(h);
    }

    private void createAuditLog(String entity, Long id, String action, String oldVal, String newVal, String changedBy) {
        AuditLog log = new AuditLog();
        log.setEntityName(entity);
        log.setEntityId(id);
        log.setAction(action);
        log.setOldValue(oldVal);
        log.setNewValue(newVal);
        log.setChangedBy(changedBy);
        log.setChangedAt(LocalDateTime.now().minusDays(1));
        log.setIpAddress("192.168.1.10");
        auditLogRepository.save(log);
    }
}
