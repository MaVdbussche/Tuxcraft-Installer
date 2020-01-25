package com.barassolutions;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

class Gui {

  private static final AtomicBoolean valuesInitiated;

  private static final JFrame initFrame;
  private static final JFrame loadingFrame;
  private static final JTextField mmcField;
  private static final JTextField zipField;
  private static final JButton nextButton;
  private static final JProgressBar loadingBar;
  private static final JLabel currentActionLabel;

  /**
   * Initiates and displays the installer's GUI.
   */
  static void initValues() {
    initFrame.pack();
    initFrame.setVisible(true);

    while (!valuesInitiated.get()) {
      Thread.yield(); // wait until values initialized before returning
    }
  }

  static void onExtract() {
    loadingBar.setValue(1);
    currentActionLabel.setText("Extracting instance...");
  }

  static void onInstancesCheck() {
    loadingBar.setValue(2);
    currentActionLabel.setText("Checking for older TuxCraft instances...");
  }

  static void onCopyOld() {
    loadingBar.setValue(3);
    currentActionLabel.setText("Copying latest installed instance...");
  }

  static void onSkipCopyOld() {
    loadingBar.setValue(3);
    currentActionLabel.setText("Skipping old instance copy");
  }

  static void onUpdating() {
    loadingBar.setValue(4);
    currentActionLabel.setText("Updating...");
  }

  static void onCleanup() {
    loadingBar.setValue(5);
    currentActionLabel.setText("Cleaning up...");
  }

  static void onDone() {
    loadingBar.setValue(loadingBar.getMaximum());
    currentActionLabel.setText("Done.");
  }

  static void exit() {
    initFrame.dispose();
    loadingFrame.dispose();
  }

  private static JPanel encapsulate(LayoutManager layoutManager, JComponent... components) {
    final JPanel panel = (layoutManager == null)? new JPanel() : new JPanel(layoutManager);
    for(JComponent component : components) panel.add(component);
    return panel;
  }

  /**
   * Creates a frame named TuxCraft Installer with given default close action.
   *
   * @param closeAction The default close action of the frame, passed to JFrame.setDefaultCloseAction()
   * @return A new JFrame properly setup
   */
  private static JFrame makeFrame(int closeAction) {
    final JFrame frame = new JFrame("TuxCraft Installer");
    frame.setSize(new Dimension(700, 300));
    frame.setFont(new Font("SansSerif", Font.PLAIN, 14));
    frame.setDefaultCloseOperation(closeAction);
    frame.setLayout(new GridLayout(5, 1));
    return frame;
  }

  /**
   * Creates a JPanel containing an path entry UI.
   *
   * @param title The label of the path entry
   * @param field The text field to use for the path entry
   * @param mode The file selection mode for file browsing, passed to a JFileChooser
   * @param filter The file filter for file browsing, passed to a JFileChooser
   * @return A new JPanel properly setup
   */
  private static JPanel makePathEntry(String title, JTextField field, int mode, FileNameExtensionFilter filter) {
    field.setEditable(true);
    final JButton browseButton = new JButton(new BrowseButtonAction(field, filter, mode));
    browseButton.setText("Browse");
    return encapsulate( new FlowLayout(FlowLayout.LEFT), new JLabel(title), field, browseButton);
  }

  /**
   * An action listener (for clicks) for browse buttons.
   */
  private static final class BrowseButtonAction extends AbstractAction {
    private final JTextField field;
    private final JFileChooser chooser;

