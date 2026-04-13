package com.Attendance.model;

public class SessionRecord {

    int id; public String course;
    public String date;
    public String time; public int present;
    public int absent; public String absenteesJson;
    public SessionRecord(int id, String course, String date, String time, int present, int absent, String absenteesJson) {
        this.id = id; this.course = course; this.date = date; this.time = time;
        this.present = present; this.absent = absent; this.absenteesJson = absenteesJson;
    }
}