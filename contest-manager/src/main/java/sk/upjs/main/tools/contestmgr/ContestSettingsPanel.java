package sk.upjs.main.tools.contestmgr;

import javax.swing.*;
import java.awt.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.Font;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

@SuppressWarnings("serial")
public class ContestSettingsPanel extends JPanel {

	private Contest contest;
	private MainFrame mainFrame;

	private JSpinner hoursSpinner;
	private JSpinner minutesSpinner;
	private JSpinner secondsSpinner;
	private JCheckBox timerEnabledCheck;
	private JSpinner initialRatingSpinner;
	private JSpinner aSpinner;
	private JSpinner bSpinner;
	private JSpinner cSpinner;
	private JFileChooser fc;
	private JSpinner portSpinner;

	private String remotePassword = "";
	private JCheckBox allowRemoteControlBox;

	/**
	 * Vytvori panel (bezparametrovy konstruktor len pre GUI editor)
	 */
	public ContestSettingsPanel() {
		initComponents();
		fc = new JFileChooser();
		fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
	}

	/**
	 * Vytvori panel asociovany s hlavnym frameom, pre ktory manazuje
	 * nastavovanie parametrov sutaze.
	 */
	public ContestSettingsPanel(MainFrame frame) {
		this();
		this.mainFrame = frame;
		contest = frame.getContest();
		loadContestParameters();
	}

	/**
	 * Nastavi komponenty panelu podla parametrov sutaze.
	 */
	private void loadContestParameters() {
		timerEnabledCheck.setSelected(contest.isTimerEnabled());

		int duration = contest.getDuration();
		hoursSpinner.setValue(duration / 3600);
		duration %= 3600;
		minutesSpinner.setValue(duration / 60);
		duration %= 60;
		secondsSpinner.setValue(duration);

		initialRatingSpinner.setValue(contest.getInitialRating());
		aSpinner.setValue(contest.getParameterA());
		bSpinner.setValue(contest.getParameterB());
		cSpinner.setValue(contest.getParameterC());

		allowRemoteControlBox.setSelected(contest.isRemoveControlEnabled());
		portSpinner.setValue(contest.getRcPort());
		remotePassword = contest.getRcPassword();

		timerEnabledChanged();
	}

	/**
	 * Nastavi parametre sutaze podla aktualneho nastavenia komponentov panelu.
	 */
	private void storeContestParameters() {
		contest.setTimerEnabled(timerEnabledCheck.isSelected());
		contest.setDuration((Integer) hoursSpinner.getValue() * 3600
				+ (Integer) minutesSpinner.getValue() * 60
				+ (Integer) secondsSpinner.getValue());
		contest.setInitialRating((Integer) initialRatingSpinner.getValue());
		contest.setParameterA((Integer) aSpinner.getValue());
		contest.setParameterB((Integer) bSpinner.getValue());
		contest.setParameterC((Integer) cSpinner.getValue());

		contest.setRemoveControlEnabled(allowRemoteControlBox.isSelected());
		contest.setRcPort((Integer) portSpinner.getValue());
		contest.setRcPassword(remotePassword);
	}

	/**
	 * Aktualizuje aktivitu komponentov nastavujucich trvanie sutaze.
	 */
	private void timerEnabledChanged() {
		hoursSpinner.setEnabled(timerEnabledCheck.isSelected());
		minutesSpinner.setEnabled(timerEnabledCheck.isSelected());
		secondsSpinner.setEnabled(timerEnabledCheck.isSelected());
	}

