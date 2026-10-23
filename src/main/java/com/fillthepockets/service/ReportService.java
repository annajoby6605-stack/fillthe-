package com.fillthepockets.service;

import com.fillthepockets.dto.RepairStatusUpdateRequest;
import com.fillthepockets.dto.RoadReportRequest;
import com.fillthepockets.dto.RoadReportResponse;
import com.fillthepockets.entity.*;
import com.fillthepockets.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    

    @Autowired
    private RoadReportRepository roadReportRepository;

    @Autowired
    private AiVerificationRepository aiVerificationRepository;

    @Autowired
    private RepairStatusUpdateRepository repairStatusUpdateRepository;

    @Autowired
    private AiImageVerificationService aiService;

    @Autowired
    private DuplicateDetectionService duplicateService;

    @Autowired
    private PointsService pointsService;

    @Autowired
    private PointsTransactionRepository pointsTransactionRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Transactional
    public RoadReportResponse submitReport(RoadReportRequest request, User villager) throws IOException {
        MultipartFile file = request.getImage();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photograph is required for road damage reporting.");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp") && !contentType.equals("image/jpg"))) {
            throw new IllegalArgumentException("Invalid file format. Please upload JPEG, PNG, or WEBP images.");
        }

        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image size exceeds maximum allowed limit of 10MB.");
        }

        // Create uploads directory if missing
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save image to disk with unique filename
        String extension = "image/png".equals(contentType)
                ? ".png"
                : "image/webp".equals(contentType) ? ".webp" : ".jpg";
        String filename = "road_" + UUID.randomUUID().toString() + extension;
        Path targetPath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        File savedFile = targetPath.toFile();
        BufferedImage bufferedImage = ImageIO.read(savedFile);
        if (bufferedImage == null) {
            Files.deleteIfExists(targetPath);
            throw new IllegalArgumentException("Uploaded file is not a valid decodable image.");
        }

        // 1. Calculate Perceptual Hash
        String imageHash = duplicateService.calculatePerceptualHash(bufferedImage);

        // Generate unique Report Number: REP-YYYYMMDD-XXXX
        String datePart = LocalDateTime.now().toString().replaceAll("[-T:]", "").substring(0, 8);
        String reportNumber = "REP-" + datePart + "-" + (1000 + new Random().nextInt(9000));

        RoadReport report = new RoadReport();
        report.setReportNumber(reportNumber);
        report.setVillager(villager);
        report.setRoadName(request.getRoadName().trim());
        report.setVillageArea(request.getVillageArea().trim());
        report.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        report.setLatitude(request.getLatitude());
        report.setLongitude(request.getLongitude());
        report.setImagePath(filename);
        report.setImageHash(imageHash);
        report.setRepairStatus(RepairStatus.PENDING);
        report.setPointsAwarded(0);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        // 2. Duplicate Detection
        DuplicateDetectionService.DuplicateCheckResult dupCheck =
                duplicateService.checkDuplicate(request.getLatitude(), request.getLongitude(), imageHash, villager.getId());

        AiVerificationResult aiResult = new AiVerificationResult();
        aiResult.setReport(report);
        aiResult.setVerifiedAt(LocalDateTime.now());

        if (dupCheck.isDuplicate()) {
            report.setStatus(ReportStatus.REJECTED_DUPLICATE);
            aiResult.setCategory("DUPLICATE");
            aiResult.setConfidenceScore(0.99);
            aiResult.setTopLabels("Duplicate Geo/Image Signature");
            aiResult.setExplanation(dupCheck.getReason());
            aiResult.setInferenceTimeMs(10L);
        } else {
            AiImageVerificationService.VerificationOutput aiOutput = aiService.verifyImage(savedFile);
            aiResult.setCategory(aiOutput.getCategory());
            aiResult.setConfidenceScore(aiOutput.getConfidenceScore());
            aiResult.setTopLabels(aiOutput.getTopLabels());
            aiResult.setExplanation(aiOutput.getExplanation());
            aiResult.setInferenceTimeMs(aiOutput.getInferenceTimeMs());
            report.setStatus("ROAD_DAMAGE".equalsIgnoreCase(aiOutput.getCategory())
                    ? ReportStatus.PENDING_EMPLOYEE_VERIFICATION
                    : ReportStatus.AI_REJECTED);
        }

        report = roadReportRepository.save(report);
        aiVerificationRepository.save(aiResult);
        return mapToResponse(report, aiResult);
    }

    @Transactional(readOnly = true)
    public List<RoadReportResponse> getVillagerReports(User villager) {
        List<RoadReport> reports = roadReportRepository.findByVillagerOrderByCreatedAtDesc(villager);
        return reports.stream().map(rep -> {
            AiVerificationResult ai = aiVerificationRepository.findByReport(rep).orElse(null);
            return mapToResponse(rep, ai);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoadReportResponse> getReportsForEmployee(ReportStatus status, RepairStatus repairStatus, String keyword) {
        List<RoadReport> reports;
        if (status == null && repairStatus == null && (keyword == null || keyword.isBlank())) {
            reports = roadReportRepository.findAllByOrderByCreatedAtDesc();
        } else {
            String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
            reports = roadReportRepository.searchReports(status, repairStatus, kw);
        }

        return reports.stream().map(rep -> {
            AiVerificationResult ai = aiVerificationRepository.findByReport(rep).orElse(null);
            return mapToResponse(rep, ai);
        }).collect(Collectors.toList());
    }

    @Transactional
    public RoadReportResponse updateRepairStatus(Long reportId, User employee, RepairStatusUpdateRequest req) {
        RoadReport report = roadReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + reportId));

        if (report.getStatus() != ReportStatus.EMPLOYEE_APPROVED
                && report.getStatus() != ReportStatus.ACCEPTED) {
            throw new IllegalArgumentException(
                    "Repair progress can only be updated after an employee verifies the report.");
        }

        RepairStatus oldStatus = report.getRepairStatus();
        report.setRepairStatus(req.getNewStatus());
        report.setUpdatedAt(LocalDateTime.now());
        report = roadReportRepository.save(report);

        RepairStatusUpdate audit = new RepairStatusUpdate(
                report,
                employee,
                oldStatus,
                req.getNewStatus(),
                req.getNotes() != null ? req.getNotes().trim() : "Status updated by " + employee.getFullName()
        );
        repairStatusUpdateRepository.save(audit);

        AiVerificationResult ai = aiVerificationRepository.findByReport(report).orElse(null);
        return mapToResponse(report, ai);
    }

    public List<RepairStatusUpdate> getRepairHistoryForReport(Long reportId) {
        RoadReport report = roadReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        return repairStatusUpdateRepository.findByReportOrderByIdDesc(report);
    }

    @Transactional(readOnly = true)
    public Path getReportImagePath(Long reportId, User requester) {
        RoadReport report = roadReportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("Report not found."));

        Role role = requester.getRole();
        boolean isReportOwner = report.getVillager().getId().equals(requester.getId());
        boolean isAuthorizedStaff = role == Role.PANCHAYAT_EMPLOYEE || role == Role.ADMIN;
        if (!isReportOwner && !isAuthorizedStaff) {
            throw new AccessDeniedException("You are not allowed to view this report image.");
        }

        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (report.getImagePath() == null || report.getImagePath().isBlank()) {
            throw new NoSuchElementException("The uploaded report image is unavailable.");
        }
        Path imagePath = uploadRoot.resolve(report.getImagePath()).normalize();
        if (!imagePath.startsWith(uploadRoot)) {
            throw new AccessDeniedException("The report image path is invalid.");
        }
        if (!Files.isRegularFile(imagePath)) {
            throw new NoSuchElementException("The uploaded report image is unavailable.");
        }
        return imagePath;
    }

    @Transactional(readOnly = true)
    public List<RoadReportResponse> getPendingEmployeeReports() {
        List<RoadReport> reports = roadReportRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(ReportStatus.PENDING_EMPLOYEE_VERIFICATION));

        return reports.stream()
                .map(report -> {
                    AiVerificationResult ai =
                            aiVerificationRepository.findByReport(report).orElse(null);
                    return mapToResponse(report, ai);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public RoadReportResponse verifyReportAsEmployee(Long reportId) {
        return verifyReportAsEmployee(reportId, null);
    }

    @Transactional
    public RoadReportResponse verifyReportAsEmployee(Long reportId, User employee) {
        RoadReport report = roadReportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Report not found with ID: " + reportId));

        if (report.getStatus() != ReportStatus.PENDING_EMPLOYEE_VERIFICATION) {
            throw new IllegalArgumentException(
                    "Only AI-approved reports awaiting employee verification can be approved.");
        }

        // Verify that AI has accepted the image
        AiVerificationResult aiResult = aiVerificationRepository.findByReport(report)
                .orElseThrow(() -> new IllegalStateException("AI verification result is missing for this report."));
        if (!"ROAD_DAMAGE".equalsIgnoreCase(aiResult.getCategory())) {
            throw new IllegalArgumentException("Points can only be awarded after the AI accepts the road-damage image.");
        }

        PointsTransaction transaction =
                pointsService.awardReportPoints(report.getVillager(), report);

        if (transaction == null) {
            throw new IllegalStateException(
                    "Points were already awarded for this report. Approval was cancelled.");
        }

        report.setStatus(ReportStatus.EMPLOYEE_APPROVED);
        if (employee != null) {
            report.setVerifiedBy(employee);
        }
        report.setVerifiedAt(LocalDateTime.now());
        report.setRejectionReason(null);
        report.setUpdatedAt(LocalDateTime.now());
        report = roadReportRepository.save(report);

        return mapToResponse(report, aiResult);
    }

    @Transactional
    public RoadReportResponse rejectReportAsEmployee(Long reportId, String reason) {
        return rejectReportAsEmployee(reportId, reason, null);
    }

    @Transactional
    public RoadReportResponse rejectReportAsEmployee(Long reportId, String reason, User employee) {
        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.isEmpty()) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }
        if (trimmedReason.length() > 2000) {
            throw new IllegalArgumentException("The rejection reason must be 2000 characters or fewer.");
        }

        RoadReport report = roadReportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Report not found with ID: " + reportId));

        if (report.getStatus() != ReportStatus.PENDING_EMPLOYEE_VERIFICATION) {
            throw new IllegalArgumentException(
                    "Only AI-approved reports awaiting employee verification can be rejected.");
        }

        report.setStatus(ReportStatus.EMPLOYEE_REJECTED);
        report.setPointsAwarded(0);
        report.setRejectionReason(trimmedReason);
        if (employee != null) {
            report.setVerifiedBy(employee);
        }
        report.setVerifiedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        report = roadReportRepository.save(report);

        AiVerificationResult ai =
                aiVerificationRepository.findByReport(report).orElse(null);

        return mapToResponse(report, ai);
    }

    private RoadReportResponse mapToResponse(RoadReport report, AiVerificationResult ai) {
        RoadReportResponse resp = new RoadReportResponse();
        resp.setId(report.getId());
        resp.setReportNumber(report.getReportNumber());
        resp.setVillagerId(report.getVillager().getId());
        resp.setVillagerName(report.getVillager().getFullName());
        resp.setVillagerEmail(report.getVillager().getEmail());
        resp.setRoadName(report.getRoadName());
        resp.setVillageArea(report.getVillageArea());
        resp.setDescription(report.getDescription());
        resp.setLatitude(report.getLatitude());
        resp.setLongitude(report.getLongitude());
        resp.setImageUrl("/api/reports/" + report.getId() + "/image");
        resp.setStatus(report.getStatus());
        resp.setRepairStatus(report.getRepairStatus());
        resp.setPointsAwarded(report.getPointsAwarded());
        resp.setRejectionReason(report.getRejectionReason());
        if (report.getVillager() != null) {
            resp.setVillagerPointsBalance(report.getVillager().getPointsBalance());
            resp.setVillagerTotalReports(roadReportRepository.countByVillager(report.getVillager()));
            resp.setVillagerTotalPointsEarned(pointsTransactionRepository.sumTotalPointsAwardedToUser(report.getVillager()));
        } else {
            resp.setVillagerPointsBalance(0);
            resp.setVillagerTotalReports(0L);
            resp.setVillagerTotalPointsEarned(0L);
        }
        resp.setCreatedAt(report.getCreatedAt());
        resp.setUpdatedAt(report.getUpdatedAt());
        resp.setVerifiedAt(report.getVerifiedAt());
        resp.setPointsGranted(report.getPointsTransactionId() != null && report.getPointsAwarded() != null && report.getPointsAwarded() > 0);

        if (report.getVerifiedBy() != null) {
            resp.setVerifiedByEmployeeName(report.getVerifiedBy().getFullName());
            employeeProfileRepository.findByUser(report.getVerifiedBy())
                    .ifPresent(profile -> resp.setVerifiedByEmployeeCode(profile.getEmployeeCode()));
        }

        if (ai != null) {
            resp.setAiCategory(ai.getCategory());
            resp.setAiConfidenceScore(ai.getConfidenceScore());
            resp.setAiExplanation(ai.getExplanation());
            resp.setAiTopLabels(ai.getTopLabels());
        }

        return resp;
    }
}
