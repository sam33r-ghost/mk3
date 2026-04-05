package com.Attendance.model;

import java.time.LocalDateTime;

public class AttendanceRecord{
    String RecordID;
    Student student;
    Subject subject;
    LocalDateTime timestamp;
    Attendance status;

    public String getRecordID() {
        return RecordID;
    }

    public Student getStudent() {
        return student;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Attendance getStatus() {
        return status;
    }

    public Subject getSubject() {
        return subject;
    }
    void setStatus(Attendance status){
        status=this.status;
    }
}