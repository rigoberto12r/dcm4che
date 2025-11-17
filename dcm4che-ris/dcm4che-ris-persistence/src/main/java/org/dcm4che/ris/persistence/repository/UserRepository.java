package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for User entity.
 * Manages system users and authentication.
 *
 * @author dcm4che-ris
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Find user by username (for authentication)
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find all active users
     */
    List<User> findByIsActiveTrue();

    /**
     * Find users by role
     */
    List<User> findByRoleAndIsActiveTrue(String role);

    /**
     * Find all radiologists
     */
    @Query("SELECT u FROM User u WHERE u.role IN ('RADIOLOGIST', 'RESIDENT', 'FELLOW') AND u.isActive = true")
    List<User> findRadiologists();

    /**
     * Find all technologists
     */
    @Query("SELECT u FROM User u WHERE u.role = 'TECHNOLOGIST' AND u.isActive = true")
    List<User> findTechnologists();

    /**
     * Find all administrators
     */
    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN' AND u.isActive = true")
    List<User> findAdministrators();

    /**
     * Find users who can sign reports
     */
    @Query("SELECT u FROM User u WHERE " +
           "(u.physician IS NOT NULL AND u.physician.canSignReports = true) OR " +
           "u.role IN ('RADIOLOGIST', 'ADMIN') " +
           "AND u.isActive = true")
    List<User> findUsersWhoCanSignReports();

    /**
     * Find locked users
     */
    @Query("SELECT u FROM User u WHERE u.isLocked = true")
    List<User> findLockedUsers();

    /**
     * Find users with expiring passwords
     */
    @Query("SELECT u FROM User u WHERE u.passwordChangedAt < :thresholdDate AND u.isActive = true")
    List<User> findUsersWithExpiringPasswords(@Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * Find users who must change password
     */
    @Query("SELECT u FROM User u WHERE u.mustChangePassword = true AND u.isActive = true")
    List<User> findUsersMustChangePassword();

    /**
     * Find users with 2FA enabled
     */
    List<User> findByTwoFactorEnabledTrue();

    /**
     * Find users by physician
     */
    @Query("SELECT u FROM User u WHERE u.physician.physicianId = :physicianId")
    List<User> findByPhysicianPhysicianId(@Param("physicianId") Long physicianId);

    /**
     * Find users logged in recently
     */
    @Query("SELECT u FROM User u WHERE u.lastLogin > :since ORDER BY u.lastLogin DESC")
    List<User> findRecentlyLoggedIn(@Param("since") LocalDateTime since);

    /**
     * Find inactive users (not logged in for a long time)
     */
    @Query("SELECT u FROM User u WHERE u.lastLogin < :since OR u.lastLogin IS NULL")
    List<User> findInactiveUsers(@Param("since") LocalDateTime since);

    /**
     * Count users by role
     */
    long countByRoleAndIsActiveTrue(String role);

    /**
     * Count active users
     */
    long countByIsActiveTrue();

    /**
     * Search users by name
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%')) AND u.isActive = true")
    List<User> searchByName(@Param("name") String name);
}
