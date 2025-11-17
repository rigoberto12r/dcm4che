package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * User entity.
 * Represents system users (radiologists, technologists, administrators, etc.)
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "ris_user", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"username"}),
    @UniqueConstraint(columnNames = {"email"})
}, indexes = {
    @Index(name = "idx_user_role", columnList = "role"),
    @Index(name = "idx_user_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /**
     * Username for login
     */
    @NotBlank
    @Size(max = 64)
    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    /**
     * Password hash (bcrypt, argon2, etc.)
     * Should never be logged or displayed
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Full name of the user
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /**
     * Email address
     */
    @Email
    @Size(max = 255)
    @Column(name = "email", unique = true, length = 255)
    private String email;

    /**
     * Phone number
     */
    @Size(max = 64)
    @Column(name = "phone", length = 64)
    private String phone;

    /**
     * User role/permission level
     * Values: ADMIN, RADIOLOGIST, RESIDENT, TECHNOLOGIST, RECEPTIONIST,
     *         REFERRING_PHYSICIAN, TRANSCRIPTIONIST, NURSE
     */
    @NotBlank
    @Size(max = 32)
    @Column(name = "role", nullable = false, length = 32)
    private String role;

    /**
     * Linked Physician entity (if user is a physician/radiologist)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physician_id")
    private Physician physician;

    /**
     * Digital signature for report signing
     */
    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;

    /**
     * User preferences (JSON)
     * Stores UI preferences, default settings, etc.
     */
    @Column(name = "preferences", columnDefinition = "TEXT")
    private String preferences;

    /**
     * Is user account active
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Account locked (after failed login attempts)
     */
    @Column(name = "is_locked")
    @Builder.Default
    private Boolean isLocked = false;

    /**
     * Last login timestamp
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Failed login attempts counter
     */
    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * Account locked until (temporary lock)
     */
    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    /**
     * Password last changed date
     */
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    /**
     * Must change password on next login
     */
    @Column(name = "must_change_password")
    @Builder.Default
    private Boolean mustChangePassword = false;

    /**
     * Two-factor authentication enabled
     */
    @Column(name = "two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    /**
     * Two-factor authentication secret (base32 encoded)
     */
    @Size(max = 255)
    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret;

    /**
     * User notes/comments
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // JPA Callbacks

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (isLocked == null) {
            isLocked = false;
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
        if (mustChangePassword == null) {
            mustChangePassword = false;
        }
        if (twoFactorEnabled == null) {
            twoFactorEnabled = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Record successful login
     */
    public void recordSuccessfulLogin() {
        this.lastLogin = LocalDateTime.now();
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    /**
     * Record failed login attempt
     */
    public void recordFailedLogin(int maxAttempts, int lockoutMinutes) {
        this.failedLoginAttempts++;

        if (this.failedLoginAttempts >= maxAttempts) {
            this.isLocked = true;
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
        }
    }

    /**
     * Unlock account
     */
    public void unlock() {
        this.isLocked = false;
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    /**
     * Check if account is currently locked
     */
    public boolean isCurrentlyLocked() {
        if (!isLocked) {
            return false;
        }

        // Check if temporary lock has expired
        if (accountLockedUntil != null && LocalDateTime.now().isAfter(accountLockedUntil)) {
            unlock();
            return false;
        }

        return true;
    }

    /**
     * Check if user has given role
     */
    public boolean hasRole(String role) {
        return this.role != null && this.role.equalsIgnoreCase(role);
    }

    /**
     * Check if user is a radiologist
     */
    public boolean isRadiologist() {
        return hasRole("RADIOLOGIST") || hasRole("RESIDENT") || hasRole("FELLOW");
    }

    /**
     * Check if user is an admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if user can sign reports
     */
    public boolean canSignReports() {
        if (physician != null && physician.getCanSignReports()) {
            return true;
        }
        return hasRole("RADIOLOGIST") || hasRole("ADMIN");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return userId != null && userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}

