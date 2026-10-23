package com.fillthepockets.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_profiles")
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 30)
    private String employeeCode; // e.g. EMP001

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(nullable = false, length = 100)
    private String panchayatName;

    @Column(nullable = false)
    private Boolean isApproved = true;

    @Column(nullable = false)
    private Boolean isDemoAccount = false;

    private LocalDateTime approvedAt = LocalDateTime.now();

    public EmployeeProfile() {}

    public EmployeeProfile(User user, String employeeCode, String designation, String panchayatName, Boolean isApproved, Boolean isDemoAccount) {
        this.user = user;
        this.employeeCode = employeeCode;
        this.designation = designation;
        this.panchayatName = panchayatName;
        this.isApproved = isApproved;
        this.isDemoAccount = isDemoAccount;
        this.approvedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getPanchayatName() {
        return panchayatName;
    }

    public void setPanchayatName(String panchayatName) {
        this.panchayatName = panchayatName;
    }

    public Boolean getIsApproved() {
        return isApproved;
    }

    public void setIsApproved(Boolean approved) {
        isApproved = approved;
    }

    public Boolean getIsDemoAccount() {
        return isDemoAccount;
    }

    public void setIsDemoAccount(Boolean demoAccount) {
        isDemoAccount = demoAccount;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}
