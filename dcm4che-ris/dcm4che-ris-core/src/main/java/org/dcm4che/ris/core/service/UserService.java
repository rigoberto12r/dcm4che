package org.dcm4che.ris.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.ris.api.entity.User;
import org.dcm4che.ris.core.exception.AuthenticationException;
import org.dcm4che.ris.core.exception.DuplicateResourceException;
import org.dcm4che.ris.core.exception.ResourceNotFoundException;
import org.dcm4che.ris.persistence.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing users and authentication.
 *
 * @author dcm4che-ris
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Configuration constants
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCKOUT_MINUTES = 30;
    private static final int DEFAULT_LOCK_TIMEOUT_MINUTES = 15;

    /**
     * Create a new user.
     */
    public User createUser(User user, String rawPassword) {
        log.info("Creating new user: {}", user.getUsername());

        // Check for duplicate username
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("User", "username", user.getUsername());
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User", "email", user.getEmail());
        }

        // Hash password
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setPasswordChangedAt(LocalDateTime.now());

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * Authenticate a user with username and password.
     */
    public User authenticate(String username, String rawPassword) {
        log.debug("Authenticating user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        // Check if account is locked
        if (user.isCurrentlyLocked()) {
            log.warn("Authentication attempt for locked account: {}", username);
            throw new AuthenticationException("Account is locked. Please try again later.");
        }

        // Check if account is active
        if (!user.getIsActive()) {
            log.warn("Authentication attempt for inactive account: {}", username);
            throw new AuthenticationException("Account is inactive");
        }

        // Verify password
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            // Record failed login
            user.recordFailedLogin(MAX_FAILED_LOGIN_ATTEMPTS, ACCOUNT_LOCKOUT_MINUTES);
            userRepository.save(user);

            log.warn("Failed login attempt for user: {}", username);
            throw new AuthenticationException("Invalid username or password");
        }

        // Successful login - record it
        user.recordSuccessfulLogin();
        userRepository.save(user);

        log.info("User authenticated successfully: {}", username);
        return user;
    }

    /**
     * Change user password.
     */
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("Changing password for user ID: {}", userId);

        User user = getUserById(userId);

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        // Hash and save new password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);

        userRepository.save(user);
        log.info("Password changed successfully for user: {}", user.getUsername());
    }

    /**
     * Reset user password (admin function).
     */
    public String resetPassword(Long userId) {
        log.info("Resetting password for user ID: {}", userId);

        User user = getUserById(userId);

        // Generate temporary password (in production, use a proper password generator)
        String tempPassword = generateTemporaryPassword();

        // Hash and save
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(true);

        userRepository.save(user);

        log.info("Password reset for user: {}. Temporary password issued.", user.getUsername());
        return tempPassword; // Return to admin to give to user
    }

    /**
     * Lock a user account (admin function).
     */
    public void lockUser(Long userId) {
        log.info("Locking user account: {}", userId);

        User user = getUserById(userId);
        user.setIsLocked(true);
        userRepository.save(user);

        log.info("User account locked: {}", user.getUsername());
    }

    /**
     * Unlock a user account.
     */
    public void unlockUser(Long userId) {
        log.info("Unlocking user account: {}", userId);

        User user = getUserById(userId);
        user.unlock();
        userRepository.save(user);

        log.info("User account unlocked: {}", user.getUsername());
    }

    /**
     * Get user by ID.
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Get user by username.
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username));
    }

    /**
     * Get user by email.
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: " + email));
    }

    /**
     * Update user information.
     */
    public User updateUser(Long userId, User updatedUser) {
        log.info("Updating user: {}", userId);

        User existingUser = getUserById(userId);

        // Check username change
        if (!existingUser.getUsername().equals(updatedUser.getUsername())) {
            if (userRepository.existsByUsername(updatedUser.getUsername())) {
                throw new DuplicateResourceException("User", "username", updatedUser.getUsername());
            }
        }

        // Check email change
        if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
            if (userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new DuplicateResourceException("User", "email", updatedUser.getEmail());
            }
        }

        // Keep the same password hash if not changing password
        updatedUser.setPasswordHash(existingUser.getPasswordHash());
        updatedUser.setPasswordChangedAt(existingUser.getPasswordChangedAt());
        updatedUser.setUserId(userId);

        User savedUser = userRepository.save(updatedUser);
        log.info("User updated: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * Deactivate user account.
     */
    public void deactivateUser(Long userId) {
        log.info("Deactivating user: {}", userId);

        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deactivated: {}", user.getUsername());
    }

    /**
     * Reactivate user account.
     */
    public void reactivateUser(Long userId) {
        log.info("Reactivating user: {}", userId);

        User user = getUserById(userId);
        user.setIsActive(true);
        userRepository.save(user);

        log.info("User reactivated: {}", user.getUsername());
    }

    /**
     * Get all active users.
     */
    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    /**
     * Get users by role.
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(String role) {
        return userRepository.findByRoleAndIsActiveTrue(role);
    }

    /**
     * Get all radiologists.
     */
    @Transactional(readOnly = true)
    public List<User> getRadiologists() {
        return userRepository.findRadiologists();
    }

    /**
     * Get all technologists.
     */
    @Transactional(readOnly = true)
    public List<User> getTechnologists() {
        return userRepository.findTechnologists();
    }

    /**
     * Get users who can sign reports.
     */
    @Transactional(readOnly = true)
    public List<User> getUsersWhoCanSignReports() {
        return userRepository.findUsersWhoCanSignReports();
    }

    /**
     * Search users by name.
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByName(String name) {
        return userRepository.searchByName(name);
    }

    /**
     * Get recently logged in users.
     */
    @Transactional(readOnly = true)
    public List<User> getRecentlyLoggedInUsers(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return userRepository.findRecentlyLoggedIn(since);
    }

    // Helper methods

    /**
     * Generate a temporary password.
     * In production, use a cryptographically secure random generator.
     */
    private String generateTemporaryPassword() {
        // Simple implementation - replace with secure generator
        return "Temp" + System.currentTimeMillis();
    }

    /**
     * Check if username exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
