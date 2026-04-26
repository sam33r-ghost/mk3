package com.Attendance.service;

import ai.djl.modality.cv.output.DetectedObjects;
import com.Attendance.database.loadData;
import com.Attendance.model.Student;
import com.Attendance.vision.FaceEncoder;
import com.Attendance.vision.RetinaFaceDetection;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

public class Pipeline {
    static {
        try {

            System.load("C:\\opencv\\build\\java\\x64\\opencv_java4120.dll");
            System.out.println("✅ OpenCV Native Library Loaded in Pipeline.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("❌ Failed to load OpenCV DLL. Check the path!");
            e.printStackTrace();
        }
    }

    public static List<Integer> pipe(String pathString) throws Exception {
        System.out.println("🚀 Starting in-memory pipeline for: " + pathString);

        List<Integer> presentRolls = new ArrayList<>();

        // 1. Load the full image from the webcam capture ONE time
        Mat fullImage = Imgcodecs.imread(pathString);
        if (fullImage.empty()) {
            System.err.println("❌ Failed to load image from path: " + pathString);
            return presentRolls;
        }

        // 2. Convert to BufferedImage for RetinaFace
        BufferedImage bi = matToBufferedImage(fullImage);
        DetectedObjects detections = RetinaFaceDetection.predict(bi);

        // 3. Load students from DB
        List<Student> studentlist = loadData.loadStudents();
        System.out.println("✅ Students extracted from database.");

        if (studentlist.isEmpty()) {
            System.out.println("No students found in the database.");
            return presentRolls;
        }

        // 4. Loop through each detected face
        for (ai.djl.modality.Classifications.Classification item : detections.items()) {

            DetectedObjects.DetectedObject obj = (DetectedObjects.DetectedObject) item;
            ai.djl.modality.cv.output.Rectangle rect = obj.getBoundingBox().getBounds();

            // Convert normalized coordinates to pixel values
            int x = (int) (rect.getX() * fullImage.cols());
            int y = (int) (rect.getY() * fullImage.rows());
            int w = (int) (rect.getWidth() * fullImage.cols());
            int h = (int) (rect.getHeight() * fullImage.rows());

            // Failsafe boundary checks to prevent out-of-bounds exceptions
            x = Math.max(0, x);
            y = Math.max(0, y);
            w = Math.min(fullImage.cols() - x, w);
            h = Math.min(fullImage.rows() - y, h);

            // Crop face entirely in RAM
            org.opencv.core.Rect roi = new org.opencv.core.Rect(x, y, w, h);
            Mat croppedFace = new Mat(fullImage, roi);

            // 5. Generate embedding directly from the cropped memory block
            byte[] targetEmbeddingBytes = FaceEncoder.encodeFaceFromMat(croppedFace);

            // 6. Compare this detected face against all students in the database
            for (Student currentStudent : studentlist) {
                int roll = currentStudent.getRollNo();
                String name = currentStudent.getName();
                byte[] dbData = currentStudent.getFileData();

                if (dbData != null && dbData.length > 0) {
                    // Compare the two byte arrays using cosine similarity
                    double similarity = FaceEncoder.similarity(dbData, targetEmbeddingBytes);

                    // Threshold check (0.39 based on your previous code)
                    if (similarity >= 0.39) {
                        System.out.println("✅ Match Found!");
                        System.out.println("Roll Number: " + roll);
                        System.out.println("Student Name: " + name);
                        System.out.println("Similarity Score: " + similarity);

                        // Prevent adding the same student twice if detected multiple times
                        if (!presentRolls.contains(roll)) {
                            presentRolls.add(roll);
                        }

                        // Stop searching DB for this specific face since we found a match
                        break;
                    }
                }
            }
        }

        System.out.println("🎉 Pipeline complete. Total present: " + presentRolls.size());
        return presentRolls;
    }

    /**
     * Helper method to convert OpenCV Mat to Java BufferedImage
     */
    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = (mat.channels() > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] b = new byte[bufferSize];
        mat.get(0, 0, b);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }
}