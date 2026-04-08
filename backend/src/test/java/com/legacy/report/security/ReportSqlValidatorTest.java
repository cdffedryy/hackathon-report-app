package com.legacy.report.security;

import com.legacy.report.config.ReportSecurityProperties;
import com.legacy.report.exception.ReportSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReportSqlValidatorTest {

    private ReportSqlValidator validator;

    @BeforeEach
    void setUp() {
        ReportSecurityProperties properties = new ReportSecurityProperties(
                List.of("customer"),
                List.of(";", "--"),
                5000
        );
        validator = new ReportSqlValidator(properties);
    }

    @Test
    void shouldPassSimpleSelect() {
        assertDoesNotThrow(() -> validator.validate("SELECT * FROM customer"));
    }

    @Test
    void shouldRejectNonSelect() {
        assertThrows(ReportSecurityException.class, () -> validator.validate("DELETE FROM customer"));
    }

    @Test
    void shouldRejectForbiddenToken() {
        assertThrows(ReportSecurityException.class, () -> validator.validate("SELECT * FROM customer; DROP TABLE"));
    }

    @Test
    void shouldRejectMissingAllowedTable() {
        assertThrows(ReportSecurityException.class, () -> validator.validate("SELECT * FROM other_table"));
    }
}
