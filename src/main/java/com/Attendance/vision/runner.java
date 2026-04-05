package com.Attendance.vision;

import ai.djl.ModelException;
import ai.djl.translate.TranslateException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Runner {
    public static void main(String[] args)  throws IOException, ModelException, TranslateException, Exception {
        String p= "src/main/java/com/Attendance/vision/photos/AI.jpg";
        String q= "extracted_faces/face_20.png";
        String l= "extracted_faces/face_53.png";
        System.out.println(FaceEncoder.encodeFace(q));
        System.out.println(FaceEncoder.isSamePerson(q,l,0.4));
        //ImageSplice.splice(p);
    }
}

