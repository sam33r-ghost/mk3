package com.Attendance.vision;

import ai.djl.ModelException;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.translate.TranslateException;

import javax.swing.*;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.Attendance.vision.Pipeline;

import static com.Attendance.vision.ImageSplice.splice;

public class Runner {
   public static void main(String[] args)  throws IOException, ModelException, TranslateException, Exception {
     //  SwingUtilities.invokeLater(() -> {
      //       new WebcamApp().setVisible(true);
      // });
          //  String q = "extracted_faces/face_50.png";
           // String l = "extracted_faces/face_76.png";
            //System.out.println(FaceEncoder.isSamePerson(q, l, 0.4));

                            String path = "src/main/java/com/Attendance/vision/photos/AI.JPG";
                    Pipeline.pipe(path);
          //     SwingUtilities.invokeLater(() -> {
         //        new register().setVisible(true);
          //    });

        }
    }


