package com.fillthepockets.service;

import com.fillthepockets.entity.AiVerificationResult;
import com.fillthepockets.entity.PointsTransaction;
import com.fillthepockets.entity.ReportStatus;
import com.fillthepockets.entity.Role;
import com.fillthepockets.entity.RoadReport;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.AiVerificationRepository;
import com.fillthepockets.repository.PointsTransactionRepository;
import com.fillthepockets.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsServiceEmployeeApprovalTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointsTransactionRepository pointsTransactionRepository;

    @Mock
    private AiVerificationRepository aiVerificationRepository;

    @InjectMocks
    private PointsService pointsService;

    @Test
    void awardsExactlyTwentyPointsAndPersistsTransactionAfterAiPass() {
        User villager = new User("villager@example.test", "encoded", "Test Villager", null, Role.VILLAGER);
        villager.setId(11L);
        RoadReport report = new RoadReport();
        report.setReportNumber("REP-TEST-1001");
        report.setRoadName("Market Road");
        report.setVillager(villager);
        report.setStatus(ReportStatus.PENDING_EMPLOYEE_VERIFICATION);
        AiVerificationResult ai = new AiVerificationResult();
        ai.setCategory("ROAD_DAMAGE");

        when(aiVerificationRepository.findByReport(report)).thenReturn(Optional.of(ai));
        when(pointsTransactionRepository.existsByReport(report)).thenReturn(false);
        when(userRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(villager));
        when(pointsTransactionRepository.save(any(PointsTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PointsTransaction transaction = pointsService.awardReportPoints(villager, report);

        assertNotNull(transaction);
        assertEquals(20, transaction.getPoints());
        assertEquals(20, transaction.getBalanceAfter());
        assertEquals(20, villager.getPointsBalance());
        assertEquals(20, report.getPointsAwarded());
        verify(pointsTransactionRepository).save(transaction);
        verify(userRepository).save(villager);
    }

    @Test
    void doesNotAwardPointsTwiceForOneReport() {
        User villager = new User("villager@example.test", "encoded", "Test Villager", null, Role.VILLAGER);
        RoadReport report = new RoadReport();
        report.setVillager(villager);
        report.setStatus(ReportStatus.PENDING_EMPLOYEE_VERIFICATION);
        AiVerificationResult ai = new AiVerificationResult();
        ai.setCategory("ROAD_DAMAGE");

        when(aiVerificationRepository.findByReport(report)).thenReturn(Optional.of(ai));
        when(pointsTransactionRepository.existsByReport(report)).thenReturn(true);

        assertNull(pointsService.awardReportPoints(villager, report));
        verify(pointsTransactionRepository, never()).save(any(PointsTransaction.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void cannotAwardPointsIfReportNotPendingEmployeeVerification() {
        User villager = new User("villager@example.test", "encoded", "Test Villager", null, Role.VILLAGER);
        RoadReport report = new RoadReport();
        report.setVillager(villager);
        report.setStatus(ReportStatus.AI_REJECTED);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pointsService.awardReportPoints(villager, report));
        assertTrue(ex.getMessage().contains("Points can only be awarded for an AI-approved report awaiting employee verification"));
        verify(pointsTransactionRepository, never()).save(any(PointsTransaction.class));
    }

    @Test
    void cannotAwardPointsIfAiCategoryIsNotRoadDamage() {
        User villager = new User("villager@example.test", "encoded", "Test Villager", null, Role.VILLAGER);
        RoadReport report = new RoadReport();
        report.setVillager(villager);
        report.setStatus(ReportStatus.PENDING_EMPLOYEE_VERIFICATION);
        AiVerificationResult ai = new AiVerificationResult();
        ai.setCategory("UNRELATED");

        when(aiVerificationRepository.findByReport(report)).thenReturn(Optional.of(ai));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pointsService.awardReportPoints(villager, report));
        assertTrue(ex.getMessage().contains("after the AI accepts the road-damage image"));
        verify(pointsTransactionRepository, never()).save(any(PointsTransaction.class));
    }
}
