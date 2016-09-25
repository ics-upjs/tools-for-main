package sk.upjs.main.tools.rfidcontroller;

import java.awt.EventQueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.*;
import java.awt.event.*;
import java.awt.Toolkit;
import java.awt.Color;

import sk.upjs.main.tools.csv.CSVTableModel;
import sk.upjs.main.tools.rfid.RFIDCardDetector;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {

    private JPanel contentPane;
    private JTextField cardIDField;
    private JTextField ipField;
    private JTextField winnerField;
    private JTextField loserField;
    private JComboBox<String> cardReaderCombo;
    private CSVTableModel playersTableModel;
    private JComboBox<String> ratingColumnCombo;
    private JComboBox<String> nameColumnCombo;
    private JComboBox<String> cardColumnCombo;
    private JFileChooser fc;

    /**
     * Detektor kariet.
     */
    private RFIDCardDetector cardDetector = null;

    /**
     * Heslo na vzdialene ovladanie hry.
     */
    private String remotePassword = "";
    private JTable playersTable;
    private JSpinner portSpinner;
    private JCheckBox prefixLookupCheckBox;

    /**
     * Konstruktor okna.
     */
    public MainFrame() {
        setTitle("RFID ovl\u00E1da\u010D");
        setIconImage(Toolkit.getDefaultToolkit().getImage(
                MainFrame.class.getResource("/images/remotecontrol.png")));
        initComponents();
        playersTableModel = new CSVTableModel();
        playersTable.setModel(playersTableModel);

        fc = new JFileChooser();
        fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
        FileNameExtensionFilter filterCSV = new FileNameExtensionFilter(
                "CSV súbor (.csv)", "csv");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(filterCSV);

        updateTerminals();
        terminalChanged();
    }

    /**
     * Aktualizuje zoznam terminalov.
     */
    private void updateTerminals() {
        List<String> terminalNames = RFIDCardDetector.getTerminalNames();
        if (terminalNames == null)
            terminalNames = Collections.emptyList();

        DefaultComboBoxModel<String> cm = new DefaultComboBoxModel<String>();
        for (String terminalName : terminalNames)
            cm.addElement(terminalName);

        cardReaderCombo.setModel(cm);
    }

    /**
     * Metoda volana pri zmene aktivneho terminalu.
     */
    private void terminalChanged() {
        // ak mame detektor, tak ho ukoncime
        if (cardDetector != null) {
            cardDetector.stop();
            cardDetector = null;
        }

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

    /**
     * Metoda volana pri detekovani karty.
     *
     * @param cardId cislo detekovanej karty
     */
    private void cardDetected(String cardId) {
        String cardOwnerName = findNameForCardId(cardId);
        if (cardOwnerName == null)
            return;

        if (winnerField.getText().length() == 0) {
            winnerField.setText(cardOwnerName);
            return;
        }

        if (winnerField.getText().length() != 0) {
            if (winnerField.getText().equals(cardOwnerName))
                return;
        }

        if (loserField.getText().length() == 0) {
            loserField.setText(cardOwnerName);
            submitDuelResult();
            winnerField.setText("");
            loserField.setText("");
        }
    }

    /**
     * Odosle vysledok duelu
     *
     * @throws IOException
     * @throws ClientProtocolException
     */
    private void submitDuelResult() {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost request = new HttpPost("http://" + ipField.getText().trim()
                + ":" + portSpinner.getValue() + "/AddDuel/");
        try {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("winner", winnerField.getText()));
            nvps.add(new BasicNameValuePair("loser", loserField.getText()));
            nvps.add(new BasicNameValuePair("password", remotePassword));
            request.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            httpclient.execute(request);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Odoslanie požiadavky sa nepodarilo.");
        } finally {
            request.releaseConnection();
        }
    }

    /**
     * Vrati meno asociovane k zadanemu cislu karty.
     *
     * @param cardId cislo karty
     */
    private String findNameForCardId(String cardId) {
        if (cardId == null)
            return null;

        if ((nameColumnCombo.getSelectedItem() == null)
                || (cardColumnCombo.getSelectedItem() == null))
            return null;

        int nameColumnIdx = playersTableModel.findColumn(nameColumnCombo
                .getSelectedItem().toString());
        int cardIdColumnIdx = playersTableModel.findColumn(cardColumnCombo
                .getSelectedItem().toString());
        if ((nameColumnIdx < 0) || (cardIdColumnIdx < 0))
            return null;

        boolean prefixLookup = prefixLookupCheckBox.isSelected();

        for (int row = 0; row < playersTableModel.getRowCount(); row++) {
            Object playerCardId = playersTableModel.getValueAt(row,
                    cardIdColumnIdx);
            if (playerCardId == null)
                continue;

            boolean cardIdMatched;
            if (prefixLookup) {
                cardIdMatched = playerCardId.toString().startsWith(cardId);
            } else {
                cardIdMatched = playerCardId.toString().equals(cardId);
            }

            if (cardIdMatched) {
                Object value = playersTableModel.getValueAt(row, nameColumnIdx);
                if (value != null)
                    return value.toString();
                else
                    return "";
            }
        }

        return null;
    }

    /**
     * Ulozi tabulku so zoznamom hracov.
     */
    private void saveTableWithPlayers() {
        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                // Ulozime table model do suboru
                playersTableModel.saveToFile(fc.getSelectedFile());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
    }

    /**
     * Nacita tabulku s udajmi hracov.
     */
    private void loadTableWithPlayers() {
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                // Vytvorime tablemodel
                playersTableModel = CSVTableModel.loadFromFile(fc
                        .getSelectedFile());
                playersTableModel.setEditable(true);
                playersTable.setModel(playersTableModel);

                // Na zaklade zoznamu stplcov aktualizujeme comba s nazvami
                // poloziek
                List<String> columns = new ArrayList<String>();
                columns.add("");
                for (int i = 0; i < playersTableModel.getColumnCount(); i++) {
                    columns.add(playersTableModel.getColumnName(i));
                }
                cardColumnCombo.setModel(new DefaultComboBoxModel<String>(
                        columns.toArray(new String[0])));
                nameColumnCombo.setModel(new DefaultComboBoxModel<String>(
                        columns.toArray(new String[0])));
                ratingColumnCombo.setModel(new DefaultComboBoxModel<String>(
                        columns.toArray(new String[0])));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
    }

    /**
     * Aktualizuje ratingy pouzivatelov podla odpovede zo sutaze.
     */
    private void updateRatings() {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost request = new HttpPost("http://" + ipField.getText().trim()
                + ":" + portSpinner.getValue() + "/Results/");
        try {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("password", remotePassword));
            request.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            HttpResponse response = httpclient.execute(request);
            String responseString = EntityUtils.toString(response.getEntity(),
                    "UTF-8");
            try (Scanner s = new Scanner(responseString)) {
                while (s.hasNextLine()) {
                    String line = s.nextLine().trim();
                    if (line.length() == 0)
                        continue;

                    try (Scanner ls = new Scanner(line)) {
                        ls.useDelimiter("\t");
                        updateRatingForContestant(ls.next(), ls.next());
                    }
                }
            }

            playersTableModel.fireTableDataChanged();
            JOptionPane.showMessageDialog(this,
                    "Výsledkovka bola aktualizovaná.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Odoslanie požiadavky sa nepodarilo.");
        } finally {
            request.releaseConnection();
        }
    }

    /**
     * Aktualizuje rating sutaziaceho.
     */
    private void updateRatingForContestant(String contestantName, String rating) {
        if ((nameColumnCombo.getSelectedItem() == null)
                || (ratingColumnCombo.getSelectedItem() == null))
            return;

        int nameColumnIdx = playersTableModel.findColumn(nameColumnCombo
                .getSelectedItem().toString());
        int ratingIdColumnIdx = playersTableModel.findColumn(ratingColumnCombo
                .getSelectedItem().toString());
        if ((nameColumnIdx < 0) || (ratingIdColumnIdx < 0))
            return;

        for (int row = 0; row < playersTableModel.getRowCount(); row++) {
            if (contestantName.equals(playersTableModel.getValueAt(row,
                    nameColumnIdx))) {
                playersTableModel.setValueAt(rating, row, ratingIdColumnIdx);
            }
        }
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 472, 506);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        JPanel cardReaderPanel = new JPanel();

        JLabel label = new JLabel("\u010C\u00EDta\u010Dka:");

        cardReaderCombo = new JComboBox<String>();
        cardReaderCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                terminalChanged();
            }
        });

        JLabel label_1 = new JLabel("\u010C\u00EDslo karty:");

        cardIDField = new JTextField();
        cardIDField.setText("");
        cardIDField.setEditable(false);
        cardIDField.setColumns(10);
        GroupLayout gl_cardReaderPanel = new GroupLayout(cardReaderPanel);
        gl_cardReaderPanel
                .setHorizontalGroup(gl_cardReaderPanel
                        .createParallelGroup(Alignment.LEADING)
                        .addGroup(
                                gl_cardReaderPanel
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(
                                                gl_cardReaderPanel
                                                        .createParallelGroup(
                                                                Alignment.LEADING)
                                                        .addComponent(label_1)
                                                        .addComponent(label))
                                        .addPreferredGap(
                                                ComponentPlacement.RELATED)
                                        .addGroup(
                                                gl_cardReaderPanel
                                                        .createParallelGroup(
                                                                Alignment.LEADING)
                                                        .addComponent(
                                                                cardReaderCombo,
                                                                0, 417,
                                                                Short.MAX_VALUE)
                                                        .addComponent(
                                                                cardIDField,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                417,
                                                                Short.MAX_VALUE))
                                        .addContainerGap()));
        gl_cardReaderPanel
                .setVerticalGroup(gl_cardReaderPanel
                        .createParallelGroup(Alignment.LEADING)
                        .addGroup(
                                gl_cardReaderPanel
                                        .createSequentialGroup()
                                        .addGap(11)
                                        .addGroup(
                                                gl_cardReaderPanel
                                                        .createParallelGroup(
                                                                Alignment.BASELINE)
                                                        .addComponent(
                                                                cardReaderCombo,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(label))
                                        .addPreferredGap(
                                                ComponentPlacement.UNRELATED)
                                        .addGroup(
                                                gl_cardReaderPanel
                                                        .createParallelGroup(
                                                                Alignment.BASELINE)
                                                        .addComponent(label_1)
                                                        .addComponent(
                                                                cardIDField,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.PREFERRED_SIZE))
                                        .addContainerGap(
                                                GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE)));
        cardReaderPanel.setLayout(gl_cardReaderPanel);

        JPanel contestPanel = new JPanel();
        contestPanel.setBorder(new TitledBorder(UIManager
                .getBorder("TitledBorder.border"), "S\u00FA\u0165a\u017E",
                TitledBorder.LEADING, TitledBorder.TOP, null, Color.BLUE));

        JPanel winnerLoserPanel = new JPanel();

        JPanel tablePanel = new JPanel();
        GroupLayout gl_contentPane = new GroupLayout(contentPane);
        gl_contentPane.setHorizontalGroup(gl_contentPane
                .createParallelGroup(Alignment.LEADING)
                .addComponent(contestPanel, GroupLayout.DEFAULT_SIZE, 457,
                        Short.MAX_VALUE)
                .addComponent(tablePanel, GroupLayout.DEFAULT_SIZE, 457,
                        Short.MAX_VALUE)
                .addComponent(winnerLoserPanel, GroupLayout.PREFERRED_SIZE,
                        398, Short.MAX_VALUE)
                .addComponent(cardReaderPanel, GroupLayout.DEFAULT_SIZE, 398,
                        Short.MAX_VALUE));
        gl_contentPane.setVerticalGroup(gl_contentPane.createParallelGroup(
                Alignment.LEADING).addGroup(
                gl_contentPane
                        .createSequentialGroup()
                        .addComponent(contestPanel, GroupLayout.PREFERRED_SIZE,
                                57, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(cardReaderPanel,
                                GroupLayout.PREFERRED_SIZE, 73,
                                GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(winnerLoserPanel,
                                GroupLayout.PREFERRED_SIZE, 68,
                                GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(tablePanel, GroupLayout.DEFAULT_SIZE,
                                476, Short.MAX_VALUE)));

        JLabel label_2 = new JLabel("\u010C\u00EDslo karty:");

        cardColumnCombo = new JComboBox<String>();

        JButton button = new JButton("Na\u010D\u00EDta\u0165");
        button.setIcon(new ImageIcon(MainFrame.class
                .getResource("/images/open.png")));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadTableWithPlayers();
            }
        });

        JButton button_1 = new JButton("Ulo\u017Ei\u0165");
        button_1.setIcon(new ImageIcon(MainFrame.class
                .getResource("/images/save.png")));
        button_1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveTableWithPlayers();
            }
        });

        final JButton readResultsButton = new JButton(
                "Na\u010D\u00EDta\u0165 v\u00FDsledkovku");
        readResultsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateRatings();
            }
        });

        ratingColumnCombo = new JComboBox<String>();
        ratingColumnCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object selected = ratingColumnCombo.getSelectedItem();
                readResultsButton.setEnabled((selected != null)
                        && (!selected.toString().trim().equals("")));
            }
        });

        nameColumnCombo = new JComboBox<String>();

        JLabel label_3 = new JLabel("Meno \u00FA\u010Dastn\u00EDka:");

        JLabel label_4 = new JLabel("Rating:");

        JScrollPane scrollPane = new JScrollPane();
        GroupLayout gl_tablePanel = new GroupLayout(tablePanel);
        gl_tablePanel
                .setHorizontalGroup(gl_tablePanel
                        .createParallelGroup(Alignment.LEADING)
                        .addGroup(
                                gl_tablePanel
                                        .createSequentialGroup()
                                        .addGroup(
                                                gl_tablePanel
                                                        .createParallelGroup(
                                                                Alignment.LEADING)
                                                        .addGroup(
                                                                gl_tablePanel
                                                                        .createSequentialGroup()
                                                                        .addGap(10)
                                                                        .addComponent(
                                                                                label_2,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                54,
                                                                                GroupLayout.PREFERRED_SIZE)
                                                                        .addGap(34)
                                                                        .addComponent(
                                                                                cardColumnCombo,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                123,
                                                                                GroupLayout.PREFERRED_SIZE)
                                                                        .addGap(29)
                                                                        .addComponent(
                                                                                button,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                129,
                                                                                GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(
                                                                gl_tablePanel
                                                                        .createSequentialGroup()
                                                                        .addGap(10)
                                                                        .addComponent(
                                                                                label_3,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                78,
                                                                                GroupLayout.PREFERRED_SIZE)
                                                                        .addGap(10)
                                                                        .addComponent(
                                                                                nameColumnCombo,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                123,
                                                                                GroupLayout.PREFERRED_SIZE)
                                                                        .addGap(29)
                                                                        .addComponent(
                                                                                button_1,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                129,
                                                                                GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(
                                                                gl_tablePanel
                                                                        .createSequentialGroup()
                                                                        .addGap(10)
                                                                        .addComponent(
                                                                                label_4,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                35,
                                                                                GroupLayout.PREFERRED_SIZE)
                                                                        .addGap(53)
                                                                        .addComponent(
                                                                                ratingColumnCombo,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                123,
                                                                                GroupLayout.PREFERRED_SIZE)
                                                                        .addGap(29)
                                                                        .addComponent(
                                                                                readResultsButton,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                129,
                                                                                GroupLayout.PREFERRED_SIZE)))
                                        .addContainerGap(78, Short.MAX_VALUE))
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE,
                                457, Short.MAX_VALUE));
        gl_tablePanel
                .setVerticalGroup(gl_tablePanel
                        .createParallelGroup(Alignment.LEADING)
                        .addGroup(
                                gl_tablePanel
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addGap(11)
                                        .addGroup(
                                                gl_tablePanel
                                                        .createParallelGroup(
                                                                Alignment.LEADING)
                                                        .addGroup(
                                                                gl_tablePanel
                                                                        .createSequentialGroup()
                                                                        .addGap(4)
                                                                        .addComponent(
                                                                                label_2))
                                                        .addGroup(
                                                                gl_tablePanel
                                                                        .createSequentialGroup()
                                                                        .addGap(1)
                                                                        .addComponent(
                                                                                cardColumnCombo,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                GroupLayout.DEFAULT_SIZE,
                                                                                GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(button))
                                        .addGap(6)
                                        .addGroup(
                                                gl_tablePanel
                                                        .createParallelGroup(
                                                                Alignment.LEADING)
                                                        .addGroup(
                                                                gl_tablePanel
                                                                        .createSequentialGroup()
                                                                        .addGap(4)
                                                                        .addComponent(
                                                                                label_3))
                                                        .addGroup(
                                                                gl_tablePanel
                                                                        .createSequentialGroup()
                                                                        .addGap(1)
                                                                        .addComponent(
                                                                                nameColumnCombo,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                GroupLayout.DEFAULT_SIZE,
                                                                                GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(button_1))
                                        .addGap(6)
                                        .addGroup(
                                                gl_tablePanel
                                                        .createParallelGroup(
                                                                Alignment.LEADING)
                                                        .addGroup(
                                                                gl_tablePanel
                                                                        .createSequentialGroup()
                                                                        .addGap(4)
                                                                        .addComponent(
                                                                                label_4))
                                                        .addGroup(
                                                                gl_tablePanel
                                                                        .createSequentialGroup()
                                                                        .addGap(1)
                                                                        .addComponent(
                                                                                ratingColumnCombo,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                GroupLayout.DEFAULT_SIZE,
                                                                                GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(
                                                                readResultsButton))
                                        .addGap(18)
                                        .addComponent(scrollPane,
                                                GroupLayout.DEFAULT_SIZE, 167,
                                                Short.MAX_VALUE)));

        playersTable = new JTable();
        scrollPane.setViewportView(playersTable);
        tablePanel.setLayout(gl_tablePanel);

        JLabel lblVaz = new JLabel("V\u00ED\u0165az:");

        winnerField = new JTextField();
        winnerField.setEditable(false);
        winnerField.setColumns(10);

        JLabel lblPorazen = new JLabel("Porazen\u00FD:");

        loserField = new JTextField();
        loserField.setEditable(false);
        loserField.setColumns(10);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                winnerField.setText("");
                loserField.setText("");
            }
        });

        prefixLookupCheckBox = new JCheckBox("Prefixové hľadanie");
        GroupLayout gl_winnerLoserPanel = new GroupLayout(winnerLoserPanel);
        gl_winnerLoserPanel
                .setHorizontalGroup(gl_winnerLoserPanel
                        .createParallelGroup(Alignment.LEADING)
                        .addGroup(
                                gl_winnerLoserPanel
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(
                                                gl_winnerLoserPanel
                                                        .createParallelGroup(
                                                                Alignment.LEADING)
                                                        .addComponent(lblVaz)
                                                        .addComponent(
                                                                lblPorazen))
                                        .addGap(10)
                                        .addGroup(
                                                gl_winnerLoserPanel
                                                        .createParallelGroup(
                                                                Alignment.LEADING,
                                                                false)
                                                        .addComponent(
                                                                loserField,
                                                                Alignment.TRAILING)
                                                        .addComponent(
                                                                winnerField,
                                                                Alignment.TRAILING,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                152,
                                                                Short.MAX_VALUE))
                                        .addPreferredGap(
                                                ComponentPlacement.RELATED)
                                        .addComponent(resetButton)
                                        .addPreferredGap(
                                                ComponentPlacement.RELATED)
                                        .addComponent(prefixLookupCheckBox)
                                        .addGap(53)));
        gl_winnerLoserPanel
                .setVerticalGroup(gl_winnerLoserPanel
                        .createParallelGroup(Alignment.LEADING)
                        .addGroup(
                                gl_winnerLoserPanel
                                        .createSequentialGroup()
                                        .addGroup(
                                                gl_winnerLoserPanel
                                                        .createParallelGroup(
                                                                Alignment.LEADING)
                                                        .addGroup(
                                                                gl_winnerLoserPanel
                                                                        .createSequentialGroup()
                                                                        .addContainerGap()
                                                                        .addGroup(
                                                                                gl_winnerLoserPanel
                                                                                        .createParallelGroup(
                                                                                                Alignment.BASELINE)
                                                                                        .addComponent(
                                                                                                lblVaz)
                                                                                        .addComponent(
                                                                                                winnerField,
                                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                                GroupLayout.DEFAULT_SIZE,
                                                                                                GroupLayout.PREFERRED_SIZE))
                                                                        .addPreferredGap(
                                                                                ComponentPlacement.RELATED)
                                                                        .addGroup(
                                                                                gl_winnerLoserPanel
                                                                                        .createParallelGroup(
                                                                                                Alignment.BASELINE)
                                                                                        .addComponent(
                                                                                                loserField,
                                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                                GroupLayout.DEFAULT_SIZE,
                                                                                                GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(
                                                                                                lblPorazen)))
                                                        .addGroup(
                                                                gl_winnerLoserPanel
                                                                        .createSequentialGroup()
                                                                        .addGap(22)
                                                                        .addGroup(
                                                                                gl_winnerLoserPanel
                                                                                        .createParallelGroup(
                                                                                                Alignment.BASELINE)
                                                                                        .addComponent(
                                                                                                resetButton)
                                                                                        .addComponent(
                                                                                                prefixLookupCheckBox))))
                                        .addContainerGap(
                                                GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE)));
        winnerLoserPanel.setLayout(gl_winnerLoserPanel);

        JLabel lblIpAdresa = new JLabel("IP adresa:");

        ipField = new JTextField();
        ipField.setText("127.0.0.1");
        ipField.setColumns(10);

        JLabel lblPort = new JLabel("Port:");

        portSpinner = new JSpinner();
        portSpinner.setModel(new SpinnerNumberModel(8080, 1, 65535, 1));
        portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"));

        JButton passwordChangeButton = new JButton("Zmena hesla");
        passwordChangeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String result = (String) JOptionPane.showInputDialog(
                        MainFrame.this, "Heslo na vzdialené ovládanie:",
                        "Nastavenie hesla", JOptionPane.INFORMATION_MESSAGE,
                        null, null, remotePassword);
                if (result != null)
                    remotePassword = result;
            }
        });
        GroupLayout gl_contestPanel = new GroupLayout(contestPanel);
        gl_contestPanel.setHorizontalGroup(gl_contestPanel.createParallelGroup(
                Alignment.LEADING).addGroup(
                gl_contestPanel
                        .createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblIpAdresa)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(ipField, GroupLayout.PREFERRED_SIZE, 118,
                                GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(lblPort)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(portSpinner, GroupLayout.PREFERRED_SIZE,
                                57, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(passwordChangeButton)
                        .addContainerGap(111, Short.MAX_VALUE)));
        gl_contestPanel
                .setVerticalGroup(gl_contestPanel
                        .createParallelGroup(Alignment.LEADING)
                        .addGroup(
                                gl_contestPanel
                                        .createSequentialGroup()
                                        .addGroup(
                                                gl_contestPanel
                                                        .createParallelGroup(
                                                                Alignment.BASELINE)
                                                        .addComponent(
                                                                lblIpAdresa)
                                                        .addComponent(
                                                                ipField,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lblPort)
                                                        .addComponent(
                                                                portSpinner,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(
                                                                passwordChangeButton))
                                        .addContainerGap(14, Short.MAX_VALUE)));
        contestPanel.setLayout(gl_contestPanel);
        contentPane.setLayout(gl_contentPane);
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
