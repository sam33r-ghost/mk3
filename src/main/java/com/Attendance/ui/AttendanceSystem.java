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

import com.Attendance.database.DB;
import com.Attendance.vision.*;
import com.Attendance.service.Pipeline;
import com.Attendance.model.Student;
import com.Attendance.database.loadData;

import static com.Attendance.database.register.registerProfessor;
import static com.Attendance.ui.showUI.*;


public class  AttendanceSystem {

    // ── Database Configuration ────────────────────────────────────────────────
    static final String DB_URL  = "jdbc:mysql://localhost:3306/vision";
    public static final String DB_USER = "root";
    public static final String DB_PASS = "root";

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
    public static String currentProfessorName = "";
    public static int currentProfessorId = -1;

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