package me.nov.infernalupscale;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Utils {
  public static String toHex(int type) {
    return String.format("0x%08X", type);
  }

  public static String toBin(int val) {
    return String.format("%16s", Integer.toBinaryString(val)).replace(" ", "0");
  }

  public static BufferedImage toBufImg(Image image) {
    BufferedImage buffered = new BufferedImage(image.getWidth(null), image.getWidth(null), BufferedImage.TYPE_INT_ARGB);
    buffered.getGraphics().drawImage(image, 0, 0, null);
    return buffered;
  }
}
