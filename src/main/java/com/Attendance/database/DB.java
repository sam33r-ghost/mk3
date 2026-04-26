package com.Attendance.database;

import com.Attendance.model.SessionRecord;


import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.Attendance.database.register.DB_URL;
import static com.Attendance.ui.AttendanceSystem.*;

public class DB {
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static boolean login(String email, String password) {
        String sql = "SELECT id, name FROM professors WHERE email = ? AND password = ?";
        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, password); // Note: Should be hashed in production
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentProfessorId = rs.getInt("id");
                currentProfessorName = rs.getString("name");
                return true;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    public static void saveSession(String course, int presentCount, int absentCount, List<Integer> absenteeRolls) {
        // Convert list to JSON string (e.g., "[101, 102]")
        String absenteesJson = absenteeRolls.toString();

        String sql = "INSERT INTO sessions (professor_id, course, session_date, session_time, present_count, absent_count, absentees) VALUES (?, ?, CURDATE(), CURTIME(), ?, ?, ?)";
        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentProfessorId);
            ps.setString(2, course);
            ps.setInt(3, presentCount);
            ps.setInt(4, absentCount);
            ps.setString(5, absenteesJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Failed to save session: " + e.getMessage());
        }
    }


    // Add these methods inside the `static class DB { ... }`
    public static List<SessionRecord> getHistory() {
        List<SessionRecord> history = new ArrayList<>();
        // Order by date and time descending (newest first)
        String sql = "SELECT id, course, session_date, session_time, present_count, absent_count, absentees " +
                "FROM sessions WHERE professor_id = ? " +
                "ORDER BY session_date DESC, session_time DESC";

        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentProfessorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(new SessionRecord(
                        rs.getInt("id"), rs.getString("course"),
                        rs.getDate("session_date").toString(), rs.getTime("session_time").toString(),
                        rs.getInt("present_count"), rs.getInt("absent_count"), rs.getString("absentees")
                ));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading history: " + e.getMessage());
        }
        return history;
    }

    public static List<String> getAbsenteeNames(String absenteesJson) {
        List<String> details = new ArrayList<>();
        // If the JSON string is empty or just "[]", return empty
        if (absenteesJson == null || absenteesJson.length() <= 2) return details;

        // Clean the string: convert "[101, 102]" to "101, 102"
        String cleanList = absenteesJson.replace("[", "").replace("]", "").trim();
        if (cleanList.isEmpty()) return details;

        // Use the clean list in a SQL IN() clause to get names from the students table
        String sql = "SELECT roll_no, name FROM students WHERE roll_no IN (" + cleanList + ")";

        try (Connection con = connect(); Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                details.add("Roll: " + rs.getInt("roll_no") + "  —  " + rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching student details: " + e.getMessage());
        }
        return details;
    }
}
