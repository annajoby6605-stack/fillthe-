package com.fillthepockets.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_verification_results")
public class AiVerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private RoadReport report;

    @Column(nullable = false, length = 40)
    private String category; // ROAD_DAMAGE, UNRELATED, UNCERTAIN

    @Column(nullable = false)
    private Double confidenceScore;

    @Column(length = 255)
    private String topLabels;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    private Long inferenceTimeMs;

    @Column(nullable = false)
    private LocalDateTime verifiedAt = LocalDateTime.now();

    public AiVerificationResult() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RoadReport getReport() {
        return report;
    }

    public void setReport(RoadReport report) {
        this.report = report;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getTopLabels() {
        return topLabels;
    }

    public void setTopLabels(String topLabels) {
        this.topLabels = topLabels;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Long getInferenceTimeMs() {
        return inferenceTimeMs;
    }

    public void setInferenceTimeMs(Long inferenceTimeMs) {
        this.inferenceTimeMs = inferenceTimeMs;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
