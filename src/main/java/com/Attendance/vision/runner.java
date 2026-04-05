package com.Attendance.vision;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.Image;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import ai.djl.ModelException;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.translate.TranslateException;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class runner {
    public static void main(String[] args) throws IOException, ModelException, TranslateException {
        Path p = Paths.get("src/main/java/com/Attendance/vision/AMBD11.jpg");
        Image img = ImageFactory.getInstance().fromFile(p);
        DetectedObjects detection = RetinaFaceDetection.predict();
        int h=img.getHeight();
        int w=img.getWidth();

        int faceCounter = 0;

// THIS IS THE LOOP
        while (faceCounter< detection.getNumberOfObjects()) {

            DetectedObjects.DetectedObject face= detection.item(faceCounter);
            face.getBoundingBox().getBounds();
            // 1. Get the bounding box from DJL
            ai.djl.modality.cv.output.Rectangle rect = face.getBoundingBox().getBounds();

            // 2. Convert DJL's normalized decimals (0.0 to 1.0) into actual pixels
            int x = (int) (rect.getX() * img.getWidth());
            int y = (int) (rect.getY() * img.getHeight());
            int width = (int) (rect.getWidth() * img.getWidth());
            int height = (int) (rect.getHeight() * img.getHeight());

            // 3. Safety Checks (Crucial!)
            // If a face is on the very edge of the photo, the model might guess coordinates
            // slightly outside the image. This prevents "Out of Bounds" crashing.
            x = Math.max(0, x);
            y = Math.max(0, y);
            width = Math.min(width, img.getWidth() - x);
            height = Math.min(height, img.getHeight() - y);

            // 4. Crop the face using DJL's native method
            Image croppedFace = img.getSubImage(x, y, width, height);

            // 5. Save the extracted face to a folder
            Path outputPath = Paths.get("extracted_faces/face_" + faceCounter + ".png");

            try {
                // Create the directory if it doesn't exist yet
                if (outputPath.getParent() != null) {
                    Files.createDirectories(outputPath.getParent());
                }

                // Write the image file
                try (OutputStream os = Files.newOutputStream(outputPath)) {
                    croppedFace.save(os, "png");
                    System.out.println("Successfully saved: " + outputPath);
                }
            } catch (Exception e) {
                System.out.println("Failed to save face " + faceCounter);
                e.printStackTrace();
            }

            faceCounter++;
        }


    }
}
