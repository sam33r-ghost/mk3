package com.Attendance.vision;

import ai.djl.ModelException;
import ai.djl.translate.TranslateException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;

public class WebcamApp extends JFrame {

    static {
        // Note: Use System.load() instead of loadLibrary(), and provide the full path including the .dll file
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4120.dll");
    }

    private JLabel displayLabel;
    private VideoCapture capture;
    private Mat currentFrame;

    public WebcamApp() {
        setTitle("AI Camera Capture");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        displayLabel = new JLabel();
        add(displayLabel, BorderLayout.CENTER);

        JButton shootButton = new JButton("SHOOT");
        shootButton.setFont(new Font("Arial", Font.BOLD, 18));
        shootButton.setBackground(Color.RED);
        shootButton.setForeground(Color.WHITE);

        // Action Listener for the button
        shootButton.addActionListener(e -> {
            try {
                savePhoto();
            } catch (ModelException ex) {
                throw new RuntimeException(ex);
            } catch (TranslateException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        add(shootButton, BorderLayout.SOUTH);

        currentFrame = new Mat();
        capture = new VideoCapture(0);

        if (!capture.isOpened()) {
            JOptionPane.showMessageDialog(this, "Cannot access webcam!");
            System.exit(0);
        }

        // Thread to update the live preview
        new Thread(this::updatePreview).start();

        pack();
        setSize(800, 600);
        setVisible(true);
    }

    private void updatePreview() {
        while (capture.read(currentFrame)) {
            // Convert Mat to Image and display it
            ImageIcon icon = new ImageIcon(matToBufferedImage(currentFrame));
            displayLabel.setIcon(icon);
            displayLabel.repaint();
        }
    }

    private void savePhoto() throws ModelException, TranslateException, IOException {
        if (currentFrame != null && !currentFrame.empty()) {
            Imgcodecs.imwrite("AI.jpg", currentFrame);
            try {
                Pipeline.pipe("AI.jpg");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }
    }

    /**
     * Converts OpenCV Mat to Java's BufferedImage
     */
    private BufferedImage matToBufferedImage(Mat mat) {
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