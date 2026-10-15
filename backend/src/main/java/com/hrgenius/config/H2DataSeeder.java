package com.hrgenius.config;

import com.hrgenius.entity.Department;
import com.hrgenius.entity.Designation;
import com.hrgenius.entity.Role;
import com.hrgenius.entity.User;
import com.hrgenius.repository.DepartmentRepository;
import com.hrgenius.repository.DesignationRepository;
import com.hrgenius.repository.RoleRepository;
import com.hrgenius.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds initial data when running with the H2 profile (since Flyway is disabled).
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Seeding H2 database...");

        // Roles
        Role adminRole = createRole(Role.RoleName.ADMIN);
        createRole(Role.RoleName.HR_MANAGER);
        createRole(Role.RoleName.MANAGER);
        createRole(Role.RoleName.EMPLOYEE);

        // Admin user
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@hrgenius.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setEnabled(true);
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
            log.info("Admin user created (admin / Admin@123)");
        }

        // Sample departments
        createDepartment("Engineering", "ENG", "Software Engineering");
        createDepartment("Human Resources", "HR", "Human Resources Department");
        createDepartment("Finance", "FIN", "Finance and Accounting");
        createDepartment("Marketing", "MKT", "Marketing Department");

        // Sample designations
        createDesignation("Software Engineer", 3, "Individual contributor");
        createDesignation("Senior Software Engineer", 4, "Senior IC");
        createDesignation("Engineering Manager", 5, "People manager");
        createDesignation("HR Executive", 3, "HR operations");
        createDesignation("Director", 7, "Department director");

        log.info("H2 seed data loaded successfully.");
    }

    private Role createRole(Role.RoleName name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private void createDepartment(String name, String code, String desc) {
        if (departmentRepository.findByCode(code).isEmpty()) {
            Department d = new Department();
            d.setName(name);
            d.setCode(code);
            d.setDescription(desc);
            departmentRepository.save(d);
        }
    }

    private void createDesignation(String title, int level, String desc) {
        if (designationRepository.findByTitle(title).isEmpty()) {
            Designation d = new Designation();
            d.setTitle(title);
            d.setLevel(level);
            d.setDescription(desc);
            designationRepository.save(d);
        }
    }
}
