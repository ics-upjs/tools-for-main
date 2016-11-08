package sk.upjs.main.tools.contestmgr;

import java.util.*;
import javax.swing.*;
import java.io.*;

import javax.swing.GroupLayout.Alignment;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Font;
import java.awt.Dimension;

@SuppressWarnings("serial")
public class ContestOperatorPanel extends JPanel {

	/**
	 * Kodovanie suborov na ulozenie priebehu hry.
	 */
	private static final String FILE_ENCODING = "UTF-8";

	/**
	 * TableModel zobrazujuci historiu duelov
	 */
	private class DuelsTableModel extends AbstractTableModel {
		// ID stlpca s casom duelu
		private static final int TIME_COLUMN = 0;

		// ID stlpca s menom vitaza
		private static final int WINNER_COLUMN = 1;

		// ID stlpca s meno prehravajuceho
		private static final int LOSER_COLUMN = 2;

		/**
		 * Zobrazeny zoznam duelov
		 */
		private List<Contest.Duel> duels = new ArrayList<Contest.Duel>();

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public int getRowCount() {
			return duels.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int colIndex) {
			int duelIndex = duels.size() - 1 - rowIndex;
			if (colIndex == TIME_COLUMN)
				return Contest.formatTime(duels.get(duelIndex).time);

			if (colIndex == WINNER_COLUMN)
				return duels.get(duelIndex).winner.getName();

			if (colIndex == LOSER_COLUMN)
				return duels.get(duelIndex).loser.getName();

			return "";
		}

		@Override
		public String getColumnName(int colIndex) {
			if (colIndex == TIME_COLUMN) {
				return "Čas";
			}

			if (colIndex == WINNER_COLUMN) {
				return "Víťaz";
			}

			if (colIndex == LOSER_COLUMN) {
				return "Porazený";
			}

			return "";
		}

		public void updateData() {
			duels = contest.getDuels();
			fireTableDataChanged();
		}
	}

	private MainFrame mainFrame;
	private Contest contest;
	private DuelsTableModel dtm;

	private JComboBox<String> winnerCombo;
	private JComboBox<String> loserCombo;
	private JButton btnAddDuel;
	private JTable duelsTable;
	private JButton stopButton;
	private JButton pauseButton;
	private JButton playButton;
	private JButton btnRemoveLastDuel;
	private File lastSaveFile;
	private JCheckBox saveContinuouslyCheckBox;

	/**
	 * Vytvori panel operatora sutaze.
	 */
	public ContestOperatorPanel() {
		initComponents();
	}

	public ContestOperatorPanel(MainFrame frame) {
		this();
		mainFrame = frame;
		contest = mainFrame.getContest();

		contest.addContestListener(new Contest.ContestListener() {
			@Override
			public void onContestChange() {
				updateDuelHistory();

				// Pri kazdej zmene ulozime priebeh hry
				if ((lastSaveFile != null) && saveContinuouslyCheckBox.isEnabled()
						&& saveContinuouslyCheckBox.isSelected()) {
					saveContestResults(lastSaveFile);
				}
			}

			@Override
			public void onContestTimeChange() {
				// nic na pracu
			}
		});

		// Vytvorime a nastavime model pre zobrazovanie historie duelov
		dtm = new DuelsTableModel();
		duelsTable.setModel(dtm);
		updateDuelHistory();
	}

	/**
	 * Aktualizuje zobrazenie panela podla prebiehajucej sutaze
	 */
	public void updateByContest() {
		// Pripravime udaje podla contestu a naplnime comboboxy
		List<String> values = new ArrayList<String>();
		values.add("");
		for (int i = 0; i < contest.getContestantsCount(); i++)
			values.add(contest.getContestant(i).getName());

		winnerCombo.setModel(new DefaultComboBoxModel<String>(values.toArray(new String[0])));
		loserCombo.setModel(new DefaultComboBoxModel<String>(values.toArray(new String[0])));

		updateAddDuelResultButton();

		// Nastavime tlacidla na ovladanie priebehu sutaze
		playButton.setEnabled(contest.isPaused());
		pauseButton.setEnabled(!contest.isPaused());
		lastSaveFile = null;
		saveContinuouslyCheckBox.setEnabled(false);
	}

	/**
	 * Nastavi aktivitu tlacidla na pridanie vysledku duelu
	 */
	private void updateAddDuelResultButton() {
		btnAddDuel.setEnabled((winnerCombo.getSelectedIndex() >= 1) && (loserCombo.getSelectedIndex() >= 1)
				&& (loserCombo.getSelectedIndex() != winnerCombo.getSelectedIndex()));
	}

