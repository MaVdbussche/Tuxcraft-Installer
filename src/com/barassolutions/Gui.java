package com.barassolutions;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class Gui {

    private static final JFrame frame = new JFrame("Tuxcraft Installer");

    /**
     * An action listener (for clicks) for browse buttons.
     */
    private static final class BrowserListener implements ActionListener {

        private final JTextField field;
        private final JFileChooser chooser;

        BrowserListener(JTextField field, FileNameExtensionFilter filter) {
            this.field = field;
            chooser = new JFileChooser();
            chooser.setFileFilter(filter);
            chooser.setFileHidingEnabled(false);
        }

        BrowserListener(JTextField field, int fileSelectionMode) {
            this.field = field;
            chooser = new JFileChooser();
            chooser.setFileSelectionMode(fileSelectionMode);
            chooser.setFileHidingEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            int ret = chooser.showOpenDialog(null);
            if (ret == JFileChooser.APPROVE_OPTION)
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            else if (ret == JFileChooser.ERROR_OPTION)
                JOptionPane.showMessageDialog(null,"Error, contact the developers or install manually :/",
                        "Not UwU", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Initiates and displays the installer's GUI.
     */
    static void initValues() {
        AtomicBoolean wait = new AtomicBoolean(true); // this is basically a lock

        // Frame setup
        frame.setSize(700, 300);
        frame.setFont(new Font("SansSerif", Font.PLAIN, 14));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(3, 1));

        // MultiMC path selection row
        JPanel mmcPanel = new JPanel();
        JLabel mmcLabel = new JLabel("MultiMC instances folder:");
        JTextField mmcField = new JTextField(255);
        JButton mmcBrowse = new JButton("Browse");

        mmcPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        mmcField.setEditable(true);
        mmcBrowse.addActionListener(new BrowserListener(mmcField, JFileChooser.DIRECTORIES_ONLY));

        mmcPanel.add(mmcLabel);
        mmcPanel.add(mmcField);
        mmcPanel.add(mmcBrowse);

        // Instance zip path selection row
        JPanel zipPanel = new JPanel();
        JLabel zipLabel = new JLabel("Instance Archive path:");
        JTextField zipField = new JTextField(255);
        JButton zipBrowse = new JButton("Browse");

        zipPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        zipField.setEditable(true);
        zipBrowse.addActionListener(new BrowserListener(zipField, new FileNameExtensionFilter("ZIP archive", "zip")));

        zipPanel.add(zipLabel);
        zipPanel.add(zipField);
        zipPanel.add(zipBrowse);

        // Next button
        JPanel nextPanel = new JPanel();
        JButton nextButton = new JButton("Next");

        nextPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        nextPanel.add(nextButton);

        nextButton.addActionListener(actionEvent -> {
            File mmcFolder = new File(mmcField.getText());
            File zipFile = new File(zipField.getText());
            if (!mmcFolder.isDirectory())
                JOptionPane.showMessageDialog(null, "\"" + mmcField.getText() + "\" is not a directory", "Oh no !",
                        JOptionPane.WARNING_MESSAGE);
            else if (!zipFile.isFile())
                JOptionPane.showMessageDialog(null, "\"" + zipField.getText() + "\" does not exist", "Oh no !",
                        JOptionPane.WARNING_MESSAGE);
            else if (!Pattern.matches(".*[\\\\/]TuxCraft-[0-9]\\.[0-9]\\.[0-9][0-9]\\.zip", zipField.getText()))
                JOptionPane.showMessageDialog(null,
                        "The file you specified either is not a TuxCraft instance, or it has been badly renamed.\n" +
                                "If this is the case, please rename in as `TuxCraft-x.x.xx.zip, replacing `x` " +
                                "appropriately by the version numbers.", "Oh no !", JOptionPane.WARNING_MESSAGE);
            else {
                Main.Values.rootInstancesFolder = mmcFolder.getAbsoluteFile();
                Main.Values.updateArchive = zipFile.getAbsoluteFile();
                Main.Values.unzippedArchive = new File(Main.Values.updateArchive.getParent(),
                        zipFile.getName().replaceAll("\\.zip$", "")).getAbsoluteFile();
                // If updateArchive is in MultiMC instances folder
                if (Main.Values.rootInstancesFolder.equals(Main.Values.updateArchive.getParentFile())) {
                    throw new RuntimeException("Unable to extract archive: Old instance copy would overwrite " +
                            "extracted new instance. Please move archive outside MultiMC's instances folder.");
                }

                // Deactivate all event handling
                mmcField.setEditable(false);
                zipField.setEditable(false);
                mmcBrowse.removeActionListener(mmcBrowse.getActionListeners()[0]);
                zipBrowse.removeActionListener(zipBrowse.getActionListeners()[0]);
                nextButton.removeActionListener(nextButton.getActionListeners()[0]);
                // Unlock main thread
                wait.set(false);
            }
        });

        frame.add(mmcPanel);
        frame.add(zipPanel);
        frame.add(nextPanel);

        frame.setVisible(true);

        /*START OF DEV SHIT*/
        mmcField.setText("/home/barasingha/.local/share/multimc/instances");
        zipField.setText("/home/barasingha/Nextcloud/Games/Minecraft/TuxCraft-1.1.00.zip");
        /*END OF DEV SHIT*/


        while (wait.get()) Thread.yield(); // wait until values initialised before returning
    }

    static void exit() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
}
