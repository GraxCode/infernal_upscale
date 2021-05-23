package me.nov.infernalupscale.gui.components;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.Theme;

import javax.swing.*;
import java.awt.*;

public class ImagePreview extends JLabel {
  private final String desc;
  private int iconWidth;
  private int iconHeight;
  private ImageIcon imageIcon;
  private String file = "";

  public ImagePreview(String desc) {
    super();
    this.desc = desc;
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = ((Graphics2D) g);
    int size = Math.min(getHeight(), getWidth());


    g.setColor(Theme.isDark(LafManager.getTheme()) ? Color.WHITE : Color.BLACK);
    g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f, new float[]{10.0f}, 0.0f));
    int x = getWidth() / 2 - size / 2;
    int y = getHeight() / 2 - size / 2;
    g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
    if (imageIcon != null)
      g.drawImage(imageIcon.getImage(), x, y, size, size, this);
    g.drawString(iconWidth + "x" + iconHeight + " / " + desc, x + 10, y + 20);
    g.drawString(file, x + 10, y + size - 10);
    g.drawRect(x, y, size - 1, size - 1);
  }

  public ImageIcon getImageIcon() {
    return imageIcon;
  }

  public void setImageIcon(String file, ImageIcon imageIcon) {
    this.file = file;
    this.imageIcon = imageIcon;
    iconWidth = imageIcon.getIconWidth();
    iconHeight = imageIcon.getIconHeight();
  }
}