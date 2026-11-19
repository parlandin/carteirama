package com.parlandim.carteirama.model;

import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.model.enums.PersonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "person_type", nullable = false, columnDefinition = "person_type")
    private PersonType personType;

    @Column(nullable = false, unique = true, length = 14)
    private String document;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "company_name")
    private String companyName;

    @Column(nullable = false, length = 11)
    private String phone;

    @Column(length = 8)
    private String zip;

    private String address;

    private String city;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 2)
    private String state;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "account_status")
    private AccountStatus status = AccountStatus.trial;

    @Column(name = "trial_ends_at", nullable = false)
    private Instant trialEndsAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "gateway_customer_id", unique = true)
    private String gatewayCustomerId;
}
