package com.lancea.personal_finance_loan_api.entity;

import com.lancea.personal_finance_loan_api.enums.LoanScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "loan_schedules")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private int paymentNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal paymentAmount;

    @Column (nullable = false, precision = 19, scale = 4)
    private BigDecimal principalPortion;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal interestPortion;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingBalance;

    // LocalDate — due date is a calendar date, not a moment in time
    @Column(nullable = false)
    private LocalDate dueDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanScheduleStatus status = LoanScheduleStatus.PENDING;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
