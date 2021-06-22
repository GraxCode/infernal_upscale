package me.nov.infernalupscale.gui.components;

import me.nov.infernalupscale.gui.UpscaleGUI;
import me.nov.infernalupscale.gui.utils.ProcessUtils;
import me.nov.infernalupscale.mat.MAT;
import me.nov.infernalupscale.mat.Texture;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessPane extends JPanel {

  /*
   find ./01_bab/mat -maxdepth 1 -type f -exec ./cndtool add material {} 01_bab_mats/01_bab.cnd --replace \;
   */

  private final ImagePreview oldImg;
  private final ImagePreview newImg;
  private final JProgressBar progress;

  public ProcessPane() {
    this.setLayout(new BorderLayout(8, 8));
    oldImg = new ImagePreview("original");
    oldImg.setHorizontalAlignment(JLabel.CENTER);
    newImg = new ImagePreview("upscaled");
    newImg.setHorizontalAlignment(JLabel.CENTER);


    JPanel images = new JPanel(new GridLayout(1, 2, 8, 8));
    images.add(oldImg);
    images.add(newImg);

    this.add(images, BorderLayout.CENTER);

    progress = new JProgressBar();
    progress.setStringPainted(true);
    progress.setString("Press \"Start\" to start");
    this.add(progress, BorderLayout.PAGE_END);
  }


  public void start(File python, File esrgan, File model, File matsFolder, boolean cuda, boolean skipAlpha, int maxPixels) throws IOException {
    UpscaleGUI.saveSettings();
    File inputs = Files.createTempDirectory("inputs").toFile();
    File outputs = Files.createTempDirectory("outputs").toFile();
    System.out.println(outputs.getAbsolutePath());
    List<File> mats = Arrays.stream(matsFolder.listFiles()).filter(file -> file.getName().endsWith(".mat")).collect(Collectors.toList());
    int length = mats.size();
    if (length == 0) {
      JOptionPane.showMessageDialog(null, "No mat files found inside the folder. Please check the path.", "Error occurred", JOptionPane.ERROR_MESSAGE);
    }
    for (int i = 0; i < mats.size(); i++) {
      System.gc(); // trying to be smarter than the JVM
      File matFile = mats.get(i);
      System.out.println("Reading MAT " + matFile.getAbsolutePath() + "...");
      MAT mat;
      try {
        mat = getMat(matFile);
      } catch (Exception e) {
        e.printStackTrace();
        continue;
      }
      System.out.println("Finished Reading, starting texture export.");
      if (skipAlpha && mat.alphaBits > 0) {
        System.out.println("Skipping MAT, has alpha.");
        continue;
      }
      for (File f : inputs.listFiles())
        f.delete(); // prep dir


      int texCount = mat.textures.size();
      boolean hasAtLeastOne = false;
      for (int j = 0; j < texCount; j++) {
        Texture tx = mat.textures.get(j);
        String texName = "TX " + j;
        SwingUtilities.invokeLater(() -> progress.setString(matFile.getName() + " (" + texName + ", " + tx.sizeX + "x" + tx.sizeY + ")"));
        BufferedImage renderedImage = tx.getImg();
        boolean smallSize = tx.sizeX * tx.sizeY <= maxPixels;

        if (smallSize && renderedImage != null) {
          hasAtLeastOne = true;
          ImageIO.write(renderedImage, "png", new File(inputs, mat.name + "_t" + j + ".png"));
        } else {
          System.out.println("Skipping TX " + j + " (" + tx.sizeX + "x" + tx.sizeY + ")");
        }
      }
      if (!hasAtLeastOne) {
        System.out.println("All TX skipped, skipping python.");
        int prog = (int) (i / (float) length * 100);
        SwingUtilities.invokeLater(() -> progress.setValue(prog));
        continue;
      }
      System.out.println("Exported textures, running python.");

      String pythonPath = python == null ? "python" : python.getAbsolutePath();
      List<String> args = new ArrayList<>(Arrays.asList(pythonPath, esrgan.getAbsolutePath(), "--alpha-mode", "alpha_separately",
              "-i", inputs.getAbsolutePath(), "-o", outputs.getAbsolutePath(), model.getAbsolutePath()));
      if (!cuda) {
        args.add(2, "--cpu");
      }

      System.out.println(ProcessUtils.runCommandForOutput(args));

      for (int j = 0; j < texCount; j++) {
        Texture texture = mat.textures.get(j);
        File override = new File(outputs, mat.name + "_t" + j + ".png");
        if (override.exists()) {

          System.out.println("Replaced TX " + j + " of " + mat.name);
          BufferedImage img = ImageIO.read(override);
          oldImg.setImageIcon(mat.name + " (TX " + j + ")", new ImageIcon(texture.getImg()));
          newImg.setImageIcon(mat.name + " (TX " + j + ")", new ImageIcon(img));
          texture.setImg(img);
          SwingUtilities.invokeLater(this::repaint);
        } else {
          System.out.println("TX " + j + " of " + mat.name + " not upscaled or left out");
        }

        float texProgPct = j / (float) texCount;
        int prog = (int) ((i + texProgPct) / (float) length * 100);
        SwingUtilities.invokeLater(() -> progress.setValue(prog));
      }
      try {
        Files.write(new File(matsFolder, mat.name).toPath(), mat.toByteArray());
      } catch (Exception e) {
        System.out.println("Error writing file " + e.toString());
      }
    }
    for (File f : outputs.listFiles())
      f.delete(); // delete every output

    SwingUtilities.invokeLater(() -> {
      progress.setString("Finished");
      progress.setValue(100);
    });
  }

  private MAT getMat(File file) {
    try {
      return new MAT(file.getName(), Files.readAllBytes(file.toPath()));
    } catch (Exception e) {
      throw new RuntimeException("File reading failed", e);
    }
  }
}
