package com.Attendance.vision;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class register extends JFrame {

    // --- Configuration (Update these!) ---
    public static final String DB_URL = "jdbc:mysql://localhost:3306/vision";
    public static final String USER = "root";
    public static final String PASS = "root";
    public static final String PHOTO_FOLDER_PATH = "D:\\College\\Semester 2\\OOPs\\VisionAttendanceM\\extracted"; // Example path

    // --- State Variables ---
    public File[] photoFiles;
    public int currentPhotoIndex = 0;

    // --- UI Components ---
    public JLabel photoLabel;
    public JTextField nameField, rollNoField, emailField;
    public JPasswordField passwordField;
    public JButton registerButton;
    public JButton skipButton;

    public register() {
        // 1. Setup the main window
        setTitle("Student Batch Registration");
        setSize(400, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 2. Load Photos
        loadPhotos();

        // 3. Build UI Panels
        buildUI();

        // 4. Load the first photo
        showCurrentPhoto();

        setLocationRelativeTo(null); // Center on screen
    }

    public void loadPhotos() {
        File folder = new File(PHOTO_FOLDER_PATH);
        if (folder.exists() && folder.isDirectory()) {
            // Filter to only get images
            photoFiles = folder.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".jpg") ||
                            name.toLowerCase().endsWith(".png") ||
                            name.toLowerCase().endsWith(".jpeg")
            );
        }

        if (photoFiles == null || photoFiles.length == 0) {
            JOptionPane.showMessageDialog(this, "No photos found in: " + PHOTO_FOLDER_PATH);
            System.exit(0);
        }
    }

    public  void buildUI() {
        // Top Panel: Photo
        JPanel photoPanel = new JPanel();
        photoLabel = new JLabel("Loading Photo...", SwingConstants.CENTER);
        photoLabel.setPreferredSize(new Dimension(250, 250));
        photoPanel.add(photoLabel);
        add(photoPanel, BorderLayout.NORTH);

        // Center Panel: Form Fields
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Roll No:"));
        rollNoField = new JTextField();
        formPanel.add(rollNoField);

        formPanel.add(new JLabel("Email:"));
        emailField = new JTextField();
        formPanel.add(emailField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        add(formPanel, BorderLayout.CENTER);

        // Bottom Panel: Button
        JPanel buttonPanel = new JPanel();
        registerButton = new JButton("Register & Next Photo");
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));

        // Button Click Logic
        registerButton.addActionListener(e -> processRegistration());

        // Bottom Panel: Buttons
        skipButton = new JButton("Skip Photo"); // <--- Create the skip button
        skipButton.setFont(new Font("Arial", Font.BOLD, 14)); // <--- Style it
        skipButton.addActionListener(e -> skipPhoto()); // <--- Give it an action

        buttonPanel.add(registerButton);
        buttonPanel.add(skipButton);
        add(buttonPanel, BorderLayout.SOUTH);

    }

    public void showCurrentPhoto() {
        if (currentPhotoIndex < photoFiles.length) {
            File currentFile = photoFiles[currentPhotoIndex];

            // Scale image to fit the label nicely
            ImageIcon icon = new ImageIcon(currentFile.getAbsolutePath());
            Image scaledImage = icon.getImage().getScaledInstance(250, 250, Image.SCALE_SMOOTH);
            photoLabel.setIcon(new ImageIcon(scaledImage));
            photoLabel.setText(""); // Clear text
        } else {
            // We ran out of photos
            photoLabel.setIcon(null);
            photoLabel.setText("All photos processed!");
            registerButton.setEnabled(false);
            disableFields();
            JOptionPane.showMessageDialog(this, "Batch registration complete!");
        }
    }

    public void processRegistration() {
        // Basic validation
        if (nameField.getText().isEmpty() || rollNoField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Roll No are required!");
            return;
        }

        try {
            // Read UI Data
            String name = nameField.getText();
            int rollNo = Integer.parseInt(rollNoField.getText());
            String email = emailField.getText();
            String password = new String(passwordField.getPassword());

            // Convert current photo to byte array
            File currentPhoto = photoFiles[currentPhotoIndex];
            Path p = Paths.get(currentPhoto.getPath());
            String path= p.toString();
            byte[] photoBytes = FaceEncoder.encodeFace(path);

            // Save to DB
            saveToDatabase(photoBytes, name, rollNo, email, password);

            // Move to next photo and reset UI
            currentPhotoIndex++;
            clearFields();
            showCurrentPhoto();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Roll number must be a valid integer!");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error reading image file.");
            ex.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveToDatabase(byte[] photo, String name, int rollNo, String email, String password) {
        String sql = "INSERT INTO students (profile_picture, name, roll_no, email, password) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBytes(1, photo);
            pstmt.setString(2, name);
            pstmt.setInt(3, rollNo);
            pstmt.setString(4, email);
            pstmt.setString(5, password);

            pstmt.executeUpdate();
            System.out.println("Saved: " + name);

        } catch (SQLException e) {
            // This will catch duplicate UNIQUE roll numbers
            if(e.getErrorCode() == 1062) { // MySQL specific duplicate entry error code
                JOptionPane.showMessageDialog(this, "Error: That Roll Number already exists in the database!");
            } else {
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    public void clearFields() {
        nameField.setText("");
        rollNoField.setText("");
        emailField.setText("");
        passwordField.setText("");
        nameField.requestFocus();
    }

    public void disableFields() {
        nameField.setEnabled(false);
        rollNoField.setEnabled(false);
        emailField.setEnabled(false);
        passwordField.setEnabled(false);
    }
    public void skipPhoto() {
        currentPhotoIndex++;
        clearFields();
        showCurrentPhoto();
    }


}