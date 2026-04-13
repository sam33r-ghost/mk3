package com.Attendance.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.Attendance.vision.*;
import com.Attendance.service.Pipeline;
import com.Attendance.model.Student;
import com.Attendance.database.loadData;


public class AttendanceSystem {

    // ── Database Configuration ────────────────────────────────────────────────
    static final String DB_URL  = "jdbc:mysql://localhost:3306/vision";
    static final String DB_USER = "root";
    static final String DB_PASS = "root";

    // ── Modern Palette & Typography ───────────────────────────────────────────
    static final Color C_BG      = new Color(248, 250, 252);  // Slate 50
    static final Color C_WHITE   = Color.WHITE;
    static final Color C_PRIMARY = new Color(37, 99, 235);    // Blue 600
    static final Color C_HOVER   = new Color(29, 78, 216);    // Blue 700
    static final Color C_SUCCESS = new Color(16, 185, 129);   // Emerald 500
    static final Color C_DANGER  = new Color(239, 68, 68);    // Red 500
    static final Color C_TEXT    = new Color(15, 23, 42);     // Slate 900
    static final Color C_MUTED   = new Color(100, 116, 139);  // Slate 500
    static final Color C_BORDER  = new Color(226, 232, 240);  // Slate 200

    static final Font F_HEADING = new Font("SansSerif", Font.BOLD, 28);
    static final Font F_SUBHEAD = new Font("SansSerif", Font.BOLD, 15);
    static final Font F_BODY    = new Font("SansSerif", Font.PLAIN, 14);

