
package com.fillthepockets.controller;

import com.fillthepockets.dto.*;
import com.fillthepockets.entity.User;
import com.fillthepockets.service.AuthService;
import com.fillthepockets.service.EmployeeVerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmployeeVerificationService employeeVerificationService;

    @PostMapping("/register-villager")
    public ResponseEntity<ApiResponse<AuthResponse>> registerVillager(
            @Valid @RequestBody RegisterRequest req) {
        try {
            AuthResponse resp = authService.registerVillager(req);
            return ResponseEntity.ok(ApiResponse.ok(
                    "Registration successful! Welcome to Fill the Pockets.", resp));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login-villager")
    public ResponseEntity<ApiResponse<AuthResponse>> loginVillager(
            @Valid @RequestBody LoginRequest req) {
        try {
            AuthResponse resp = authService.loginVillager(req);
            return ResponseEntity.ok(ApiResponse.ok("Login successful!", resp));
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }

    // Administrator login
    @PostMapping("/login-admin")
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdmin(
            @Valid @RequestBody LoginRequest req) {
        try {
            AuthResponse resp = authService.loginAdmin(req);
            return ResponseEntity.ok(
                    ApiResponse.ok("Administrator login successful!", resp));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/employee/request-access")
    public ResponseEntity<ApiResponse<Void>> requestEmployeeAccess(
            @Valid @RequestBody EmployeeRegisterRequest req) {
        try {
            authService.requestEmployeeAccess(req);
            return ResponseEntity.ok(ApiResponse.ok(
                    "Access request submitted successfully. Your account is in PENDING status. "
                    + "An authorized administrator must review and approve your credentials "
                    + "before access is granted.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Step 1: Panchayat Employee credential verification.
     */
    @PostMapping("/employee/initiate-login")
    public ResponseEntity<ApiResponse<EmployeeOtpResponse>> initiateEmployeeLogin(
            @Valid @RequestBody EmployeeOtpRequest req) {
        try {
            EmployeeOtpResponse resp =
                    employeeVerificationService.initiateEmployeeLogin(
                            req.getEmail(), req.getPassword());
            return ResponseEntity.ok(ApiResponse.ok(resp.getMessage(), resp));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Step 2: Panchayat Employee verification code submission.
     */
    @PostMapping("/employee/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmployeeOtp(
            @Valid @RequestBody EmployeeOtpVerifyRequest req) {
        try {
            User employee = employeeVerificationService.verifyEmployeeCode(
                    req.getEmail(), req.getCode());
            AuthResponse resp = authService.generateEmployeeAuthResponse(employee);
            return ResponseEntity.ok(
                    ApiResponse.ok("Employee verification successful! Welcome.", resp));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }
}