	/**
	 * Ulozi parametre sutaze do suboru.
	 */
	private void saveSettingsToFile() {
		int returnVal = fc.showSaveDialog(this.getRootPane().getParent());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			storeContestParameters();
			contest.saveToFile(fc.getSelectedFile());
		}
	}

	/**
	 * Nacita parametre sutaze zo suboru.
	 */
	private void loadSettingsFromFile() {
		int returnVal = fc.showOpenDialog(this.getRootPane().getParent());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			contest.loadFromFile(fc.getSelectedFile());
			loadContestParameters();
			mainFrame.refreshContestSettings();
		}
	}

	/**
	 * Inicializuje komponenty panelu.
	 */
	private void initComponents() {
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(UIManager
				.getBorder("TitledBorder.border"),
				"Trvanie s\u00FA\u0165a\u017Ee", TitledBorder.LEADING,
				TitledBorder.TOP, null, Color.BLUE));

		hoursSpinner = new JSpinner();
		hoursSpinner.setModel(new SpinnerNumberModel(0, 0, 9, 1));

		minutesSpinner = new JSpinner();
		minutesSpinner.setModel(new SpinnerNumberModel(0, 0, 59, 1));

		secondsSpinner = new JSpinner();
		secondsSpinner.setModel(new SpinnerNumberModel(0, 0, 59, 1));

		JLabel lblNewLabel = new JLabel(":");
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 14));

		JLabel label = new JLabel(":");
		label.setFont(new Font("Tahoma", Font.BOLD, 14));

		timerEnabledCheck = new JCheckBox("");
		timerEnabledCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				timerEnabledChanged();
			}
		});
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(
				Alignment.LEADING).addGroup(
				gl_panel.createSequentialGroup()
						.addContainerGap()
						.addComponent(timerEnabledCheck,
								GroupLayout.PREFERRED_SIZE, 22,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(hoursSpinner, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addGap(2)
						.addComponent(label, GroupLayout.PREFERRED_SIZE, 5,
								GroupLayout.PREFERRED_SIZE)
						.addGap(2)
						.addComponent(minutesSpinner,
								GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addGap(2)
						.addComponent(lblNewLabel)
						.addGap(2)
						.addComponent(secondsSpinner,
								GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addContainerGap(278, Short.MAX_VALUE)));
		gl_panel.setVerticalGroup(gl_panel
				.createParallelGroup(Alignment.LEADING)
				.addComponent(timerEnabledCheck, GroupLayout.PREFERRED_SIZE,
						19, GroupLayout.PREFERRED_SIZE)
				.addGroup(
						gl_panel.createParallelGroup(Alignment.BASELINE)
								.addComponent(hoursSpinner,
										GroupLayout.PREFERRED_SIZE,
										GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(label)
								.addComponent(minutesSpinner,
										GroupLayout.PREFERRED_SIZE,
										GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblNewLabel)
								.addComponent(secondsSpinner,
										GroupLayout.PREFERRED_SIZE,
										GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)));
		panel.setLayout(gl_panel);

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(UIManager
				.getBorder("TitledBorder.border"), "Parametre",
				TitledBorder.LEADING, TitledBorder.TOP, null, Color.BLUE));

		JButton saveButton = new JButton("Ulo\u017Ei\u0165");
		saveButton.setIcon(new ImageIcon(ContestSettingsPanel.class
				.getResource("/images/save.png")));
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveSettingsToFile();
			}
		});

		JButton loadButton = new JButton("Na\u010D\u00EDta\u0165");
		loadButton.setIcon(new ImageIcon(ContestSettingsPanel.class
				.getResource("/images/open.png")));
		loadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadSettingsFromFile();
			}
		});

		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new TitledBorder(UIManager
				.getBorder("TitledBorder.border"),
				"Vzdialen\u00E9 ovl\u00E1danie", TitledBorder.LEADING,
				TitledBorder.TOP, null, Color.BLUE));

		JPanel panel_4 = new JPanel();
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout
				.createParallelGroup(Alignment.LEADING)
				.addComponent(panel_1, GroupLayout.DEFAULT_SIZE, 456,
						Short.MAX_VALUE)
				.addComponent(panel, GroupLayout.DEFAULT_SIZE, 456,
						Short.MAX_VALUE)
				.addComponent(panel_3, GroupLayout.DEFAULT_SIZE, 456,
						Short.MAX_VALUE)
				.addGroup(
						groupLayout.createSequentialGroup().addContainerGap()
								.addComponent(saveButton)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(loadButton)
								.addContainerGap(263, Short.MAX_VALUE))
				.addComponent(panel_4, GroupLayout.DEFAULT_SIZE, 456,
						Short.MAX_VALUE));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(
				Alignment.LEADING)
				.addGroup(
						groupLayout
								.createSequentialGroup()
								.addComponent(panel,
										GroupLayout.PREFERRED_SIZE,
										GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(panel_1,
										GroupLayout.PREFERRED_SIZE, 180,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(panel_3,
										GroupLayout.PREFERRED_SIZE, 83,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addGroup(
										groupLayout
												.createParallelGroup(
														Alignment.BASELINE)
												.addComponent(saveButton)
												.addComponent(loadButton))
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(panel_4,
										GroupLayout.PREFERRED_SIZE, 39,
										Short.MAX_VALUE)));

		JButton startButton = new JButton("Začať súťaž");
		panel_4.add(startButton);
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				storeContestParameters();
				mainFrame.changeToContestView();
			}
		});
		startButton.setForeground(Color.BLACK);
		startButton.setFont(new Font("Arial Black", startButton.getFont()
				.getStyle(), 15));

		allowRemoteControlBox = new JCheckBox(
				"Povoliť vzdialené ovládanie na porte");
		allowRemoteControlBox.setSelected(true);

		portSpinner = new JSpinner();
		portSpinner.setModel(new SpinnerNumberModel(8080, 1, 65535, 1));
		portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"));

		JButton passwordButton = new JButton("Nastaviť heslo");
		passwordButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String result = (String) JOptionPane.showInputDialog(
						ContestSettingsPanel.this.getRootPane().getParent(),
						"Heslo na vzdialené ovládanie:", "Nastavenie hesla",
						JOptionPane.INFORMATION_MESSAGE, null, null,
						remotePassword);
				if (result != null)
					remotePassword = result;
			}
		});
		GroupLayout gl_panel_3 = new GroupLayout(panel_3);
		gl_panel_3
				.setHorizontalGroup(gl_panel_3
						.createParallelGroup(Alignment.LEADING)
						.addGroup(
								gl_panel_3
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												gl_panel_3
														.createParallelGroup(
																Alignment.LEADING)
														.addGroup(
																gl_panel_3
																		.createSequentialGroup()
																		.addComponent(
																				allowRemoteControlBox)
																		.addPreferredGap(
																				ComponentPlacement.RELATED)
																		.addComponent(
																				portSpinner,
																				GroupLayout.PREFERRED_SIZE,
																				60,
																				GroupLayout.PREFERRED_SIZE))
														.addComponent(
																passwordButton))
										.addContainerGap(183, Short.MAX_VALUE)));
		gl_panel_3
				.setVerticalGroup(gl_panel_3
						.createParallelGroup(Alignment.LEADING)
						.addGroup(
								gl_panel_3
										.createSequentialGroup()
										.addGroup(
												gl_panel_3
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																allowRemoteControlBox)
														.addComponent(
																portSpinner,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addComponent(passwordButton)
										.addContainerGap(
												GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)));
		panel_3.setLayout(gl_panel_3);

		JLabel lblNewLabel_1 = new JLabel("Po\u010Diato\u010Dn\u00FD rating:");

		initialRatingSpinner = new JSpinner();
		initialRatingSpinner.setModel(new SpinnerNumberModel(new Integer(1000),
				null, null, new Integer(1)));

		JLabel lblNewLabel_2 = new JLabel("Vzorec zmeny ratingu:");

		JLabel lblNewLabel_3 = new JLabel(
				"a-b*(rating vyhr\u00E1vaj\u00FAceho - rating prehr\u00E1vaj\u00FAceho) / c");
		lblNewLabel_3.setFont(new Font("Times New Roman", Font.ITALIC, 13));
		lblNewLabel_3.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel panel_2 = new JPanel();
		GroupLayout gl_panel_1 = new GroupLayout(panel_1);
		gl_panel_1
				.setHorizontalGroup(gl_panel_1
						.createParallelGroup(Alignment.LEADING)
						.addGroup(
								gl_panel_1
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												gl_panel_1
														.createParallelGroup(
																Alignment.LEADING)
														.addComponent(
																panel_2,
																Alignment.TRAILING,
																GroupLayout.DEFAULT_SIZE,
																414,
																Short.MAX_VALUE)
														.addComponent(
																lblNewLabel_3,
																GroupLayout.DEFAULT_SIZE,
																414,
																Short.MAX_VALUE)
														.addGroup(
																gl_panel_1
																		.createSequentialGroup()
																		.addComponent(
																				lblNewLabel_1)
																		.addPreferredGap(
																				ComponentPlacement.RELATED)
																		.addComponent(
																				initialRatingSpinner,
																				GroupLayout.PREFERRED_SIZE,
																				68,
																				GroupLayout.PREFERRED_SIZE))
														.addComponent(
																lblNewLabel_2))
										.addContainerGap()));
		gl_panel_1
				.setVerticalGroup(gl_panel_1
						.createParallelGroup(Alignment.LEADING)
						.addGroup(
								gl_panel_1
										.createSequentialGroup()
										.addGroup(
												gl_panel_1
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblNewLabel_1)
														.addComponent(
																initialRatingSpinner,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addComponent(lblNewLabel_2)
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addComponent(lblNewLabel_3)
										.addGap(10)
										.addComponent(panel_2,
												GroupLayout.PREFERRED_SIZE, 79,
												GroupLayout.PREFERRED_SIZE)
										.addContainerGap(115, Short.MAX_VALUE)));
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[] { 0, 0, 0 };
		gbl_panel_2.rowHeights = new int[] { 0, 0, 0, 0 };
		gbl_panel_2.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		gbl_panel_2.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panel_2.setLayout(gbl_panel_2);

		JLabel lblParameterA = new JLabel("Parameter a:");
		GridBagConstraints gbc_lblParameterA = new GridBagConstraints();
		gbc_lblParameterA.insets = new Insets(0, 0, 5, 5);
		gbc_lblParameterA.gridx = 0;
		gbc_lblParameterA.gridy = 0;
		panel_2.add(lblParameterA, gbc_lblParameterA);

		aSpinner = new JSpinner();
		aSpinner.setPreferredSize(new Dimension(50, 20));
		GridBagConstraints gbc_aSpinner = new GridBagConstraints();
		gbc_aSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_aSpinner.gridx = 1;
		gbc_aSpinner.gridy = 0;
		panel_2.add(aSpinner, gbc_aSpinner);
		aSpinner.setModel(new SpinnerNumberModel(new Integer(100), null, null,
				new Integer(1)));

		JLabel lblParameterB = new JLabel("Parameter b:");
		GridBagConstraints gbc_lblParameterB = new GridBagConstraints();
		gbc_lblParameterB.insets = new Insets(0, 0, 5, 5);
		gbc_lblParameterB.gridx = 0;
		gbc_lblParameterB.gridy = 1;
		panel_2.add(lblParameterB, gbc_lblParameterB);

		bSpinner = new JSpinner();
		bSpinner.setPreferredSize(new Dimension(50, 20));
		GridBagConstraints gbc_bSpinner = new GridBagConstraints();
		gbc_bSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_bSpinner.gridx = 1;
		gbc_bSpinner.gridy = 1;
		panel_2.add(bSpinner, gbc_bSpinner);
		bSpinner.setModel(new SpinnerNumberModel(new Integer(1), null, null,
				new Integer(1)));

		JLabel lblParameterC = new JLabel("Parameter c:");
		GridBagConstraints gbc_lblParameterC = new GridBagConstraints();
		gbc_lblParameterC.insets = new Insets(0, 0, 0, 5);
		gbc_lblParameterC.gridx = 0;
		gbc_lblParameterC.gridy = 2;
		panel_2.add(lblParameterC, gbc_lblParameterC);

		cSpinner = new JSpinner();
		cSpinner.setPreferredSize(new Dimension(50, 20));
		GridBagConstraints gbc_cSpinner = new GridBagConstraints();
		gbc_cSpinner.gridx = 1;
		gbc_cSpinner.gridy = 2;
		panel_2.add(cSpinner, gbc_cSpinner);
		cSpinner.setModel(new SpinnerNumberModel(new Integer(3), null, null,
				new Integer(1)));
		panel_1.setLayout(gl_panel_1);
		setLayout(groupLayout);
	}
}
