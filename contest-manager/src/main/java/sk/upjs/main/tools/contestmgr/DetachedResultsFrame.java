package sk.upjs.main.tools.contestmgr;

import java.awt.BorderLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Odpojene okno zobrazujuce vysledky sutaze.
 */
@SuppressWarnings("serial")
public class DetachedResultsFrame extends JFrame {

    private JPanel contentPane;

    public DetachedResultsFrame() {
        setIconImage(Toolkit.getDefaultToolkit().getImage(
                DetachedResultsFrame.class.getResource("/images/contest.png")));
        setTitle("Priebeh súťaže");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);
    }

    /**
     * Vytvori okno zobrazujuce vysledky zadanej sutaze.
     */
    public DetachedResultsFrame(Contest contest) {
        this();
        final ContestTablePanel panel = new ContestTablePanel(contest);
        contentPane.add(panel);
        panel.refresh();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent arg) {
                panel.stopListenContest();
            }
        });
    }

}
