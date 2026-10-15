package com.hrgenius.service;

import com.hrgenius.dto.employee.EmployeeResponse;
import com.hrgenius.dto.recruitment.*;
import com.hrgenius.entity.Department;
import com.hrgenius.entity.Designation;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.Role;
import com.hrgenius.entity.User;
import com.hrgenius.entity.notification.AppNotification;
import com.hrgenius.entity.payroll.SalaryStructure;
import com.hrgenius.entity.recruitment.*;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.DuplicateResourceException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.mapper.EmployeeMapper;
import com.hrgenius.mapper.RecruitmentMapper;
import com.hrgenius.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final JobPostingRepository jobPostingRepository;
    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final OfferRepository offerRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecruitmentMapper recruitmentMapper;
    private final EmployeeMapper employeeMapper;
    private final OnboardingService onboardingService;
    private final NotificationService notificationService;

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    // --- Job Postings ---
    @Transactional(readOnly = true)
    public List<JobPostingResponse> getAllJobPostings(JobPosting.JobPostingStatus status) {
        List<JobPosting> list = (status == null) ? jobPostingRepository.findAll() : jobPostingRepository.findByStatus(status);
        return list.stream().map(recruitmentMapper::toJobPostingResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobPostingResponse getJobPostingById(Long id) {
        JobPosting jp = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "id", id));
        return recruitmentMapper.toJobPostingResponse(jp);
    }

    @Transactional
    public JobPostingResponse createJobPosting(JobPostingRequest request) {
        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
        }

        Designation desig = null;
        if (request.getDesignationId() != null) {
            desig = designationRepository.findById(request.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", request.getDesignationId()));
        }

        JobPosting jp = recruitmentMapper.toJobPostingEntity(request, dept, desig);
        JobPosting saved = jobPostingRepository.save(jp);
        return recruitmentMapper.toJobPostingResponse(saved);
    }

    @Transactional
    public JobPostingResponse updateJobPosting(Long id, JobPostingRequest request) {
        JobPosting jp = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "id", id));

        jp.setTitle(request.getTitle());
        jp.setDescription(request.getDescription());
        jp.setRequirements(request.getRequirements());
        jp.setLocation(request.getLocation());
        jp.setMinExperienceYears(request.getMinExperienceYears());
        jp.setMaxExperienceYears(request.getMaxExperienceYears());
        jp.setOpenings(request.getOpenings());
        if (request.getStatus() != null) {
            jp.setStatus(request.getStatus());
        }

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            jp.setDepartment(dept);
        } else {
            jp.setDepartment(null);
        }

        if (request.getDesignationId() != null) {
            Designation desig = designationRepository.findById(request.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", request.getDesignationId()));
            jp.setDesignation(desig);
        } else {
            jp.setDesignation(null);
        }

        JobPosting saved = jobPostingRepository.save(jp);
        return recruitmentMapper.toJobPostingResponse(saved);
    }

    @Transactional
    public void deleteJobPosting(Long id) {
        JobPosting jp = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "id", id));
        jobPostingRepository.delete(jp);
    }

    // --- Candidates ---
    @Transactional(readOnly = true)
    public List<CandidateResponse> getAllCandidates() {
        return candidateRepository.findAll().stream()
                .map(recruitmentMapper::toCandidateResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidateById(Long id) {
        Candidate c = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));
        return recruitmentMapper.toCandidateResponse(c);
    }

    @Transactional
    public CandidateResponse createCandidate(CandidateRequest request) {
        if (candidateRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Candidate with email '" + request.getEmail() + "' already exists");
        }
        Candidate candidate = recruitmentMapper.toCandidateEntity(request);
        Candidate saved = candidateRepository.save(candidate);
        return recruitmentMapper.toCandidateResponse(saved);
    }

    @Transactional
    public CandidateResponse uploadResume(Long candidateId, MultipartFile file) throws IOException {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", candidateId));

        Path targetDir = Paths.get(uploadDir, "resumes").toAbsolutePath().normalize();
        Files.createDirectories(targetDir);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        Path targetPath = targetDir.resolve(storedFilename);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        candidate.setResumeFileName(originalFilename);
        candidate.setResumeFilePath(targetPath.toString());
        Candidate saved = candidateRepository.save(candidate);
        return recruitmentMapper.toCandidateResponse(saved);
    }

    @Transactional(readOnly = true)
    public Resource downloadResume(Long candidateId) throws IOException {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", candidateId));

        if (candidate.getResumeFilePath() == null) {
            throw new ResourceNotFoundException("Resume file not found for candidate " + candidateId);
        }

        Path path = Paths.get(candidate.getResumeFilePath());
        Resource res = new UrlResource(path.toUri());
        if (!res.exists()) {
            throw new ResourceNotFoundException("Resume file could not be read from disk");
        }
        return res;
    }

    // --- Applications ---
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplications(Long jobPostingId, Application.ApplicationStage stage) {
        List<Application> list;
        if (jobPostingId != null) {
            list = applicationRepository.findByJobPostingId(jobPostingId);
            if (stage != null) {
                list = list.stream().filter(a -> a.getStage() == stage).collect(Collectors.toList());
            }
        } else if (stage != null) {
            list = applicationRepository.findByStage(stage);
        } else {
            list = applicationRepository.findAll();
        }
        return list.stream().map(recruitmentMapper::toApplicationResponse).collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponse createApplication(ApplicationRequest request) {
        JobPosting jp = jobPostingRepository.findById(request.getJobPostingId())
                .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "id", request.getJobPostingId()));
        Candidate c = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", request.getCandidateId()));

        if (applicationRepository.findByJobPostingIdAndCandidateId(jp.getId(), c.getId()).isPresent()) {
            throw new DuplicateResourceException("Candidate already applied for this job posting");
        }

        Application application = new Application();
        application.setJobPosting(jp);
        application.setCandidate(c);
        application.setStage(Application.ApplicationStage.APPLIED);
        application.setAppliedAt(LocalDateTime.now());
        application.setNotes(request.getNotes());

        Application saved = applicationRepository.save(application);
        return recruitmentMapper.toApplicationResponse(saved);
    }

    @Transactional
    public ApplicationResponse updateApplicationStage(Long applicationId, String stageStr) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        Application.ApplicationStage newStage = Application.ApplicationStage.valueOf(stageStr.toUpperCase());
        application.setStage(newStage);
        Application saved = applicationRepository.save(application);
        return recruitmentMapper.toApplicationResponse(saved);
    }

    // --- Interviews ---
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByApplication(Long applicationId) {
        return interviewRepository.findByApplicationId(applicationId).stream()
                .map(recruitmentMapper::toInterviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InterviewResponse scheduleInterview(InterviewRequest request) {
        Application app = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId()));
        Employee interviewer = employeeRepository.findById(request.getInterviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee (Interviewer)", "id", request.getInterviewerId()));

        Interview interview = new Interview();
        interview.setApplication(app);
        interview.setInterviewer(interviewer);
        interview.setScheduledAt(request.getScheduledAt());
        interview.setRoundName(request.getRoundName());
        interview.setStatus(Interview.InterviewStatus.SCHEDULED);

        // Auto move application stage to INTERVIEW
        if (app.getStage() == Application.ApplicationStage.APPLIED || app.getStage() == Application.ApplicationStage.SCREENING) {
            app.setStage(Application.ApplicationStage.INTERVIEW);
            applicationRepository.save(app);
        }

        Interview saved = interviewRepository.save(interview);

        // Notify interviewer
        if (interviewer.getUser() != null) {
            notificationService.notifyUser(
                    interviewer.getUser(),
                    "Interview Scheduled: " + interview.getRoundName(),
                    "You are scheduled to interview " + app.getCandidate().getFirstName() + " " + app.getCandidate().getLastName() +
                            " on " + interview.getScheduledAt() + " for " + app.getJobPosting().getTitle(),
                    AppNotification.NotificationType.INTERVIEW,
                    "/recruitment/pipeline"
            );
        }

        return recruitmentMapper.toInterviewResponse(saved);
    }

    @Transactional
    public InterviewResponse submitInterviewFeedback(Long interviewId, Integer rating, String feedback, String status) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (rating != null) {
            interview.setRating(rating);
        }
        if (feedback != null) {
            interview.setFeedback(feedback);
        }
        if (status != null) {
            interview.setStatus(Interview.InterviewStatus.valueOf(status.toUpperCase()));
        } else {
            interview.setStatus(Interview.InterviewStatus.COMPLETED);
        }

        Interview saved = interviewRepository.save(interview);
        return recruitmentMapper.toInterviewResponse(saved);
    }

    // --- Offers ---
    @Transactional(readOnly = true)
    public OfferResponse getOfferByApplication(Long applicationId) {
        Offer offer = offerRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer for application", "id", applicationId));
        return recruitmentMapper.toOfferResponse(offer);
    }

    @Transactional
    public OfferResponse createOrUpdateOffer(OfferRequest request) {
        Application app = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId()));

        Offer offer = offerRepository.findByApplicationId(app.getId()).orElseGet(() -> {
            Offer o = new Offer();
            o.setApplication(app);
            return o;
        });

        offer.setOfferDate(request.getOfferDate());
        offer.setJoiningDate(request.getJoiningDate());
        offer.setOfferedSalaryCTC(request.getOfferedSalaryCTC());
        if (request.getStatus() != null) {
            offer.setStatus(Offer.OfferStatus.valueOf(request.getStatus().toUpperCase()));
        }
        offer.setRemarks(request.getRemarks());

        app.setStage(Application.ApplicationStage.OFFERED);
        applicationRepository.save(app);

        Offer saved = offerRepository.save(offer);
        return recruitmentMapper.toOfferResponse(saved);
    }

    // --- Convert HIRED Candidate to Employee ---
    @Transactional
    public EmployeeResponse hireCandidate(HireCandidateRequest request) {
        Application app = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId()));
        Candidate candidate = app.getCandidate();

        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new DuplicateResourceException("Employee code '" + request.getEmployeeCode() + "' is already in use");
        }
        if (employeeRepository.existsByEmail(request.getOfficialEmail())) {
            throw new DuplicateResourceException("Email '" + request.getOfficialEmail() + "' is already in use");
        }

        Department dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
        Designation desig = designationRepository.findById(request.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", request.getDesignationId()));

        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", request.getManagerId()));
        }

        // Create User account
        Role employeeRole = roleRepository.findByName(Role.RoleName.EMPLOYEE)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", Role.RoleName.EMPLOYEE));


        User user = new User();
        user.setUsername(request.getEmployeeCode().toLowerCase());
        user.setEmail(request.getOfficialEmail());
        user.setPassword(passwordEncoder.encode(request.getInitialPassword()));
        user.setEnabled(true);
        user.setRoles(Collections.singleton(employeeRole));
        User savedUser = userRepository.save(user);

        // Create Employee
        Employee emp = new Employee();
        emp.setEmployeeCode(request.getEmployeeCode());
        emp.setFirstName(candidate.getFirstName());
        emp.setLastName(candidate.getLastName());
        emp.setEmail(request.getOfficialEmail());
        emp.setPhone(candidate.getPhone());
        emp.setJoiningDate(request.getJoiningDate());
        emp.setDepartment(dept);
        emp.setDesignation(desig);
        emp.setReportingManager(manager);
        emp.setSalaryCTC(candidate.getExpectedSalaryCTC() != null ? candidate.getExpectedSalaryCTC() : BigDecimal.ZERO);
        emp.setStatus(Employee.EmployeeStatus.ACTIVE);
        emp.setUser(savedUser);

        Employee savedEmp = employeeRepository.save(emp);

        // Set Salary Structure if provided
        if (request.getBasicSalary() != null) {
            SalaryStructure ss = new SalaryStructure();
            ss.setEmployee(savedEmp);
            ss.setBasicSalary(request.getBasicSalary());
            ss.setHra(request.getHra() != null ? request.getHra() : BigDecimal.ZERO);
            ss.setSpecialAllowance(request.getSpecialAllowance() != null ? request.getSpecialAllowance() : BigDecimal.ZERO);
            ss.setOtherAllowances(request.getOtherAllowances() != null ? request.getOtherAllowances() : BigDecimal.ZERO);
            ss.setEffectiveFrom(request.getJoiningDate());
            salaryStructureRepository.save(ss);
        }

        // Auto-assign Onboarding Checklist
        if (request.getOnboardingTemplateId() != null) {
            onboardingService.assignChecklistToEmployee(savedEmp.getId(), request.getOnboardingTemplateId(), manager != null ? manager.getId() : null);
        }

        // Update application stage to HIRED
        app.setStage(Application.ApplicationStage.HIRED);
        applicationRepository.save(app);

        // Notify
        notificationService.notifyUser(
                savedUser,
                "Welcome to the Team!",
                "Hello " + candidate.getFirstName() + ", your HRGenius account has been activated with code " + emp.getEmployeeCode() + ".",
                AppNotification.NotificationType.GENERAL,
                "/employees/" + savedEmp.getId()
        );

        return employeeMapper.toResponse(savedEmp);
    }
}
