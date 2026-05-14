package com.cc91.config;

import com.cc91.entity.User;
import com.cc91.entity.UserProfile;
import com.cc91.repository.UserProfileRepository;
import com.cc91.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.dev-data", name = "enabled", havingValue = "true")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureUser("admin", "admin@cc91.com", "admin123", "ADMIN");
        ensureUser("test", "test@cc91.com", "admin123", "USER");
    }

    private void ensureUser(String username, String email, String password, String role) {
        User user = userRepository.findByUsername(username).orElseGet(() -> {
            User created = new User(username, email, passwordEncoder.encode(password));
            logger.info("Created development user: {}", username);
            return created;
        });

        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(role);
        user.setIsLocked(false);
        user.setLockUntil(null);
        user.setFailedLoginAttempts(0);
        User savedUser = userRepository.save(user);

        userProfileRepository.findByUserId(savedUser.getId()).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUser(savedUser);
            return userProfileRepository.save(profile);
        });
    }
}
