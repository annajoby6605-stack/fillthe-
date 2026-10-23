package com.fillthepockets.service;

import com.fillthepockets.entity.ReportStatus;
import com.fillthepockets.entity.RepairStatus;
import com.fillthepockets.entity.Role;
import com.fillthepockets.entity.RoadReport;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.AiVerificationRepository;
import com.fillthepockets.repository.PointsTransactionRepository;
import com.fillthepockets.repository.RepairStatusUpdateRepository;
import com.fillthepockets.repository.RoadReportRepository;
import com.fillthepockets.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportImageAccessTest {

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
    private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void reportOwnerAndEmployeeCanReadPersistedImage() throws Exception {
        ReflectionTestUtils.setField(reportService, "uploadDir", uploadDirectory.toString());
        User owner = user(10L, Role.VILLAGER);
        User employee = user(20L, Role.PANCHAYAT_EMPLOYEE);
        RoadReport report = report(owner);
        Path storedImage = Files.write(uploadDirectory.resolve("road_saved.png"), new byte[]{1, 2, 3});
        when(roadReportRepository.findById(7L)).thenReturn(Optional.of(report));

        assertEquals(storedImage, reportService.getReportImagePath(7L, owner));
        assertEquals(storedImage, reportService.getReportImagePath(7L, employee));
    }

    @Test
    void unrelatedVillagerCannotReadReportImage() throws Exception {
        ReflectionTestUtils.setField(reportService, "uploadDir", uploadDirectory.toString());
        User owner = user(10L, Role.VILLAGER);
        User otherVillager = user(11L, Role.VILLAGER);
        RoadReport report = report(owner);
        Files.write(uploadDirectory.resolve("road_saved.png"), new byte[]{1, 2, 3});
        when(roadReportRepository.findById(7L)).thenReturn(Optional.of(report));

        assertThrows(AccessDeniedException.class,
                () -> reportService.getReportImagePath(7L, otherVillager));
    }

    private User user(Long id, Role role) {
        User user = new User("user" + id + "@example.test", "encoded", "Test User", null, role);
        user.setId(id);
        return user;
    }

    private RoadReport report(User owner) {
        RoadReport report = new RoadReport();
        report.setId(7L);
        report.setVillager(owner);
        report.setImagePath("road_saved.png");
        report.setStatus(ReportStatus.PENDING_EMPLOYEE_VERIFICATION);
        report.setRepairStatus(RepairStatus.PENDING);
        return report;
    }
}
