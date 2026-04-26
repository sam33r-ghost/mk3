package com.Attendance.vision;

import ai.onnxruntime.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collections;

public class FaceEncoder {

    private static final int IMG_SIZE = 112;
    private static OrtEnvironment env;
    private static OrtSession session;

    static {
        try {
            System.load("C:\\opencv\\build\\java\\x64\\opencv_java4120.dll");

            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setIntraOpNumThreads(4);

            // Direct absolute path — no classpath needed
            String modelPath = "src/main/resources/models/w600k_r50.onnx";

            session = env.createSession(modelPath, opts);
            System.out.println("✅ ArcFace model loaded successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Encodes a face image into a 512-dim ArcFace embedding as byte[].
     */
    public static byte[] encodeFace(String imagePath) throws Exception {
        float[] embedding = getEmbedding(imagePath);

        ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
        for (float f : embedding) buffer.putFloat(f);
        return buffer.array();
    }

    /**
     * Returns raw float[512] L2-normalized ArcFace embedding.
     */
    public static float[] getEmbedding(String imagePath) throws Exception {

        // 1. Load image
        Mat img = Imgcodecs.imread(imagePath);
        if (img.empty()) {
            throw new IllegalArgumentException("Cannot load image: " + imagePath);
        }

        // 2. Resize to 112×112
        Mat resized = new Mat();
        Imgproc.resize(img, resized, new Size(IMG_SIZE, IMG_SIZE));

        // 3. BGR → RGB
        Mat rgb = new Mat();
        Imgproc.cvtColor(resized, rgb, Imgproc.COLOR_BGR2RGB);

        // 4. Normalize: (pixel - 127.5) / 128.0
        rgb.convertTo(rgb, CvType.CV_32F);
        Core.subtract(rgb, new Scalar(127.5, 127.5, 127.5), rgb);
        Core.divide(rgb, new Scalar(128.0, 128.0, 128.0), rgb);

        // 5. HWC → NCHW [1, 3, 112, 112]
        float[] nchw = hwcToNchw(rgb);

        // 6. Create input tensor
        long[] shape = {1, 3, IMG_SIZE, IMG_SIZE};
        OnnxTensor inputTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(nchw), shape);

        // 7. Run inference
        String inputName = session.getInputNames().iterator().next();
        OrtSession.Result result = session.run(
                Collections.singletonMap(inputName, inputTensor));

        // 8. Extract [1, 512] output
        float[][] output = (float[][]) ((OnnxTensor) result.get(0)).getValue();

        // 9. L2-normalize and return
        return l2Normalize(output[0]);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Decode byte[] back to float[512] for comparison. */
    public static float[] bytesToEmbedding(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] embedding = new float[bytes.length / 4];
        for (int i = 0; i < embedding.length; i++) embedding[i] = buffer.getFloat();
        return embedding;
    }

    /** Cosine similarity — returns 1.0 for same face, lower for different. */
    public static double similarity(byte[] a, byte[] b) {
        float[] fa = bytesToEmbedding(a);
        float[] fb = bytesToEmbedding(b);
        double dot = 0;
        for (int i = 0; i < fa.length; i++) dot += fa[i] * fb[i];
        return dot;
    }

    /** Returns true if similarity >= threshold (recommended: 0.5) */
    public static boolean isSamePerson(String path1, String path2, double threshold)
            throws Exception {
        return similarity(encodeFace(path1), encodeFace(path2)) >= threshold;
    }
    public static boolean isSamePerson(byte[] byte1, String path2, double threshold)
            throws Exception {
        return similarity(byte1, encodeFace(path2)) >= threshold;
    }

    private static float[] hwcToNchw(Mat mat) {
        int h = mat.rows(), w = mat.cols(), c = mat.channels();
        float[] hwc = new float[h * w * c];
        mat.get(0, 0, hwc);
        float[] nchw = new float[c * h * w];
        for (int ch = 0; ch < c; ch++)
            for (int row = 0; row < h; row++)
                for (int col = 0; col < w; col++)
                    nchw[ch * h * w + row * w + col] = hwc[(row * w + col) * c + ch];
        return nchw;
    }

    private static float[] l2Normalize(float[] vec) {
        double norm = 0;
        for (float v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        float[] out = new float[vec.length];
        for (int i = 0; i < vec.length; i++) out[i] = (float) (vec[i] / norm);
        return out;
    }
    /**
     * NEW: Encodes a face directly from an in-memory OpenCV Mat (Zero disk I/O)
     */
    public static byte[] encodeFaceFromMat(Mat faceMat) throws Exception {
        float[] embedding = getEmbeddingFromMat(faceMat);
        ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
        for (float f : embedding) buffer.putFloat(f);
        return buffer.array();
    }

    /**
     * NEW: Generates embedding directly from an in-memory OpenCV Mat
     */
    public static float[] getEmbeddingFromMat(Mat faceMat) throws Exception {
        if (faceMat.empty()) {
            throw new IllegalArgumentException("Provided Mat is empty.");
        }

        // 2. Resize to 112×112
        Mat resized = new Mat();
        Imgproc.resize(faceMat, resized, new Size(IMG_SIZE, IMG_SIZE));

        // 3. BGR → RGB
        Mat rgb = new Mat();
        Imgproc.cvtColor(resized, rgb, Imgproc.COLOR_BGR2RGB);

        // 4. Normalize: (pixel - 127.5) / 128.0
        rgb.convertTo(rgb, CvType.CV_32F);
        Core.subtract(rgb, new Scalar(127.5, 127.5, 127.5), rgb);
        Core.divide(rgb, new Scalar(128.0, 128.0, 128.0), rgb);

        // 5. HWC → NCHW [1, 3, 112, 112]
        float[] nchw = hwcToNchw(rgb);

        // 6. Create input tensor
        long[] shape = {1, 3, IMG_SIZE, IMG_SIZE};
        OnnxTensor inputTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(nchw), shape);

        // 7. Run inference
        String inputName = session.getInputNames().iterator().next();
        OrtSession.Result result = session.run(
                Collections.singletonMap(inputName, inputTensor));

        // 8. Extract [1, 512] output
        float[][] output = (float[][]) ((OnnxTensor) result.get(0)).getValue();

        // 9. L2-normalize and return
        return l2Normalize(output[0]);
    }
}