package me.nov.infernalupscale.gui.components;

import me.nov.infernalupscale.gui.UpscaleGUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class MaxSizePanel extends JPanel implements DocumentListener {

  private final JNumberTextField x;
  private final JNumberTextField y;

  public MaxSizePanel() {
    setLayout(new FlowLayout());

    this.add(new JLabel("<html>Max size <small>(px)</small>: "));

    x = new JNumberTextField(4);
    y = new JNumberTextField(4);
    x.setInt(Integer.parseInt(UpscaleGUI.PROPS.getProperty("max_x", "256")));
    y.setInt(Integer.parseInt(UpscaleGUI.PROPS.getProperty("max_y", "256")));
    x.getDocument().addDocumentListener(this);
    y.getDocument().addDocumentListener(this);

    this.add(x);
    this.add(new JLabel("<html><b> x "));
    this.add(y);
  }

  public int calc() {
    return x.getInt() * y.getInt();
  }

  @Override
  public void insertUpdate(DocumentEvent documentEvent) {
    updateProps();
  }

  @Override
  public void removeUpdate(DocumentEvent documentEvent) {
    updateProps();
  }

  @Override
  public void changedUpdate(DocumentEvent documentEvent) {
    updateProps();
  }

  private void updateProps() {
    UpscaleGUI.PROPS.setProperty("max_x", x.getText());
    UpscaleGUI.PROPS.setProperty("max_y", y.getText());
  }
}
