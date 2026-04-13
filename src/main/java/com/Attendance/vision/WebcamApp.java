package com.Attendance.vision;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.function.Consumer;

public class WebcamApp extends JFrame {

    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4120.dll");
    }

    private JLabel displayLabel;
    private VideoCapture capture;
    private Mat currentFrame;
    private Consumer<String> onPhotoCaptured; // This handles passing the data back!

    // Constructor now requires the callback function
    public WebcamApp(Consumer<String> onPhotoCaptured) {
        this.onPhotoCaptured = onPhotoCaptured;

        setTitle("AI Camera Capture");
        setLayout(new BorderLayout());

        displayLabel = new JLabel();
        add(displayLabel, BorderLayout.CENTER);

        JButton shootButton = new JButton("SHOOT");
        shootButton.setFont(new Font("Arial", Font.BOLD, 18));
        shootButton.setBackground(Color.RED);
        shootButton.setForeground(Color.WHITE);
        shootButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        shootButton.addActionListener(e -> {
            try {
                savePhoto();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        add(shootButton, BorderLayout.SOUTH);

        currentFrame = new Mat();
        capture = new VideoCapture(0);

        if (!capture.isOpened()) {
            JOptionPane.showMessageDialog(this, "Cannot access webcam!");
            dispose();
            return;
        }

        // Properly release camera if user closes window using the 'X' button
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (capture != null && capture.isOpened()) {
                    capture.release();
                }
            }
        });

        new Thread(this::updatePreview).start();

        pack();
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void updatePreview() {
        while (capture.isOpened() && capture.read(currentFrame)) {
            ImageIcon icon = new ImageIcon(matToBufferedImage(currentFrame));
            displayLabel.setIcon(icon);
            displayLabel.repaint();
        }
    }

    private void savePhoto() {
        if (currentFrame != null && !currentFrame.empty()) {
            String imagePath = "AI.jpg";
            Imgcodecs.imwrite(imagePath, currentFrame);

            // Release the camera hardware so it's free for the next time!
            if (capture != null && capture.isOpened()) {
                capture.release();
            }
            dispose(); // Close the webcam window

            // Send the image path back to the AttendanceSystem
            if (onPhotoCaptured != null) {
                onPhotoCaptured.accept(imagePath);
            }
        }
    }

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