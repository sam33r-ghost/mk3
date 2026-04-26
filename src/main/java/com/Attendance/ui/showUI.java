package com.Attendance.ui;

import com.Attendance.database.DB;
import com.Attendance.database.loadData;
import com.Attendance.database.register;
import com.Attendance.model.SessionRecord;
import com.Attendance.model.Student;
import com.Attendance.service.Pipeline;
import com.Attendance.vision.WebcamApp;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.Attendance.database.register.registerProfessor;
import static com.Attendance.ui.AttendanceSystem.*;


public class showUI{


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

        btnPanel.add(dashCard("📸 Take Attendance", "Start a live tracking session", e -> showUI.TakeAttendance(frame)));
        // Replace the old Register Student button with this:
        btnPanel.add(dashCard("👤 Register Student", "Add faces to the database", e -> {
            showRegisterStudentDialog(frame);
        }));
        btnPanel.add(dashCard("📋 History", "View past sessions", e -> showHistory(frame)));

        root.add(btnPanel, BorderLayout.CENTER);
        frame.add(root);
        frame.setVisible(true);
    }

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


    static void showRegisterStudentDialog(JFrame parent) {
        // Must be non-modal (false) so we can interact with the Webcam window!
        JDialog dlg = new JDialog(parent, "Capture Student Photo", false);
        dlg.setSize(500, 400); // Reduced size since we removed the form fields
        dlg.setLocationRelativeTo(parent);

        JPanel p = createCardPanel();
        p.add(mkLabel("Step 1: Capture Face Data", F_HEADING, C_TEXT));
        p.add(gap(20));

        // ── Image Preview Area ──
        JPanel previewContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        previewContainer.setOpaque(false);

        JLabel imgPreviewLabel = mkLabel("Waiting for Image...", F_SUBHEAD, C_MUTED);
        imgPreviewLabel.setPreferredSize(new Dimension(150, 150));
        imgPreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgPreviewLabel.setBorder(new LineBorder(C_BORDER, 2, true));
        previewContainer.add(imgPreviewLabel);
        p.add(previewContainer);
        p.add(gap(30));

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
                    imgPreviewLabel.setText("Processing...");
                    handleCapturedImage(imagePath, dlg);
                });
            });
        });

        JButton fileBtn = secondaryBtn("📁 Upload");
        fileBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                String path = fc.getSelectedFile().getAbsolutePath();
                imgPreviewLabel.setText("Processing...");
                handleCapturedImage(path, dlg);
            }
        });

        captureActions.add(webcamBtn);
        captureActions.add(fileBtn);
        p.add(captureActions);

        dlg.add(p);
        dlg.setVisible(true);
    }

    /**
     * Helper method to copy the captured/uploaded image to the extraction folder
     * and launch the register UI.
     */
    private static void handleCapturedImage(String sourceImagePath, JDialog dialog) {
        try {
            Path sourcePath = Paths.get(sourceImagePath);
            String fileName = sourcePath.getFileName().toString();

            // Use the path defined in your untouched register.java class
            Path destDir = Paths.get(register.PHOTO_FOLDER_PATH);

            // Create the directory if it doesn't exist
            if (!Files.exists(destDir)) {
                Files.createDirectories(destDir);
            }

            // Copy the file over to the folder so the register class can find it
            Path destPath = destDir.resolve("captured_" + System.currentTimeMillis() + "_" + fileName);
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);

            // Close this capture dialog
            dialog.dispose();

            // Launch your original register UI
            SwingUtilities.invokeLater(() -> {
                try {
                    Pipeline.pipe(sourceImagePath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                new register().setVisible(true);
            });

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(dialog, "Error copying image to extraction folder: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }


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

static void TakeAttendance(JFrame parent) {
    // FIX 1: Change 'true' to 'false' to make the dialog Non-Modal
    // This allows you to interact with the WebcamApp frame while this dialog is open.
    JDialog dlg = new JDialog(parent, "Live Attendance Session", false);
    dlg.setSize(600, 650);
    dlg.setLocationRelativeTo(parent);

    // 1. Fetch real enrolled students from Database
    List<Student> allEnrolled = loadData.loadStudents();
    List<Integer> allEnrolledRolls = new ArrayList<>();
    for (Student s : allEnrolled) {
        allEnrolledRolls.add(s.getRollNo());
    }

    // 2. State Memory: Everyone is absent until seen
    Set<Integer> currentAbsentees = new HashSet<>(allEnrolledRolls);

    JPanel p = createCardPanel();
    p.add(mkLabel("Active Session Tracker", F_HEADING, C_TEXT));
    p.add(gap(25));

    p.add(mkLabel("Course Name/ID:", F_SUBHEAD, C_TEXT));
    p.add(gap(8));
    JTextField courseField = new JTextField("e.g., AI-301");
    courseField.setFont(F_BODY);
    p.add(courseField);
    p.add(gap(25));

    // Stats Display Box
    JPanel statsPanel = new JPanel(new GridLayout(1, 2, 15, 0));
    statsPanel.setOpaque(false);
    JLabel presentLbl = mkLabel("Present: 0", new Font("SansSerif", Font.BOLD, 22), C_SUCCESS);
    JLabel absentLbl = mkLabel("Absent: " + currentAbsentees.size(), new Font("SansSerif", Font.BOLD, 22), C_DANGER);

    JPanel pCard = createMiniCard();
    pCard.add(presentLbl);
    JPanel aCard = createMiniCard();
    aCard.add(absentLbl);
    statsPanel.add(pCard);
    statsPanel.add(aCard);

    p.add(statsPanel);
    p.add(gap(20));

    p.add(mkLabel("Pending Absentees:", F_SUBHEAD, C_TEXT));
    p.add(gap(8));
    JTextArea absenteeArea = new JTextArea(4, 20);
    absenteeArea.setEditable(false);
    absenteeArea.setFont(new Font("Monospaced", Font.BOLD, 15));
    absenteeArea.setForeground(C_DANGER);
    absenteeArea.setBackground(new Color(254, 242, 242)); // Light red tint
    absenteeArea.setText(currentAbsentees.toString());
    absenteeArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JScrollPane scroll = new JScrollPane(absenteeArea);
    scroll.setBorder(new LineBorder(C_DANGER, 1, true));
    p.add(scroll);
    p.add(gap(30));

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

    actions.add(webcamBtn);
    actions.add(fileBtn);
    p.add(actions);
    p.add(gap(35));

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
}

