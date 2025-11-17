package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.dcm4che.ris.api.enums.OrderStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Imaging Service Request (Order) entity.
 * Represents an order for imaging procedures from HIS/EMR.
 * Based on IHE Scheduled Workflow Profile and HL7 ORM message.
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "imaging_service_request", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"placer_order_number"}),
    @UniqueConstraint(columnNames = {"filler_order_number"})
}, indexes = {
    @Index(name = "idx_order_status", columnList = "order_status"),
    @Index(name = "idx_order_date", columnList = "order_date_time")
})
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

    /**
     * Patient for this order
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Visit/Admission (optional)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    /**
     * Placer Order Number (0040,2016)
     * Order number from the ordering system (HIS/EMR)
     * HL7: ORC-2
     */
    @Size(max = 64)
    @Column(name = "placer_order_number", unique = true, length = 64)
    private String placerOrderNumber;

    /**
     * Filler Order Number (0040,2017)
     * Order number assigned by RIS
     * HL7: ORC-3
     */
    @Size(max = 64)
    @Column(name = "filler_order_number", unique = true, length = 64)
    private String fillerOrderNumber;

    /**
     * Requesting/Ordering Physician
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_physician_id")
    private Physician requestingPhysician;

    /**
     * Order Date/Time
     * HL7: ORC-9
     */
    @Column(name = "order_date_time")
    private LocalDateTime orderDateTime;

    /**
     * Order Status
     * HL7: ORC-5
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 16)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    /**
     * Order Priority
     * Values: ROUTINE, URGENT, STAT
     * HL7: ORC-7
     */
    @Size(max = 16)
    @Column(name = "order_priority", length = 16)
    @Builder.Default
    private String orderPriority = "ROUTINE";

    /**
     * Reason for Exam (0040,1002)
     * HL7: OBR-31
     */
    @Column(name = "reason_for_exam", columnDefinition = "TEXT")
    private String reasonForExam;

    /**
     * Reason for Exam Code (ICD-10, SNOMED, etc.)
     * (0040,100A)
     */
    @Size(max = 64)
    @Column(name = "reason_for_exam_code", length = 64)
    private String reasonForExamCode;

    /**
     * Clinical Information
     * HL7: OBR-13
     */
    @Column(name = "clinical_info", columnDefinition = "TEXT")
    private String clinicalInfo;

    /**
     * Patient Transport Arrangements
     */
    @Size(max = 64)
    @Column(name = "patient_transport", length = 64)
    private String patientTransport;

    /**
     * Confidentiality Code
     * Values: NORMAL, RESTRICTED, VIP
     */
    @Size(max = 16)
    @Column(name = "confidentiality_code", length = 16)
    @Builder.Default
    private String confidentialityCode = "NORMAL";

    /**
     * Requesting Department/Service
     */
    @Size(max = 64)
    @Column(name = "requesting_department", length = 64)
    private String requestingDepartment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships

    /**
     * Requested Procedures in this order
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequestedProcedure> requestedProcedures = new ArrayList<>();

    // JPA Callbacks

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderDateTime == null) {
            orderDateTime = LocalDateTime.now();
        }
        if (orderStatus == null) {
            orderStatus = OrderStatus.PENDING;
        }
        if (orderPriority == null) {
            orderPriority = "ROUTINE";
        }
        if (confidentialityCode == null) {
            confidentialityCode = "NORMAL";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Add a Requested Procedure to this order
     */
    public void addRequestedProcedure(RequestedProcedure procedure) {
        requestedProcedures.add(procedure);
        procedure.setOrder(this);
    }

    /**
     * Remove a Requested Procedure
     */
    public void removeRequestedProcedure(RequestedProcedure procedure) {
        requestedProcedures.remove(procedure);
        procedure.setOrder(null);
    }

    /**
     * Transition order status with validation
     */
    public void transitionTo(OrderStatus newStatus) {
        if (!orderStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition order from %s to %s", orderStatus, newStatus));
        }
        this.orderStatus = newStatus;
    }

    /**
     * Check if order is urgent
     */
    public boolean isUrgent() {
        return "URGENT".equalsIgnoreCase(orderPriority) ||
               "STAT".equalsIgnoreCase(orderPriority);
    }

    /**
     * Check if all requested procedures are completed
     */
    public boolean areAllProceduresCompleted() {
        return requestedProcedures.stream()
            .allMatch(RequestedProcedure::areAllSPSCompleted);
    }

    /**
     * Auto-update order status based on procedure statuses
     */
    public void updateStatusFromProcedures() {
        if (requestedProcedures.isEmpty()) {
            return;
        }

        boolean anyInProgress = requestedProcedures.stream()
            .anyMatch(rp -> rp.getScheduledProcedureSteps().stream()
                .anyMatch(sps -> sps.getSpsStatus() == org.dcm4che.ris.api.enums.SPSStatus.STARTED));

        boolean allCompleted = areAllProceduresCompleted();

        if (allCompleted && orderStatus.canTransitionTo(OrderStatus.COMPLETED)) {
            transitionTo(OrderStatus.COMPLETED);
        } else if (anyInProgress && orderStatus.canTransitionTo(OrderStatus.IN_PROGRESS)) {
            transitionTo(OrderStatus.IN_PROGRESS);
        } else if (!requestedProcedures.isEmpty() &&
                   orderStatus.canTransitionTo(OrderStatus.SCHEDULED)) {
            transitionTo(OrderStatus.SCHEDULED);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImagingServiceRequest)) return false;
        ImagingServiceRequest that = (ImagingServiceRequest) o;
        return orderId != null && orderId.equals(that.orderId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ImagingServiceRequest{" +
                "orderId=" + orderId +
                ", placerOrderNumber='" + placerOrderNumber + '\'' +
                ", fillerOrderNumber='" + fillerOrderNumber + '\'' +
                ", orderStatus=" + orderStatus +
                ", orderPriority='" + orderPriority + '\'' +
                '}';
    }
}
