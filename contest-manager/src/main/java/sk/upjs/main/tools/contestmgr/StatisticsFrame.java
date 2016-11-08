package sk.upjs.main.tools.contestmgr;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.GroupLayout.Alignment;
import javax.swing.border.TitledBorder;
import javax.swing.DefaultComboBoxModel;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

@SuppressWarnings("serial")
public class StatisticsFrame extends JFrame {

	/**
	 * Trieda uchovavajuca zaznam o jednom duele.
	 */
	private static class DuelRecord {
		int winnerIdx;
		int loserIdx;
	}

	/**
	 * Trieda implementujuca panel vykreslujuci priebeh sutaze pre jedneho
	 * hraca.
	 */
	private class SuccessPanel extends JPanel {

		/**
		 * Obsahuje zapasy hraca a pre kazdy zapas informaciu, ci ide o vyhru
		 * alebo prehru.
		 */
		private boolean[] winsInDuels;

		/**
		 * Konstruktor.
		 */
		public SuccessPanel() {
			setBackground(Color.white);
			changeHistory(new boolean[0]);
		}

		/**
		 * Zmeni zobrazovanu historiu.
		 *
		 * @param winsInDuels
		 */
		public void changeHistory(boolean[] winsInDuels) {
			if (winsInDuels == null)
				winsInDuels = new boolean[0];

			this.winsInDuels = winsInDuels.clone();

			// Nastavime novu sirku a vysku
			int newWidth = this.winsInDuels.length * Math.max(winImage.getWidth(), lossImage.getWidth());
			int newHeight = winImage.getHeight() + lossImage.getHeight();
			setPreferredSize(new Dimension(newWidth, newHeight));
			setSize(new Dimension(newWidth, newHeight));
			invalidate();
			repaint();
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			int slotWidth = Math.max(winImage.getWidth(), lossImage.getWidth());

			for (int i = 0; i < winsInDuels.length; i++) {
				if (winsInDuels[i]) {
					g.drawImage(winImage, i * slotWidth + (slotWidth - winImage.getWidth()) / 2, 0, null);
				} else {
					g.drawImage(lossImage, i * slotWidth + (slotWidth - lossImage.getWidth()) / 2, winImage.getHeight(),
							null);
				}
			}
		}
	}

	private JPanel contentPane;
	private JComboBox<String> contestantComboBox;
	private JLabel duelCountLabel;
	private JLabel avgDuelPerContestantLabel;
	private JLabel favouritePlayerLabel;
	private JLabel unpopularContestantLabel;
	private JLabel bestContestantLabel;
	private JLabel worstContestantLabel;
	private JLabel duelsLabel;
	private SuccessPanel successPanel;
	private JLabel worstAdversaryLabel;
	private JLabel bestAdversaryLabel;
	private JLabel unpopAdversaryLabel;
	private JLabel favAdversaryLabel;

	private BufferedImage winImage;
	private BufferedImage lossImage;

	private List<String> contestants;
	private List<DuelRecord> duels;

	/**
	 * Vytvori okno (bezparametrovy konstruktor len pre GUI editor)
	 */
	public StatisticsFrame() {
		setTitle("Štatistika súťaže");
		setIconImage(Toolkit.getDefaultToolkit().getImage(StatisticsFrame.class.getResource("/images/statistics.png")));

		try {
			winImage = ImageIO.read(StatisticsFrame.class.getResource("/images/happyFace.png"));
			lossImage = ImageIO.read(StatisticsFrame.class.getResource("/images/sadFace.png"));
		} catch (IOException ignore) {
		}

		initComponents();
	}

	/**
	 * Vytvori okno na zobrazenie statistiky pre aktualny stav sutaze.
	 */
	public StatisticsFrame(Contest contest) {
		this();

		// Skopirujeme aktualnu statistiku do lokalnych premennych
		Map<Contest.Contestant, Integer> reverseMap = new HashMap<Contest.Contestant, Integer>();
		contestants = new ArrayList<String>();
		for (int i = 0; i < contest.getContestantsCount(); i++) {
			contestants.add(contest.getContestant(i).getName());
			reverseMap.put(contest.getContestant(i), i);
		}

		duels = new ArrayList<DuelRecord>();
		for (Contest.Duel duel : contest.getDuels()) {
			DuelRecord dr = new DuelRecord();
			dr.winnerIdx = reverseMap.get(duel.winner);
			dr.loserIdx = reverseMap.get(duel.loser);
			duels.add(dr);
		}

		// Naplnime combo s menami
		contestantComboBox.setModel(new DefaultComboBoxModel<String>(contestants.toArray(new String[0])));

		showGlobalStatistics();
		updateIndividualStatistics();
	}

