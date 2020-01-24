package com.barassolutions;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Gui {

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
        }

        BrowserListener(JTextField field, int fileSelectionMode) {
            this.field = field;
            chooser = new JFileChooser();
            chooser.setFileSelectionMode(fileSelectionMode);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            int ret = chooser.showOpenDialog(null);
            if (ret == JFileChooser.APPROVE_OPTION)
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            else if (ret == JFileChooser.ERROR_OPTION)
                JOptionPane.showMessageDialog(null,"Error", "Not UwU", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Initiates and displays the installer's GUI.
     */
    static void init() {
        // Frame setup
        JFrame frame = new JFrame("TuxCraft Installer");
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
            if (!mmcFolder.isDirectory() || !new File(mmcFolder, "instgroups.json").isFile())
                JOptionPane.showMessageDialog(null,
                        "You did not specified a valid MultiMC instances folder",
                        "Oh no !", JOptionPane.WARNING_MESSAGE);
            else
                JOptionPane.showMessageDialog(null, "TODO: implement ze program my dudes", "puet puet", JOptionPane.INFORMATION_MESSAGE);
        });

        frame.add(mmcPanel);
        frame.add(zipPanel);
        frame.add(nextPanel);

        // Finalize window and display
        frame.setSize(700, 300);
        frame.setVisible(true);
    }

    /**
     * Encapsulate a component into another default placement behavior, and return the parent component.
     *
     * Used to avoid creating a bunch of variables, not gonna lie.
     *
     * @param parent The parent component
     * @param child The child component added to the parent
     * @return The parent component
     */
    private static JComponent encapsulate(JComponent parent, JComponent child) {
        parent.add(child);
        return parent;
    }
}
