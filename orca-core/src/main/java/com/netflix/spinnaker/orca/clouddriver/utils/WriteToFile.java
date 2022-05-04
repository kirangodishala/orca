package com.netflix.spinnaker.orca.clouddriver.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

public class WriteToFile {

  public static void createTempFile(byte[] contents) {
    try {
      File tempFile = new File("/tmp/spinflow1.log");
      if (!tempFile.exists()) {
        tempFile.createNewFile();
      }
      FileOutputStream fileOutputStream = new FileOutputStream(tempFile, true);
      BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

      bufferedOutputStream.write(("\n" + LocalDateTime.now() + "  ORCA ").getBytes());
      bufferedOutputStream.write(contents);
      bufferedOutputStream.write(("\n").getBytes());
      bufferedOutputStream.close();
      fileOutputStream.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void createTempFile(String contents) {
    createTempFile(contents.getBytes());
  }
}
