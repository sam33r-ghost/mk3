/*package com.Attendance.vision;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import javax.swing.JFrame;

public class LiveCameraWindow {
    public static void main(String[] args) {
        Webcam webcam = Webcam.getDefault();

        // Create a panel that constantly updates with the live feed
        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setFPSDisplayed(true);
        panel.setImageSizeDisplayed(true);
        panel.setMirrored(true); // Mirrors the image so it feels natural to the user

        // Put it in a standard Java window
        JFrame window = new JFrame("Live Attendance Scanner");
        window.add(panel);
        window.setResizable(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.pack();
        window.setVisible(true);
    }
}*/