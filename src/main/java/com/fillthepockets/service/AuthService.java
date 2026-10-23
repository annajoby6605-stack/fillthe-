
package com.fillthepockets.service;

import com.fillthepockets.dto.AuthResponse;
import com.fillthepockets.dto.EmployeeRegisterRequest;
import com.fillthepockets.dto.LoginRequest;
import com.fillthepockets.dto.RegisterRequest;
import com.fillthepockets.entity.EmployeeProfile;
import com.fillthepockets.entity.Role;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.EmployeeProfileRepository;
import com.fillthepockets.repository.UserRepository;
import com.fillthepockets.security.JwtUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Transactional
    public AuthResponse registerVillager(RegisterRequest req) {

        if (req.getConfirmPassword() != null
                && !req.getConfirmPassword().isBlank()) {

            if (!req.getPassword().equals(req.getConfirmPassword())) {
                throw new IllegalArgumentException(
                        "Passwords do not match. Please verify both password fields.");
            }
        }

        String email = req.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists.");
        }

        User user = new User(
                email,
                passwordEncoder.encode(req.getPassword()),
                req.getFullName().trim(),
                req.getPhone() != null ? req.getPhone().trim() : null,
                Role.VILLAGER
        );

        user = userRepository.save(user);

        String token = jwtUtils.generateJwtToken(
                user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getPointsBalance()
        );
    }

    @Transactional
    public void requestEmployeeAccess(EmployeeRegisterRequest req) {

        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException(
                    "Passwords do not match. Please verify both password fields.");
        }

        String email = req.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email address already exists.");
        }

        String code = req.getEmployeeCode().trim().toUpperCase();

        if (employeeProfileRepository.existsByEmployeeCode(code)) {
            throw new IllegalArgumentException(
                    "An employee profile with ID '" + code + "' already exists.");
        }

        User user = new User(
                email,
                passwordEncoder.encode(req.getPassword()),
                req.getFullName().trim(),
                req.getPhone() != null ? req.getPhone().trim() : null,
                Role.PANCHAYAT_EMPLOYEE
        );

        user = userRepository.save(user);

        String designation = (req.getDesignation() != null
                && !req.getDesignation().isBlank())
                ? req.getDesignation().trim()
                : "Junior Public Works Inspector";

        String panchayat = (req.getPanchayatName() != null
                && !req.getPanchayatName().isBlank())
                ? req.getPanchayatName().trim()
                : "Sundarapuram Gram Panchayat";

        EmployeeProfile profile = new EmployeeProfile(
                user,
                code,
                designation,
                panchayat,
                false,
                false
        );

        profile.setApprovedAt(null);
        employeeProfileRepository.save(profile);
    }

    public AuthResponse loginVillager(LoginRequest req) {

        User user = userRepository.findByEmail(
                        req.getEmail().trim().toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password."));

        if (!passwordEncoder.matches(
                req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException(
                    "Invalid email or password.");
        }

        if (user.getRole() != Role.VILLAGER) {
            throw new SecurityException(
                    "Employee accounts must use the Panchayat Employee Login portal.");
        }

        String token = jwtUtils.generateJwtToken(
                user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getPointsBalance()
        );
    }

    public AuthResponse generateEmployeeAuthResponse(User employee) {

        String token = jwtUtils.generateJwtToken(
                employee.getEmail(), employee.getRole().name());

        AuthResponse response = new AuthResponse(
                token,
                employee.getId(),
                employee.getEmail(),
                employee.getFullName(),
                employee.getRole(),
                employee.getPointsBalance()
        );

        employeeProfileRepository.findByUser(employee)
                .ifPresent(profile -> {
                    response.setEmployeeCode(profile.getEmployeeCode());
                    response.setDesignation(profile.getDesignation());
                    response.setPanchayatName(profile.getPanchayatName());
                });

        return response;
    }

    public AuthResponse loginAdmin(LoginRequest req) {

        User user = userRepository.findByEmail(
                        req.getEmail().trim().toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password."));

        if (!passwordEncoder.matches(
                req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException(
                    "Invalid email or password.");
        }

        if (user.getRole() != Role.ADMIN) {
            throw new SecurityException(
                    "Access denied. Only administrators can log in here.");
        }

        String token = jwtUtils.generateJwtToken(
                user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getPointsBalance()
        );
    }
}
