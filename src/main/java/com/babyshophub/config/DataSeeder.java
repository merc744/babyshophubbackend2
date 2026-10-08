package com.babyshophub.config;

import com.babyshophub.entity.User;
import com.babyshophub.enums.Role;
import com.babyshophub.repository.UserRepository;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${APP_ADMIN_EMAIL:admin@babyshophub.com}")
    private String adminEmail;

    // Added a default fallback password ("Admin12345!") so it seeds immediately without extra setup
    @Value("${APP_ADMIN_INITIAL_PASSWORD:Admin12345!}")
    private String initialPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (initialPassword == null || initialPassword.isBlank()) {
            System.out.println(">>> Admin bootstrap skipped: set APP_ADMIN_INITIAL_PASSWORD to seed an admin account.");
            return;
        }

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(initialPassword));
            admin.setPhoneNumber("08000000000");
            admin.setEnabled(true);
            admin.setDob(LocalDate.of(1990, 1, 1));

            admin.getRoles().add(Role.ROLE_ADMIN);

            userRepository.save(admin);
            System.out.println(">>> Default Admin account created successfully: " + adminEmail);
        }
    }
}
