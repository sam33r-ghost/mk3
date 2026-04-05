package com.Attendance.model;

public class Student extends User{
    byte[] faceEncoding;
    byte [] getFaceEncoding()
    {
        return faceEncoding;
    };

    public void setFaceEncoding(byte[] faceEncoding) {
        this.faceEncoding = faceEncoding;
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }
}