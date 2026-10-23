package com.fillthepockets.service;

import com.fillthepockets.dto.EmployeeOtpResponse;
import com.fillthepockets.entity.EmployeeProfile;
import com.fillthepockets.entity.OtpRecord;
import com.fillthepockets.entity.Role;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.EmployeeProfileRepository;
import com.fillthepockets.repository.OtpRecordRepository;
import com.fillthepockets.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmployeeVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeVerificationService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private OtpRecordRepository otpRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.demo.mode:true}")
    private boolean demoMode;

    @Value("${app.otp.expiration-seconds:300}")
    private int otpExpirationSeconds;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Step 1 of Employee Login: Validate credentials, verify PANCHAYAT_EMPLOYEE role,
     * generate a 6-digit verification code, and prepare the response for the pop-up modal.
     */
    @Transactional
    public EmployeeOtpResponse initiateEmployeeLogin(String email, String rawPassword) {
        String normalizedEmail = (email != null) ? email.trim().toLowerCase() : "";
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee email or password."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid employee email or password.");
        }

        if (user.getRole() != Role.PANCHAYAT_EMPLOYEE) {
            throw new SecurityException("Access Denied: This account does not have Panchayat Employee privileges.");
        }

        EmployeeProfile profile = employeeProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Employee profile not found."));

        if (!profile.getIsApproved()) {
            throw new SecurityException("Employee account pending administrative approval.");
        }

        // Invalidate previous unexpired OTPs
        otpRecordRepository.findTopByEmailAndUsedFalseOrderByIdDesc(normalizedEmail)
                .ifPresent(existing -> {
                    existing.setUsed(true);
                    otpRecordRepository.save(existing);
                });

        // Generate 6-digit secure code
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(otpExpirationSeconds);

        OtpRecord otpRecord = new OtpRecord(normalizedEmail, code, expiresAt);
        otpRecordRepository.save(otpRecord);

        log.info("[2FA OTP] Generated code for {}: {} (Expires in {}s)", normalizedEmail, code, otpExpirationSeconds);

        String message;
        String demoCodeValue = null;
        if (demoMode) {
            message = "College Demo Mode: A 6-digit verification code has been generated.";
            demoCodeValue = code;
        } else {
            message = "Verification code generated.";
            // In real production, trigger SMS/Email gateway here
        }

        return new EmployeeOtpResponse(
                user.getEmail(),
                user.getFullName(),
                profile.getEmployeeCode(),
                profile.getDesignation(),
                message,
                demoMode,
                demoCodeValue,
                otpExpirationSeconds
        );
    }

    /**
     * Step 2 of Employee Login: Validate verification code, check expiry & rate limit,
     * and confirm authorization.
     */
    @Transactional(noRollbackFor = {IllegalArgumentException.class, SecurityException.class})
    public User verifyEmployeeCode(String email, String code) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required for verification.");
        }
        if (code == null || code.trim().length() != 6) {
            throw new IllegalArgumentException("Verification code must be exactly 6 digits.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Employee account not found."));

        if (user.getRole() != Role.PANCHAYAT_EMPLOYEE) {
            throw new SecurityException("Access Denied: Role mismatch.");
        }

        EmployeeProfile profile = employeeProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Employee profile not found."));

        if (!profile.getIsApproved()) {
            throw new SecurityException("Employee account pending administrative approval.");
        }

        Optional<OtpRecord> latestOtpOpt = otpRecordRepository.findTopByEmailOrderByIdDesc(normalizedEmail);
        if (latestOtpOpt.isEmpty()) {
            throw new IllegalArgumentException("No verification code found. Please log in first.");
        }

        OtpRecord otp = latestOtpOpt.get();

        // 1. Check if previously used
        if (Boolean.TRUE.equals(otp.getUsed())) {
            throw new IllegalArgumentException("This verification code has already been used. Please log in again to generate a new code.");
        }

        // 2. Check if expired
        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            otp.setUsed(true);
            otpRecordRepository.save(otp);
            throw new IllegalArgumentException("Verification code has expired. Please log in again.");
        }

        // 3. Check if retry limit already reached
        if (otp.getAttempts() >= maxAttempts) {
            otp.setUsed(true);
            otpRecordRepository.save(otp);
            throw new SecurityException("Maximum incorrect verification attempts exceeded. Code has been invalidated. Please log in again.");
        }

        // 4. Check if code matches
        if (!otp.getOtpCode().equals(code.trim())) {
            int currentAttempts = otp.getAttempts() + 1;
            otp.setAttempts(currentAttempts);
            if (currentAttempts >= maxAttempts) {
                otp.setUsed(true);
                otpRecordRepository.save(otp);
                throw new SecurityException("Maximum incorrect verification attempts exceeded. Code has been invalidated. Please log in again.");
            }
            otpRecordRepository.save(otp);
            int remaining = maxAttempts - currentAttempts;
            throw new IllegalArgumentException("Incorrect verification code. " + remaining + " attempt(s) remaining.");
        }

        // Mark code as successfully used
        otp.setUsed(true);
        otpRecordRepository.save(otp);

        log.info("Employee {} successfully verified 2FA code.", normalizedEmail);
        return user;
    }
}
