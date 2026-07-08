package com.lancea.personal_finance_loan_api.entity;

import com.lancea.personal_finance_loan_api.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loans")
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private String loanName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal principal;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal annualRate;

    @Column(nullable = false)
    private int termMonths;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal monthlyPayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(nullable = false)
    private Instant disbursedAt;

    @Column(nullable = false)
    private Instant maturityDate;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column
    private Instant deletedAt;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paymentNumber ASC")
    private List<LoanSchedule> schedule = new ArrayList<>();
}
