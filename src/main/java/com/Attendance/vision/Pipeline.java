package com.Attendance.vision;

import ai.djl.ModelException;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.translate.TranslateException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;


import java.io.*;
import java.sql.*;

import static com.Attendance.vision.FaceEncoder.encodeFace;
import static com.Attendance.vision.FaceEncoder.isSamePerson;
import static com.Attendance.vision.ImageSplice.splice;
import static com.Attendance.vision.loadData.loadStudents;

public class Pipeline {
    public static void pipe(String pathString) throws Exception {
        Path path = Paths.get(pathString);
        DetectedObjects detection = RetinaFaceDetection.predict(path);
        splice(pathString,detection);
        List<Student> studentlist = loadData.loadStudents();
        System.out.println("students extracted");

        int faceCounter =0;
; // Required for comparing byte arrays

        int l=0;
                    while(l< detection.getNumberOfObjects())
                    {
                        String path2= "extracted_faces/face_" + l + ".png";
                    boolean matchFound = false;

                        if (studentlist.isEmpty()) {
                            System.out.println("No students found in the database.");
                            return;
                        }

                        // 4. Iterate through the ArrayList using a for-each loop
                        for (Student currentStudent : studentlist) {

                            // --- ACCESSING INDIVIDUAL COMPONENTS ---
                            // Use the getters from the Student class to access the data
                            int roll = currentStudent.getRollNo();
                            String name = currentStudent.getName();
                            byte[] data = currentStudent.getFileData();

                            // Print the text data


                            // Handle the byte array
                            if (data != null && data.length > 0) {
                            } else {
                                System.out.println("No file data attached for this student.");
                            }

                        // Compare the database bytes with your target bytes
                        if (data != null && FaceEncoder.isSamePerson(data, path2, 0.39)) {

                            System.out.println("Roll Number: " + roll);
                            System.out.println("Student Name: " + name);
                            System.out.println(l);

                            matchFound = true;

                            // Stop searching once we find our match to save processing time
                            break;
                        }
                    }
                        l++;
                    }


            String folderPath= "extracted_faces";
        File folder = new File(folderPath);

        // Check if the folder exists and is actually a directory
        if (folder.exists() && folder.isDirectory()) {

            // Get all image files in the directory
            File[] photoFiles = folder.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".jpg") ||
                            name.toLowerCase().endsWith(".png") ||
                            name.toLowerCase().endsWith(".jpeg")
            );

            if (photoFiles != null) {
                // Loop through the array and delete each file
                for (File file : photoFiles) {
                    if (file.delete()) {

                    } else {
                        System.err.println("Failed to delete: " + file.getName());
                    }
                }
            }
        } else {
            System.err.println("Folder does not exist or is not a directory.");
        }
        }

    }