	/**
	 * Aktualizuje komponenty zobrazujuce historiu duelov
	 */
	private void updateDuelHistory() {
		dtm.updateData();
		btnRemoveLastDuel.setEnabled(contest.getDuelsCount() != 0);
	}

	/**
	 * Prida vysledok duelu do sutaze
	 */
	private void addDuelResultClicked() {
		contest.addDuel(contest.getContestant(winnerCombo.getSelectedIndex() - 1),
				contest.getContestant(loserCombo.getSelectedIndex() - 1));
		winnerCombo.setSelectedIndex(0);
		loserCombo.setSelectedIndex(0);
		updateAddDuelResultButton();
	}

	/**
	 * Otvori nove okno na zobrazenie vysledkov sutaze
	 */
	private void openNewContestWindowClicked() {
		Frame f = new DetachedResultsFrame(contest);
		f.setVisible(true);
		f.setLocationRelativeTo(getRootPane().getParent());
	}

	/**
	 * Spracuje poziadavku na ulozenie priebehu sutaze.
	 */
	private void saveContestResultsClicked() {
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
		int returnVal = fc.showSaveDialog(this.getRootPane().getParent());
		if (returnVal != JFileChooser.APPROVE_OPTION)
			return;

		saveContestResults(fc.getSelectedFile());
		lastSaveFile = fc.getSelectedFile();
		saveContinuouslyCheckBox.setEnabled(true);
	}

	/**
	 * Ulozi priebeh sutaze do suboru.
	 */
	private void saveContestResults(File dstFile) {
		try (PrintWriter writer = new PrintWriter(dstFile, FILE_ENCODING)) {
			int nCountestants = contest.getContestantsCount();
			for (int i = 0; i < nCountestants; i++) {
				Contest.Contestant c = contest.getContestantAtRank(i);
				writer.println(c.getName() + "\t" + c.getRating());
			}
			writer.println("[Duels]");

			List<Contest.Duel> duels = contest.getDuels();
			for (Contest.Duel duel : duels) {
				writer.println(duel.winner.getName() + "\t" + duel.loser.getName() + "\t" + duel.time);
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Zápis výsledkov zlyhal.");
		}
	}

	/**
	 * Spracuje poziadavku na nacitanie priebehu sutaze.
	 */
	private void loadContestResultsClicked() {
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
		int returnValue = fc.showOpenDialog(this.getRootPane().getParent());
		if (returnValue != JFileChooser.APPROVE_OPTION)
			return;

		lastSaveFile = null;
		saveContinuouslyCheckBox.setEnabled(false);
		loadContestResults(fc.getSelectedFile());
	}

	/**
	 * Nacita priebeh sutaze zo suboru.
	 *
	 * @param srcFile
	 */
	private void loadContestResults(File srcFile) {
		boolean paused = contest.isPaused();
		contest.stop();
		contest.clear();

		try (Scanner fileScanner = new Scanner(srcFile, FILE_ENCODING)) {
			// Nacitame mena ucastnikov - riadky az po oddelovac historie duelov
			// [Duels]
			while (fileScanner.hasNextLine()) {
				String line = fileScanner.nextLine();
				if ("[Duels]".equals(line.trim())) {
					break;
				}

				try (Scanner contestantScanner = new Scanner(line)) {
					contestantScanner.useDelimiter("\t");
					if (!contestantScanner.hasNext()) {
						continue;
					}

					String contestantName = contestantScanner.next().trim();
					if (contestantName.isEmpty()) {
						continue;
					}

					contest.addNewContestant(contest.getContestantsCount());
					contest.getContestant(contest.getContestantsCount() - 1).setName(contestantName);
				}
			}

			contest.start();
			contest.pause();

			// Nacitame historiu duelov
			while (fileScanner.hasNextLine()) {
				try (Scanner duelScanner = new Scanner(fileScanner.nextLine())) {
					duelScanner.useDelimiter("\t");
					String winnerName = duelScanner.next().trim();
					String looserName = duelScanner.next().trim();
					int time = duelScanner.nextInt();
					contest.setTime(time);
					contest.addDuel(contest.getContestant(winnerName), contest.getContestant(looserName));
				}
			}

			contest.resume();
			contest.pause();
		} catch (Exception e) {

		}

		if (!paused) {
			contest.resume();
		}

		updateByContest();
	}

	/**
	 * Nastavi vysledok duelu.
	 */
	public void setDuelRemotely(String winner, String loser) {
		winnerCombo.setSelectedIndex(0);
		loserCombo.setSelectedIndex(0);

		setSelectedContestant(winner, winnerCombo);
		setSelectedContestant(loser, loserCombo);
	}

	/**
	 * Nastavi v combobox-e zvoleneho hraca.
	 */
	private void setSelectedContestant(String contestant, JComboBox<String> combo) {
		if (contestant == null)
			return;

		ComboBoxModel<String> model = combo.getModel();
		for (int i = 0; i < model.getSize(); i++)
			if (contestant.equals(model.getElementAt(i))) {
				combo.setSelectedIndex(i);
				break;
			}
	}

	/**
	 * Vrati sutaz riadenu tymto komponentom.
	 *
	 * @return
	 */
	public Contest getContest() {
		return contest;
	}

	/**
	 * Inicializuje komponenty
	 */
	private void initComponents() {

		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(null, "Pridanie v\u00FDsledku duelu", TitledBorder.LEADING, TitledBorder.TOP,
				null, Color.BLUE));

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(null, "Hist\u00F3ria duelov", TitledBorder.LEADING, TitledBorder.TOP, null,
				Color.BLUE));

