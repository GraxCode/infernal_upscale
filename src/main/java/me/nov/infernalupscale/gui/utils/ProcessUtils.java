package me.nov.infernalupscale.gui.utils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class ProcessUtils {
  public static String runCommandForOutput(List<String> params) {
    ProcessBuilder pb = new ProcessBuilder(params);
    pb.redirectErrorStream(true);
    Process p;
    String result = "No output";
    try {
      p = pb.start();
      p.waitFor();
      BufferedReader reader =
              new BufferedReader(new InputStreamReader(p.getInputStream()));
      StringBuilder builder = new StringBuilder();
      String line = null;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
        builder.append(System.getProperty("line.separator"));
      }
      result = builder.toString();

      if (p.exitValue() != 0 && result.length() > 0) {
        JOptionPane.showMessageDialog(null, result, "Error occurred", JOptionPane.ERROR_MESSAGE);
        if (JOptionPane.showConfirmDialog(null, "Suspend the upscaling process now?", "Suspend", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
          Thread.currentThread().interrupt();
        }
      }
      p.destroy();


    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }
}
