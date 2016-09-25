package sk.upjs.main.tools.attendance;

import sk.upjs.main.tools.csv.CSVTableModel;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;

/**
 * Panel zobrazujuci akceptovanych ucastnikov.
 */
@SuppressWarnings("serial")
public class AttendantsTab extends JPanel {
    private JTable listTable;
    private CSVTableModel tableModel;
    private JCheckBox checkNoInsertOfDuplicates;

    /**
     * Vytvori panel.
     */
    public AttendantsTab() {
        initComponents();
    }

    /**
     * Ulozi zoznam ucastnikov.
     */
    private void storeListOfAttendants() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
        FileNameExtensionFilter filterCSV = new FileNameExtensionFilter("CSV súbor (.csv)", "csv");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(filterCSV);

        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                // Ulozime table model do suboru
                tableModel.saveToFile(fc.getSelectedFile());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
    }

    /**
     * Aktualizuje zoznam atributov ucastnikov v zozname ucastnikov.
     *
     * @param columns
     */
    public void updateColumnsForAttendantList(List<String> columns) {
        tableModel = new CSVTableModel(columns, null);
        tableModel.setEditable(true);
        listTable.setModel(tableModel);
    }

    /**
     * Prida noveho ucastnika
     *
     * @param record
     */
    public void addAttendant(Map<String, String> record) {
        tableModel.insertNewRow(0, record);
    }

    /**
     * Vrati, ci su duplikaty povolene
     *
     * @return
     */
    public boolean isDuplicateAllowed() {
        return !checkNoInsertOfDuplicates.isSelected();
    }

    /**
     * Vrati, ci zoznam obsahuje ucastnika so zadanou hodnotou vlastnosti
     *
     * @param property
     * @param value
     */
    public boolean containsAttendantWith(String property, String value) {
        if (property == null)
            return false;

        int propertyIndex = -1;
        for (int i = 0; i < tableModel.getColumnCount(); i++)
            if (property.equals(tableModel.getColumnName(i))) {
                propertyIndex = i;
                break;
            }

        if (propertyIndex < 0)
            return false;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object rowValue = tableModel.getValueAt(i, propertyIndex);
            if (value == null) {
                if (rowValue == null)
                    return true;
            } else {
                if (value.equals(rowValue))
                    return true;
            }
        }

        return false;
    }

    /**
     * Inicializuje komponenty
     */
    private void initComponents() {
        JButton btnStoreList = new JButton("Ulo\u017E zoznam");
        btnStoreList.setIcon(new ImageIcon(AttendantsTab.class.getResource("/images/save.png")));
        btnStoreList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                storeListOfAttendants();
            }
        });

        JScrollPane scrollPane = new JScrollPane();

        checkNoInsertOfDuplicates = new JCheckBox("Nevkladaj duplicitne");
        checkNoInsertOfDuplicates.setSelected(true);
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                groupLayout
                                        .createParallelGroup(Alignment.LEADING)
                                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                                        .addGroup(
                                                groupLayout
                                                        .createSequentialGroup()
                                                        .addComponent(btnStoreList)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 242,
                                                                Short.MAX_VALUE)
                                                        .addComponent(checkNoInsertOfDuplicates))).addContainerGap()));
        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(btnStoreList)
                                        .addComponent(checkNoInsertOfDuplicates))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE).addContainerGap()));

        listTable = new JTable();
        scrollPane.setViewportView(listTable);

        tableModel = new CSVTableModel();
        tableModel.setEditable(true);
        listTable.setModel(tableModel);
        setLayout(groupLayout);
    }
}
