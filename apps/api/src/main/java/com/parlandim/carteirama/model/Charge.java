package com.parlandim.carteirama.model;

import com.parlandim.carteirama.model.enums.ChargeKind;
import com.parlandim.carteirama.model.enums.ChargeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "charges")
public class Charge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "charge_kind")
    private ChargeKind kind = ChargeKind.recurring;

    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "charge_status")
    private ChargeStatus status = ChargeStatus.pending;

    @Column(name = "paid_at")
    private LocalDate paidAt;

    private String note;

    @Setter(lombok.AccessLevel.NONE)
    @Formula("(select coalesce(sum(e.amount_cents), 0) from client_extras e where e.client_id = client_id and e.kind = 'bonus' and kind = 'recurring' and e.start_month <= reference_month and (e.end_month is null or e.end_month >= reference_month))")
    private long bonusCents;

    @Setter(lombok.AccessLevel.NONE)
    @Formula("(select coalesce(sum(e.amount_cents), 0) from client_extras e where e.client_id = client_id and e.kind = 'cost' and kind = 'recurring' and e.start_month <= reference_month and (e.end_month is null or e.end_month >= reference_month))")
    private long costCents;

    public long getTotalCents() {
        return amountCents + bonusCents;
    }

    public long getNetCents() {
        return getTotalCents() - costCents;
    }
}
