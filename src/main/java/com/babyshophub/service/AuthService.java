package com.babyshophub.service;

import com.babyshophub.dto.ChangePasswordRequest;
import com.babyshophub.dto.LoginRequest;
import com.babyshophub.dto.RegisterRequest;
import com.babyshophub.dto.ResetPasswordRequest;
import com.babyshophub.entity.User;
import com.babyshophub.entity.PasswordResetOtp;
import com.babyshophub.exception.LoginFailedException;
import com.babyshophub.exception.LoginVerificationRequiredException;
import com.babyshophub.enums.Role;
import com.babyshophub.repository.UserRepository;
import com.babyshophub.repository.PasswordResetOtpRepository;


import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.security.SecureRandom;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final ActivityLogService activityLogService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       AuthenticationManager authenticationManager,
                       PasswordResetOtpRepository passwordResetOtpRepository,
                       ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.activityLogService = activityLogService;
    }

    public String registerCustomer(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered!");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDob(request.getDob());
        user.setPhoneNumber(request.getPhoneNumber());
        user.getRoles().add(Role.ROLE_CUSTOMER);

        return saveUserAndSendOtp(user);
    }

    private String saveUserAndSendOtp(User user) {
        String otp = generateVerificationCode();
        user.setVerificationCode(otp);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);

        userRepository.save(user);
        boolean emailSent = emailService.sendVerificationEmail(user.getEmail(), otp);
        activityLogService.record(user, "REGISTER", "Customer account registered");

        return emailSent
            ? "Registration successful! Please check your email for the verification code."
            : "Registration successful! Check the server console for the verification code.";
    }

    public String verifyAccount(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) {
            return "Account is already verified.";
        }

        if (user.getVerificationCodeExpiresAt() != null && user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code has expired. Please request a new one.");
        }

        if (!user.getVerificationCode().equals(code)) {
            throw new RuntimeException("Invalid verification code.");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
        activityLogService.record(user, "ACCOUNT_VERIFIED", "Email verified successfully");

        return "Email verified successfully! You can now log in.";
    }

    public String resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) {
            return "Account is already verified.";
        }

        String otp = generateVerificationCode();
        user.setVerificationCode(otp);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        boolean emailSent = emailService.sendVerificationEmail(user.getEmail(), otp);

        return emailSent
            ? "A new verification code has been sent to your email."
            : "A new verification code was printed in the server console.";
    }

    public Authentication loginUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new LoginFailedException("Invalid email or password"));

        if (user.isLocked()) {
            throw new LoginFailedException("Account temporarily locked due to too many failed login attempts. Please try again later.");
        }

        if (!user.isEnabled()) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                registerFailedLogin(user);
                throw new LoginFailedException("Invalid email or password");
            }
            String response = resendVerificationCode(user.getEmail());
            throw new LoginVerificationRequiredException(response);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
            return authentication;
        } catch (org.springframework.security.core.AuthenticationException e) {
            registerFailedLogin(user);
            throw new LoginFailedException("Invalid email or password");
        }
    }

    private void registerFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            user.setFailedLoginAttempts(0);
            activityLogService.record(user, "ACCOUNT_LOCKED",
                    "Account locked for " + LOCKOUT_MINUTES + " minutes after " + MAX_LOGIN_ATTEMPTS + " failed login attempts");
        }
        userRepository.save(user);
    }

    public void recordLogin(String email) {
        activityLogService.record(email, "LOGIN", "Successful login");
    }
    private String generateVerificationCode() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
    
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with this email"));

        var previous = passwordResetOtpRepository.findTopByUserEmailOrderByCreatedAtDesc(email);
        if (previous.isPresent() && previous.get().getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
            throw new RuntimeException("Please wait before requesting another password reset code.");
        }
        passwordResetOtpRepository.findAllByUserEmailAndUsedFalse(email).forEach(otp -> {
            otp.setUsed(true);
            passwordResetOtpRepository.save(otp);
        });

        String resetCode = generateVerificationCode();
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setUser(user);
        otp.setOtpHash(passwordEncoder.encode(resetCode));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        passwordResetOtpRepository.save(otp);

        boolean emailSent = emailService.sendPasswordResetEmail(user.getEmail(), resetCode);

        return emailSent
            ? "Password reset code has been sent to your email."
            : "Password reset code was printed in the server console.";
    }

    public String resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match.");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new RuntimeException("Invalid reset code."));
        PasswordResetOtp otp = passwordResetOtpRepository.findTopByUserEmailAndUsedFalseOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset code."));
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setUsed(true);
            passwordResetOtpRepository.save(otp);
            throw new RuntimeException("Reset code has expired.");
        }
        if (otp.getAttempts() >= 5) throw new RuntimeException("Too many attempts. Request a new reset code.");
        if (!passwordEncoder.matches(request.getResetCode(), otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= 5) otp.setUsed(true);
            passwordResetOtpRepository.save(otp);
            throw new RuntimeException("Invalid reset code.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        otp.setUsed(true);
        passwordResetOtpRepository.save(otp);
        userRepository.save(user);
        activityLogService.record(user, "PASSWORD_RESET", "Password reset via reset code");

        return "Password has been successfully reset. You can now log in.";
    }

    public String changePassword(String email, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password changed successfully!";
    }
    
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        userRepository.delete(user);
    }
}