	/**
	 * Vypocita a zobrazi globalne statistiky
	 */
	private void showGlobalStatistics() {
		duelCountLabel.setText(Integer.toString(duels.size()));
		if (contestants.size() == 0) {
			avgDuelPerContestantLabel.setText("-");
		} else {
			avgDuelPerContestantLabel
					.setText(Integer.toString((int) Math.round(2 * duels.size() / (double) contestants.size())));
		}

		Map<Integer, Integer> winMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> lossMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> duelsMap = new HashMap<Integer, Integer>();
		zeroMap(winMap, contestants.size());
		zeroMap(lossMap, contestants.size());
		zeroMap(duelsMap, contestants.size());

		for (DuelRecord dr : duels) {
			incrementValueInMap(winMap, dr.winnerIdx);
			incrementValueInMap(lossMap, dr.loserIdx);
			incrementValueInMap(duelsMap, dr.winnerIdx);
			incrementValueInMap(duelsMap, dr.loserIdx);
		}

		if (contestants.size() == 0) {
			favouritePlayerLabel.setText("-");
			unpopularContestantLabel.setText("-");
			bestContestantLabel.setText("-");
			worstContestantLabel.setText("-");
			return;
		}

		favouritePlayerLabel.setText(contestansToString(keysWithValue(duelsMap, Collections.max(duelsMap.values()))));
		unpopularContestantLabel
				.setText(contestansToString(keysWithValue(duelsMap, Collections.min(duelsMap.values()))));
		bestContestantLabel.setText(contestansToString(keysWithValue(winMap, Collections.max(winMap.values()))));
		worstContestantLabel.setText(contestansToString(keysWithValue(lossMap, Collections.max(lossMap.values()))));
	}

	/**
	 * Aktualizuje osobnu statistiku.
	 */
	private void updateIndividualStatistics() {
		int selectedIdx = contestantComboBox.getSelectedIndex();
		if (selectedIdx == -1)
			return;

		// Aktualizujeme pocet zapasov, pocet vyhier a prehier.
		int winCount = 0;
		int lossCount = 0;
		for (DuelRecord dr : duels) {
			if (dr.winnerIdx == selectedIdx)
				winCount++;
			if (dr.loserIdx == selectedIdx)
				lossCount++;
		}

		duelsLabel.setText(
				Integer.toString(winCount + lossCount) + " (" + winCount + " výhier, " + lossCount + " prehier)");

		Map<Integer, Integer> winMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> lossMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> duelsMap = new HashMap<Integer, Integer>();
		zeroMap(winMap, contestants.size());
		zeroMap(lossMap, contestants.size());
		zeroMap(duelsMap, contestants.size());
		List<Boolean> winsInDuelsList = new ArrayList<Boolean>();

		for (DuelRecord dr : duels)
			if ((dr.winnerIdx == selectedIdx) || (dr.loserIdx == selectedIdx)) {
				incrementValueInMap(winMap, dr.winnerIdx);
				incrementValueInMap(lossMap, dr.loserIdx);
				incrementValueInMap(duelsMap, dr.winnerIdx);
				incrementValueInMap(duelsMap, dr.loserIdx);

				winsInDuelsList.add(dr.winnerIdx == selectedIdx);
			}

		winMap.remove(selectedIdx);
		lossMap.remove(selectedIdx);
		duelsMap.remove(selectedIdx);

		if (contestants.size() <= 1) {
			favAdversaryLabel.setText("-");
			unpopAdversaryLabel.setText("-");
			bestAdversaryLabel.setText("-");
			worstAdversaryLabel.setText("-");
			return;
		}

		favAdversaryLabel.setText(contestansToString(keysWithValue(duelsMap, Collections.max(duelsMap.values()))));
		unpopAdversaryLabel.setText(contestansToString(keysWithValue(duelsMap, Collections.min(duelsMap.values()))));

		int max = Collections.max(winMap.values());
		if (max > 0) {
			bestAdversaryLabel.setText(contestansToString(keysWithValue(winMap, max)));
		} else {
			bestAdversaryLabel.setText("-");
		}

		max = Collections.max(lossMap.values());
		if (max > 0) {
			worstAdversaryLabel.setText(contestansToString(keysWithValue(lossMap, max)));
		} else {
			worstAdversaryLabel.setText("-");
		}

		boolean[] winsInDuels = new boolean[winsInDuelsList.size()];
		for (int i = 0; i < winsInDuels.length; i++)
			winsInDuels[i] = winsInDuelsList.get(i);

		successPanel.changeHistory(winsInDuels);
	}

	/**
	 * Vytvori retazec s nazvami hracov, ktori maju priradene zadane indexy v
	 * zozname contestants.
	 */
	private String contestansToString(List<Integer> indices) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (int idx : indices) {
			if (!first)
				sb.append(", ");
			sb.append(contestants.get(idx));
			first = false;
		}

