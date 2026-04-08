package com.legacy.report.exception;

public class ReportSecurityException extends RuntimeException {

    public ReportSecurityException(String message) {
        super(message);
    }

    public ReportSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
