package sk.upjs.main.tools.attendance;

import sk.upjs.main.tools.rfid.RFIDCardDetector;

import java.awt.EventQueue;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Toolkit;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {

    private JPanel contentPane;
    private JTextField cardIDField;
    private CompleteListTab completeListTab;
    private AttendantsTab attendantsTab;
    private JComboBox<String> cardReaderCombo;
    private RFIDCardDetector cardDetector;
    private String lastKnownCardId = "";

    /**
     * Konstruktor okna.
     */
    public MainFrame() {
        setIconImage(Toolkit.getDefaultToolkit().getImage(
                MainFrame.class.getResource("/images/attendance.png")));
        initializeComponents();
        updateTerminals();
        terminalChanged();
    }

    private void updateTerminals() {
        List<String> terminalNames = RFIDCardDetector.getTerminalNames();
        if (terminalNames == null)
            terminalNames = Collections.emptyList();

        DefaultComboBoxModel<String> cm = new DefaultComboBoxModel<String>();
        for (String terminalName : terminalNames)
            cm.addElement(terminalName);

        cardReaderCombo.setModel(cm);
    }

    private void terminalChanged() {
        // ak mame detektor, tak ho ukoncime
        if (cardDetector != null) {
            cardDetector.stop();
            cardDetector = null;
            lastKnownCardId = "";
        }

        completeListTab.updateForAttendantWithModifiedCardId();
        cardIDField.setText("");

        // Vytvorime novy detektor pre aktivny terminal
        if (cardReaderCombo.getSelectedItem() != null) {
            String cardReaderId = cardReaderCombo.getSelectedItem().toString();
            cardDetector = RFIDCardDetector.createDetector(cardReaderId);
        }

        // Ak sa podarilo detektor, tak zaregistujeme listenera
        if (cardDetector != null) {
            cardDetector.addCardListener(new RFIDCardDetector.CardListener() {
                @Override
                public void onCardDetected(final String cardId, byte[] rawCardId) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            cardDetected(cardId);
                        }
                    });
                }
            });

            cardDetector.start();
        } else {
            cardIDField.setText("Žiadna čítačka nie je aktívna.");
        }
    }

    private void cardDetected(String cardId) {
        lastKnownCardId = cardId;
        completeListTab.updateForAttendantWithModifiedCardId();

        cardIDField.setText(cardId);
        Map<String, String> record = completeListTab.getRecordForCard(cardId);
        if (record == null) {
            cardIDField.setText(cardId + " (Neznáma karta)");
            return;
        }

        // Ak nie su dovolene duplikaty, overime, ci uz kartu s danym kodom
        // nemame
        if (!attendantsTab.isDuplicateAllowed()) {
            if (attendantsTab.containsAttendantWith(
                    completeListTab.getCardIdColumnName(), cardId)) {
                cardIDField.setText(cardId + " (Duplicitný zápis účastníka)");
                return;
            }
        }

        attendantsTab.addAttendant(record);
        completeListTab.notifyAcceptedAttendant(cardId);
    }

    public boolean createAttendantWithModifiedCard(Map<String, String> record) {
        record.put(completeListTab.getCardIdColumnName(), lastKnownCardId);
        String cardId = lastKnownCardId;

        // Ak nie su dovolene duplikaty, overime, ci uz kartu s danym kodom
        // nemame
        if (!attendantsTab.isDuplicateAllowed()) {
            if (attendantsTab.containsAttendantWith(
                    completeListTab.getCardIdColumnName(), cardId)) {
                cardIDField.setText(cardId + " (Duplicitný zápis účastníka)");
                JOptionPane.showMessageDialog(this,
                        "Duplicitný zápis účastníka.");
                return false;
            }
        }

        attendantsTab.addAttendant(record);
        return true;
    }

    private void initializeComponents() {
        setTitle("Prezen\u010Dka");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 417, 469);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        JPanel panel = new JPanel();

        JTabbedPane tabContainer = new JTabbedPane(JTabbedPane.TOP);
        GroupLayout gl_contentPane = new GroupLayout(contentPane);
        gl_contentPane.setHorizontalGroup(gl_contentPane
                .createParallelGroup(Alignment.LEADING)
                .addComponent(panel, GroupLayout.DEFAULT_SIZE, 401,
                        Short.MAX_VALUE)
                .addComponent(tabContainer, GroupLayout.DEFAULT_SIZE, 342,
                        Short.MAX_VALUE));
        gl_contentPane.setVerticalGroup(gl_contentPane.createParallelGroup(
                Alignment.LEADING).addGroup(
                gl_contentPane
                        .createSequentialGroup()
                        .addComponent(panel, GroupLayout.PREFERRED_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(tabContainer, GroupLayout.DEFAULT_SIZE,
                                397, Short.MAX_VALUE)));

        cardReaderCombo = new JComboBox<String>();
        cardReaderCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                terminalChanged();
            }
        });

        JLabel lbltaka = new JLabel("\u010C\u00EDta\u010Dka:");

        JLabel lblsloKarty = new JLabel("\u010C\u00EDslo karty:");

        cardIDField = new JTextField();
        cardIDField.setEditable(false);
        cardIDField.setColumns(10);
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(gl_panel
                .createParallelGroup(Alignment.LEADING)
                .addGroup(
                        gl_panel.createSequentialGroup()
                                .addGroup(
                                        gl_panel.createParallelGroup(
                                                Alignment.LEADING)
                                                .addGroup(
                                                        gl_panel.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(
                                                                        lbltaka)
                                                                .addPreferredGap(
                                                                        ComponentPlacement.RELATED)
                                                                .addComponent(
                                                                        cardReaderCombo,
                                                                        0,
                                                                        328,
                                                                        Short.MAX_VALUE))
                                                .addGroup(
                                                        gl_panel.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(
                                                                        lblsloKarty)
                                                                .addPreferredGap(
                                                                        ComponentPlacement.RELATED)
                                                                .addComponent(
                                                                        cardIDField,
                                                                        GroupLayout.DEFAULT_SIZE,
                                                                        313,
                                                                        Short.MAX_VALUE)))
                                .addContainerGap()));
        gl_panel.setVerticalGroup(gl_panel
                .createParallelGroup(Alignment.LEADING)
                .addGroup(
                        gl_panel.createSequentialGroup()
                                .addGap(11)
                                .addGroup(
                                        gl_panel.createParallelGroup(
                                                Alignment.BASELINE)
                                                .addComponent(
                                                        cardReaderCombo,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE)
                                                .addComponent(lbltaka))
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGroup(
                                        gl_panel.createParallelGroup(
                                                Alignment.BASELINE)
                                                .addComponent(lblsloKarty)
                                                .addComponent(
                                                        cardIDField,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)));
        panel.setLayout(gl_panel);

        attendantsTab = new AttendantsTab();
        completeListTab = new CompleteListTab();
        completeListTab.setAttendantsTab(attendantsTab);
        completeListTab.setMainFrame(this);
        tabContainer.addTab("Účastníci", attendantsTab);
        tabContainer.addTab("Kompletný zoznam", completeListTab);

        contentPane.setLayout(gl_contentPane);
    }

    /**
     * Vrati, ci je nacitana nejaka karta
     *
     * @return
     */
    public boolean hasCardId() {
        return !("".equals(lastKnownCardId));
    }

    /**
     * "Spustac" aplikacie.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager
                            .getSystemLookAndFeelClassName());
                    MainFrame frame = new MainFrame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
