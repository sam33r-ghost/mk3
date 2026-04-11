package com.Attendance.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class dbManager {

    // Database credentials - replace with your actual details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/vision";
    private static final String USER = "root";
    private static final String PASS = "root";

    /**
     * Stores student data including a byte array into the SQL database.
     *
     * @param fileData The byte array (e.g., an image or file)
     * @param name     The student's name
     * @param rollNo   The student's roll number
     * @param email    The student's email
     * @param password The student's password (should be hashed in production!)
     */
    public void insertStudentData(byte[] fileData, String name, int rollNo, String email, String password) {

        // The ? characters are placeholders for our data
        String sql = "INSERT INTO students (profile_picture, name, roll_no, email, password) VALUES (?, ?, ?, ?, ?)";

        // Using try-with-resources ensures that the Connection and PreparedStatement
        // are automatically closed after execution, preventing memory leaks.
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Bind the parameters to the placeholders
            pstmt.setBytes(1, fileData); // Binds the byte array
            pstmt.setString(2, name);
            pstmt.setInt(3, rollNo);
            pstmt.setString(4, email);
            pstmt.setString(5, password);

            // Execute the insert operation
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Student data saved successfully!");
            }

        } catch (SQLException e) {
            System.err.println("Error saving data to the database:");
            e.printStackTrace();
        }
    }
}