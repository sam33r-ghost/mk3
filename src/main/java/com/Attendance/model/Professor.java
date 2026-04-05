package com.Attendance.model;

public class Professor extends User{
    String Department;

    public String getDepartment() {
        return Department;
    }

    public Role getRole(){
        return Role.PROFESSOR;
    }
}