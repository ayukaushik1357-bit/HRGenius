package com.hrgenius.service;

import com.hrgenius.dto.common.PagedResponse;
import com.hrgenius.dto.user.UserResponse;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.Role;
import com.hrgenius.entity.User;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.repository.RefreshTokenRepository;
import com.hrgenius.repository.RoleRepository;
import com.hrgenius.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(String search, Boolean enabled, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.DESC.name())
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }

            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<UserResponse> content = userPage.getContent().stream()
                .map(this::toUserResponse)
                .toList();

        return PagedResponse.<UserResponse>builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setEnabled(enabled);
        if (!enabled) {
            log.info("User '{}' disabled by admin. Revoking active refresh tokens.", user.getUsername());
            refreshTokenRepository.revokeAllByUser(user);
        }
        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse unlockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setFailedAttempts(0);
        user.setLockoutTime(null);
        User saved = userRepository.save(user);
        log.info("User '{}' unlocked by admin.", user.getUsername());
        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse updateUserRoles(Long id, Set<String> roleNames) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (roleNames == null || roleNames.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            String cleanName = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
            try {
                Role.RoleName enumRole = Role.RoleName.valueOf(cleanName);
                Role role = roleRepository.findByName(enumRole)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", enumRole));
                roles.add(role);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid role name: " + roleName);
            }
        }

        user.setRoles(roles);
        User saved = userRepository.save(user);
        log.info("Roles updated for user '{}': {}", user.getUsername(), roleNames);
        return toUserResponse(saved);
    }

    public UserResponse toUserResponse(User user) {
        boolean isLocked = user.getLockoutTime() != null && user.getLockoutTime().isAfter(LocalDateTime.now());
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        Employee emp = user.getEmployee();
        Long employeeId = emp != null ? emp.getId() : null;
        String employeeCode = emp != null ? emp.getEmployeeCode() : null;
        String employeeName = emp != null ? (emp.getFirstName() + " " + emp.getLastName()) : null;

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .mustChangePassword(user.isMustChangePassword())
                .failedAttempts(user.getFailedAttempts())
                .lockoutTime(user.getLockoutTime())
                .locked(isLocked)
                .roles(roleNames)
                .employeeId(employeeId)
                .employeeCode(employeeCode)
                .employeeName(employeeName)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
