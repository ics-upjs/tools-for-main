package sk.upjs.main.tools.rfid;

import java.math.BigInteger;
import java.net.URL;
import java.util.*;

import javax.smartcardio.*;
import javax.sound.sampled.*;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

public class RFIDCardDetector {

	/**
	 * Cas, na aky sa uspi kontrolne vlakno pri detekovani chyby
	 */
	private static final long ERROR_SLEEP_DURATION = 1000;

	/**
	 * Cas, na aky sa uspava vlakno po korektnom detekovani karty
	 */
	private static final long SLEEP_DURATION = 200;

	/**
	 * Cas merania, ktory je detekovany ako odobranie a opatovne prilozenie
	 * tej istej karty
	 */
	private static final long CARD_RESET_INTERVAL = 8000;

	/**
	 * Listener na notifikovanie, ze bola detekovana karta
	 */
	public interface CardListener {
		/**
		 * Metoda volana pri detekovani karty.
		 * 
		 * @param cardId
		 *            id karty ako hexadecimalny retazec
		 * @param rawCardId
		 *            id karty v poli bajtov
		 */
		public void onCardDetected(String cardId, byte[] rawCardId);
	}

	/**
	 * Vlakno realizujuce citanie
	 */
	private class ScanningThread extends Thread {
		private volatile boolean stopFlag = false;

		@Override
		public void run() {
			detectCardInLoop(this);
		}

		public void stopScanning() {
			stopFlag = true;
		}
	}

	/**
	 * Indikuje, ci sa ma prehrat potvrdzovacia hlaska o prilozeni karty.
	 */
	private boolean playBeep = true;

	/**
	 * Terminal, nad ktorym bezi detektor kariet
	 */
	private final CardTerminal terminal;

	/**
	 * Seriovy port, nad ktorym bezi detektor kariet
	 */
	private final SerialPort serialPort;

	/**
	 * Zoznam registrovanych listenerov.
	 */
	private final List<CardListener> listeners;

	/**
	 * Vlakno realizujuce periodicke citanie obsahu karty
	 */
	private ScanningThread scanningThread;

	/**
	 * Vytvori detector nad terminalom.
	 * 
	 * @param terminal
	 *            terminal, ktory realizuje spojenie na kartu
	 */
	private RFIDCardDetector(CardTerminal terminal) {
		this.terminal = terminal;
		this.serialPort = null;
		this.listeners = new ArrayList<CardListener>();
	}

	/**
	 * Vytvori detector nad arduinom pripojenym seriovym portom
	 * 
	 * @param serialPort
	 *            seriovy port, ktory realizuje spojenie s arduino citackou.
	 */
	private RFIDCardDetector(SerialPort serialPort) {
		this.terminal = null;
		this.serialPort = serialPort;
		this.listeners = new ArrayList<CardListener>();
	}

