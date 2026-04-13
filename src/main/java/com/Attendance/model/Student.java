package com.Attendance.model;

public class Student {
    private int rollNo; // Use String if your roll numbers contain letters
    private String name;
    private byte[] fileData; // This holds the byte array (e.g., an image or document)

    public Student(int rollNo, String name, byte[] fileData) {
        this.rollNo = rollNo;
        this.name = name;
        this.fileData = fileData;
    }

    // Getters
    public int getRollNo() { return rollNo; }
    public String getName() { return name; }
    public byte[] getFileData() { return fileData; }
}