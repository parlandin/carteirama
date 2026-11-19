package com.parlandim.carteirama.common.time;

import com.parlandim.carteirama.common.error.ValidationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Component
public class BusinessClock {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");

    private final Clock clock;

    public BusinessClock(Clock clock) {
        this.clock = clock;
    }

    public Instant now() {
        return clock.instant();
    }

    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZONE);
    }

    public YearMonth currentMonth() {
        return YearMonth.from(today());
    }

    public boolean isLate(boolean pending, LocalDate dueDate) {
        return pending && dueDate.isBefore(today());
    }

    public YearMonth parseMonth(String field, String value) {
        if (value == null || value.isBlank()) {
            return currentMonth();
        }
        if (!MONTH.matcher(value).matches()) {
            throw ValidationException.of(field, "invalid_format");
        }
        YearMonth month;
        try {
            month = YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw ValidationException.of(field, "invalid_format");
        }
        YearMonth current = currentMonth();
        if (month.isBefore(current.minusMonths(12)) || month.isAfter(current.plusMonths(12))) {
            throw ValidationException.of(field, "out_of_range");
        }
        return month;
    }
}
