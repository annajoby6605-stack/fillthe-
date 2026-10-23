package com.fillthepockets;

import com.fillthepockets.dto.*;
import com.fillthepockets.entity.*;
import com.fillthepockets.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TwoStageVerificationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoadReportRepository roadReportRepository;

    @Autowired
    private PointsTransactionRepository pointsTransactionRepository;

    @Autowired
    private AiVerificationRepository aiVerificationRepository;

    @Autowired
    private OtpRecordRepository otpRecordRepository;

    private String baseUrl;

    @BeforeEach
    void setup() {
        baseUrl = "http://localhost:" + port;
        roadReportRepository.findAll().stream()
                .filter(r -> "Main Panchayat Road".equals(r.getRoadName()) || (r.getVillager() != null && r.getVillager().getEmail().startsWith("rajesh.")))
                .forEach(r -> {
                    pointsTransactionRepository.findByReport(r).ifPresent(pointsTransactionRepository::delete);
                    aiVerificationRepository.findByReport(r).ifPresent(aiVerificationRepository::delete);
                    roadReportRepository.delete(r);
                });
    }

    private byte[] createTestImage(boolean roadLike) throws Exception {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        if (roadLike) {
            // Neutral asphalt/gray tones with edge variance and timestamp to ensure unique image hash
            int seed = (int) (System.currentTimeMillis() % 40);
            g.setColor(new Color(60 + seed, 60 + seed, 60 + seed));
            g.fillRect(0, 0, 100, 100);
            g.setColor(new Color(20, 20, 20));
            g.fillOval(15 + (seed % 20), 15 + (seed % 20), 35 + (seed % 20), 35 + (seed % 20)); // pothole cavity
            g.setColor(new Color(110, 110, 110));
            g.drawRect(5 + (seed % 10), 5 + (seed % 10), 80, 80);
            g.drawString(String.valueOf(System.nanoTime()), 10, 90);
        } else {
            // Bright skin/flower colors (unrelated)
            g.setColor(new Color(245, 180, 160));
            g.fillRect(0, 0, 100, 100);
        }
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private String loginVillager(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of("email", email, "password", password), headers);

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login-villager", request, ApiResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        return (String) data.get("token");
    }

    private String loginEmployee(String email, String password) {
        // Step 1: Initiate
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> initReq = new HttpEntity<>(
                Map.of("email", email, "password", password), headers);
        ResponseEntity<ApiResponse> initResp = restTemplate.postForEntity(
                baseUrl + "/api/auth/employee/initiate-login", initReq, ApiResponse.class);
        assertEquals(HttpStatus.OK, initResp.getStatusCode());

        // In demo mode or database, retrieve latest OTP
        OtpRecord otp = otpRecordRepository.findTopByEmailOrderByIdDesc(email.toLowerCase())
                .orElseThrow();

        // Step 2: Verify OTP
        HttpEntity<Map<String, String>> verifyReq = new HttpEntity<>(
                Map.of("email", email, "code", otp.getOtpCode()), headers);
        ResponseEntity<ApiResponse> verifyResp = restTemplate.postForEntity(
                baseUrl + "/api/auth/employee/verify-otp", verifyReq, ApiResponse.class);
        assertEquals(HttpStatus.OK, verifyResp.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) verifyResp.getBody().getData();
        return (String) data.get("token");
    }

    @Test
    void completeTwoStageVerificationWorkflow() throws Exception {
        // 1. Register a fresh villager
        String villagerEmail = "rajesh." + System.currentTimeMillis() + "@gmail.com";
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> regReq = new HttpEntity<>(Map.of(
                "email", villagerEmail,
                "password", "Password@123",
                "fullName", "Rajesh Kumar",
                "phone", "9876543210"
        ), jsonHeaders);
        ResponseEntity<ApiResponse> regResp = restTemplate.postForEntity(
                baseUrl + "/api/auth/register-villager", regReq, ApiResponse.class);
        assertEquals(HttpStatus.OK, regResp.getStatusCode());

        String villagerToken = loginVillager(villagerEmail, "Password@123");
        assertNotNull(villagerToken);

        // 2. Villager submits a road problem report with road-damage image
        byte[] imageBytes = createTestImage(true);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("roadName", "Main Panchayat Road");
        form.add("villageArea", "Ward 3");
        form.add("description", "Pothole near post office");
        double randLat = 10.0 + (System.currentTimeMillis() % 10000) * 0.001;
        double randLon = 76.0 + (System.currentTimeMillis() % 10000) * 0.001;
        form.add("latitude", String.valueOf(randLat));
        form.add("longitude", String.valueOf(randLon));
        form.add("image", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "road_damage.png";
            }
        });

        HttpHeaders formHeaders = new HttpHeaders();
        formHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        formHeaders.setBearerAuth(villagerToken);
        HttpEntity<MultiValueMap<String, Object>> uploadReq = new HttpEntity<>(form, formHeaders);

        ResponseEntity<ApiResponse> uploadResp = restTemplate.postForEntity(
                baseUrl + "/api/reports/submit", uploadReq, ApiResponse.class);
        assertEquals(HttpStatus.OK, uploadResp.getStatusCode());

        Map<String, Object> reportData = (Map<String, Object>) uploadResp.getBody().getData();
        Long reportId = Long.valueOf(reportData.get("id").toString());
        String status = (String) reportData.get("status");
        Integer points = (Integer) reportData.get("pointsAwarded");
        System.out.println("DUP REASON: " + reportData.get("aiExplanation"));

        // Stage 1 Verification Rule: AI passed -> PENDING_EMPLOYEE_VERIFICATION, NO points awarded yet!
        assertEquals("PENDING_EMPLOYEE_VERIFICATION", status);
        assertEquals(0, points);

        // 3. Verify villager dashboard: shows 0 points balance and report awaiting employee verification
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(villagerToken);
        HttpEntity<Void> dashReq = new HttpEntity<>(authHeaders);
        ResponseEntity<ApiResponse> dashResp = restTemplate.exchange(
                baseUrl + "/api/villager/dashboard", HttpMethod.GET, dashReq, ApiResponse.class);
        assertEquals(HttpStatus.OK, dashResp.getStatusCode());
        Map<String, Object> dashData = (Map<String, Object>) dashResp.getBody().getData();
        assertEquals(0, ((Number) dashData.get("pointsBalance")).intValue());

        // 4. Unauthorized check: Villager attempts to approve their own report -> 403 Forbidden
        ResponseEntity<ApiResponse> unauthApproveResp = restTemplate.exchange(
                baseUrl + "/api/employee/reports/" + reportId + "/approve",
                HttpMethod.POST,
                dashReq,
                ApiResponse.class
        );
        assertEquals(HttpStatus.FORBIDDEN, unauthApproveResp.getStatusCode(),
                "Only authorized employees can access /api/employee/**");

        // 5. Authorized employee logs in (Arun Kumar)
        String employeeToken = loginEmployee("arun.kumar@fillthepockets.demo", "Demo@Emp123");
        assertNotNull(employeeToken);

        HttpHeaders empHeaders = new HttpHeaders();
        empHeaders.setBearerAuth(employeeToken);

        // 6. Employee verifies and approves the report (Stage 2)
        HttpEntity<Void> approveReq = new HttpEntity<>(empHeaders);
        ResponseEntity<ApiResponse> approveResp = restTemplate.exchange(
                baseUrl + "/api/employee/reports/" + reportId + "/approve",
                HttpMethod.POST,
                approveReq,
                ApiResponse.class
        );
        assertEquals(HttpStatus.OK, approveResp.getStatusCode());
        Map<String, Object> approvedData = (Map<String, Object>) approveResp.getBody().getData();
        assertEquals("EMPLOYEE_APPROVED", approvedData.get("status"));
        assertEquals(20, ((Number) approvedData.get("pointsAwarded")).intValue());
        assertEquals("Arun Kumar", approvedData.get("verifiedByEmployeeName"));
        assertNotNull(approvedData.get("verifiedAt"));
        assertTrue((Boolean) approvedData.get("pointsGranted"));

        // 7. Duplicate approval protection: Repeated approval attempt must fail
        ResponseEntity<ApiResponse> repeatResp = restTemplate.exchange(
                baseUrl + "/api/employee/reports/" + reportId + "/approve",
                HttpMethod.POST,
                approveReq,
                ApiResponse.class
        );
        assertTrue(repeatResp.getStatusCode().is4xxClientError(),
                "Repeated approval attempt must be rejected");

        // 8. Stage 4: Verify villager portal reflects verified report, updated balance (+20 pts)
        ResponseEntity<ApiResponse> updatedDashResp = restTemplate.exchange(
                baseUrl + "/api/villager/dashboard", HttpMethod.GET, dashReq, ApiResponse.class);
        assertEquals(HttpStatus.OK, updatedDashResp.getStatusCode());
        Map<String, Object> updatedDashData = (Map<String, Object>) updatedDashResp.getBody().getData();
        assertEquals(20, ((Number) updatedDashData.get("pointsBalance")).intValue());
        assertEquals(20, ((Number) updatedDashData.get("totalPointsEarned")).intValue());

        // 9. Verify wallet ledger recorded the transaction
        ResponseEntity<ApiResponse> walletResp = restTemplate.exchange(
                baseUrl + "/api/villager/wallet", HttpMethod.GET, dashReq, ApiResponse.class);
        assertEquals(HttpStatus.OK, walletResp.getStatusCode());
        Map<String, Object> walletData = (Map<String, Object>) walletResp.getBody().getData();
        assertEquals(20, ((Number) walletData.get("pointsBalance")).intValue());

        // 10. Clean up test report from database
        roadReportRepository.findById(reportId).ifPresent(r -> {
            pointsTransactionRepository.findByReport(r).ifPresent(pointsTransactionRepository::delete);
            aiVerificationRepository.findByReport(r).ifPresent(aiVerificationRepository::delete);
            roadReportRepository.delete(r);
        });
        userRepository.findByEmail(villagerEmail).ifPresent(userRepository::delete);
    }
}
