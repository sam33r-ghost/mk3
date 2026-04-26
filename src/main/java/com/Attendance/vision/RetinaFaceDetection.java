package com.Attendance.vision;

import ai.djl.Device;
import ai.djl.engine.Engine;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RetinaFaceDetection {

    private static final Logger logger = LoggerFactory.getLogger(RetinaFaceDetection.class);

    // Hold the model and predictor in memory
    private static ZooModel<Image, DetectedObjects> model;
    private static Predictor<Image, DetectedObjects> predictor;

    // This static block runs EXACTLY ONCE when the class is loaded
    static {
        try {
            // 🔥 FORCE DJL TO DOWNLOAD AND USE NVIDIA GPU (CUDA) 🔥
           // System.setProperty("PYTORCH_FLAVOR", "cu121");

            double confThresh = 0.85f;
            double nmsThresh = 0.45f;
            double[] variance = {0.1f, 0.2f};
            int topK = 5000;
            int[][] scales = {{16, 32}, {64, 128}, {256, 512}};
            int[] steps = {8, 16, 32};

            FaceDetectionTranslator translator =
                    new FaceDetectionTranslator(confThresh, nmsThresh, variance, topK, scales, steps);

            Path localModelDir = Paths.get("src/main/resources/models/");

            Device device = Device.gpu(); // Changes from Device.cpu()
            if (!device.isGpu()) {
                logger.warn("GPU not found or CUDA not configured. Falling back to CPU.");
                device = Device.cpu();
            }

            Criteria<Image, DetectedObjects> criteria =
                    Criteria.builder()
                            .setTypes(Image.class, DetectedObjects.class)
                            .optModelPath(localModelDir)
                            .optModelName("retinaface")
                            .optTranslator(translator)
                            .optProgress(new ProgressBar())
                            .optEngine("PyTorch")
                            .optDevice(device) // Crucial step
                            .build();
            System.out.println("🔥 DJL is currently using: " + Engine.getInstance().defaultDevice());
            model = criteria.loadModel();
            predictor = model.newPredictor();
            logger.info("✅ RetinaFace model loaded into memory successfully.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize RetinaFace model", e);
        }
    }

    private RetinaFaceDetection() {}

    // Accepts BufferedImage directly from the webcam to avoid disk I/O
    public static DetectedObjects predict(BufferedImage bi) throws Exception {
        Image img = ImageFactory.getInstance().fromImage(bi);

        DetectedObjects detection;
        synchronized (predictor) {
            detection = predictor.predict(img);
        }
        return detection;
    }

    private static void saveBoundingBoxImage(Image img, DetectedObjects detection)
            throws IOException {
        Path outputDir = Paths.get("build/output");
        Files.createDirectories(outputDir);
        img.drawBoundingBoxes(detection);
        Path imagePath = outputDir.resolve("AI.png");
        img.save(Files.newOutputStream(imagePath), "png");
    }

    // Call this when shutting down the server/app to free memory
    public static void close() {
        if (predictor != null) predictor.close();
        if (model != null) model.close();
    }
}