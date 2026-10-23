package com.fillthepockets.service;

import com.fillthepockets.entity.RoadReport;
import com.fillthepockets.repository.RoadReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;

@Service
public class DuplicateDetectionService {

    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double PROXIMITY_RADIUS_METERS = 15.0; // 15 meters duplicate radius threshold

    @Autowired
    private RoadReportRepository roadReportRepository;

    /**
     * Compute a 64-bit difference hash (dHash) for an image.
     * Robust against minor resizing, compression, and brightness shifts.
     */
    public String calculatePerceptualHash(BufferedImage image) {
        // Resize to 9 columns x 8 rows grayscale
        BufferedImage resized = new BufferedImage(9, 8, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.drawImage(image.getScaledInstance(9, 8, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        long hash = 0L;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int left = resized.getRaster().getSample(x, y, 0);
                int right = resized.getRaster().getSample(x + 1, y, 0);
                if (left > right) {
                    hash |= (1L << (y * 8 + x));
                }
            }
        }
        return String.format("%016x", hash);
    }

    /**
     * Calculate Hamming distance between two hex hashes (0 to 64).
     */
    public int calculateHammingDistance(String hash1, String hash2) {
        if (hash1 == null || hash2 == null || hash1.length() != 16 || hash2.length() != 16) {
            return 64;
        }
        long h1 = Long.parseUnsignedLong(hash1, 16);
        long h2 = Long.parseUnsignedLong(hash2, 16);
        return Long.bitCount(h1 ^ h2);
    }

    /**
     * Compute distance in meters between two GPS coordinates using Haversine formula.
     */
    public double calculateHaversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(rLat1) * Math.cos(rLat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    public static class DuplicateCheckResult {
        private final boolean duplicate;
        private final String reason;
        private final RoadReport existingReport;

        public DuplicateCheckResult(boolean duplicate, String reason, RoadReport existingReport) {
            this.duplicate = duplicate;
            this.reason = reason;
            this.existingReport = existingReport;
        }

        public boolean isDuplicate() {
            return duplicate;
        }

        public String getReason() {
            return reason;
        }

        public RoadReport getExistingReport() {
            return existingReport;
        }
    }

    /**
     * Evaluate whether a report is duplicate based on GPS proximity and image hashing.
     */
    public DuplicateCheckResult checkDuplicate(Double latitude, Double longitude, String currentImageHash, Long villagerId) {
        // Approximate 50m bounding box (1 deg lat ~ 111km)
        double latDelta = 0.0005;
        double lonDelta = 0.0005;

        List<RoadReport> nearbyReports = roadReportRepository.findReportsInArea(
                latitude - latDelta, latitude + latDelta,
                longitude - lonDelta, longitude + lonDelta
        );

        for (RoadReport existing : nearbyReports) {
            double distanceMeters = calculateHaversineDistanceMeters(
                    latitude, longitude,
                    existing.getLatitude(), existing.getLongitude()
            );

            int hammingDist = calculateHammingDistance(currentImageHash, existing.getImageHash());

            // 1. Same spot (<15m) AND similar image (hamming <= 12)
            if (distanceMeters <= PROXIMITY_RADIUS_METERS && hammingDist <= 12) {
                return new DuplicateCheckResult(
                        true,
                        String.format("Duplicate report detected: This pothole was already reported (%s) at %.1f meters away with matching image signature.",
                                existing.getReportNumber(), distanceMeters),
                        existing
                );
            }

            // 2. Same spot (<15m) reported by same villager within recent time
            if (distanceMeters <= PROXIMITY_RADIUS_METERS && existing.getVillager().getId().equals(villagerId)) {
                return new DuplicateCheckResult(
                        true,
                        String.format("Duplicate submission: You have already filed a report (%s) for this road location.",
                                existing.getReportNumber()),
                        existing
                );
            }
        }

        // 3. Exact or near-identical image reused elsewhere (fraud check)
        List<RoadReport> allRecent = roadReportRepository.findAllByOrderByCreatedAtDesc();
        for (RoadReport rep : allRecent) {
            int dist = calculateHammingDistance(currentImageHash, rep.getImageHash());
            if (dist <= 4) { // virtually identical photo
                return new DuplicateCheckResult(
                        true,
                        String.format("Duplicate photograph detected: This image was previously submitted in report %s.",
                                rep.getReportNumber()),
                        rep
                );
            }
        }

        return new DuplicateCheckResult(false, "No duplicate reports found. Eligible for verification.", null);
    }
}
