package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.dcm4che.ris.api.enums.OrderStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Imaging Service Request (Order) entity.
 * TODO: Complete implementation
 */
@Entity
@Table(name = "imaging_service_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagingServiceRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "placer_order_number", length = 64)
    private String placerOrderNumber;

    @Column(name = "filler_order_number", length = 64)
    private String fillerOrderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 16)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(name = "order_date_time")
    private LocalDateTime orderDateTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderDateTime == null) {
            orderDateTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
