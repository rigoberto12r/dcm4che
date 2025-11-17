package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Physician entity (stub).
 * TODO: Complete implementation
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "physician")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Physician implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "physician_id")
    private Long physicianId;

    @Column(name = "physician_name", length = 255)
    private String physicianName;

    @Column(name = "specialty", length = 64)
    private String specialty;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
