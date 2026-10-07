package com.babyshophub.config;

import com.babyshophub.entity.User;
import com.babyshophub.enums.Role;
import com.babyshophub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${APP_ADMIN_EMAIL:admin@example.com}")
    private String adminEmail;

    @Value("${APP_ADMIN_INITIAL_PASSWORD:}")
    private String adminInitialPassword;

    @Value("${APP_DEMO_USERS_INITIAL_PASSWORD:}")
    private String demoUsersInitialPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedDemoUsers();
    }

    private void seedAdmin() {
        if (adminInitialPassword == null || adminInitialPassword.isBlank()) {
            System.out.println(">>> Admin bootstrap skipped: set APP_ADMIN_INITIAL_PASSWORD to seed an admin account.");
            return;
        }

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = createUser(
                    "System Admin",
                    adminEmail,
                    adminInitialPassword,
                    "08000000000",
                    LocalDate.of(1990, 1, 1),
                    Role.ROLE_ADMIN
            );
            userRepository.save(admin);
            System.out.println(">>> Admin account created successfully: " + adminEmail);
        }
    }

    private void seedDemoUsers() {
        if (demoUsersInitialPassword == null || demoUsersInitialPassword.isBlank()) {
            System.out.println(">>> Demo user seeding skipped: set APP_DEMO_USERS_INITIAL_PASSWORD to seed demo accounts.");
            return;
        }

        List<User> users = List.of(
                createUser("John", "john@example.com", demoUsersInitialPassword, "08000000001", LocalDate.of(1995, 1, 1), Role.ROLE_CUSTOMER),
                createUser("Jane", "jane@example.com", demoUsersInitialPassword, "08000000002", LocalDate.of(1996, 1, 1), Role.ROLE_CUSTOMER),
                createUser("Mike", "mike@example.com", demoUsersInitialPassword, "08000000003", LocalDate.of(1994, 1, 1), Role.ROLE_CUSTOMER)
        );

        users.stream()
                .filter(user -> !userRepository.existsByEmail(user.getEmail()))
                .forEach(userRepository::save);
    }

    private User createUser(
            String name,
            String email,
            String password,
            String phoneNumber,
            LocalDate dob,
            Role role) {

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhoneNumber(phoneNumber);
        user.setDob(dob);
        user.setEnabled(true);
        user.getRoles().add(role);
        return user;
    }
}
