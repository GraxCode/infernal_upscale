package me.nov.infernalupscale;

import me.nov.infernalupscale.mat.MAT;
import me.nov.infernalupscale.mat.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Manual upscaling / mat texture replacement
 */
public class Conversion {

  public static void main(String[] args) throws IOException {
    File file = new File("mat");
    if (!file.isDirectory() || !file.exists())
      throw new IllegalStateException("/mat directory not found");

    File extracted = new File("extracted");
    clearDir(extracted);
    File matRewritten = new File("mat_rewr");
    clearDir(matRewritten);
    doMatDirRecursively(file);
    File newTex = new File("new_tex");
    clearDir(newTex);
  }

  private static void clearDir(File file) {
    if (file.exists()) {
      for (File f : file.listFiles())
        f.delete();
      file.delete();
    }
    file.mkdir();
  }

  private static void doMatDirRecursively(File dir) throws IOException {
    for (File file : dir.listFiles()) {
      if (file.isDirectory())
        doMatDirRecursively(file);
      else if (file.getName().endsWith(".mat"))
        doMatFile(file);
    }
  }

  private static void doMatFile(File file) throws IOException {
    byte[] bytes = Files.readAllBytes(file.toPath());
    try {
      String name = file.getName();
      System.out.println("Handling " + name);

      MAT mat = new MAT(name, bytes);

      for (int i = 0; i < mat.textures.size(); i++) {
        Texture tx = mat.textures.get(i);
        BufferedImage renderedImage = tx.getImg();
        boolean smallSize = tx.sizeX <= 256 && tx.sizeY <= 256;
        if (mat.alphaBits == 0 && smallSize && renderedImage != null)
          ImageIO.write(renderedImage, "png", new File("extracted/" + name + "_t" + i + ".png"));
      }

      for (int i = 0; i < mat.textures.size(); i++) {
        Texture texture = mat.textures.get(i);
        File override = new File("new_tex/" + name + "_t" + i + "-upscaled.png");
        if (override.exists()) {
          System.out.println("Found a new texture (" + i + ") for " + name);

          texture.setImg(ImageIO.read(override));

          Files.write(new File("mat_rewr/" + name).toPath(), mat.toByteArray());
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
