package sk.upjs.main.tools.contestmgr;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.table.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Font;

/**
 * Panel umoznujuci editovanie ucastnikov sutaze.
 */
@SuppressWarnings("serial")
public class ContestantsPanel extends JPanel {

    private Contest contest;

    private JTable contestantsTable;
    private AbstractTableModel tm;

    /**
     * Vytvori panel na editovanie sutaziacich v sutazi (bezparametrovy
     * konstruktor len pre GUI)
     */
    public ContestantsPanel() {
        initComponents();
    }

    /**
     * Vytvori panel na editovanie sutaziacich v sutazi pre sutaz manazovany
     * hlavnym frameom.
     */
    public ContestantsPanel(MainFrame frame) {
        this();
        this.contest = frame.getContest();

        this.tm = new AbstractTableModel() {
            /**
             * ID stlpca s menom sutaziaceho.
             */
        	private static final int NAME_COLUMN = 0;
        	
        	@Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public int getRowCount() {
                return contest.getContestantsCount();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == NAME_COLUMN) {
                    return contest.getContestant(rowIndex).getName();
                }

                return null;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (columnIndex == NAME_COLUMN) {
                    return true;
                }

                return false;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (columnIndex == NAME_COLUMN) {
                    contest.getContestant(rowIndex).setName(aValue.toString());
                }
            }

            @Override
            public String getColumnName(int columnIndex) {
                if (columnIndex == NAME_COLUMN)
                    return "Súťažiaci";

                return null;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == NAME_COLUMN)
                    return String.class;

                return null;
            }
        };

        contestantsTable.setModel(tm);
    }

    public void refreshView() {
        tm.fireTableDataChanged();
    }

    /**
     * Prida noveho sutaziaceho.
     */
    private void addContestant() {
        int idx = contestantsTable.getSelectedRow();
        if (idx >= 0)
            idx = contestantsTable.convertRowIndexToModel(idx);
        else
            idx = contest.getContestantsCount();

        contest.addNewContestant(idx);
        tm.fireTableDataChanged();
        if (idx < tm.getRowCount())
            contestantsTable.getSelectionModel().setSelectionInterval(idx, idx);
    }

    /**
     * Odstrani vybraneho sutaziaceho.
     */
    private void removeContestant() {
        int idx = contestantsTable.getSelectedRow();
        if (idx >= 0)
            idx = contestantsTable.convertRowIndexToModel(idx);
        else
            return;

        contest.removeContestant(idx);
        tm.fireTableDataChanged();
        if (idx < tm.getRowCount()) {
            contestantsTable.getSelectionModel().setSelectionInterval(idx, idx);
        } else {
            if (tm.getRowCount() > 0)
                contestantsTable.getSelectionModel().setSelectionInterval(
                        tm.getRowCount() - 1, tm.getRowCount() - 1);
        }
    }

    /**
     * Inicializuje komponenty panelu.
     */
    private void initComponents() {
        JPanel panel = new JPanel();

        JScrollPane scrollPane = new JScrollPane();
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout
                .setHorizontalGroup(groupLayout
                        .createParallelGroup(Alignment.LEADING)
                        .addGroup(
                                groupLayout
                                        .createSequentialGroup()
                                        .addComponent(panel,
                                                GroupLayout.DEFAULT_SIZE, 450,
                                                Short.MAX_VALUE)
                                        .addContainerGap())
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE,
                                450, Short.MAX_VALUE));
        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(
                Alignment.LEADING).addGroup(
                Alignment.TRAILING,
                groupLayout
                        .createSequentialGroup()
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE,
                                472, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel, GroupLayout.PREFERRED_SIZE, 32,
                                GroupLayout.PREFERRED_SIZE)));

        JButton removeContestantButton = new JButton("Odstráňiť");
        removeContestantButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        removeContestantButton.setIcon(new ImageIcon(ContestantsPanel.class
                .getResource("/images/delete.png")));
        removeContestantButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                removeContestant();
            }
        });

        JButton addContestantButton = new JButton("Pridať");
        addContestantButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addContestantButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addContestant();
            }
        });
        addContestantButton.setIcon(new ImageIcon(ContestantsPanel.class
                .getResource("/images/add.png")));
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(
                Alignment.LEADING).addGroup(
                gl_panel.createSequentialGroup().addGap(7)
                        .addComponent(addContestantButton)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(removeContestantButton)
                        .addContainerGap(233, Short.MAX_VALUE)));
        gl_panel.setVerticalGroup(gl_panel
                .createParallelGroup(Alignment.TRAILING)
                .addGroup(
                        Alignment.LEADING,
                        gl_panel.createSequentialGroup()
                                .addGroup(
                                        gl_panel.createParallelGroup(
                                                Alignment.BASELINE)
                                                .addComponent(
                                                        addContestantButton)
                                                .addComponent(
                                                        removeContestantButton))
                                .addContainerGap(29, Short.MAX_VALUE)));
        panel.setLayout(gl_panel);

        contestantsTable = new JTable();
        contestantsTable.setAutoCreateRowSorter(true);
        scrollPane.setViewportView(contestantsTable);
        setLayout(groupLayout);
    }
}
