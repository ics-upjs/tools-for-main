package sk.upjs.main.tools.contestmgr;

import java.awt.EventQueue;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Trieda realizujuca vzdialene ovladanie cez https
 */
public class RemoteControlServer extends NanoHTTPD {

	/**
	 * Panel ovladany tymto controllerom.
	 */
	private final ContestOperatorPanel operatorPanel;

	/**
	 * Pristupove heslo na akceptovanie zaslanych prikazov.
	 */
	private final String password;

	public RemoteControlServer(int port, String password,
			ContestOperatorPanel operatorPanel) {
		super(port);
		this.operatorPanel = operatorPanel;
		this.password = password.trim();
	}

	@Override
	public Response serve(final String uri, Method method,
			Map<String, String> headers, final Map<String, String> parms,
			Map<String, String> files) {

		final StringBuilder responseBuilder = new StringBuilder();

		// jednoducha autorizacia
		if ((password.length() != 0)
				&& (!password.equals(parms.get("password")))) {
			responseBuilder.append("Unauthorized access");
		} else {
			// Spracovanie prikazu v EDT
			try {
				EventQueue.invokeAndWait(new Runnable() {
					public void run() {
						responseBuilder.append(executeCommand(uri, parms));
					}
				});
			} catch (Exception ignore) {

			}
		}

		return new NanoHTTPD.Response(responseBuilder.toString());
	}

	private String executeCommand(String uri, Map<String, String> params) {
		// Odstranime zaverecne /
		int lastIndex = uri.length() - 1;
		while ((lastIndex >= 0) && (uri.charAt(lastIndex) == '/')) {
			lastIndex--;
		}
		lastIndex++;
		uri = uri.substring(0, lastIndex);

		switch (uri) {
		case "/AddDuel":
			return addDuel(params.get("winner"), params.get("loser"));

		case "/Results":
			return getResults();
		}

		return "Unknown command";
	}

	private String addDuel(String winner, String loser) {
		System.out.println(winner + " " + loser);
		operatorPanel.setDuelRemotely(winner, loser);
		return "OK";
	}

	private String getResults() {
		Contest contest = operatorPanel.getContest();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < contest.getContestantsCount(); i++) {
			Contest.Contestant contestant = contest.getContestant(i);
			sb.append(contestant.getName() + "\t" + contestant.getRating()
					+ "\n");
		}
		return sb.toString();
	}
}
