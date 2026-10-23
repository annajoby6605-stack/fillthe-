package com.fillthepockets;

import com.fillthepockets.controller.SubmissionVerificationController;
import com.fillthepockets.dto.ApiResponse;
import com.fillthepockets.dto.RoadReportResponse;
import com.fillthepockets.dto.VerifySubmissionRequest;
import com.fillthepockets.entity.ReportStatus;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.UserRepository;
import com.fillthepockets.security.UserDetailsImpl;
import com.fillthepockets.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionVerificationControllerTest {

    @Mock
    private ReportService reportService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubmissionVerificationController controller;

    private UserDetailsImpl employeeUserDetails;
    private User employeeUser;

    @BeforeEach
    void setUp() {
        employeeUserDetails = new UserDetailsImpl(
                5L, "arun.kumar@fillthepockets.demo", "Password@123", "Arun Kumar",
                List.of(new SimpleGrantedAuthority("ROLE_PANCHAYAT_EMPLOYEE")), true
        );
        employeeUser = new User();
        employeeUser.setId(5L);
        employeeUser.setFullName("Arun Kumar");
        employeeUser.setEmail("arun.kumar@fillthepockets.demo");
    }

    @Test
    void testApproveSubmissionSuccess() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(employeeUser));

        RoadReportResponse approvedResp = new RoadReportResponse();
        approvedResp.setId(101L);
        approvedResp.setReportNumber("REP-20261023-1001");
        approvedResp.setStatus(ReportStatus.EMPLOYEE_APPROVED);
        approvedResp.setVillagerName("Ramesh Kumar");
        approvedResp.setPointsAwarded(20);
        approvedResp.setVerifiedByEmployeeName("Arun Kumar");
        approvedResp.setVerifiedAt(LocalDateTime.now());
        approvedResp.setPointsGranted(true);

        when(reportService.verifyReportAsEmployee(eq(101L), eq(employeeUser))).thenReturn(approvedResp);

        VerifySubmissionRequest request = new VerifySubmissionRequest(101L, "APPROVE", null);
        ResponseEntity<ApiResponse<RoadReportResponse>> response = controller.verifySubmission(request, employeeUserDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Photo verified! 20 points credited to Ramesh Kumar.", response.getBody().getMessage());
        assertEquals(20, response.getBody().getData().getPointsAwarded());
        assertEquals(ReportStatus.EMPLOYEE_APPROVED, response.getBody().getData().getStatus());

        verify(reportService, times(1)).verifyReportAsEmployee(101L, employeeUser);
    }

    @Test
    void testRejectSubmissionSuccess() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(employeeUser));

        RoadReportResponse rejectedResp = new RoadReportResponse();
        rejectedResp.setId(102L);
        rejectedResp.setStatus(ReportStatus.EMPLOYEE_REJECTED);
        rejectedResp.setPointsAwarded(0);

        when(reportService.rejectReportAsEmployee(eq(102L), eq("Not road damage"), eq(employeeUser))).thenReturn(rejectedResp);

        VerifySubmissionRequest request = new VerifySubmissionRequest(102L, "REJECT", "Not road damage");
        ResponseEntity<ApiResponse<RoadReportResponse>> response = controller.verifySubmission(request, employeeUserDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Photo rejected. No points were awarded.", response.getBody().getMessage());
        assertEquals(0, response.getBody().getData().getPointsAwarded());

        verify(reportService, times(1)).rejectReportAsEmployee(102L, "Not road damage", employeeUser);
    }

    @Test
    void testMissingSubmissionIdReturnsBadRequest() {
        VerifySubmissionRequest request = new VerifySubmissionRequest();
        ResponseEntity<ApiResponse<RoadReportResponse>> response = controller.verifySubmission(request, employeeUserDetails);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Submission or report ID is required.", response.getBody().getMessage());
        verifyNoInteractions(reportService);
    }

    @Test
    void testDuplicateApprovalReturnsConflict() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(employeeUser));
        when(reportService.verifyReportAsEmployee(eq(103L), eq(employeeUser)))
                .thenThrow(new IllegalStateException("Report has already been approved by employee Arun Kumar"));

        VerifySubmissionRequest request = new VerifySubmissionRequest(103L, "APPROVE", null);
        ResponseEntity<ApiResponse<RoadReportResponse>> response = controller.verifySubmission(request, employeeUserDetails);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Report has already been approved by employee Arun Kumar", response.getBody().getMessage());
    }

    @Test
    void testUnverifiedAiReportApprovalReturnsBadRequest() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(employeeUser));
        when(reportService.verifyReportAsEmployee(eq(104L), eq(employeeUser)))
                .thenThrow(new IllegalArgumentException("Report 104 is in status AI_REJECTED. Only reports in PENDING_EMPLOYEE_VERIFICATION can be approved."));

        VerifySubmissionRequest request = new VerifySubmissionRequest(104L, "APPROVE", null);
        ResponseEntity<ApiResponse<RoadReportResponse>> response = controller.verifySubmission(request, employeeUserDetails);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("status AI_REJECTED"));
    }
}
