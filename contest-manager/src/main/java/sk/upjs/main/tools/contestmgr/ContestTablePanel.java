package sk.upjs.main.tools.contestmgr;

import javax.swing.JPanel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.ImageIcon;

@SuppressWarnings("serial")
public class ContestTablePanel extends JPanel {

    /**
     * TableModel na zobrazovanie aktualnych vysledkov sutaze
     */
    private class ContestResultsTableModel extends AbstractTableModel {

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return contest.getContestantsCount();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            if ((rowIndex < 0) || (rowIndex >= contest.getContestantsCount()))
                return null;

            Contest.Contestant c = contest.getContestantAtRank(rowIndex);
            if (colIndex == 0) {
                return c.getName();
            }

            if (colIndex == 1)
                return c.getRating();

            return null;
        }

        @Override
        public String getColumnName(int colIndex) {
            if (colIndex == 0)
                return "Súťažiaci";

            if (colIndex == 1)
                return "Rating";

            return null;
        }
    }

    private Contest contest;
    private Contest.ContestListener contestListener;

    private JTable table;
    private JLabel timeLabel;
    private ContestResultsTableModel tableModel;
    private JLabel lastDuelLabel;

    /**
     * Vytvori panel (lebn pre GUI)
     */
    public ContestTablePanel() {
        initComponents();
        tableModel = new ContestResultsTableModel();
        table.setModel(tableModel);
    }

    /**
     * Vytvori panel zobrazujuci informacie o priebehu zadanej sutaze.
     *
     * @param contest zobrazovana sutaz.
     */
    public ContestTablePanel(Contest contest) {
        this();
        this.contest = contest;

        contestListener = new Contest.ContestListener() {
            @Override
            public void onContestChange() {
                updateContestInfo();
            }

            @Override
            public void onContestTimeChange() {
                updateTimer();
            }
        };

        contest.addContestListener(contestListener);
    }

    /**
     * Aktualizuje zobrazenie komponentu
     */
    public void refresh() {
        updateContestInfo();
        updateTimer();
    }

    /**
     * Aktualizuje zobrazenie casu trvania sutaze.
     */
    private void updateTimer() {
        int showTime = 0;
        if (contest.isTimerEnabled())
            showTime = contest.getRemainingTime();
        else
            showTime = contest.getTime();

        if (showTime < 0) {
            timeLabel.setForeground(Color.red);
            timeLabel.setText("-" + Contest.formatTime(-showTime));
        } else {
            timeLabel.setForeground(Color.black);
            timeLabel.setText(Contest.formatTime(showTime));
        }
    }

    /**
     * Aktualizuje zobrazene informacie o stave sutaze (vysledkovka, posledny
     * duel)
     */
    private void updateContestInfo() {
        tableModel.fireTableDataChanged();
        List<Contest.Duel> duels = contest.getDuels();
        if (duels.size() > 0) {
            Contest.Duel lastDuel = duels.get(duels.size() - 1);
            lastDuelLabel.setText(lastDuel.winner.getName() + " vs. "
                    + lastDuel.loser.getName());
        } else {
            lastDuelLabel.setText("");
        }
    }

    /**
     * Zmeni velkost pouzitych fontov pre zobrazenie "dolezitych informacii".
     */
    private void changeTableFont(int change) {
        Font currentFont = table.getFont();
        table.setFont(currentFont.deriveFont(Math.max(5,
                currentFont.getSize2D() + change)));
        table.setRowHeight(Math.max(5, table.getRowHeight() + change));

        currentFont = timeLabel.getFont();
        timeLabel.setFont(currentFont.deriveFont(Math.max(5,
                currentFont.getSize2D() + change)));
    }

    /**
     * Ukonci pocuvanie zmien sutaze.
     */
    public void stopListenContest() {
        contest.removeContestListener(contestListener);
    }

    /**
     * Inicializuje komponenty panelu.
     */
    private void initComponents() {
        JScrollPane scrollPane = new JScrollPane();

        JPanel panel = new JPanel();

        JButton btnNewButton = new JButton("");
        btnNewButton.setToolTipText("Zväčšiť font");
        btnNewButton.setIcon(new ImageIcon(ContestTablePanel.class
                .getResource("/images/font_increase.png")));
        btnNewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeTableFont(+1);
            }
        });

        JButton btnNewButton_1 = new JButton("");
        btnNewButton_1.setToolTipText("Zmenšiť font");
        btnNewButton_1.setIcon(new ImageIcon(ContestTablePanel.class
                .getResource("/images/font_decrease.png")));
        btnNewButton_1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                changeTableFont(-1);
            }
        });

        timeLabel = new JLabel("0:00:00");
        timeLabel.setFont(new Font("Arial Black", Font.PLAIN, 18));
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout
                .setHorizontalGroup(groupLayout
                        .createParallelGroup(Alignment.LEADING)
                        .addGroup(
                                groupLayout
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(
                                                groupLayout
                                                        .createParallelGroup(
                                                                Alignment.TRAILING)
                                                        .addComponent(
                                                                scrollPane,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                430,
                                                                Short.MAX_VALUE)
                                                        .addComponent(
                                                                panel,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                430,
                                                                Short.MAX_VALUE)
                                                        .addGroup(
                                                                groupLayout
                                                                        .createSequentialGroup()
                                                                        .addComponent(
                                                                                timeLabel)
                                                                        .addPreferredGap(
                                                                                ComponentPlacement.RELATED,
                                                                                274,
                                                                                Short.MAX_VALUE)
                                                                        .addComponent(
                                                                                btnNewButton_1)
                                                                        .addPreferredGap(
                                                                                ComponentPlacement.RELATED)
                                                                        .addComponent(
                                                                                btnNewButton)))
                                        .addContainerGap()));
        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(
                Alignment.LEADING)
                .addGroup(
                        groupLayout
                                .createSequentialGroup()
                                .addGap(5)
                                .addGroup(
                                        groupLayout
                                                .createParallelGroup(
                                                        Alignment.BASELINE)
                                                .addComponent(timeLabel)
                                                .addComponent(btnNewButton)
                                                .addComponent(btnNewButton_1))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(scrollPane,
                                        GroupLayout.DEFAULT_SIZE, 289,
                                        Short.MAX_VALUE)
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addComponent(panel,
                                        GroupLayout.PREFERRED_SIZE, 35,
                                        GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()));

        JLabel lblPoslednDuel = new JLabel("Posledný duel:");

        lastDuelLabel = new JLabel("winner vs. loser");
        lastDuelLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(
                Alignment.LEADING).addGroup(
                gl_panel.createSequentialGroup().addContainerGap()
                        .addComponent(lblPoslednDuel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(lastDuelLabel)
                        .addContainerGap(298, Short.MAX_VALUE)));
        gl_panel.setVerticalGroup(gl_panel.createParallelGroup(
                Alignment.LEADING)
                .addGroup(
                        Alignment.TRAILING,
                        gl_panel.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)
                                .addGroup(
                                        gl_panel.createParallelGroup(
                                                Alignment.BASELINE)
                                                .addComponent(lblPoslednDuel)
                                                .addComponent(lastDuelLabel))
                                .addContainerGap()));
        panel.setLayout(gl_panel);

        table = new JTable();
        scrollPane.setViewportView(table);
        setLayout(groupLayout);
    }
}
