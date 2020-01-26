package com.barassolutions;

import com.barassolutions.Main.Values;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

class Gui {

  private static final AtomicBoolean valuesInitiated;

  private static final JFrame initFrame;
  private static final JFrame loadingFrame;
  private static final JFrame logFrame;
  private static final JTextArea logArea;
  private static final JTextField mmcField;
  private static final JTextField zipField;
  private static final JButton nextButton;
  private static final JProgressBar loadingBar;
  private static final JLabel currentActionLabel;

  static void logDebug(String msg) {
    msg = "[*] " + msg.replace("\n", "\n\t");
    System.out.println(msg);
    logArea.append(msg + "\n");
  }

  static void logInfo(String msg) {
    msg = "[=] " + msg.replace("\n", "\n\t");
    System.out.println(msg);
    logArea.append(msg + "\n");
  }

  static void logWarning(String msg) {
    msg = "[!] " + msg.replace("\n", "\n\t");
    System.out.println(msg);
    logArea.append(msg + "\n");
  }

  private static void logError(String msg) {
    msg = "[#] " + msg.replace("\n", "\n\t");
    System.out.println(msg);
    logArea.append(msg + "\n");
  }

  /**
   * Logs an error message, then displays it in a popup and returns when it is closed.
   *
   * @param msg The error message
   */
  private static void popError(String msg) {
    logError(msg);
    JOptionPane.showMessageDialog(null, msg, "Oh no !", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Logs the exception, then displays it in a popup and exit program when it is closed.
   * Cleanup code in Main.main() is executed.
   * It is displayed in the same style as a Java stacktrace.
   *
   * @param e The exception
   */
  static void popError(Exception e) {
    StringBuilder msg = new StringBuilder("An unexpected error occurred !\n")
        .append("Please send this report to developers if it happens reliably:\n")
        .append(e.getMessage())
        .append("\n\n-----\nStacktrace (least recent up) :");

    StackTraceElement[] trace = e.getStackTrace();

    for (int i = Math.min(trace.length - 1, 14); i >= 0; i--) {
      msg.append(trace[i].toString()).append("\n");
    }

    if (trace.length > 15) {
      msg.append("\nand ").append(trace.length - 15).append(" more...");
    }

    popError(msg.toString());
    logError("Critical error, aborting !");
    dumpLogs();
    throw new RuntimeException(e); // Abort, executing cleanup code of Main
  }

  /**
   * Logs a warning message and displays it in a popup and returns when it is closed.
   *
   * @param msg The warning message
   */
  private static void popWarning(String msg) {
    logWarning(msg);
    JOptionPane.showMessageDialog(null, msg, "Oh no !", JOptionPane.WARNING_MESSAGE);
  }

  /**
   * Dumps all the content of logArea (i.e. all logs) into a log file.
   * Log file names include the local date time
   * Logs dumps are visible in dumped logs.
   */
  private static void dumpLogs() {
    File logFile = new File(Values.rootInstancesFolder,
        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            + "_TuxCraft-Installer.log");
    logInfo(String.format("Dumping %d lines of log in %s ...",
        logArea.getLineCount(), logFile.getAbsolutePath()));
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
      writer.write(logArea.getText());
    } catch (IOException e) {
      popError("Unable to write log file !");
    }
  }

  /**
   * Initiates and displays the installer's GUI.
   */
  static void initValues() {
    initFrame.pack();
    initFrame.setVisible(true);
    logDebug("GUI: init frame displayed");
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
    logDebug("GUI: exit called");
    initFrame.dispose();
    loadingFrame.dispose();
    logFrame.dispose();
  }

  private static JPanel encapsulate(LayoutManager layoutManager, JComponent... components) {
    final JPanel panel = (layoutManager == null) ? new JPanel() : new JPanel(layoutManager);
    for (JComponent component : components) {
      panel.add(component);
    }
    return panel;
  }

  /**
   * Creates a frame named TuxCraft Installer with given default close action and no fixed size.
   *
   * @param closeAction The default close action of the frame,
   *                    passed to JFrame.setDefaultCloseAction()
   * @return A new JFrame properly setup
   */
  private static JFrame makeSimpleFrame(int closeAction) {
    final JFrame frame = new JFrame("TuxCraft Installer");
    frame.setFont(new Font("SansSerif", Font.PLAIN, 14));
    frame.setDefaultCloseOperation(closeAction);
    frame.setResizable(true);
    frame.setLayout(new GridLayout(1, 1));
    return frame;
  }

  /**
   * Creates a frame named TuxCraft Installer with given default close action and dimensions.
   *
   * @param closeAction The default close action of the frame,
   *                    passed to JFrame.setDefaultCloseAction()
   * @return A new JFrame properly setup
   */
  private static JFrame makeFrame(int closeAction) {
    final JFrame frame = makeSimpleFrame(closeAction);
    frame.setSize(new Dimension(700, 300));
    frame.setLayout(new GridLayout(5, 1));
    return frame;
  }

  /**
   * Creates a JPanel containing an path entry UI.
   *
   * @param title  The label of the path entry
   * @param field  The text field to use for the path entry
   * @param mode   The file selection mode for file browsing, passed to a JFileChooser
   * @param filter The file filter for file browsing, passed to a JFileChooser
   * @return A new JPanel properly setup
   */
  private static JPanel makePathEntry(String title, JTextField field, int mode,
      FileNameExtensionFilter filter) {
    field.setEditable(true);
    final JButton browseButton = new JButton(new BrowseButtonAction(field, filter, mode));
    browseButton.setText("Browse");
    return encapsulate(new FlowLayout(FlowLayout.LEFT), new JLabel(title), field, browseButton);
  }

  private static Box makeLogBox() {
    Box box = new Box(BoxLayout.Y_AXIS);
    box.add(new JScrollPane(logArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    return box;
  }

  /**
   * Creates a menu bar with a "Logs" button to display logs.
   *
   * @return a new JMenuBar properly setup
   */
  private static JMenuBar makeMenu() {
    JMenu menu = new JMenu();
    menu.setText("Logs");
    JMenuItem button = new JMenuItem(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        logFrame.pack();
        logFrame.setVisible(true);
        logDebug("GUI: log button callback called, log frame displayed");
      }
    });
    button.setText("Display logs");
    menu.add(button);
    button = new JMenuItem(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        logDebug("GUI: dump log button callback called");
        dumpLogs();
      }
    });
    button.setText("Dump logs");
    menu.add(button);
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(menu);
    return menuBar;
  }

  /**
   * An action listener (for clicks) for browse buttons.
   */
  private static final class BrowseButtonAction extends AbstractAction {

    private final JTextField field;
    private final JFileChooser chooser;

    BrowseButtonAction(JTextField field, FileNameExtensionFilter filter, int mode) {
      this.field = field;
      chooser = new JFileChooser(field.getText());
      chooser.setFileHidingEnabled(false);
      chooser.setFileSelectionMode(mode);
      if (filter != null) {
        chooser.setFileFilter(filter);
      }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      logDebug("GUI: browse button callback called");
      int ret = chooser.showOpenDialog(null);
      if (ret == JFileChooser.APPROVE_OPTION) {
        field.setText(chooser.getSelectedFile().getAbsolutePath());
      } else if (ret == JFileChooser.ERROR_OPTION) {
        popError("Unexpected and unreported error, contact the developers or install manually :/");
      }
    }
  }

  private static final class NextButtonAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      logDebug("GUI: next button callback called");
      final File mmcInstancesFolder = new File(mmcField.getText());
      final File zipFile = new File(zipField.getText());
      if (!mmcInstancesFolder.isDirectory()) {
        popWarning("\"" + mmcField.getText() + "\" is not a directory");
      } else if (!zipFile.isFile()) {
        popWarning("\"" + zipField.getText() + "\" does not exist");
      } else if (!Pattern
          .matches(".*[\\\\/]TuxCraft-[0-9]\\.[0-9]\\.[0-9][0-9].*\\.zip", zipField.getText())) {
        popWarning(
            "The specified file either is not a TuxCraft instance, or it is badly named.\n"
            + "If this is the case, please rename in as `TuxCraft-x.x.xx.zip, "
            + "replacing `x` appropriately by the version numbers.");
      } else if (mmcInstancesFolder.getAbsolutePath()
          .equals(zipFile.getParentFile().getAbsolutePath())) {
        popWarning(
            "It is not allowed to place the zip file in the MultiMC instances folder, as this will"
                + " cause conflicts during the extraction.\n Please place it somewhere else, "
                + "then press the \"" + nextButton.getText() + "\" button again.");
      } else {
        Main.Values.rootInstancesFolder = mmcInstancesFolder.getAbsoluteFile();
        Main.Values.updateArchive = zipFile.getAbsoluteFile();
        Main.Values.unzippedArchive = new File(Main.Values.updateArchive.getParent(),
            zipFile.getName().replaceAll("\\.zip$", "")).getAbsoluteFile();

        // Clear up gui and switch to progress bar
        loadingFrame.pack();
        loadingFrame.setLocation(initFrame.getLocation());
        loadingFrame.setVisible(true);
        initFrame.dispose();
        logDebug("GUI: init frame disposed, loading frame displayed");

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
    logFrame = makeSimpleFrame(JFrame.HIDE_ON_CLOSE);
    logArea = new JTextArea(50, 80);
    mmcField = new JTextField("C:\\Program Files\\MultiMC\\instances", 30);
    zipField = new JTextField("C:\\User\\" + System.getProperty("user.name")
     + "\\Downloads\\TuxCraft-X.X.XX.zip", 30);
    nextButton = new JButton(new NextButtonAction());
    loadingBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 6);
    currentActionLabel = new JLabel("Tuxcraft installation should begin soon... "
        + "Please be patient..");

    nextButton.setText("Next");
    currentActionLabel.setVerticalAlignment(JLabel.CENTER);
    logArea.setEditable(false);

    //TODO REMOVE at production ! custom pre-filled path entries
    mmcField.setText("/home/barasingha/.local/share/multimc/instances");
    zipField.setText("/home/barasingha/Nextcloud/Games/Minecraft/TuxCraft-1.1.01.zip");
    mmcField.setText("/doc/morgane/projects/dev/Tuxcraft-Installer/instances");
    zipField.setText("/doc/morgane/dwl/TuxCraft-1.1.01.zip");

    initFrame.add(encapsulate(null));
    initFrame.add(makePathEntry("MultiMC instances folder:", mmcField,
        JFileChooser.DIRECTORIES_ONLY, null));
    initFrame.add(makePathEntry("TuxCraft instance archive:", zipField,
        JFileChooser.FILES_AND_DIRECTORIES, new FileNameExtensionFilter("ZIP archive", "zip")));
    initFrame.add(encapsulate(null));
    initFrame.add(encapsulate(new FlowLayout(FlowLayout.RIGHT), nextButton));

    loadingFrame.add(encapsulate(null));
    loadingFrame.add(currentActionLabel);
    loadingFrame.add(encapsulate(null, loadingBar));

    logFrame.add(makeLogBox());

    initFrame.setJMenuBar(makeMenu());
    loadingFrame.setJMenuBar(makeMenu());
    logFrame.setJMenuBar(makeMenu());

    logDebug("GUI: setup done");
  }
}