    BrowseButtonAction(JTextField field, FileNameExtensionFilter filter, int mode) {
      this.field = field;
      chooser = new JFileChooser();
      chooser.setFileHidingEnabled(false);
      chooser.setFileSelectionMode(mode);
      if (filter != null) chooser.setFileFilter(filter);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      int ret = chooser.showOpenDialog(null);
      if (ret == JFileChooser.APPROVE_OPTION) {
        field.setText(chooser.getSelectedFile().getAbsolutePath());
      } else if (ret == JFileChooser.ERROR_OPTION) {
        JOptionPane
            .showMessageDialog(null, "Error, contact the developers or install manually :/",
                "Not UwU", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private static final class NextButtonAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      final File mmcInstancesFolder = new File(mmcField.getText());
      final File zipFile = new File(zipField.getText());
      if (!mmcInstancesFolder.isDirectory()) {
        JOptionPane
            .showMessageDialog(null,
                "\"" + mmcField.getText() + "\" is not a directory",
                "Oh no !",
                JOptionPane.WARNING_MESSAGE);
      } else if (!zipFile.isFile()) {
        JOptionPane
            .showMessageDialog(null,
                "\"" + zipField.getText() + "\" does not exist", "Oh no !",
                JOptionPane.WARNING_MESSAGE);
      } else if (!Pattern
          .matches(".*[\\\\/]TuxCraft-[0-9]\\.[0-9]\\.[0-9][0-9].*\\.zip", zipField.getText())) {
        JOptionPane.showMessageDialog(null,
            "The file you specified either is not a TuxCraft instance, or it is badly named.\n"
                + "If this is the case, please rename in as `TuxCraft-x.x.xx.zip, replacing `x` "
                + "appropriately by the version numbers.", "Oh no !",
            JOptionPane.WARNING_MESSAGE);
      } else if (mmcInstancesFolder.getAbsolutePath()
          .equals(zipFile.getParentFile().getAbsolutePath())) {
        JOptionPane.showMessageDialog(null,
            "It is not allowed to place the zip file in the MultiMC instances folder, as this will"
                + " cause conflicts during the extraction.\n Please place it somewhere else, "
                + "then press the \"" + nextButton.getText() + "\" button again.", "Oh no !",
            JOptionPane.WARNING_MESSAGE);
      } else {
        Main.Values.rootInstancesFolder = mmcInstancesFolder.getAbsoluteFile();
        Main.Values.updateArchive = zipFile.getAbsoluteFile();
        Main.Values.unzippedArchive = new File(Main.Values.updateArchive.getParent(),
            zipFile.getName().replaceAll("\\.zip$", "")).getAbsoluteFile();

        // Clear up gui and switch to progress bar
        loadingFrame.pack();
        loadingFrame.setVisible(true);
        initFrame.dispose();

        // Unlock main thread
        valuesInitiated.set(true);
      }

    }
  }

  /* Initiates all gui components, actually displays nothing */
  static {
    valuesInitiated = new AtomicBoolean(false);

    initFrame = makeFrame(JFrame.EXIT_ON_CLOSE);
    loadingFrame = makeFrame(JFrame.DO_NOTHING_ON_CLOSE);
    mmcField = new JTextField("C:\\Program Files\\MultiMC\\instances", 30);
    zipField = new JTextField("C:\\User\\" + System.getProperty("user.name") + "\\Downloads\\TuxCraft-X.X.XX.zip", 30);
    nextButton = new JButton(new NextButtonAction());
    loadingBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 6);
    currentActionLabel = new JLabel("Tuxcraft installation should begin soon... Please be patient..");

    nextButton.setText("Next");
    currentActionLabel.setVerticalAlignment(JLabel.CENTER);

    initFrame.add(encapsulate(null));
    initFrame.add(makePathEntry("MultiMC instances folder:", mmcField, JFileChooser.DIRECTORIES_ONLY, null));
    initFrame.add(makePathEntry("TuxCraft instance archive:", zipField, JFileChooser.FILES_AND_DIRECTORIES, new FileNameExtensionFilter("ZIP archive", "zip")));
    initFrame.add(encapsulate(null));
    initFrame.add(encapsulate(new FlowLayout(FlowLayout.RIGHT), nextButton));

    loadingFrame.add(encapsulate(null));
    loadingFrame.add(currentActionLabel);
    loadingFrame.add(encapsulate(null, loadingBar));
  }
}
