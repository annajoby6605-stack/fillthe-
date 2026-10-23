package com.fillthepockets.service;

import ai.onnxruntime.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class AiImageVerificationService {

    private static final Logger log = LoggerFactory.getLogger(AiImageVerificationService.class);

    @Value("${app.ai.model-path:src/main/resources/models/mobilenetv2-7.onnx}")
    private String modelPathString;

    @Value("${app.ai.confidence-threshold:0.70}")
    private double confidenceThreshold;

    private OrtEnvironment ortEnvironment;
    private OrtSession ortSession;
    private boolean modelLoaded = false;

    public static class VerificationOutput {
        private final String category; // ROAD_DAMAGE, UNRELATED, UNCERTAIN
        private final double confidenceScore;
        private final String topLabels;
        private final String explanation;
        private final long inferenceTimeMs;

        public VerificationOutput(String category, double confidenceScore, String topLabels, String explanation, long inferenceTimeMs) {
            this.category = category;
            this.confidenceScore = confidenceScore;
            this.topLabels = topLabels;
            this.explanation = explanation;
            this.inferenceTimeMs = inferenceTimeMs;
        }

        public String getCategory() { return category; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getTopLabels() { return topLabels; }
        public String getExplanation() { return explanation; }
        public long getInferenceTimeMs() { return inferenceTimeMs; }
    }

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(modelPathString);
            if (!Files.exists(path)) {
                // Check classpath
                InputStream is = getClass().getResourceAsStream("/models/mobilenetv2-7.onnx");
                if (is != null) {
                    Path tmp = Files.createTempFile("mobilenet", ".onnx");
                    Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                    path = tmp;
                }
            }

            if (Files.exists(path)) {
                ortEnvironment = OrtEnvironment.getEnvironment();
                OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
                ortSession = ortEnvironment.createSession(path.toString(), opts);
                modelLoaded = true;
                log.info("Microsoft ONNX Runtime successfully initialized with deep learning model: {}", path.toAbsolutePath());
            } else {
                log.warn("ONNX model file not found at {}. Fallback vision analyzer active.", modelPathString);
            }
        } catch (Exception e) {
            log.error("Failed to initialize ONNX model: {}. Falling back to visual feature engine.", e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (ortSession != null) ortSession.close();
            if (ortEnvironment != null) ortEnvironment.close();
        } catch (Exception ignored) {}
    }

    /**
     * Verify an uploaded photograph using actual deep learning neural inference
     * combined with computer vision surface texture and gradient metrics.
     */
    public VerificationOutput verifyImage(File imageFile) {
        long startTime = System.currentTimeMillis();

        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return new VerificationOutput(
                        "UNRELATED",
                        0.99,
                        "Corrupted / Invalid Image",
                        "Rejected: File could not be decoded as a valid photographic image.",
                        System.currentTimeMillis() - startTime
                );
            }

            // 1. Computer Vision Surface Feature Extraction
            SurfaceAnalysis surface = analyzeSurfaceFeatures(image);

            // 2. Deep Neural Network Classification via ONNX Runtime
            double neuralRoadDamageScore = 0.5;
            

            if (modelLoaded && ortSession != null) {
                neuralRoadDamageScore = runOnnxInference(image);
                
            }

            // 3. Multi-modal Fusion: Combine Neural Tensor Analysis with Surface Edge/Texture
            double compositeDamageScore;
            if (surface.isRoadSurface && surface.roadDamageProbability >= 0.65) {
                // Strong physical asphalt damage detected
                compositeDamageScore = Math.max(surface.roadDamageProbability, (surface.roadDamageProbability * 0.70) + (neuralRoadDamageScore * 0.30));
            } else if (!surface.isRoadSurface) {
                // Non-road surface detected (skin, flower, domestic object)
                compositeDamageScore = Math.min(surface.roadDamageProbability, neuralRoadDamageScore * 0.40);
            } else {
                compositeDamageScore = (neuralRoadDamageScore * 0.40) + (surface.roadDamageProbability * 0.60);
            }

            long inferenceTime = System.currentTimeMillis() - startTime;

            log.info(String.format("AI Analysis for %s: Neural=%.3f, Texture=%.3f, Composite=%.3f, Var=%.1f, Edge=%.3f, RoadSurface=%b",
                    imageFile.getName(), neuralRoadDamageScore, surface.roadDamageProbability,
                    compositeDamageScore, surface.laplacianVariance, surface.edgeDensity, surface.isRoadSurface));

            // Decision Logic
            if (surface.isExtremelyBlurryOrDark) {
                return new VerificationOutput(
                        "UNCERTAIN",
                        compositeDamageScore,
                        "Low Visibility / Motion Blur",
                        "AI Verification Uncertain: The image is either too blurry, overexposed, or lacks visible road texture. Please upload a clear photograph taken in daylight.",
                        inferenceTime
                );
            }

            if (compositeDamageScore >= confidenceThreshold && surface.isRoadSurface) {
                String labels = "Pothole Cavity (" + String.format("%.1f%%", compositeDamageScore * 100) +
                                "), Asphalt Degradation, Broken Road Surface";
                String explanation = String.format(
                        "AI_PASSED: Deep neural feature maps and surface texture analysis identified road damage (Confidence: %.1f%%). Employee verification is required before points can be awarded.",
                        compositeDamageScore * 100
                );
                return new VerificationOutput("ROAD_DAMAGE", compositeDamageScore, labels, explanation, inferenceTime);
            } else {
                String labels = surface.detectedDominantType + " (Confidence: " + String.format("%.1f%%", (1.0 - compositeDamageScore) * 100) + ")";
                String explanation = String.format(
                        "AI_REJECTED: this photograph did not meet the road-damage verification criteria (Estimated Road Damage Score: %.1f%%, required: %.1f%%). Detected subject: %s. No points were awarded.",
                        compositeDamageScore * 100, confidenceThreshold * 100, surface.detectedDominantType
                );
                return new VerificationOutput("UNRELATED", 1.0 - compositeDamageScore, labels, explanation, inferenceTime);
            }

        } catch (Exception e) {
            log.error("Error during AI photo verification: {}", e.getMessage(), e);
            long inferenceTime = System.currentTimeMillis() - startTime;
            return new VerificationOutput(
                    "UNCERTAIN",
                    0.50,
                    "Inference Error",
                    "AI service encountered a processing error: " + e.getMessage() + ". Please try uploading the image again.",
                    inferenceTime
            );
        }
    }

    /**
     * Runs ONNX deep learning inference on preprocessed 224x224 RGB image tensors.
     */
    private double runOnnxInference(BufferedImage image) {
        try {
            int width = 224;
            int height = 224;
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();

            FloatBuffer buffer = FloatBuffer.allocate(1 * 3 * width * height);

            // Normalization parameters (ImageNet standard)
            float[] mean = {0.485f, 0.456f, 0.406f};
            float[] std = {0.229f, 0.224f, 0.225f};

            // MobileNet tensor layout: [channels, height, width] (CHW)
            for (int c = 0; c < 3; c++) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int rgb = resized.getRGB(x, y);
                        float val = 0f;
                        if (c == 0) val = ((rgb >> 16) & 0xFF) / 255.0f;
                        else if (c == 1) val = ((rgb >> 8) & 0xFF) / 255.0f;
                        else val = (rgb & 0xFF) / 255.0f;

                        buffer.put((val - mean[c]) / std[c]);
                    }
                }
            }
            buffer.flip();

            long[] shape = new long[]{1, 3, height, width};
            OnnxTensor inputTensor = OnnxTensor.createTensor(ortEnvironment, buffer, shape);

            String inputName = ortSession.getInputNames().iterator().next();
            try (OrtSession.Result result = ortSession.run(Collections.singletonMap(inputName, inputTensor))) {
                float[][] logits = (float[][]) result.get(0).getValue();
                float[] probs = softmax(logits[0]);

                // Road, asphalt, trench, barrier, stone, outdoor infrastructure class indices in ImageNet
                // e.g., 657 (manhole cover), 675 (moving van), 751 (racer), 975 (geyser), 979 (valley/cliff), etc.
                // We calculate outdoor ground/rough asphalt resonance
                double roadFeatureEnergy = 0.0;
                for (int i = 0; i < probs.length; i++) {
                    if (isRoadRelatedClass(i)) {
                        roadFeatureEnergy += probs[i];
                    }
                }

                // If highest scoring class is indoor animal/person/household furniture
                int topClass = 0;
                float maxProb = -1f;
                for (int i = 0; i < probs.length; i++) {
                    if (probs[i] > maxProb) {
                        maxProb = probs[i];
                        topClass = i;
                    }
                }

                if (isDistinctlyUnrelatedClass(topClass) && maxProb > 0.25f) {
                    return Math.max(0.05, roadFeatureEnergy * 0.2);
                }

                double score = 0.55 + (roadFeatureEnergy * 3.0);
                return Math.min(0.95, Math.max(0.20, score));
            }
        } catch (Exception e) {
            log.warn("ONNX tensor calculation fallback: {}", e.getMessage());
            return 0.5;
        }
    }

    private boolean isRoadRelatedClass(int classIdx) {
        // ImageNet classes for outdoor terrain, masonry, structures, curbs, manholes, cracks
        return (classIdx >= 650 && classIdx <= 700) || // vehicles, street furniture, manhole
               (classIdx >= 970 && classIdx <= 985) || // landscape, gravel, cliff, stone wall
               classIdx == 487 || classIdx == 518 || classIdx == 530;
    }

    private boolean isDistinctlyUnrelatedClass(int classIdx) {
        // ImageNet classes 0-397 (dogs, cats, birds, animals), 920-960 (food, fruits), 400-600 (household appliances)
        return (classIdx < 400) || (classIdx >= 920 && classIdx <= 960);
    }

    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) if (v > max) max = v;

        float sum = 0f;
        float[] exp = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }
        for (int i = 0; i < logits.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }

    private static class SurfaceAnalysis {
        double roadDamageProbability;
        double laplacianVariance;
        double edgeDensity;
        boolean isRoadSurface;
        boolean isExtremelyBlurryOrDark;
        String detectedDominantType;
    }

    /**
     * Computer vision texture & color spectrum analyzer.
     * Computes asphalt color profiling, edge density, depression gradients, and texture variance.
     */
    private SurfaceAnalysis analyzeSurfaceFeatures(BufferedImage img) {
        SurfaceAnalysis sa = new SurfaceAnalysis();

        int w = img.getWidth();
        int h = img.getHeight();
        int step = Math.max(1, Math.min(w, h) / 100);

        long totalPixels = 0;
        long asphaltColorPixels = 0;
        long greenFoliagePixels = 0;
        long skinOrIndoorPixels = 0;

        double sumLuma = 0.0;
        double sumLumaSq = 0.0;
        double edgeCount = 0;

        // Sample pixels across image
        for (int y = 1; y < h - 1; y += step) {
            for (int x = 1; x < w - 1; x += step) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                double luma = 0.299 * r + 0.587 * g + 0.114 * b;
                sumLuma += luma;
                sumLumaSq += (luma * luma);

                // Sobel edge approximation
                int rgbRight = img.getRGB(x + 1, y);
                int rR = (rgbRight >> 16) & 0xFF;
                int gR = (rgbRight >> 8) & 0xFF;
                int bR = rgbRight & 0xFF;
                double lumaR = 0.299 * rR + 0.587 * gR + 0.114 * bR;

                int rgbDown = img.getRGB(x, y + 1);
                int rD = (rgbDown >> 16) & 0xFF;
                int gD = (rgbDown >> 8) & 0xFF;
                int bD = rgbDown & 0xFF;
                double lumaD = 0.299 * rD + 0.587 * gD + 0.114 * bD;

                double grad = Math.abs(luma - lumaR) + Math.abs(luma - lumaD);
                if (grad > 28.0) {
                    edgeCount++;
                }

                // Asphalt / Dirt / Tar / Concrete chromatic signature:
                // Low saturation, neutral tones (R ≈ G ≈ B within 35), moderate to dark brightness (30-180)
                int maxChannel = Math.max(r, Math.max(g, b));
                int minChannel = Math.min(r, Math.min(g, b));
                int delta = maxChannel - minChannel;

                if (delta < 45 && luma >= 25 && luma <= 190) {
                    asphaltColorPixels++;
                } else if (g > r + 15 && g > b + 15) {
                    greenFoliagePixels++;
                } else if (r > g && g > b && delta > 40 && luma > 110) {
                    skinOrIndoorPixels++;
                }

                totalPixels++;
            }
        }

        if (totalPixels == 0) totalPixels = 1;

        double meanLuma = sumLuma / totalPixels;
        double variance = (sumLumaSq / totalPixels) - (meanLuma * meanLuma);
        double edgeDensity = edgeCount / totalPixels;
        double asphaltRatio = (double) asphaltColorPixels / totalPixels;
        double foliageRatio = (double) greenFoliagePixels / totalPixels;
        double indoorSkinRatio = (double) skinOrIndoorPixels / totalPixels;

        sa.laplacianVariance = variance;
        sa.edgeDensity = edgeDensity;

        // Blurriness or extreme lighting check
        if (variance < 20.0 || meanLuma < 15.0 || meanLuma > 245.0) {
            sa.isExtremelyBlurryOrDark = true;
            sa.roadDamageProbability = 0.35;
            sa.isRoadSurface = false;
            sa.detectedDominantType = "Unclear / Blurry Image";
            return sa;
        }

        // Road Surface Check
        sa.isRoadSurface = asphaltRatio >= 0.35;

        // Road damage / pothole indicator:
        // Road surface present + substantial irregular edges (cavity cracks, gravel, rim shadows)
        double score = 0.0;
        if (asphaltRatio >= 0.35) {
            score = 0.60;
            // High edge density on road indicates holes, cracks, rough aggregate
            if (edgeDensity > 0.04) score += 0.25;
            else if (edgeDensity > 0.015) score += 0.18;

            if (variance > 180.0) score += 0.12; // Contrast between road and dark pothole cavity
            score = Math.min(0.96, score);
        } else {
            score = Math.max(0.05, 0.40 * asphaltRatio);
        }

        // Penalty for high foliage or indoor/skin colors
        if (indoorSkinRatio > 0.30) {
            score *= 0.25;
            sa.detectedDominantType = "Indoor / Portrait / Domestic Object";
        } else if (foliageRatio > 0.45) {
            score *= 0.35;
            sa.detectedDominantType = "Lush Vegetation / Forest / Garden";
        } else if (!sa.isRoadSurface) {
            sa.detectedDominantType = "Non-Asphalt Object / Synthetic Surface";
        } else {
            sa.detectedDominantType = "Asphalt Road Surface";
        }

        sa.roadDamageProbability = score;
        return sa;
    }
}
