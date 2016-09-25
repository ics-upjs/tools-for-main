package sk.upjs.main.tools.attendance;

import sk.upjs.main.tools.csv.CSVTableModel;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.event.*;
import java.io.File;
import java.util.*;

@SuppressWarnings("serial")
public class CompleteListTab extends JPanel {

    private JTable table;
    private CSVTableModel tableModel;
    private JComboBox<String> cardColumnCombo;
    private AttendantsTab attendantsTab;
    private MainFrame mainFrame;
    private JButton btnCopyWithID;
    private JCheckBox removeAttendantsBox;

    /**
     * Vytvori panel.
     */
    public CompleteListTab() {
        initComponents();
    }

    /**
     * Aktualizuje aktivitu tlacidla na kopirovanie s ID.
     */
    public void updateForAttendantWithModifiedCardId() {
        btnCopyWithID.setEnabled((table.getSelectedRow() >= 0) && (mainFrame != null) && (mainFrame.hasCardId()));
    }

    /**
     * Nastavi tab, ktory zobrazuje vybranu podmnozinu ucastnikov.
     *
     * @param attendantsTab
     */
    public void setAttendantsTab(AttendantsTab attendantsTab) {
        this.attendantsTab = attendantsTab;
    }

    /**
     * Nastavi hlavny frame.
     *
     * @param mainFrame
     */
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /**
     * Vrati pomenovanie stlpca, ktory oznacuje id karty
     *
     * @return
     */
    public String getCardIdColumnName() {
        Object result = cardColumnCombo.getSelectedItem();
        if (result != null)
            return result.toString();
        else
            return null;
    }

    private void createAttendantWithModifiedCardId() {
        if (mainFrame != null) {
            int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
            if (selectedRow >= 0) {
                boolean success = mainFrame.createAttendantWithModifiedCard(tableModel.getRow(selectedRow));
                if (removeAttendantsBox.isSelected() && success) {
                    tableModel.removeRow(selectedRow);
                }
            }
        }
    }

    /**
     * Nacita zoznam ucastnikov z CSV suboru
     */
    private void loadCompleteList() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
        FileNameExtensionFilter filterCSV = new FileNameExtensionFilter("CSV súbor (.csv)", "csv");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(filterCSV);

        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                // Vytvorime tablemodel
                tableModel = CSVTableModel.loadFromFile(fc.getSelectedFile());
                tableModel.setEditable(false);
                table.setModel(tableModel);

                // Na zaklade zoznamu stplcov aktualizujeme combo s nazvami
                // poloziek
                DefaultComboBoxModel<String> lm = new DefaultComboBoxModel<String>();
                List<String> columns = new ArrayList<String>();
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    lm.addElement(tableModel.getColumnName(i));
                    columns.add(tableModel.getColumnName(i));
                }
                cardColumnCombo.setModel(lm);

                // Nastavime, ze je treba zmenu aj zozname poloziek vybranych
                // ucastnikov
                if (attendantsTab != null)
                    attendantsTab.updateColumnsForAttendantList(columns);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
    }

    /**
     * Vrati zaznam ku karte so zadanym id
     *
     * @param cardId identifikator karty
     * @return zaznam osoby s danou kartou alebo null, ak zaznam neexistuje
     */
    public Map<String, String> getRecordForCard(String cardId) {
        if (cardId == null)
            return null;

        // Vyberieme stlpec, ktory definuje cislo karty
        Object cardColumn = cardColumnCombo.getSelectedItem();
        if (cardColumn == null)
            return null;

        // Vyberieme index stlpca
        int cardIdIdx = tableModel.indexOfColumn(cardColumn.toString());

        // Overime, ci niektory z riadkov neobsahuje hladanu hodnotu
        for (int rowIdx = 0; rowIdx < tableModel.getRowCount(); rowIdx++)
            if (cardId.equals(tableModel.getValueAt(rowIdx, cardIdIdx))) {
                return tableModel.getRow(rowIdx);
            }

        return null;
    }

    /**
     * Prijme oznam, ze osoba so zadanym cislom karty bola prijata ako ucastnik.
     *
     * @param cardId
     */
    public void notifyAcceptedAttendant(String cardId) {
        if (!removeAttendantsBox.isSelected())
            return;

        if (cardId == null)
            return;

        // Vyberieme stlpec, ktory definuje cislo karty
        Object cardColumn = cardColumnCombo.getSelectedItem();
        if (cardColumn == null)
            return;

        // Vyberieme index stlpca
        int cardIdIdx = tableModel.indexOfColumn(cardColumn.toString());

        // Overime, ci niektory z riadkov neobsahuje hladanu hodnotu
        for (int rowIdx = 0; rowIdx < tableModel.getRowCount(); rowIdx++)
            if (cardId.equals(tableModel.getValueAt(rowIdx, cardIdIdx))) {
                tableModel.removeRow(rowIdx);
                return;
            }
    }

    /**
     * Inicializuje komponenty
     */
    private void initComponents() {
        JButton btnLoadList = new JButton("Na\u010D\u00EDtaj zoznam");
        btnLoadList.setIcon(new ImageIcon(CompleteListTab.class.getResource("/images/open.png")));
        btnLoadList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                loadCompleteList();
            }
        });

        JLabel lblStpecSId = new JLabel("St\u013Apec s ID karty:");

        cardColumnCombo = new JComboBox<String>();

        JScrollPane listTable = new JScrollPane();

        JPanel panel = new JPanel();
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.TRAILING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                groupLayout
                                        .createParallelGroup(Alignment.TRAILING)
                                        .addComponent(listTable, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 508,
                                                Short.MAX_VALUE)
                                        .addGroup(
                                                groupLayout.createSequentialGroup().addComponent(btnLoadList)
                                                        .addGap(10).addComponent(lblStpecSId)
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addComponent(cardColumnCombo, 0, 306, Short.MAX_VALUE))
                                        .addComponent(panel, GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE))
                        .addContainerGap()));
        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                groupLayout
                                        .createParallelGroup(Alignment.BASELINE)
                                        .addComponent(lblStpecSId)
                                        .addComponent(cardColumnCombo, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnLoadList)).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(listTable, GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap()));

        btnCopyWithID = new JButton("\u00DA\u010Dastn\u00EDk so zmenou ID karty");
        btnCopyWithID.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                createAttendantWithModifiedCardId();
            }
        });

        removeAttendantsBox = new JCheckBox("Odstr\u00E1ni\u0165 \u00FA\u010Dastn\u00EDka");
        removeAttendantsBox.setSelected(true);
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(Alignment.TRAILING).addGroup(
                gl_panel.createSequentialGroup().addComponent(removeAttendantsBox)
                        .addPreferredGap(ComponentPlacement.RELATED, 242, Short.MAX_VALUE).addComponent(btnCopyWithID)));
        gl_panel.setVerticalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel.createSequentialGroup()
                        .addGroup(
                                gl_panel.createParallelGroup(Alignment.BASELINE).addComponent(btnCopyWithID)
                                        .addComponent(removeAttendantsBox))
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        panel.setLayout(gl_panel);

        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        listTable.setViewportView(table);
        tableModel = new CSVTableModel();
        table.setModel(tableModel);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent arg0) {
                updateForAttendantWithModifiedCardId();
            }
        });
        setLayout(groupLayout);
    }
}