    // ── State ─────────────────────────────────────────────────────────────────
    static String currentProfessorName = "";
    static int currentProfessorId = -1;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                // Global UI tweaks for cleaner inputs
                UIManager.put("TextField.margin", new Insets(8, 12, 8, 12));
                UIManager.put("PasswordField.margin", new Insets(8, 12, 8, 12));
            } catch (Exception ignored) {}
            showLogin();
        });
    }

    // Add a helper class to hold session data at the top of AttendanceSystem
    static class SessionRecord {
        int id; String course, date, time; int present, absent; String absenteesJson;
        public SessionRecord(int id, String course, String date, String time, int present, int absent, String absenteesJson) {
            this.id = id; this.course = course; this.date = date; this.time = time;
            this.present = present; this.absent = absent; this.absenteesJson = absenteesJson;
        }
    }
    // ══════════════════════════════════════════════════════════════════════════
    // DATABASE MANAGER
    // ══════════════════════════════════════════════════════════════════════════
    static class DB {
        static Connection connect() throws SQLException {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }

        static boolean login(String email, String password) {
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

        static void saveSession(String course, int presentCount, int absentCount, List<Integer> absenteeRolls) {
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
        static List<SessionRecord> getHistory() {
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

        static List<String> getAbsenteeNames(String absenteesJson) {
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

    // ══════════════════════════════════════════════════════════════════════════
    // AUTHENTICATION PAGES
    // ══════════════════════════════════════════════════════════════════════════

    static void showLogin() {
        JFrame frame = createBaseFrame("Login - Attendance Vision", 480, 560);
        JPanel card = createCardPanel();

        card.add(mkLabel("Welcome Back", F_HEADING, C_TEXT));
        card.add(gap(8));
        card.add(mkLabel("Sign in to your SVNIT professor dashboard", F_BODY, C_MUTED));
        card.add(gap(35));

        JTextField emailField = new JTextField();
        emailField.setFont(F_BODY);
        JPasswordField passField = new JPasswordField();
        passField.setFont(F_BODY);

        card.add(mkLabel("Email Address", F_SUBHEAD, C_TEXT)); card.add(gap(6)); card.add(emailField); card.add(gap(20));
        card.add(mkLabel("Password", F_SUBHEAD, C_TEXT)); card.add(gap(6)); card.add(passField); card.add(gap(35));

        JButton loginBtn = primaryBtn("Sign In", C_PRIMARY);
        JButton signupRedirect = ghostBtn("Don't have an account? Sign Up");
        signupRedirect.addActionListener(e -> { frame.dispose(); showSignUp(); });

        card.add(loginBtn); card.add(gap(15)); card.add(signupRedirect);
        frame.add(card); frame.setVisible(true);

        loginBtn.addActionListener(e -> {
            String pass = new String(passField.getPassword());
            if (DB.login(emailField.getText(), pass)) {
                frame.dispose();
                showDashboard();
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid credentials or DB offline.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    static void showSignUp() {
        JFrame frame = createBaseFrame("Sign Up - Attendance Vision", 480, 650);
        JPanel card = createCardPanel();

        card.add(mkLabel("Create Account", F_HEADING, C_TEXT));
        card.add(gap(8));
        card.add(mkLabel("Register as a new SVNIT professor", F_BODY, C_MUTED));
        card.add(gap(35));

        // Form Fields
        JTextField nameField = new JTextField();
        nameField.setFont(F_BODY);
        JTextField emailField = new JTextField();
        emailField.setFont(F_BODY);
        JPasswordField passField = new JPasswordField();
        passField.setFont(F_BODY);

        card.add(mkLabel("Full Name", F_SUBHEAD, C_TEXT)); card.add(gap(6)); card.add(nameField); card.add(gap(15));
        card.add(mkLabel("Email Address", F_SUBHEAD, C_TEXT)); card.add(gap(6)); card.add(emailField); card.add(gap(15));
        card.add(mkLabel("Password", F_SUBHEAD, C_TEXT)); card.add(gap(6)); card.add(passField); card.add(gap(35));

        // Buttons
        JButton signupBtn = primaryBtn("Sign Up", C_SUCCESS);
        JButton loginRedirect = ghostBtn("Already have an account? Log In");

        loginRedirect.addActionListener(e -> {
            frame.dispose();
            showLogin();
        });

        // Registration Logic
        signupBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String pass = new String(passField.getPassword()).trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please fill in all fields.", "Missing Information", JOptionPane.WARNING_MESSAGE);
                return;
            }

            signupBtn.setText("⏳ Registering...");
            signupBtn.setEnabled(false);

            // Run DB call in background to prevent UI freezing
            new Thread(() -> {
                boolean success = registerProfessor(name, email, pass);

                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(frame, "Account created successfully! Please log in.", "Welcome", JOptionPane.INFORMATION_MESSAGE);
                        frame.dispose();
                        showLogin();
                    } else {
                        signupBtn.setText("Sign Up");
                        signupBtn.setEnabled(true);
                    }
                });
            }).start();
        });

        card.add(signupBtn); card.add(gap(15)); card.add(loginRedirect);
        frame.add(card); frame.setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DASHBOARD
    // ══════════════════════════════════════════════════════════════════════════

    static void showDashboard() {
        JFrame frame = createBaseFrame("Dashboard - Attendance Vision", 850, 550);

        JPanel root = new JPanel(new BorderLayout(0, 30));
        root.setBackground(C_BG);
        root.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.add(mkLabel("Welcome, " + currentProfessorName, F_HEADING, C_TEXT));
        titlePanel.add(gap(5));
        titlePanel.add(mkLabel("Select an action below to manage your classes.", F_BODY, C_MUTED));

        header.add(titlePanel, BorderLayout.WEST);

        JButton logoutBtn = secondaryBtn("Logout");
        logoutBtn.setPreferredSize(new Dimension(100, 40));
        logoutBtn.addActionListener(e -> {
            currentProfessorId = -1; currentProfessorName = "";
            frame.dispose(); showLogin();
        });
        header.add(logoutBtn, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 30, 0));
        btnPanel.setOpaque(false);

        btnPanel.add(dashCard("📸 Take Attendance", "Start a live tracking session", e -> showTakeAttendance(frame)));
        // Replace the old Register Student button with this:
        btnPanel.add(dashCard("👤 Register Student", "Add faces to the database", e -> {
            showRegisterStudentDialog(frame);
        }));
        btnPanel.add(dashCard("📋 History", "View past sessions", e -> showHistory(frame)));

        root.add(btnPanel, BorderLayout.CENTER);
        frame.add(root);
        frame.setVisible(true);
    }
    // ══════════════════════════════════════════════════════════════════════════
    // STUDENT REGISTRATION MODULE
    // ══════════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════════
    // STUDENT REGISTRATION MODULE
    // ══════════════════════════════════════════════════════════════════════════

    static void showRegisterStudentDialog(JFrame parent) {
        // Must be non-modal (false) so we can interact with the Webcam window!
        JDialog dlg = new JDialog(parent, "Register New Student", false);
        dlg.setSize(500, 750);
        dlg.setLocationRelativeTo(parent);

        // State to hold the image path before saving
        final String[] currentImagePath = {null};

        JPanel p = createCardPanel();
        p.add(mkLabel("New Student Registration", F_HEADING, C_TEXT));
        p.add(gap(20));

        // ── Image Preview Area ──
        JPanel previewContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        previewContainer.setOpaque(false);

        JLabel imgPreviewLabel = mkLabel("No Image Selected", F_SUBHEAD, C_MUTED);
        imgPreviewLabel.setPreferredSize(new Dimension(150, 150));
        imgPreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgPreviewLabel.setBorder(new LineBorder(C_BORDER, 2, true));
        previewContainer.add(imgPreviewLabel);
        p.add(previewContainer);
        p.add(gap(20));

        // ── Form Fields ──
        p.add(mkLabel("Roll Number:", F_SUBHEAD, C_TEXT)); p.add(gap(5));
        JTextField rollField = new JTextField(); rollField.setFont(F_BODY); p.add(rollField); p.add(gap(15));

        p.add(mkLabel("Full Name:", F_SUBHEAD, C_TEXT)); p.add(gap(5));
        JTextField nameField = new JTextField(); nameField.setFont(F_BODY); p.add(nameField); p.add(gap(15));

        p.add(mkLabel("Email Address:", F_SUBHEAD, C_TEXT)); p.add(gap(5));
        JTextField emailField = new JTextField(); emailField.setFont(F_BODY); p.add(emailField); p.add(gap(15));

        p.add(mkLabel("Password:", F_SUBHEAD, C_TEXT)); p.add(gap(5));
        JPasswordField passField = new JPasswordField(); passField.setFont(F_BODY); p.add(passField); p.add(gap(25));

        // ── Capture Actions (Webcam / Browse) ──
        p.add(mkLabel("Face Data Source:", F_SUBHEAD, C_TEXT)); p.add(gap(8));
        JPanel captureActions = new JPanel(new GridLayout(1, 2, 15, 0));
        captureActions.setOpaque(false);

        JButton webcamBtn = secondaryBtn("📷 Webcam");
        webcamBtn.addActionListener(e -> {
            webcamBtn.setText("⏳ Opening...");
            webcamBtn.setEnabled(false);

            new WebcamApp(imagePath -> {
                SwingUtilities.invokeLater(() -> {
                    currentImagePath[0] = imagePath;
                    try {
                        ImageIcon icon = new ImageIcon(new ImageIcon(imagePath).getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH));
                        imgPreviewLabel.setIcon(icon);
                        imgPreviewLabel.setText(""); // Clear the "No Image" text
                    } catch (Exception ex) {
                        imgPreviewLabel.setText("Preview Error");
                    }
                    webcamBtn.setText("📷 Webcam");
                    webcamBtn.setEnabled(true);
                });
            });
        });

        JButton fileBtn = secondaryBtn("📁 Upload");
        fileBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                String path = fc.getSelectedFile().getAbsolutePath();
                currentImagePath[0] = path;
                try {
                    ImageIcon icon = new ImageIcon(new ImageIcon(path).getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH));
                    imgPreviewLabel.setIcon(icon);
                    imgPreviewLabel.setText("");
                } catch (Exception ex) {
                    imgPreviewLabel.setText("Preview Error");
                }
            }
        });

        captureActions.add(webcamBtn); captureActions.add(fileBtn);
        p.add(captureActions); p.add(gap(30));

        // ── Save Action ──
        JButton saveBtn = primaryBtn("💾 Save & Encode Face", C_SUCCESS);
        saveBtn.addActionListener(e -> {
            try {
                // Validate inputs
                int rollNo = Integer.parseInt(rollField.getText().trim());
                String name = nameField.getText().trim();
                String email = emailField.getText().trim();
                String password = new String(passField.getPassword()).trim();

                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "All fields are required!", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (currentImagePath[0] == null) {
                    JOptionPane.showMessageDialog(dlg, "You must capture or upload a face image!", "Missing Data", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                saveBtn.setText("⏳ Encoding Face...");
                saveBtn.setEnabled(false);

                // Run the Heavy encoding on a background thread
                new Thread(() -> {
                    try {
                        byte[] faceData = FaceEncoder.encodeFace(currentImagePath[0]);
                        registerStudent(rollNo, name, email, password, faceData);

                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(dlg, "Student Registered Successfully!");
                            dlg.dispose();
                        });

                    } catch (SQLException ex) {
                        SwingUtilities.invokeLater(() -> {
                            saveBtn.setText("💾 Save & Encode Face");
                            saveBtn.setEnabled(true);
                            if (ex.getErrorCode() == 1062) {
                                JOptionPane.showMessageDialog(dlg, "Roll Number or Email already exists.", "Duplicate Error", JOptionPane.ERROR_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(dlg, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            saveBtn.setText("💾 Save & Encode Face");
                            saveBtn.setEnabled(true);
                            JOptionPane.showMessageDialog(dlg, "Error encoding face data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }).start();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Roll Number must be a valid integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        p.add(saveBtn);
        dlg.add(p);
        dlg.setVisible(true);
    }

    static void registerStudent(int rollNo, String name, String email, String password, byte[] faceData) throws SQLException {
        String sql = "INSERT INTO students (roll_no, name, email, password, profile_picture) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DB.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rollNo);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setString(4, password);
            ps.setBytes(5, faceData);
            ps.executeUpdate();
        }
    }
    // ══════════════════════════════════════════════════════════════════════════
    // CONTINUOUS ATTENDANCE TRACKER
    // ══════════════════════════════════════════════════════════════════════════

    static void showTakeAttendance(JFrame parent) {
        // FIX 1: Change 'true' to 'false' to make the dialog Non-Modal
        // This allows you to interact with the WebcamApp frame while this dialog is open.
        JDialog dlg = new JDialog(parent, "Live Attendance Session", false);
        dlg.setSize(600, 650);
        dlg.setLocationRelativeTo(parent);

        // 1. Fetch real enrolled students from Database
        List<Student> allEnrolled = loadData.loadStudents();
        List<Integer> allEnrolledRolls = new ArrayList<>();
        for(Student s : allEnrolled) {
            allEnrolledRolls.add(s.getRollNo());
        }

        // 2. State Memory: Everyone is absent until seen
        Set<Integer> currentAbsentees = new HashSet<>(allEnrolledRolls);

        JPanel p = createCardPanel();
        p.add(mkLabel("Active Session Tracker", F_HEADING, C_TEXT));
        p.add(gap(25));

        p.add(mkLabel("Course Name/ID:", F_SUBHEAD, C_TEXT)); p.add(gap(8));
        JTextField courseField = new JTextField("e.g., AI-301");
        courseField.setFont(F_BODY);
        p.add(courseField); p.add(gap(25));

        // Stats Display Box
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        statsPanel.setOpaque(false);
        JLabel presentLbl = mkLabel("Present: 0", new Font("SansSerif", Font.BOLD, 22), C_SUCCESS);
        JLabel absentLbl = mkLabel("Absent: " + currentAbsentees.size(), new Font("SansSerif", Font.BOLD, 22), C_DANGER);

        JPanel pCard = createMiniCard(); pCard.add(presentLbl);
        JPanel aCard = createMiniCard(); aCard.add(absentLbl);
        statsPanel.add(pCard); statsPanel.add(aCard);

        p.add(statsPanel); p.add(gap(20));

        p.add(mkLabel("Pending Absentees:", F_SUBHEAD, C_TEXT)); p.add(gap(8));
        JTextArea absenteeArea = new JTextArea(4, 20);
        absenteeArea.setEditable(false);
        absenteeArea.setFont(new Font("Monospaced", Font.BOLD, 15));
        absenteeArea.setForeground(C_DANGER);
        absenteeArea.setBackground(new Color(254, 242, 242)); // Light red tint
        absenteeArea.setText(currentAbsentees.toString());
        absenteeArea.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JScrollPane scroll = new JScrollPane(absenteeArea);
        scroll.setBorder(new LineBorder(C_DANGER, 1, true));
        p.add(scroll); p.add(gap(30));

        // UI Updater Helper
        Runnable updateUI = () -> {
            int presentCount = allEnrolledRolls.size() - currentAbsentees.size();
            presentLbl.setText("Present: " + presentCount);
            absentLbl.setText("Absent: " + currentAbsentees.size());
            absenteeArea.setText(currentAbsentees.toString());
        };

        // Capture Buttons
        JPanel actions = new JPanel(new GridLayout(1, 2, 20, 0));
        actions.setOpaque(false);

        JButton webcamBtn = secondaryBtn("📷 Webcam Capture");
        webcamBtn.addActionListener(e -> {
            webcamBtn.setText("⏳ Opening Camera...");
            webcamBtn.setEnabled(false);

            // Open the webcam frame and pass the lambda function
            new WebcamApp(imagePath -> {

                // Update UI text on the main thread
                SwingUtilities.invokeLater(() -> {
                    webcamBtn.setText("🧠 Processing Faces...");
                });

                // Run heavy AI processing on a Background Thread so the UI doesn't freeze
                new Thread(() -> {
                    try {
                        List<Integer> newlyRecognized = Pipeline.pipe(imagePath);

                        // Push the UI updates back to the main Swing thread
                        SwingUtilities.invokeLater(() -> {
                            for (int roll : newlyRecognized) {
                                currentAbsentees.remove(roll);
                            }
                            updateUI.run();
                            webcamBtn.setText("📷 Webcam Capture");
                            webcamBtn.setEnabled(true);
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(dlg, "Error processing image: " + ex.getMessage());
                            webcamBtn.setText("📷 Webcam Capture");
                            webcamBtn.setEnabled(true);
                        });
                    }
                }).start();
            });
        });

        // Also fix the File Upload button so it doesn't freeze the UI
        JButton fileBtn = secondaryBtn("📁 Upload Image");
        fileBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                String path = fc.getSelectedFile().getAbsolutePath();
                fileBtn.setText("🧠 Processing...");
                fileBtn.setEnabled(false);

                new Thread(() -> {
                    try {
                        List<Integer> newlyRecognized = Pipeline.pipe(path);
                        for (int roll : newlyRecognized) currentAbsentees.remove(roll);

                        SwingUtilities.invokeLater(() -> {
                            updateUI.run();
                            fileBtn.setText("📁 Upload Image");
                            fileBtn.setEnabled(true);
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(dlg, "Error processing image: " + ex.getMessage());
                            fileBtn.setText("📁 Upload Image");
                            fileBtn.setEnabled(true);
                        });
                    }
                }).start();
            }
        });

        actions.add(webcamBtn); actions.add(fileBtn);
        p.add(actions); p.add(gap(35));

        JButton saveBtn = primaryBtn("💾 Finalize & Save Session", C_PRIMARY);
        saveBtn.addActionListener(e -> {
            if (courseField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Enter a course name!", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int presentCount = allEnrolledRolls.size() - currentAbsentees.size();
            DB.saveSession(courseField.getText(), presentCount, currentAbsentees.size(), new ArrayList<>(currentAbsentees));

            JOptionPane.showMessageDialog(dlg, "Session Saved Successfully!\nFinal Absentees: " + currentAbsentees.size());
            dlg.dispose();
        });

        p.add(saveBtn);
        dlg.add(p);
        dlg.setVisible(true);
    }
    static boolean registerProfessor(String name, String email, String password) {
        String sql = "INSERT INTO professors (name, email, password) VALUES (?, ?, ?)";
        try (Connection con = DB.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, email.trim());
            ps.setString(3, password); // Note: In production, hash this password!
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // MySQL error code for duplicate entry
                JOptionPane.showMessageDialog(null, "An account with this email already exists!", "Registration Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
    }



    // ══════════════════════════════════════════════════════════════════════════
// HISTORY MODULE
// ══════════════════════════════════════════════════════════════════════════

    static void showHistory(JFrame parent) {
        JDialog dlg = new JDialog(parent, "Session History", true);
        dlg.setSize(800, 500);
        dlg.setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(C_BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        root.add(mkLabel("Attendance History", F_HEADING, C_TEXT), BorderLayout.NORTH);

        // Fetch data
        List<SessionRecord> sessions = DB.getHistory();

        // Setup Table Columns
        String[] cols = {"Date", "Time", "Course", "Present", "Absent", "Details"};
        Object[][] data = new Object[sessions.size()][6];

        for (int i = 0; i < sessions.size(); i++) {
            SessionRecord s = sessions.get(i);
            data[i] = new Object[]{s.date, s.time, s.course, s.present, s.absent, "View Absentees ➔"};
        }

        JTable table = new JTable(data, cols) {
            public boolean isCellEditable(int row, int column) { return false; }
        };

        // Beautiful Table Styling
        table.setFont(F_BODY);
        table.setForeground(C_TEXT);
        table.setRowHeight(40);
        table.setBackground(C_WHITE);
        table.setGridColor(C_BORDER);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(239, 246, 255)); // Light Blue
        table.setSelectionForeground(C_PRIMARY);

        table.getTableHeader().setFont(F_SUBHEAD);
        table.getTableHeader().setBackground(C_WHITE);
        table.getTableHeader().setForeground(C_MUTED);
        table.getTableHeader().setBorder(new MatteBorder(0, 0, 2, 0, C_BORDER));
        table.getTableHeader().setPreferredSize(new Dimension(0, 45));

        // Custom renderer to make the "Details" column look like a clickable link
        table.getColumnModel().getColumn(5).setCellRenderer((tbl, value, isSelected, hasFocus, row, column) -> {
            JLabel label = mkLabel(value.toString(), F_SUBHEAD, C_PRIMARY);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (isSelected) label.setForeground(C_HOVER);
            return label;
        });

        // Handle clicks on the table
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    SessionRecord selectedSession = sessions.get(row);
                    showAbsenteeDetails(dlg, selectedSession);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(C_BORDER, 1, true));
        scroll.getViewport().setBackground(C_WHITE);

        root.add(scroll, BorderLayout.CENTER);

        JButton closeBtn = ghostBtn("Close History");
        closeBtn.addActionListener(e -> dlg.dispose());
        root.add(closeBtn, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }






// ── Absentee Details Popup ────────────────────────────────────────────────

    static void showAbsenteeDetails(JDialog parent, SessionRecord session) {
        JDialog dlg = new JDialog(parent, "Absentee Details", true);
        dlg.setSize(400, 500);
        dlg.setLocationRelativeTo(parent);

        JPanel p = createCardPanel();

        p.add(mkLabel(session.course, F_HEADING, C_TEXT));
        p.add(gap(5));
        p.add(mkLabel(session.date + " at " + session.time, F_BODY, C_MUTED));
        p.add(gap(25));

        p.add(mkLabel("Absent Students (" + session.absent + ")", F_SUBHEAD, C_DANGER));
        p.add(gap(10));

        // Fetch actual names from DB based on JSON array
        List<String> absenteeDetails = DB.getAbsenteeNames(session.absenteesJson);

        // Create a beautiful list UI
        DefaultListModel<String> listModel = new DefaultListModel<>();
        if (absenteeDetails.isEmpty()) {
            listModel.addElement("No absentees recorded.");
        } else {
            for (String detail : absenteeDetails) listModel.addElement(detail);
        }

        JList<String> list = new JList<>(listModel);
        list.setFont(new Font("Monospaced", Font.BOLD, 14));
        list.setForeground(C_TEXT);
        list.setFixedCellHeight(35);
        list.setSelectionBackground(C_WHITE); // Prevent highlight on click
        list.setSelectionForeground(C_TEXT);

        // Add custom padding to list items
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
                return label;
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(new LineBorder(C_BORDER, 1, true));
        p.add(scroll);
        p.add(gap(20));

        JButton closeBtn = primaryBtn("Done", C_PRIMARY);
        closeBtn.addActionListener(e -> dlg.dispose());
        p.add(closeBtn);

        dlg.add(p);
        dlg.setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CUSTOM SWING GRAPHICS & STYLING UTILS
    // ══════════════════════════════════════════════════════════════════════════

    static JFrame createBaseFrame(String title, int width, int height) {
        JFrame f = new JFrame(title);
        f.setSize(width, height);
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().setBackground(C_BG);
        return f;
    }

    static JPanel createCardPanel() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 20, 20));
                g2.setColor(C_BORDER);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 20, 20));
                g2.dispose();
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        return p;
    }

    static JPanel createMiniCard() {
        JPanel p = new JPanel(new GridBagLayout()); // For centering
        p.setBackground(C_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        return p;
    }

    static JLabel mkLabel(String text, Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f); l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // Beautiful rounded button implementation
    static JButton primaryBtn(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? C_HOVER : bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(F_SUBHEAD); b.setForeground(C_WHITE);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton secondaryBtn(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(241, 245, 249) : C_WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 15, 15));
                g2.setColor(C_PRIMARY);
                g2.setStroke(new BasicStroke(2));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-3, getHeight()-3, 15, 15));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(F_SUBHEAD); b.setForeground(C_PRIMARY);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton ghostBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(F_BODY); b.setForeground(C_PRIMARY);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton dashCard(String title, String desc, ActionListener action) {
        JButton b = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(248, 250, 252) : C_WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 20, 20));
                g2.setColor(getModel().isRollover() ? C_PRIMARY : C_BORDER);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 20, 20));
                g2.dispose();
            }
        };
        b.setLayout(new BoxLayout(b, BoxLayout.Y_AXIS));
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(35, 25, 35, 25));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        b.add(mkLabel(title, F_HEADING, C_PRIMARY));
        b.add(gap(12));
        b.add(mkLabel(desc, F_BODY, C_MUTED));
        b.addActionListener(action);
        return b;
    }

    static Component gap(int h) { return Box.createRigidArea(new Dimension(0, h)); }
}