		return sb.toString();
	}

	/**
	 * Vyberie kluce, ktore maju asociovanu zadanu hodnotu.
	 */
	private List<Integer> keysWithValue(Map<Integer, Integer> map, int value) {
		List<Integer> result = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> entry : map.entrySet())
			if (entry.getValue() == value)
				result.add(entry.getKey());

		return result;
	}

	/**
	 * Inicializuje mapu na hodnotu 0 pre prvky 0..count-1
	 */
	private void zeroMap(Map<Integer, Integer> map, int count) {
		for (int i = 0; i < count; i++)
			map.put(i, 0);
	}

	/**
	 * Inkrementuje hodnotu priradenu k danemu klucu.
	 */
	private void incrementValueInMap(Map<Integer, Integer> map, int key) {
		map.put(key, map.get(key) + 1);
	}

	/**
	 * Inicializuje komponenty okna
	 */
	private void initComponents() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 421, 475);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		JPanel globalPanel = new JPanel();
		globalPanel.setBorder(new TitledBorder(null, "Celkov\u00E1 \u0161tatistika", TitledBorder.LEADING,
				TitledBorder.TOP, null, null));

		JPanel individualPanel = new JPanel();
		individualPanel.setBorder(new TitledBorder(null, "Individu\u00E1lna \u0161tatistika", TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addComponent(globalPanel, GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
				.addComponent(individualPanel, GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE));
		gl_contentPane.setVerticalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
						.addComponent(globalPanel, GroupLayout.PREFERRED_SIZE, 158, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(individualPanel, GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)));

		JLabel lblHr = new JLabel("Hráč:");

		contestantComboBox = new JComboBox<String>();
		contestantComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				updateIndividualStatistics();
			}
		});

		JLabel lblPoetDuelov_1 = new JLabel("Počet duelov:");

		duelsLabel = new JLabel("???");

		JLabel lblNajobbenejSper = new JLabel("Najobľúbenejší súper:");

		JLabel lblNajneobbenejSper = new JLabel("Najneobľúbenejší súper:");

		JLabel lblNajdeptajcejSper = new JLabel("Najdeptajúcejší súper:");

		JLabel lblNajzdeptanejSper = new JLabel("Najzdeptanejší súper:");

		worstAdversaryLabel = new JLabel("???");

		bestAdversaryLabel = new JLabel("???");

		unpopAdversaryLabel = new JLabel("???");

		favAdversaryLabel = new JLabel("???");

		JScrollPane scrollPane = new JScrollPane();
		GroupLayout gl_individualPanel = new GroupLayout(individualPanel);
		gl_individualPanel.setHorizontalGroup(gl_individualPanel.createParallelGroup(Alignment.LEADING).addGroup(
				Alignment.TRAILING,
				gl_individualPanel.createSequentialGroup().addContainerGap().addGroup(gl_individualPanel
						.createParallelGroup(Alignment.TRAILING)
						.addComponent(scrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
						.addGroup(Alignment.LEADING,
								gl_individualPanel.createSequentialGroup()
										.addComponent(lblHr).addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(contestantComboBox, 0, 333, Short.MAX_VALUE))
						.addGroup(Alignment.LEADING,
								gl_individualPanel.createSequentialGroup().addComponent(lblPoetDuelov_1)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(duelsLabel, GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE))
						.addGroup(Alignment.LEADING, gl_individualPanel.createSequentialGroup()
								.addComponent(lblNajobbenejSper, GroupLayout.PREFERRED_SIZE, 112,
										GroupLayout.PREFERRED_SIZE)
								.addGap(18).addComponent(favAdversaryLabel, GroupLayout.PREFERRED_SIZE, 233,
										GroupLayout.PREFERRED_SIZE))
						.addGroup(Alignment.LEADING,
								gl_individualPanel.createSequentialGroup()
										.addComponent(lblNajneobbenejSper, GroupLayout.PREFERRED_SIZE, 124,
												GroupLayout.PREFERRED_SIZE)
										.addGap(6).addComponent(unpopAdversaryLabel, GroupLayout.PREFERRED_SIZE, 233,
												GroupLayout.PREFERRED_SIZE))
						.addGroup(Alignment.LEADING,
								gl_individualPanel.createSequentialGroup()
										.addGroup(gl_individualPanel.createParallelGroup(Alignment.TRAILING, false)
												.addComponent(lblNajdeptajcejSper, GroupLayout.DEFAULT_SIZE,
														GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addComponent(lblNajzdeptanejSper, GroupLayout.DEFAULT_SIZE, 112,
														Short.MAX_VALUE))
										.addGroup(gl_individualPanel.createParallelGroup(Alignment.LEADING)
												.addGroup(gl_individualPanel.createSequentialGroup().addGap(18)
														.addComponent(worstAdversaryLabel, GroupLayout.PREFERRED_SIZE,
																233, GroupLayout.PREFERRED_SIZE))
												.addGroup(Alignment.TRAILING,
														gl_individualPanel.createSequentialGroup().addGap(18)
																.addComponent(bestAdversaryLabel,
																		GroupLayout.PREFERRED_SIZE, 233,
																		GroupLayout.PREFERRED_SIZE)))))
						.addContainerGap()));
		gl_individualPanel.setVerticalGroup(gl_individualPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_individualPanel.createSequentialGroup().addContainerGap()
						.addGroup(gl_individualPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblHr)
								.addComponent(contestantComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_individualPanel.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblPoetDuelov_1).addComponent(duelsLabel))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_individualPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(favAdversaryLabel).addComponent(lblNajobbenejSper))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_individualPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(lblNajneobbenejSper).addComponent(unpopAdversaryLabel))
						.addGap(6)
						.addGroup(gl_individualPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(bestAdversaryLabel).addComponent(lblNajdeptajcejSper))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_individualPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(worstAdversaryLabel).addComponent(lblNajzdeptanejSper))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE).addContainerGap()));

		successPanel = new SuccessPanel();
		scrollPane.setViewportView(successPanel);
		individualPanel.setLayout(gl_individualPanel);

		JLabel lblPoetDuelov = new JLabel("Počet duelov:");

		duelCountLabel = new JLabel("0");

		JLabel lblPriemernPoetDuelov = new JLabel("Priemerný počet duelov hráča:");

		avgDuelPerContestantLabel = new JLabel("0");

		JLabel lblNajobbenejHr = new JLabel("Najobľúbenejší hráč:");

		JLabel lblNajneobbenejHr = new JLabel("Najneobľúbenejší hráč:");

		JLabel lblNajdeptajcejHr = new JLabel("Najdeptajúcejší hráč:");

		JLabel lblNajzdeptanejHr = new JLabel("Najzdeptanejší hráč:");

		favouritePlayerLabel = new JLabel("???");

		unpopularContestantLabel = new JLabel("???");

		bestContestantLabel = new JLabel("???");

		worstContestantLabel = new JLabel("???");
		GroupLayout gl_globalPanel = new GroupLayout(globalPanel);
		gl_globalPanel.setHorizontalGroup(gl_globalPanel.createParallelGroup(Alignment.LEADING).addGroup(gl_globalPanel
				.createSequentialGroup().addContainerGap()
				.addGroup(gl_globalPanel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_globalPanel.createSequentialGroup().addComponent(lblPoetDuelov)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(duelCountLabel))
						.addGroup(gl_globalPanel.createSequentialGroup().addComponent(lblPriemernPoetDuelov)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(avgDuelPerContestantLabel,
										GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_globalPanel.createSequentialGroup()
								.addGroup(gl_globalPanel.createParallelGroup(Alignment.LEADING)
										.addComponent(lblNajobbenejHr)
										.addComponent(lblNajneobbenejHr, GroupLayout.PREFERRED_SIZE, 124,
												GroupLayout.PREFERRED_SIZE)
										.addComponent(lblNajdeptajcejHr).addComponent(lblNajzdeptanejHr))
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_globalPanel.createParallelGroup(Alignment.LEADING)
										.addComponent(unpopularContestantLabel, GroupLayout.DEFAULT_SIZE, 233,
												Short.MAX_VALUE)
										.addComponent(favouritePlayerLabel, GroupLayout.DEFAULT_SIZE, 233,
												Short.MAX_VALUE)
										.addComponent(bestContestantLabel, GroupLayout.DEFAULT_SIZE, 233,
												Short.MAX_VALUE)
										.addComponent(worstContestantLabel, GroupLayout.DEFAULT_SIZE, 233,
												Short.MAX_VALUE))))
				.addContainerGap()));
		gl_globalPanel.setVerticalGroup(gl_globalPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_globalPanel.createSequentialGroup().addContainerGap()
						.addGroup(gl_globalPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblPoetDuelov)
								.addComponent(duelCountLabel))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_globalPanel.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblPriemernPoetDuelov).addComponent(avgDuelPerContestantLabel,
										GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_globalPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblNajobbenejHr)
								.addComponent(favouritePlayerLabel))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_globalPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblNajneobbenejHr)
								.addComponent(unpopularContestantLabel, GroupLayout.PREFERRED_SIZE, 14,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_globalPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblNajdeptajcejHr)
								.addComponent(bestContestantLabel))
						.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGroup(gl_globalPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblNajzdeptanejHr)
								.addComponent(worstContestantLabel))
						.addContainerGap()));
		globalPanel.setLayout(gl_globalPanel);
		contentPane.setLayout(gl_contentPane);
	}
}
