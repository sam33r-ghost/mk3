package com.Attendance.model;

import java.time.LocalDateTime;

public class Subject {
    String subjectID;
    String name;

    String getSubjectID() {
        return subjectID;
    }

    String getName() {
        return name;
    }

    Professor assignedProfessor;

    Professor getProfessor() {
        return assignedProfessor;
    }

}
