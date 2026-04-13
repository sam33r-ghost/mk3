package com.Attendance.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.Attendance.model.Student;

public  class loadData {

    // Database credentials (replace with your actual database details)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/vision";
    private static final String USER = "root";
    private static final String PASS = "root";

    /**
     * Loads student records from the database into an ArrayList.
     * * @return ArrayList containing Student objects
     */
    public static List<com.Attendance.model.Student> loadStudents() {
        List<com.Attendance.model.Student> studentList = new ArrayList<>();

        // SQL query - replace column and table names with your actual schema
        String query = "SELECT roll_no, name, profile_picture FROM students";

        // Try-with-resources ensures that Connection, PreparedStatement, and ResultSet are closed
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            // Iterate through the result set
            while (rs.next()) {
                // Extract data from the current row
                int rollNo = rs.getInt("roll_no");
                String name = rs.getString("name");

                // Retrieve the BLOB/VarBinary data as a byte array
                byte[] data = rs.getBytes("profile_picture");

                // Create a new Student object and add it to the list
                Student student = new com.Attendance.model.Student(rollNo, name, data);
                studentList.add(student);
            }

        } catch (SQLException e) {
            System.err.println("Database error occurred while loading students.");
            e.printStackTrace();
        }

        return studentList;
    }
}