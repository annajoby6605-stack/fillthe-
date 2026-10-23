package com.fillthepockets.controller;

import com.fillthepockets.dto.ApiResponse;
import com.fillthepockets.dto.RoadReportRequest;
import com.fillthepockets.dto.RoadReportResponse;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.UserRepository;
import com.fillthepockets.security.UserDetailsImpl;
import com.fillthepockets.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoadReportResponse>> submitReport(
            @RequestParam("roadName") String roadName,
            @RequestParam("villageArea") String villageArea,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            User villager = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            RoadReportRequest req = new RoadReportRequest();
            req.setRoadName(roadName);
            req.setVillageArea(villageArea);
            req.setDescription(description);
            req.setLatitude(latitude);
            req.setLongitude(longitude);
            req.setImage(image);

            RoadReportResponse resp = reportService.submitReport(req, villager);
            return ResponseEntity.ok(ApiResponse.ok(
                    "Report submitted for AI verification. Points are awarded only after employee approval.", resp));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to process image upload: " + e.getMessage()));
        }
    }

    @GetMapping("/my-reports")
    public ResponseEntity<ApiResponse<List<RoadReportResponse>>> getMyReports(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User villager = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<RoadReportResponse> reports = reportService.getVillagerReports(villager);
        return ResponseEntity.ok(ApiResponse.ok("User road reports retrieved", reports));
    }

    @GetMapping("/{reportId}/image")
    public ResponseEntity<Resource> getReportImage(
            @PathVariable Long reportId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User requester = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new NoSuchElementException("User not found."));
            Path imagePath = reportService.getReportImagePath(reportId, requester);
            String filename = imagePath.getFileName().toString().toLowerCase();
            MediaType mediaType = filename.endsWith(".png")
                    ? MediaType.IMAGE_PNG
                    : filename.endsWith(".webp")
                        ? MediaType.parseMediaType("image/webp")
                        : MediaType.IMAGE_JPEG;
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header("X-Content-Type-Options", "nosniff")
                    .cacheControl(org.springframework.http.CacheControl.noStore())
                    .body(new FileSystemResource(imagePath));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
