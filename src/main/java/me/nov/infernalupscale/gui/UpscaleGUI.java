package me.nov.infernalupscale.gui;

import com.github.weisj.darklaf.components.help.HelpMenuItem;
import com.github.weisj.darklaf.settings.ThemeSettings;
import me.nov.infernalupscale.gui.components.FileSelectionField;
import me.nov.infernalupscale.gui.components.MaxSizePanel;
import me.nov.infernalupscale.gui.components.ProcessPane;
import me.nov.infernalupscale.gui.utils.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Supplier;

public class UpscaleGUI extends JFrame {
  private static final File PROP_FILE = new File("infernalupscale.xml");
  public static final Properties PROPS = new Properties();
  private static final float RATIO = 21 / 9f;

  private ProcessPane processPane;

  static {
    if (PROP_FILE.exists()) {
      try {
        PROPS.loadFromXML(new FileInputStream(PROP_FILE));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void dispose() {
    saveSettings();
    super.dispose();
    System.exit(0);
  }

  public static void saveSettings() {
    try {
      PROPS.storeToXML(new FileOutputStream(PROP_FILE), "Infernal upscale settings");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public UpscaleGUI() {
    this.setTitle("Infernal Upscaler 1.1");
    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.setIconImage(SwingUtils.iconToFrameImage(SwingUtils.getIcon("/colors.svg"), this));
    this.initializeFrame();
    this.initializeMenu();
    this.initBounds();
  }

  private void initializeMenu() {
    JMenuBar bar = new JMenuBar();
    JMenu help = new JMenu("Help");
    JMenuItem laf = new JMenuItem("Look and feel settings");
    laf.setIcon(ThemeSettings.getIcon());
    laf.addActionListener(l -> ThemeSettings.showSettingsDialog(this));
    JMenuItem about = new HelpMenuItem("About");
    about.addActionListener(l -> JOptionPane.showMessageDialog(this,
            "<html>This tool does not contain any copyright-infringing material, and does also not guarantee any warranty.<br>It should only be used for demonstration purposes.",
            "About", JOptionPane.INFORMATION_MESSAGE));
    JMenuItem sysInfo = new JMenuItem("System information");
    sysInfo.addActionListener(l -> JOptionPane.showMessageDialog(this,
            String.format("<html>Java version:\t<b>%s</b>" +
                            "<br>VM name:\t<b>%s</b><br>VM vendor:\t<b>%s</b><br>Java path:\t<b>%s</b>", System.getProperty("java.version"),
                    System.getProperty("java.vm.name"), System.getProperty("java.vm.vendor"), System.getProperty("java.home")),
            "About", JOptionPane.INFORMATION_MESSAGE));
    help.add(about);
    help.add(sysInfo);
    help.add(laf);
    bar.add(help);
    this.setJMenuBar(bar);
  }

  private void initializeFrame() {
    JPanel cp = new JPanel(new BorderLayout());
    JTabbedPane jtp = new JTabbedPane();

    JPanel selection = new JPanel(new GridBagLayout());
    selection.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    GridBagConstraints gbc = SwingUtils.createGridBagConstraints(1, 0);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;

    Supplier<File> python = addField(selection, gbc, "Python 3+ executable", "", ", leave empty for default");
    Supplier<File> esrgan = addField(selection, gbc, "JoeyBallentine's ESRGAN upscale.py", "py", "");
    Supplier<File> model = addField(selection, gbc, "ESRGAN model", "pth", " (recommended: \"Fatality x4\")");
    Supplier<File> mats = addField(selection, gbc, "Texture folder with MATs", null, " with all MAT files");

    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.gridwidth = 1;

    gbc.gridx = 1;
    JCheckBox cpu = new JCheckBox("CUDA (Nvidia GPU)");
    cpu.addChangeListener(l -> PROPS.setProperty("cuda", String.valueOf(cpu.isSelected())));
    cpu.setSelected(Boolean.parseBoolean(PROPS.getProperty("cuda", "false")));
    selection.add(cpu, gbc);

    gbc.gridx = 2;
    JCheckBox alphaMode = new JCheckBox("Skip transparent MATs");
    alphaMode.addChangeListener(l -> PROPS.setProperty("alpha", String.valueOf(alphaMode.isSelected())));
    alphaMode.setSelected(Boolean.parseBoolean(PROPS.getProperty("alpha", "false")));
    selection.add(alphaMode, gbc);

    gbc.gridx = 3;
    MaxSizePanel maxSize = new MaxSizePanel();
    selection.add(maxSize, gbc);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridy++;
    gbc.gridx = 3;

    JButton start = new JButton("Start", SwingUtils.getIcon("/compile.svg", true));
    start.addActionListener(a -> {
      jtp.setSelectedIndex(1);
      new Thread(() -> {
        try {
          processPane.start(python.get(), esrgan.get(), model.get(), mats.get(), cpu.isSelected(), alphaMode.isSelected(), maxSize.calc());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();
    });
    selection.add(start, gbc);

    jtp.addTab("Config", SwingUtils.getIcon("/config.svg", true), selection);
    jtp.addTab("Progress", SwingUtils.getIcon("/compile.svg", true), SwingUtils.pad(processPane = new ProcessPane(), 32, 32, 32, 32));
    cp.add(jtp, BorderLayout.CENTER);
    setContentPane(cp);
  }

  private Supplier<File> addField(JPanel selection, GridBagConstraints gbc, String desc, String fileType, String hint) {
    gbc.gridx = 0;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    selection.add(new JLabel(desc), gbc);
    gbc.gridx = 1;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.CENTER;
    FileSelectionField component = new FileSelectionField(desc, fileType, hint);
    selection.add(component, gbc);
    gbc.gridy++;
    return component.makeSupplier();
  }

  private void initBounds() {
    Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    int w = screenSize.width;
    int h = screenSize.height;
    pack();
    int width = getWidth();
    int height = (int) (width / RATIO);
    setBounds(w / 2 - width / 2, h / 2 - height / 2, width, height);
  }


  public static void main(String[] args) {
    DarkLAF.setLookAndFeel();
    new UpscaleGUI().setVisible(true);

  }
}