	/**
	 * Zaregistruje novy listener
	 * 
	 * @param l
	 */
	public void addCardListener(CardListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	/**
	 * Odregistruje listener
	 * 
	 * @param l
	 */
	public void removeCardListener(CardListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	/**
	 * Indikuje, ci sa prehrava zvuk pri prilozeni karty.
	 * 
	 * @return
	 */
	public boolean isPlayBeep() {
		synchronized (this) {
			return playBeep;
		}
	}

	/**
	 * Nastavuje prehravanie zvuku pri prilozeni karty.
	 * 
	 * @param playBeep
	 */
	public void setPlayBeep(boolean playBeep) {
		synchronized (this) {
			this.playBeep = playBeep;
		}
	}

	/**
	 * Nastartuje detekovanie kariet na citacke
	 */
	public void start() {
		synchronized (this) {
			if (scanningThread != null) {
				throw new RuntimeException("Detector is already running.");
			}

			// Vytvorime citacie vlakno
			scanningThread = new ScanningThread();
			scanningThread.setDaemon(true);
			scanningThread.start();
		}
	}

	/**
	 * Ukonci detekovanie kariet na citacke
	 */
	public void stop() {
		synchronized (this) {
			if (scanningThread == null) {
				throw new RuntimeException("No detector is running.");
			}

			scanningThread.stopScanning();
			scanningThread = null;
		}
	}

	/**
	 * Realizuje detekciu karty v slucke
	 */
	private void detectCardInLoop(final ScanningThread thisThread) {
		// Ak komunikujeme cez seriovu linku, inicializujeme port
		if (serialPort != null) {
			try {
				serialPort.openPort();
				serialPort.setParams(SerialPort.BAUDRATE_9600,
						SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);

				try {
					Thread.sleep(5000);
				} catch (Exception ignore) {

				}
			} catch (SerialPortException e) {
				System.err.println(e);
				return;
			}
		}

		// ID naposledy detekovanej karty
		String lastKnownCardId = "";
		long lastKnownCardFired = System.currentTimeMillis();
		while (!thisThread.stopFlag) {
			// Precitame ID vlozenej karty
			byte[] rawCardId = new byte[0];
			if (terminal != null)
				rawCardId = getCardUIDFromTerminal();

			if (serialPort != null) {
				rawCardId = getCardUIDFromSerial();
			}

			long now = System.currentTimeMillis();
			if (thisThread.isInterrupted())
				break;

			if (now - lastKnownCardFired >= CARD_RESET_INTERVAL)
				lastKnownCardId = "";

			if (rawCardId.length != 0) {
				String cardId = String.format("%0" + (rawCardId.length << 1)
						+ "X", new BigInteger(1, rawCardId));
				if (!lastKnownCardId.equals(cardId)) {
					lastKnownCardFired = now;
					lastKnownCardId = cardId;
					fireCardDetected(rawCardId, cardId);
				}

				try {
					Thread.sleep(SLEEP_DURATION);
				} catch (Exception ignore) {

				}
			} else {
				// V pripade problemu s detekovanim karty uspime vlakno na
				// nejaky cas
				try {
					Thread.sleep(ERROR_SLEEP_DURATION);
				} catch (Exception ignore) {

				}
			}
		}

		// Ak sa komunikovalo cez seriovy port, ukoncime spojenie
		if (serialPort != null) {
			try {
				serialPort.closePort();
			} catch (SerialPortException ignore) {

			}
		}
	}

	/**
	 * Vyvola udalost detekcie karty
	 * 
	 * @param rawCardId
	 * @param cardId
	 */
	private void fireCardDetected(byte[] rawCardId, String cardId) {
		if (isPlayBeep())
			playNewCardSound();

		List<CardListener> ls = null;
		synchronized (listeners) {
			ls = new ArrayList<CardListener>(listeners);
		}

		for (CardListener l : ls) {
			l.onCardDetected(cardId, rawCardId);
		}
	}

	/**
	 * Vrati cislo karty cez terminal
	 * 
	 * @return
	 * @throws CardException
	 */
	private byte[] getCardUIDFromTerminal() {
		try {
			Card card = terminal.connect("*");
			ResponseAPDU response = card.getBasicChannel().transmit(
					new CommandAPDU(new byte[] { (byte) 0xFF, 0x00, 0x00, 0x00,
							0x04, (byte) 0xD4, 0x4A, 0x01, 0x00 }));
			card.disconnect(true);
			if (response.getSW1() == 0x90) {
				byte[] data = response.getData();
				data = Arrays.copyOfRange(data, 0x08, data.length);
				return data;
			}
			return new byte[] {};
		} catch (CardException e) {
			return new byte[] {};
		}
	}

	/**
	 * Vrati cislo karty cez seriovy port (Arduino citacka)
	 * 
	 * @return
	 */
	private byte[] getCardUIDFromSerial() {
		try {
			// Odosleme poziadavku na zaslanie ID aktualne prilozenej karty
			serialPort.writeString("GC" + "\n");

			// Nacitame riadok
			StringBuilder sb = new StringBuilder();
			do {
				String inputChar = serialPort.readString(1,
						(sb.length() == 0) ? 2000 : 500);
				if ((inputChar == null) || (inputChar.isEmpty())) {
					break;
				}

				if ("\n".equals(inputChar)) {
					break;
				}

				sb.append(inputChar);
			} while (true);

			// Prazdne retazce alebo retazce neparnej dlzky ignorujeme
			String uidString = sb.toString().trim().toLowerCase();
			if (uidString.isEmpty() || (uidString.length() % 2 != 0)) {
				return new byte[] {};
			}

			// Vyrobime pole byteov
			byte[] result = new byte[uidString.length() / 2];
			for (int i = 0; i < result.length; i++) {
				result[i] = (byte) Integer.parseInt(
						uidString.substring(i * 2, i * 2 + 2), 16);
			}

			return result;
		} catch (Exception e) {
			return new byte[] {};
		}
	}

	/**
	 * Vrati zoznam kartovych terminalov.
	 * 
	 * @return
	 */
	private static List<CardTerminal> getTerminals() {
		TerminalFactory factory = TerminalFactory.getDefault();
		List<CardTerminal> terminals;
		try {
			terminals = factory.terminals().list();
			return terminals;
		} catch (CardException e) {
			return null;
		}
	}

	/**
	 * Vrati zoznam mien kartovych terminalov.
	 * 
	 * @return
	 */
	public static List<String> getTerminalNames() {
		List<String> result = new ArrayList<String>();
		TerminalFactory factory = TerminalFactory.getDefault();
		try {
			List<CardTerminal> terminals = factory.terminals().list();
			if (terminals == null)
				return null;

			for (CardTerminal ct : terminals)
				result.add(ct.toString());
		} catch (CardException ignore) {

		}

		// Vylistujeme seriove porty (connector cez seriovy port)
		String[] portNames = SerialPortList.getPortNames();
		for (int i = 0; i < portNames.length; i++) {
			result.add("Arduino Reader (" + portNames[i] + ")");
		}

		return result;
	}

	private void playNewCardSound() {
		try {
			URL url = RFIDCardDetector.class
					.getResource("/sounds/cardSound.wav");
			AudioInputStream audioInputStream = AudioSystem
					.getAudioInputStream(url);
			final Clip clip = AudioSystem.getClip();
			clip.addLineListener(new LineListener() {
				@Override
				public void update(LineEvent event) {
					if (event.getType() == LineEvent.Type.STOP) {
						clip.close();
					}
				}
			});
			clip.open(audioInputStream);
			clip.start();
		} catch (Exception ignore) {

		}
	}

	/**
	 * Vytvori detektor pre terminal so zadanym menom
	 * 
	 * @param terminalName
	 *            nazov terminalu
	 * @return
	 */
	public static RFIDCardDetector createDetector(String terminalName) {
		// Overime, ci meno terminalu je zo zoznamu mien terminalov
		List<String> terminalNames = getTerminalNames();
		if ((terminalNames == null) || (!terminalNames.contains(terminalName))) {
			return null;
		}

		List<CardTerminal> terminals = getTerminals();
		if (terminals != null) {
			for (CardTerminal t : terminals)
				if (t.toString().equals(terminalName))
					return new RFIDCardDetector(t);
		}

		if (terminalName.startsWith("Arduino Reader (")) {
			int startIdx = terminalName.indexOf('(');
			String portName = terminalName.substring(startIdx + 1,
					terminalName.length() - 1);
			return new RFIDCardDetector(new SerialPort(portName));
		}

		return null;
	}
}
