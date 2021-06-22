package me.nov.infernalupscale.gui.components;

import com.github.weisj.darklaf.ui.text.DarkTextFieldUI;
import me.nov.infernalupscale.gui.UpscaleGUI;
import me.nov.infernalupscale.gui.utils.SwingUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.function.Supplier;

public class FileSelectionField extends JTextField {

  protected final String desc;
  protected final String fileType;
  public File file;
  private String hint;

  public FileSelectionField(String desc, String fileType, String hint) {
    this.desc = desc;
    this.fileType = fileType;
    this.hint = hint;
    setDefaultText();
    this.setLayout(new BorderLayout());
    this.requestFocus();
    JLabel label = new JLabel(SwingUtils.getIcon("/menu-open.svg", true));
    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    this.add(label, BorderLayout.EAST);
    String oldPath = UpscaleGUI.PROPS.getProperty(getPropName());
    if (oldPath != null) {
      File input = new File(oldPath);
      if (input.exists()) {
        setFile(input);
        setText(input.getName());
      }
    }
    label.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
        jfc.setMultiSelectionEnabled(false);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.setDialogTitle("Select " + desc);

        if (fileType == null)
          jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        else if (!fileType.isEmpty())
          jfc.setFileFilter(new FileNameExtensionFilter(fileType.toUpperCase() + " file", fileType));

        int result = jfc.showOpenDialog(FileSelectionField.this);
        if (result == JFileChooser.APPROVE_OPTION) {
          File input = jfc.getSelectedFile();
          setFile(input);
          if (file != null)
            setText(file.getName());
          else
            setDefaultText();
        }
      }
    });
    this.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent focusEvent) {
        if (file != null)
          setText(file.getAbsolutePath());
        else
          setText("");
      }

      @Override
      public void focusLost(FocusEvent focusEvent) {
        if (getText().isEmpty())
          file = null;
        handleType();
        if (file != null)
          setText(file.getName());
        else
          setDefaultText();
      }
    });
    this.setColumns(1);
  }

  private void handleType() {
    setFile(new File(getText()));
  }

  private void setDefaultText() {
    this.setText("");
    putClientProperty(DarkTextFieldUI.KEY_DEFAULT_TEXT, "Select a " + (fileType == null ? "folder" : "file") + hint);
  }

  private boolean setFile(File input) {
    if (input.exists()) {
      file = input;
      UpscaleGUI.PROPS.setProperty(getPropName(), file.getAbsolutePath());
      return true;
    }
    return false;
  }

  private String getPropName() {
    return desc.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
  }


  public Supplier<File> makeSupplier() {
    return () -> file;
  }

}