package com.fillthepockets.dto;

public class EmployeeOtpResponse {
    private String email;
    private String employeeName;
    private String employeeCode;
    private String designation;
    private String message;
    private boolean demoMode;
    private String demoOtpCode; // Only populated when demoMode=true for college presentation
    private int expiresInSeconds;

    public EmployeeOtpResponse() {}

    public EmployeeOtpResponse(String email, String employeeName, String employeeCode, String designation, String message, boolean demoMode, String demoOtpCode, int expiresInSeconds) {
        this.email = email;
        this.employeeName = employeeName;
        this.employeeCode = employeeCode;
        this.designation = designation;
        this.message = message;
        this.demoMode = demoMode;
        this.demoOtpCode = demoOtpCode;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public String getDemoOtpCode() {
        return demoOtpCode;
    }

    public void setDemoOtpCode(String demoOtpCode) {
        this.demoOtpCode = demoOtpCode;
    }

    public String getDemoCode() {
        return demoOtpCode;
    }

    public void setDemoCode(String demoCode) {
        this.demoOtpCode = demoCode;
    }

    public int getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(int expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
