package com.fillthepockets.service;

import com.fillthepockets.dto.RoadReportRequest;
import com.fillthepockets.dto.RoadReportResponse;
import com.fillthepockets.entity.*;
import com.fillthepockets.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportReviewWorkflowTest {

    @TempDir
    Path uploadDirectory;

    @Mock
    private RoadReportRepository roadReportRepository;

    @Mock
    private AiVerificationRepository aiVerificationRepository;

    @Mock
    private RepairStatusUpdateRepository repairStatusUpdateRepository;

    @Mock
    private AiImageVerificationService aiService;

    @Mock
    private DuplicateDetectionService duplicateService;

    @Mock
    private PointsService pointsService;

    @Mock
    private PointsTransactionRepository pointsTransactionRepository;

    @Mock
    private EmployeeProfileRepository employeeProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void unrelatedImageIsAiRejectedWithoutAwardingPoints() throws Exception {
        ReflectionTestUtils.setField(reportService, "uploadDir", uploadDirectory.toString());
        when(duplicateService.calculatePerceptualHash(any(BufferedImage.class))).thenReturn("hash");
        when(duplicateService.checkDuplicate(anyDouble(), anyDouble(), anyString(), anyLong()))
                .thenReturn(new DuplicateDetectionService.DuplicateCheckResult(false, "No duplicate", null));
        when(aiService.verifyImage(any())).thenReturn(new AiImageVerificationService.VerificationOutput(
                "UNRELATED", 0.95, "car", "Image does not appear to show road damage.", 12L));
        when(roadReportRepository.save(any(RoadReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoadReportResponse response = reportService.submitReport(
                createRequest(createPng()), createVillager());

        assertEquals(ReportStatus.AI_REJECTED, response.getStatus());
        assertEquals(0, response.getPointsAwarded());
        verify(roadReportRepository).save(argThat(report ->
                report.getStatus() == ReportStatus.AI_REJECTED
                        && report.getPointsAwarded() == 0));
        verify(pointsService, never()).awardReportPoints(any(), any());
    }

    @Test
    void aiApprovedImageWaitsForEmployeeWithoutAwardingPoints() throws Exception {
        ReflectionTestUtils.setField(reportService, "uploadDir", uploadDirectory.toString());
        when(duplicateService.calculatePerceptualHash(any(BufferedImage.class))).thenReturn("hash");
        when(duplicateService.checkDuplicate(anyDouble(), anyDouble(), anyString(), anyLong()))
                .thenReturn(new DuplicateDetectionService.DuplicateCheckResult(false, "No duplicate", null));
        when(aiService.verifyImage(any())).thenReturn(new AiImageVerificationService.VerificationOutput(
                "ROAD_DAMAGE", 0.95, "pothole", "Road damage detected.", 12L));
        when(roadReportRepository.save(any(RoadReport.class)))
                .thenAnswer(invocation -> {
                    RoadReport report = invocation.getArgument(0);
                    report.setId(99L);
                    return report;
                });

        RoadReportResponse response = reportService.submitReport(
                createRequest(createPng()), createVillager());

        assertEquals(ReportStatus.PENDING_EMPLOYEE_VERIFICATION, response.getStatus());
        assertEquals(0, response.getPointsAwarded());
        assertEquals("/api/reports/99/image", response.getImageUrl());
        verify(roadReportRepository).save(argThat(report ->
                Files.isRegularFile(uploadDirectory.resolve(report.getImagePath()))));
        verify(pointsService, never()).awardReportPoints(any(), any());
    }

    @Test
    void duplicateImageIsRejectedWithoutCallingAiOrAwardingPoints() throws Exception {
        ReflectionTestUtils.setField(reportService, "uploadDir", uploadDirectory.toString());
        when(duplicateService.calculatePerceptualHash(any(BufferedImage.class))).thenReturn("hash");
        when(duplicateService.checkDuplicate(anyDouble(), anyDouble(), anyString(), anyLong()))
                .thenReturn(new DuplicateDetectionService.DuplicateCheckResult(true, "Possible duplicate", null));
        when(roadReportRepository.save(any(RoadReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoadReportResponse response = reportService.submitReport(
                createRequest(createPng()), createVillager());

        assertEquals(ReportStatus.REJECTED_DUPLICATE, response.getStatus());
        assertEquals("DUPLICATE", response.getAiCategory());
        verify(aiService, never()).verifyImage(any());
        verify(pointsService, never()).awardReportPoints(any(), any());
    }

    @Test
    void employeeApprovalAwardsPointsAndRecordsEmployeeAudit() {
        User villager = createVillager();
        User employee = createEmployee();
        EmployeeProfile empProfile = new EmployeeProfile(employee, "EMP001", "Executive Officer", "Sundarapuram Gram Panchayat", true, true);
        RoadReport approved = createReport(villager, ReportStatus.PENDING_EMPLOYEE_VERIFICATION);
        AiVerificationResult aiResult = new AiVerificationResult();
        aiResult.setCategory("ROAD_DAMAGE");
        aiResult.setConfidenceScore(0.92);

        when(roadReportRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(approved));
        when(aiVerificationRepository.findByReport(approved)).thenReturn(Optional.of(aiResult));
        when(roadReportRepository.save(any(RoadReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(roadReportRepository.countByVillager(villager)).thenReturn(1L);
        when(pointsTransactionRepository.sumTotalPointsAwardedToUser(villager)).thenReturn(20L);
        when(employeeProfileRepository.findByUser(employee)).thenReturn(Optional.of(empProfile));
        when(pointsService.awardReportPoints(villager, approved)).thenAnswer(invocation -> {
            villager.setPointsBalance(20);
            approved.setPointsAwarded(20);
            approved.setPointsTransactionId(101L);
            return new PointsTransaction();
        });

        RoadReportResponse approvedResponse = reportService.verifyReportAsEmployee(1L, employee);

        assertEquals(ReportStatus.EMPLOYEE_APPROVED, approvedResponse.getStatus());
        assertEquals(20, approvedResponse.getPointsAwarded());
        assertEquals(20, approvedResponse.getVillagerPointsBalance());
        assertEquals("Arun Kumar", approvedResponse.getVerifiedByEmployeeName());
        assertEquals("EMP001", approvedResponse.getVerifiedByEmployeeCode());
        assertNotNull(approvedResponse.getVerifiedAt());
        assertTrue(approvedResponse.isPointsGranted());
        verify(pointsService).awardReportPoints(villager, approved);
    }

    @Test
    void employeeRejectionDoesNotAwardPointsAndRecordsReason() {
        User villager = createVillager();
        User employee = createEmployee();
        RoadReport rejected = createReport(villager, ReportStatus.PENDING_EMPLOYEE_VERIFICATION);
        rejected.setId(2L);
        when(roadReportRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(rejected));
        when(aiVerificationRepository.findByReport(rejected)).thenReturn(Optional.empty());
        when(roadReportRepository.save(any(RoadReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(roadReportRepository.countByVillager(villager)).thenReturn(1L);
        when(pointsTransactionRepository.sumTotalPointsAwardedToUser(villager)).thenReturn(0L);

        RoadReportResponse rejectedResponse =
                reportService.rejectReportAsEmployee(2L, "Image does not show this road.", employee);

        assertEquals(ReportStatus.EMPLOYEE_REJECTED, rejectedResponse.getStatus());
        assertEquals("Image does not show this road.", rejectedResponse.getRejectionReason());
        assertEquals(0, rejectedResponse.getPointsAwarded());
        assertEquals("Arun Kumar", rejectedResponse.getVerifiedByEmployeeName());
        assertNotNull(rejectedResponse.getVerifiedAt());
        assertFalse(rejectedResponse.isPointsGranted());
        verify(pointsService, never()).awardReportPoints(any(), any());
    }

    @Test
    void repeatedApprovalIsRejectedAfterFirstApproval() {
        User villager = createVillager();
        User employee = createEmployee();
        RoadReport report = createReport(villager, ReportStatus.PENDING_EMPLOYEE_VERIFICATION);
        AiVerificationResult aiResult = new AiVerificationResult();
        aiResult.setCategory("ROAD_DAMAGE");

        when(roadReportRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(report));
        when(aiVerificationRepository.findByReport(report)).thenReturn(Optional.of(aiResult));
        when(roadReportRepository.save(any(RoadReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(roadReportRepository.countByVillager(villager)).thenReturn(1L);
        when(pointsTransactionRepository.sumTotalPointsAwardedToUser(villager)).thenReturn(20L);
        when(pointsService.awardReportPoints(villager, report)).thenAnswer(invocation -> {
            villager.setPointsBalance(20);
            report.setPointsAwarded(20);
            report.setPointsTransactionId(101L);
            return new PointsTransaction();
        });

        reportService.verifyReportAsEmployee(1L, employee);

        assertThrows(IllegalArgumentException.class,
                () -> reportService.verifyReportAsEmployee(1L, employee));
        verify(pointsService, times(1)).awardReportPoints(villager, report);
    }

    @Test
    void employeeRejectionRequiresAReason() {
        User employee = createEmployee();
        assertThrows(IllegalArgumentException.class,
                () -> reportService.rejectReportAsEmployee(1L, "   ", employee));
        verify(roadReportRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void approvalFailsIfAiVerificationMissingOrUnrelated() {
        User villager = createVillager();
        User employee = createEmployee();
        RoadReport report = createReport(villager, ReportStatus.PENDING_EMPLOYEE_VERIFICATION);
        AiVerificationResult unrelatedAi = new AiVerificationResult();
        unrelatedAi.setCategory("UNRELATED");

        when(roadReportRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(report));
        when(aiVerificationRepository.findByReport(report)).thenReturn(Optional.of(unrelatedAi));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reportService.verifyReportAsEmployee(1L, employee));
        assertTrue(ex.getMessage().contains("AI accepts the road-damage image"));
        verify(pointsService, never()).awardReportPoints(any(), any());
    }

    private RoadReportRequest createRequest(byte[] imageBytes) {
        RoadReportRequest request = new RoadReportRequest();
        request.setRoadName("Market Road");
        request.setVillageArea("Ward 1");
        request.setLatitude(10.0);
        request.setLongitude(76.0);
        request.setImage(new MockMultipartFile(
                "image", "report.png", "image/png", imageBytes));
        return request;
    }

    private byte[] createPng() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private User createVillager() {
        User villager = new User(
                "villager@example.test", "encoded", "Test Villager", null, Role.VILLAGER);
        villager.setId(1L);
        return villager;
    }

    private User createEmployee() {
        User employee = new User(
                "arun.kumar@fillthepockets.demo", "encoded", "Arun Kumar", null, Role.PANCHAYAT_EMPLOYEE);
        employee.setId(5L);
        return employee;
    }

    private RoadReport createReport(User villager, ReportStatus status) {
        RoadReport report = new RoadReport();
        report.setId(1L);
        report.setReportNumber("REP-TEST-1000");
        report.setVillager(villager);
        report.setRoadName("Market Road");
        report.setVillageArea("Ward 1");
        report.setLatitude(10.0);
        report.setLongitude(76.0);
        report.setImagePath("road_test.png");
        report.setImageHash("hash");
        report.setStatus(status);
        report.setRepairStatus(RepairStatus.PENDING);
        return report;
    }
}