		JPanel panel_2 = new JPanel();

		JPanel panel_3 = new JPanel();
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(panel_2, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
				.addComponent(panel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
				.addComponent(panel_1, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
				.addComponent(panel_3, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE));
		groupLayout
				.setVerticalGroup(
						groupLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(groupLayout.createSequentialGroup()
										.addComponent(panel_2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(panel, GroupLayout.PREFERRED_SIZE, 134,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(panel_1, GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.RELATED).addComponent(panel_3,
												GroupLayout.PREFERRED_SIZE, 99, GroupLayout.PREFERRED_SIZE)));

		JButton saveButton = new JButton("Uložiť priebeh");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				saveContestResultsClicked();
			}
		});
		saveButton.setIcon(new ImageIcon(ContestOperatorPanel.class.getResource("/images/save.png")));

		JButton statisticsButton = new JButton("Aktuálna štatistika");
		statisticsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				StatisticsFrame frame = new StatisticsFrame(contest);
				frame.setVisible(true);
				frame.setLocationRelativeTo(ContestOperatorPanel.this.getRootPane().getParent());
			}
		});
		statisticsButton.setIcon(new ImageIcon(ContestOperatorPanel.class.getResource("/images/statistics.png")));

		JButton loadButton = new JButton("Načítať priebeh");
		loadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				loadContestResultsClicked();
			}
		});
		loadButton.setPreferredSize(new Dimension(99, 23));
		loadButton.setIcon(new ImageIcon(ContestOperatorPanel.class.getResource("/images/open.png")));

		saveContinuouslyCheckBox = new JCheckBox("Ukladať priebežne");
		saveContinuouslyCheckBox.setSelected(true);
		GroupLayout gl_panel_3 = new GroupLayout(panel_3);
		gl_panel_3
				.setHorizontalGroup(
						gl_panel_3.createParallelGroup(Alignment.LEADING).addGroup(gl_panel_3.createSequentialGroup()
								.addContainerGap().addGroup(gl_panel_3
										.createParallelGroup(Alignment.LEADING)
										.addComponent(
												saveContinuouslyCheckBox)
										.addGroup(gl_panel_3.createSequentialGroup()
												.addGroup(gl_panel_3.createParallelGroup(Alignment.TRAILING, false)
														.addComponent(saveButton, GroupLayout.DEFAULT_SIZE,
																GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
														.addComponent(statisticsButton, GroupLayout.DEFAULT_SIZE,
																GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
												.addPreferredGap(ComponentPlacement.RELATED).addComponent(loadButton)))
								.addContainerGap(166, Short.MAX_VALUE)));
		gl_panel_3.setVerticalGroup(gl_panel_3.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_3.createSequentialGroup().addContainerGap()
						.addGroup(gl_panel_3.createParallelGroup(Alignment.BASELINE).addComponent(saveButton)
								.addComponent(loadButton))
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(saveContinuouslyCheckBox)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(statisticsButton)
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		panel_3.setLayout(gl_panel_3);

		playButton = new JButton("");
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				contest.resume();
				pauseButton.setEnabled(true);
				playButton.setEnabled(false);
			}
		});
		playButton.setToolTipText("Pokračuj v súťaži");
		playButton.setIcon(new ImageIcon(ContestOperatorPanel.class.getResource("/images/play.png")));
		panel_2.add(playButton);

		pauseButton = new JButton("");
		pauseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				contest.pause();
				pauseButton.setEnabled(false);
				playButton.setEnabled(true);
			}
		});
		pauseButton.setToolTipText("Pozastav súťaž");
		pauseButton.setIcon(new ImageIcon(ContestOperatorPanel.class.getResource("/images/pause.png")));
		panel_2.add(pauseButton);

		stopButton = new JButton("");
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String dialogText = "Skutočne chcete ukončiť súťaž bez uloženia priebehu a výsledkov?";
				if ((lastSaveFile != null) && saveContinuouslyCheckBox.isEnabled()
						&& saveContinuouslyCheckBox.isSelected()) {
					dialogText = "Skutočne chcete ukončiť súťaž?";
				}
				int dialogResult = JOptionPane.showConfirmDialog(ContestOperatorPanel.this.getRootPane().getParent(),
						dialogText, "Upozornenie", JOptionPane.YES_NO_OPTION);
				if (dialogResult == JOptionPane.YES_OPTION) {
					mainFrame.changeToEditContestView();
				}
			}
		});
		stopButton.setToolTipText("Ukonči súťaž");
		stopButton.setIcon(new ImageIcon(ContestOperatorPanel.class.getResource("/images/stop.png")));
		panel_2.add(stopButton);

		JButton newWindowButton = new JButton("");
		newWindowButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openNewContestWindowClicked();
			}
		});
		newWindowButton.setToolTipText("Otvoriť okno s výsledkami");
		newWindowButton.setIcon(new ImageIcon(ContestOperatorPanel.class.getResource("/images/newWindow.png")));
		panel_2.add(newWindowButton);

		btnRemoveLastDuel = new JButton("Odstráň posledný duel");
		btnRemoveLastDuel.setEnabled(false);
		btnRemoveLastDuel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				contest.removeLastDuel();
			}
		});

		JScrollPane scrollPane = new JScrollPane();
		GroupLayout gl_panel_1 = new GroupLayout(panel_1);
		gl_panel_1.setHorizontalGroup(gl_panel_1.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel_1.createSequentialGroup().addContainerGap()
						.addGroup(gl_panel_1.createParallelGroup(Alignment.LEADING)
								.addComponent(scrollPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 325,
										Short.MAX_VALUE)
								.addComponent(btnRemoveLastDuel, Alignment.TRAILING))
						.addContainerGap()));
		gl_panel_1.setVerticalGroup(gl_panel_1.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel_1.createSequentialGroup().addContainerGap()
						.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnRemoveLastDuel).addGap(5)));

		duelsTable = new JTable();
		scrollPane.setViewportView(duelsTable);
		panel_1.setLayout(gl_panel_1);

		winnerCombo = new JComboBox<String>();
		winnerCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				updateAddDuelResultButton();
			}
		});

		JLabel lblVaz = new JLabel("Víťaz:");

		JLabel lblPorazen = new JLabel("Porazený:");

		loserCombo = new JComboBox<String>();
		loserCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateAddDuelResultButton();
			}
		});

		btnAddDuel = new JButton("Pridaj výsledok");
		btnAddDuel.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnAddDuel.setIcon(new ImageIcon(ContestOperatorPanel.class.getResource("/images/write.png")));
		btnAddDuel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addDuelResultClicked();
			}
		});
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup().addContainerGap()
						.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel.createSequentialGroup().addComponent(lblVaz).addGap(32)
										.addComponent(winnerCombo, 0, 255, Short.MAX_VALUE))
								.addGroup(gl_panel.createSequentialGroup().addComponent(lblPorazen).addGap(10)
										.addComponent(loserCombo, 0, 256, Short.MAX_VALUE))
								.addComponent(btnAddDuel, Alignment.TRAILING))
						.addContainerGap()));
		gl_panel.setVerticalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(Alignment.TRAILING,
				gl_panel.createSequentialGroup().addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
								.addComponent(winnerCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblVaz))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
								.addComponent(loserCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblPorazen))
						.addGap(7).addComponent(btnAddDuel).addContainerGap()));
		panel.setLayout(gl_panel);
		setLayout(groupLayout);
	}
}
