package com.hrgenius.controller;

import com.hrgenius.dto.common.PagedResponse;
import com.hrgenius.dto.user.UserRolesRequest;
import com.hrgenius.dto.user.UserResponse;
import com.hrgenius.dto.user.UserStatusRequest;
import com.hrgenius.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin endpoints for managing user accounts, roles, lockouts, and status")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List and search all users with pagination")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(userService.getAllUsers(search, enabled, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user account details by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Enable or disable a user account")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(userService.updateUserStatus(id, request.getEnabled()));
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "Unlock a locked user account and reset failed attempts")
    public ResponseEntity<UserResponse> unlockUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.unlockUser(id));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Assign or remove roles for a user")
    public ResponseEntity<UserResponse> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRolesRequest request) {
        return ResponseEntity.ok(userService.updateUserRoles(id, request.getRoles()));
    }
}
