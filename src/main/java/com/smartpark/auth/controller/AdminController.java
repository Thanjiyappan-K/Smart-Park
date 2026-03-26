package com.smartpark.auth.controller;

import com.smartpark.auth.service.AdminService;
import com.smartpark.common.response.ApiResponse;
import com.smartpark.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/verify-owner/{id}")
    public ResponseEntity<ApiResponse<String>> verifyOwner(@PathVariable Long id) {
        adminService.verifyOwner(id);
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Owner verified successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending-owners")
    public ResponseEntity<ApiResponse<List<User>>> getPendingOwners() {
        List<User> pendingOwners = adminService.getPendingOwners();
        ApiResponse<List<User>> response = ApiResponse.<List<User>>builder()
                .success(true)
                .message("Pending owners retrieved successfully")
                .data(pendingOwners)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/block-user/{id}")
    public ResponseEntity<ApiResponse<String>> blockUser(@PathVariable Long id) {
        adminService.blockUser(id);
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("User blocked successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unblock-user/{id}")
    public ResponseEntity<ApiResponse<String>> unblockUser(@PathVariable Long id) {
        adminService.unblockUser(id);
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("User unblocked successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }
}
