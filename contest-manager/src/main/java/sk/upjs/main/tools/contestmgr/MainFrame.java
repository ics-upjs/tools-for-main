package sk.upjs.main.tools.contestmgr;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;

/**
 * Hlavne okno aplikacie.
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame {

	private final Contest contest;
	private ContestantsPanel contestantsPanel;
	private ContestOperatorPanel contestOperatorPanel;
	private ContestSettingsPanel contestSettingsPanel;
	private ContestTablePanel contestTablePanel;
	private RemoteControlServer server;

	private JPanel contentPane;
	private JPanel leftPanel;
	private JPanel rightPanel;
	private JSplitPane splitContentPane;

	public MainFrame() {
		setIconImage(Toolkit.getDefaultToolkit().getImage(
				MainFrame.class.getResource("/images/contest.png")));
		setTitle("Súťaž");
		contest = new Contest();
		initComponents();
	}

	/**
	 * Vrati sutaz, ktora je spravovana touto aplikaciou.
	 */
	public Contest getContest() {
		return contest;
	}

	/**
	 * Aktualizuje zobrazenie ucastnikov sutaze pri nastaveniach sutaze.
	 */
	public void refreshContestSettings() {
		contestantsPanel.refreshView();
	}

	/**
	 * Zmeni zobrazenie okna na zobrazenie priebehu sutaze.
	 */
	public void changeToContestView() {
		leftPanel.removeAll();
		rightPanel.removeAll();
		rightPanel.add(contestTablePanel);
		leftPanel.add(contestOperatorPanel);
		splitContentPane.revalidate();
		repaint();
		contest.start();
		contest.pause();
		contestOperatorPanel.updateByContest();

		if (contest.isRemoveControlEnabled()) {
			try {
				server = new RemoteControlServer(contest.getRcPort(),
						contest.getRcPassword(), contestOperatorPanel);
				server.start();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this,
						"Server sa nepodarilo naštartovať: " + e);
				server = null;
			}
		}
	}

	/**
	 * Zmeni zobrazenie okna na zobrazenie editovania parametrov sutaze.
	 */
	public void changeToEditContestView() {
		if (server != null) {
			server.stop();
			server = null;
		}

		contest.stop();
		leftPanel.removeAll();
		rightPanel.removeAll();
		rightPanel.add(contestantsPanel);
		leftPanel.add(contestSettingsPanel);
		splitContentPane.revalidate();
		repaint();
	}

	/**
	 * Inicializuje komponenty v okne aplikacie.
	 */
	private void initComponents() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 774, 472);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		splitContentPane = new JSplitPane();
		contentPane.add(splitContentPane);

		leftPanel = new JPanel();
		leftPanel.setPreferredSize(new Dimension(200, 10));
		splitContentPane.setLeftComponent(leftPanel);

		rightPanel = new JPanel();
		splitContentPane.setRightComponent(rightPanel);

		contestantsPanel = new ContestantsPanel(this);
		contestSettingsPanel = new ContestSettingsPanel(this);
		contestOperatorPanel = new ContestOperatorPanel(this);
		contestTablePanel = new ContestTablePanel(contest);

		rightPanel.setLayout(new BorderLayout(0, 0));
		leftPanel.setLayout(new BorderLayout(0, 0));
		changeToEditContestView();
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
