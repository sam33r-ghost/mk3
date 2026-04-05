package com.Attendance.vision;
import org.w3c.dom.css.Rect;

import java.util.List;
import java.util.Map;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

import java.io.IOException;
import java.nio.file.Path;

public class FaceRecognitionEngine {
         private ZooModel<Image, DetectedObjects> faceDetector;
         private ZooModel<Image, float[]> faceRecognizer;

         public FaceRecognitionEngine() throws Exception {
                 // 1. Initialize Face Detection Model (e.g., RetinaFace)
                 Criteria<Image, DetectedObjects> detectCriteria = Criteria.builder()
                                .setTypes(Image.class, DetectedObjects.class)
                                .optArtifactId("ai.djl.localmodelzoo:retinaface") // Use a local or zoo model
                                .build();
                        this.faceDetector = detectCriteria.loadModel();

                        // 2. Initialize Face Recognition Model (e.g., FaceNet / ArcFace)
                        Criteria<Image, float[]> recognizeCriteria = Criteria.builder()
                                .setTypes(Image.class, float[].class)
                                .optArtifactId("ai.djl.localmodelzoo:facenet") // Loads your pre-trained model
                                .build();
                        this.faceRecognizer = recognizeCriteria.loadModel();
                }

                /**
                 * Extracts the face embedding from a raw camera frame.
                 */
                public float[] getEmbeddingFromFrame(Path imagePath) throws IOException, ModelException, TranslateException {
                        Image img = ImageFactory.getInstance().fromFile(imagePath);

                        // Step A: Detect all faces in the image
                        try (Predictor<Image, DetectedObjects> detector = faceDetector.newPredictor()) {
                                DetectedObjects detections = detector.predict(img);

                                List<DetectedObjects.Item> faces = detections.items();
                                if (faces.isEmpty()) {
                                        throw new RuntimeException("No face detected in front of the camera.");
                                }

                                // Assume the largest/most confident face is the student logging in
                                BoundingBox bounds = faces.get(0).getBoundingBox();
                                Rectangle rect = bounds.getBounds();

                                // Crop the image to just the face
                                int x = (int) (rect.getX() * img.getWidth());
                                int y = (int) (rect.getY() * img.getHeight());
                                int w = (int) (rect.getWidth() * img.getWidth());
                                int h = (int) (rect.getHeight() * img.getHeight());

                                Image croppedFace = img.getSubImage(x, y, w, h);

                                // Step B: Pass the cropped face to FaceNet to get the embedding
                                try (Predictor<Image, float[]> recognizer = faceRecognizer.newPredictor()) {
                                        return recognizer.predict(croppedFace); // Returns a vector (e.g., 128 or 512 floats)
                                }
                        }
                }
}