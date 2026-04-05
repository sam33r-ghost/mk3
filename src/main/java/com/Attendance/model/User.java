package com.Attendance.model;
import jdk.jshell.Snippet;

import java.time.LocalDateTime;
import java.util.Scanner;

public abstract class User {

    String userID;
    String name;
    String email;
    String passwordHash;
    enum Role {STUDENT, PROFESSOR, ADMIN}
    public abstract Role getRole();

    boolean login(String email, String password) {
        System.out.println("login");
        return true;
    }
    void logout() {
        System.out.println("logout");
    }
    Scanner sc = new Scanner(System.in);
}






