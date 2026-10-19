package com.hrgenius.security;

import com.hrgenius.entity.Employee;
import com.hrgenius.entity.User;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.repository.EmployeeRepository;
import com.hrgenius.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    public static final String NO_EMPLOYEE_PROFILE =
            "Your account is not linked to an employee record yet. HR will link it, and then your details will appear here.";

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return Optional.of(up);
        }
        return Optional.empty();
    }

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UserPrincipal up) {
            return userRepository.findById(up.getId());
        }
        String username = auth.getName();
        if (username != null && !username.isBlank()) {
            return userRepository.findByUsername(username);
        }
        return Optional.empty();
    }

    public User getCurrentUserOrThrow() {
        return getCurrentUser()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));
    }

    public Optional<Employee> getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UserPrincipal up) {
            Optional<Employee> emp = employeeRepository.findByUserId(up.getId());
            if (emp.isPresent()) {
                return emp;
            }
            if (up.getEmail() != null) {
                emp = employeeRepository.findByEmail(up.getEmail());
                if (emp.isPresent()) {
                    return emp;
                }
            }
        }
        String username = auth.getName();
        if (username != null && !username.isBlank()) {
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isPresent()) {
                Optional<Employee> emp = employeeRepository.findByUserId(user.get().getId());
                if (emp.isPresent()) {
                    return emp;
                }
                if (user.get().getEmail() != null) {
                    emp = employeeRepository.findByEmail(user.get().getEmail());
                    if (emp.isPresent()) {
                        return emp;
                    }
                }
            }
            Optional<Employee> byEmail = employeeRepository.findByEmail(username);
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }
        return Optional.empty();
    }

    public Employee getCurrentEmployeeOrThrow() {
        return getCurrentEmployee()
                .orElseThrow(() -> new com.hrgenius.exception.ResourceNotFoundException(NO_EMPLOYEE_PROFILE));
    }

    public Optional<Long> getCurrentEmployeeId() {
        return getCurrentEmployee().map(Employee::getId);
    }

    public Long getCurrentEmployeeIdOrThrow() {
        return getCurrentEmployeeId()
                .orElseThrow(() -> new com.hrgenius.exception.ResourceNotFoundException(NO_EMPLOYEE_PROFILE));
    }

    public Long getEmployeeIdForUser(UserPrincipal principal, Long requestedEmployeeId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (principal == null) {
            Optional<UserPrincipal> current = getCurrentUserPrincipal();
            if (current.isPresent()) {
                principal = current.get();
            } else if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
            }
        }

        if (requestedEmployeeId == null) {
            return getCurrentEmployeeIdOrThrow();
        }

        // 1. ADMIN and HR_MANAGER can act on any employee
        boolean isHrOrAdmin = (principal != null && hasAnyRole(principal, "ADMIN", "HR_MANAGER"))
                || hasAnyRole("ADMIN", "HR_MANAGER");
        if (isHrOrAdmin) {
            return requestedEmployeeId;
        }

        Long currentEmpId = getCurrentEmployeeId().orElse(null);
        if (currentEmpId == null) {
            throw new org.springframework.security.access.AccessDeniedException("No employee profile associated with current account");
        }

        // 2. If acting on self, allowed
        if (currentEmpId.equals(requestedEmployeeId)) {
            return requestedEmployeeId;
        }

        // 3. If manager, can act only on direct reports
        boolean isManager = (principal != null && hasRole(principal, "MANAGER")) || hasRole("MANAGER");
        if (isManager) {
            boolean isDirectReport = employeeRepository.findById(requestedEmployeeId)
                    .map(emp -> emp.getReportingManager() != null && currentEmpId.equals(emp.getReportingManager().getId()))
                    .orElse(false);
            if (isDirectReport) {
                return requestedEmployeeId;
            }
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Managers can only act on direct reports");
        }

        // 4. Regular employee acting on someone else
        throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only act on your own employee profile");
    }

    public Long getEmployeeIdForUser(Long requestedEmployeeId) {
        return getEmployeeIdForUser(getCurrentUserPrincipal().orElse(null), requestedEmployeeId);
    }

    public Set<String> getCurrentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return Collections.emptySet();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    public boolean hasRole(UserPrincipal principal, String role) {
        if (principal == null || principal.getAuthorities() == null || role == null) {
            return false;
        }
        String cleanRole = role.toUpperCase();
        String roleWithPrefix = cleanRole.startsWith("ROLE_") ? cleanRole : "ROLE_" + cleanRole;
        String roleWithoutPrefix = cleanRole.startsWith("ROLE_") ? cleanRole.substring(5) : cleanRole;

        return principal.getAuthorities().stream().anyMatch(auth -> {
            String authority = auth.getAuthority().toUpperCase();
            return authority.equals(roleWithPrefix) || authority.equals(roleWithoutPrefix);
        });
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null || role == null) {
            return false;
        }
        String cleanRole = role.toUpperCase();
        String roleWithPrefix = cleanRole.startsWith("ROLE_") ? cleanRole : "ROLE_" + cleanRole;
        String roleWithoutPrefix = cleanRole.startsWith("ROLE_") ? cleanRole.substring(5) : cleanRole;

        return auth.getAuthorities().stream().anyMatch(a -> {
            String authority = a.getAuthority().toUpperCase();
            return authority.equals(roleWithPrefix) || authority.equals(roleWithoutPrefix);
        });
    }

    public boolean hasAnyRole(UserPrincipal principal, String... roles) {
        for (String r : roles) {
            if (hasRole(principal, r)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyRole(String... roles) {
        for (String r : roles) {
            if (hasRole(r)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isHrManager() {
        return hasRole("HR_MANAGER");
    }

    public boolean isManager() {
        return hasRole("MANAGER");
    }

    public boolean isEmployee() {
        return hasRole("EMPLOYEE");
    }

    public boolean isHrOrAdmin() {
        return hasAnyRole("ADMIN", "HR_MANAGER");
    }
}
