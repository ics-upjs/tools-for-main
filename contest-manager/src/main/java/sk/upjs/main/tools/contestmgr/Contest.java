package sk.upjs.main.tools.contestmgr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;

import javax.swing.Timer;

/**
 * Trieda reprezentujuca a implementujuca sutaz.
 */
public class Contest {

    // ------------------------------------------------------------------------

    /**
     * Rozhranie listenera na pocuvanie zmien v sutazi
     */
    public interface ContestListener {
        /**
         * Metoda volana pri zmene stavu sutaze
         */
        public void onContestChange();

        /**
         * Metoda volana pri zmene casu sutaze
         */
        public void onContestTimeChange();
    }

    // ------------------------------------------------------------------------

    /**
     * Trieda reprezentujuca jedneho sutaziaceho
     */
    public static class Contestant {
        private String name;
        private int rating;

        public Contestant(String name) {
            this.name = name;
            this.rating = 0;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getRating() {
            return rating;
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Trieda reprezentujuca vysledok duelu
     */
    public static class Duel {
        public final Contestant winner;
        public final Contestant loser;

        /**
         * Cas od zaciatku sutaze, kedy bol vysledok duelu zapisany
         */
        public final int time;

        public Duel(Contestant winner, Contestant loser, int time) {
            this.winner = winner;
            this.loser = loser;
            this.time = time;
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Comparator realizujuci porovnavanie sutaziacich podla ratingu
     */
    private final Comparator<Contestant> ratingComparator = new Comparator<Contestant>() {
        @Override
        public int compare(Contestant c1, Contestant c2) {
            return -Integer.compare(c1.rating, c2.rating);
        }
    };

    // ------------------------------------------------------------------------

    /**
     * Trvanie sutaze v sekundach.
     */
    private int duration = 2700;
    /**
     * Indikuje, ci ma sutaz casovo ohranicene trvanie.
     */
    private boolean timerEnabled = true;

    /**
     * Parameter A pri vypocte zmeny ratingov.
     */
    private int parameterA = 100;
    /**
     * Parameter B pri vypocte zmeny ratingov.
     */
    private int parameterB = 1;
    /**
     * Parameter C pri vypocte zmeny ratingov.
     */
    private int parameterC = 3;
    /**
     * Pociatocny rating sutaziaceho.
     */
    private int initialRating = 1000;

    /**
     * Port, na ktorom pocuva vzdialena ovladanie.
     */
    private int rcPort = 8080;

    /**
     * Indikuje, ci je povolene vzdialene ovladanie.
     */
    private boolean removeControlEnabled = true;

    /**
     * Heslo autorizujuce zasielanie prikazov na vzdialene ovladanie.
     */
    private String rcPassword = "";

    /**
     * Aktualny cas v sekundach od zaciatku sutaze.
     */
    private int time = 0;

    /**
     * Cas sutaze, v ktorom bol odstartovany aktualny beh sutaze.
     */
    private int contestTimeAtCountingStart = 0;

    /**
     * Systemovy cas, v ktorom bol odstartovany aktualny beh sutaze.
     */
    private long systemTimeAtCountingStart = 0;

    /**
     * Indikuje, ci sutaz bezi.
     */
    private boolean running = false;

    /**
     * Indikuje, ci je sutaz pozastavena.
     */
    private boolean paused = false;

    /**
     * Timer pouzivany na udrziavanie casu trvania sutaze
     */
    final private Timer timer;

    /**
     * Zoznam ucastnikov sutaze.
     */
    private final List<Contestant> contestants = new ArrayList<Contestant>();

    /**
     * Zoznam ucastnikov sutaze podla ratingu.
     */
    private final List<Contestant> orderedContestants = new ArrayList<Contestant>();

    /**
     * Zoznam duelov sutaze.
     */
    private final List<Duel> duels = new ArrayList<Duel>();

    /**
     * Zoznam registrovanych listenerov zmeny stavu sutaze.
     */
    private final List<ContestListener> listeners = new ArrayList<ContestListener>();

    /**
     * Vytvori sutaz.
     */
    public Contest() {
        timer = new Timer(500, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg) {
                onTimerTick();
            }
        });
    }

    /**
     * Vrati trvanie sutaze v sekundach.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Nastavi trvanie sutaze v sekundach.
     *
     * @param duration trvanie sutaze v sekundach
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * Vrati, ci ma sutaz casovo ohranicene trvanie
     */
    public boolean isTimerEnabled() {
        return timerEnabled;
    }

    /**
     * Nastavi, ci ma sutaz casovo ohranicene trvanie
     */
    public void setTimerEnabled(boolean timerEnabled) {
        this.timerEnabled = timerEnabled;
    }

    public int getParameterA() {
        return parameterA;
    }

    public void setParameterA(int parameterA) {
        this.parameterA = parameterA;
    }

    public int getParameterB() {
        return parameterB;
    }

    public void setParameterB(int parameterB) {
        this.parameterB = parameterB;
    }

    public int getParameterC() {
        return parameterC;
    }

    public void setParameterC(int parameterC) {
        this.parameterC = parameterC;
    }

    public int getInitialRating() {
        return initialRating;
    }

    public void setInitialRating(int initialRating) {
        this.initialRating = initialRating;
    }

    public int getRcPort() {
        return rcPort;
    }

    public void setRcPort(int rcPort) {
        this.rcPort = rcPort;
    }

    public boolean isRemoveControlEnabled() {
        return removeControlEnabled;
    }

    public void setRemoveControlEnabled(boolean removeControlEnabled) {
        this.removeControlEnabled = removeControlEnabled;
    }

    public String getRcPassword() {
        return rcPassword;
    }

    public void setRcPassword(String rcPassword) {
        this.rcPassword = rcPassword;
    }

    /**
     * Vrati pocet sutaziacich sutaze
     */
    public int getContestantsCount() {
        return contestants.size();
    }

    /**
     * Vrati sutaziaceho podla poradia v zozname sutaziacich
     */
    public Contestant getContestant(int index) {
        return contestants.get(index);
    }

    /**
     * Vrati sutaziaceho so zadanym menom.
     */
    public Contestant getContestant(String name) {
        for (Contestant contestant : contestants) {
            if (contestant.name.equals(name)) {
                return contestant;
            }
        }

        return null;
    }

    /**
     * Vrati sutaziaceho podla poradia v aktualnej vysledkovke
     *
     * @param rank poradie vo vysledkovke
     */
    public Contestant getContestantAtRank(int rank) {
        return orderedContestants.get(rank);
    }

    /**
     * Prida noveho sutaziaceho do zoznamu sutaciacich
     *
     * @param index
     */
    public void addNewContestant(int index) {
        contestants.add(index, new Contestant(""));
        updateOrderOfContestants();
    }

    /**
     * Odstrani sutaziaceho zo zoznamu sutaziach
     *
     * @param index
     */
    public void removeContestant(int index) {
        contestants.remove(index);
        updateOrderOfContestants();
    }

    /**
     * Odstani vsetkych sutaziacich a historiu duelov.
     */
    public void clear() {
        contestants.clear();
        duels.clear();
        updateOrderOfContestants();
    }

    /**
     * Aktualizuje poradie sutaziacich vo vysledkovke podla ratingu
     */
    private void updateOrderOfContestants() {
        if (orderedContestants.size() != contestants.size()) {
            orderedContestants.clear();
            orderedContestants.addAll(contestants);
        }

        Collections.sort(orderedContestants, ratingComparator);
    }

    /**
     * Vrati cas, ktory uplynul od zaciatku sutaze
     *
     * @return
     */
    public int getTime() {
        return time;
    }

    /**
     * Vrati cas, ktory ostava do konca sutaze.
     */
    public int getRemainingTime() {
        if (timerEnabled)
            return duration - time;
        else
            return 0;
    }

    /**
     * Vrati, ci sutaz prave prebieha.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Vrati, ci sutaz je pozastavena.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Nastartuje vykonavanie sutaze.
     */
    public void start() {
        if (running)
            throw new RuntimeException("Súťaž už bola naštartovaná.");

        running = true;
        paused = true;
        time = 0;
        duels.clear();
        for (Contestant c : contestants)
            c.rating = initialRating;

        resume();

        fireContestChange();
    }

    /**
     * Ukonci vykonavanie sutaze.
     */
    public void stop() {
        running = false;
        paused = false;
        timer.stop();

        fireContestTimeChange();
        fireContestChange();
    }

    /**
     * Pozastavi priebeh sutaze.
     */
    public void pause() {
        if (paused)
            return;

        paused = true;
        timer.stop();

        fireContestTimeChange();
        fireContestChange();
    }

    /**
     * Obnovi beh pozastavenej sutaze.
     */
    public void resume() {
        if (!paused)
            return;

        paused = false;
        contestTimeAtCountingStart = time;
        systemTimeAtCountingStart = (new Date()).getTime() / 1000;

        timer.start();

        fireContestTimeChange();
        fireContestChange();
    }

    /**
     * Nastavi novy cas behu sutaze.
     */
    public void setTime(int newTime) {
        if (!paused) {
            throw new RuntimeException(
                    "Time of contest can be changed only for a paused contest.");
        }

        if (newTime < time) {
            throw new RuntimeException(
                    "New contest time must be greater than current time.");
        }

        time = newTime;
    }

    private void onTimerTick() {
        long now = (new Date()).getTime() / 1000;
        time = (int) (contestTimeAtCountingStart + (now - systemTimeAtCountingStart));
        fireContestTimeChange();
    }

    public void addContestListener(ContestListener l) {
        listeners.add(l);
    }

    public void removeContestListener(ContestListener l) {
        listeners.remove(l);
    }

    public void removeAllContestListeners() {
        listeners.clear();
    }

    private void fireContestChange() {
        for (ContestListener l : listeners)
            l.onContestChange();
    }

    private void fireContestTimeChange() {
        for (ContestListener l : listeners)
            l.onContestTimeChange();
    }

    /**
     * Prida vysledok duelu.
     *
     * @param winner
     * @param loser
     */
    public void addDuel(Contestant winner, Contestant loser) {
        duels.add(new Duel(winner, loser, time));
        int diff = (int) Math.round(parameterA - parameterB
                * (winner.rating - loser.rating) / (double) parameterC);
        winner.rating += diff;
        loser.rating -= diff;
        updateOrderOfContestants();
        fireContestChange();
    }

    /**
     * Odstrani vysledok posledneho duelu.
     */
    public void removeLastDuel() {
        if (!duels.isEmpty()) {
            duels.remove(duels.size() - 1);
            recalculateRatings();
        }
    }

    /**
     * Na zaklade zoznamu duelov vypocita ratingy sutaziacich.
     */
    private void recalculateRatings() {
        for (Contestant c : contestants)
            c.rating = initialRating;

        for (Duel duel : duels) {
            int diff = (int) Math.round(parameterA - parameterB
                    * (duel.winner.rating - duel.loser.rating)
                    / (double) parameterC);
            duel.winner.rating += diff;
            duel.loser.rating -= diff;
        }

        updateOrderOfContestants();
        fireContestChange();
    }

    /**
     * Ulozi nastavenie sutaze do suboru.
     *
     * @param f
     */
    public void saveToFile(File f) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(f, "UTF-8");
            pw.println("Duration=" + duration);
            if (timerEnabled)
                pw.println("Timer=true");
            else
                pw.println("Timer=false");
            pw.println("A=" + parameterA);
            pw.println("B=" + parameterB);
            pw.println("C=" + parameterC);
            pw.println("Initial=" + initialRating);

            if (removeControlEnabled)
                pw.println("RemoteControl=true");
            else
                pw.println("RemoteControl=false");
            pw.println("Port=" + rcPort);
            pw.println("Password=" + rcPassword);

            pw.println("[Contestants]");
            for (Contestant c : contestants) {
                pw.println(c.getName());
            }
        } catch (Exception e) {
            System.err.println("Chyba pri zapisovaní do súboru.");
        } finally {
            if (pw != null)
                pw.close();
        }
    }

    /**
     * Nacita nastavenie sutaze zo suboru.
     *
     * @param f
     */
    public void loadFromFile(File f) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(f, "UTF-8");
            while (scanner.hasNextLine()) {
                // Citame parametre, kym nenajdeme [Contestants]
                String line = scanner.nextLine().trim();
                if (line.equals("[Contestants]")) {
                    readContestants(scanner);
                } else {
                    readParameter(line);
                }
            }
        } catch (Exception e) {
            System.err.println("Chyba pri čítaní zo súboru." + e.getMessage());
        } finally {
            if (scanner != null)
                scanner.close();
        }
    }

    /**
     * Rozparsuje riadok definujuci parameter sutaze.
     *
     * @param line
     */
    private void readParameter(String line) {
        int eqPos = line.indexOf('=');
        if (eqPos < 0)
            return;

        String paramName = line.substring(0, eqPos).trim().toLowerCase();
        String paramValue = line.substring(eqPos + 1).trim().toLowerCase();

        switch (paramName) {
            case "duration":
                duration = Math.abs(Integer.parseInt(paramValue));
                break;
            case "timer":
                timerEnabled = paramValue.equals("true");
                break;
            case "a":
                parameterA = Math.abs(Integer.parseInt(paramValue));
                break;
            case "b":
                parameterB = Math.abs(Integer.parseInt(paramValue));
                break;
            case "c":
                parameterC = Math.abs(Integer.parseInt(paramValue));
                break;
            case "initial":
                initialRating = Math.abs(Integer.parseInt(paramValue));
                break;
            case "remotecontrol":
                removeControlEnabled = paramValue.equals("true");
                break;
            case "port":
                rcPort = Math.abs(Integer.parseInt(paramValue));
                break;
            case "password":
                rcPassword = line.substring(eqPos + 1).trim();
                break;
        }
    }

    /**
     * Precita zoznam sutaziacich.
     *
     * @param scanner
     */
    private void readContestants(Scanner scanner) {
        String line;
        contestants.clear();
        while (scanner.hasNextLine()) {
            line = scanner.nextLine().trim();
            if (line.isEmpty())
                continue;

            contestants.add(new Contestant(line));
        }
        updateOrderOfContestants();
    }

    /**
     * Vrati aktualny zoznam duelov.
     */
    public List<Duel> getDuels() {
        return new ArrayList<Duel>(duels);
    }

    /**
     * Vrati pocet duelov sutaze.
     *
     * @return
     */
    public int getDuelsCount() {
        return duels.size();
    }

    /**
     * Sformatuje cas v sekundach do textoveho retazca.
     */
    public static String formatTime(int timeInSeconds) {
        int sec = timeInSeconds % 60;
        timeInSeconds /= 60;
        int min = timeInSeconds % 60;
        timeInSeconds /= 60;
        int hours = timeInSeconds % 24;
        timeInSeconds /= 24;

        StringBuilder sb = new StringBuilder();
        sb.append(hours);
        sb.append(":");
        if (min < 10)
            sb.append("0" + min);
        else
            sb.append(min);

        sb.append(":");
        if (sec < 10)
            sb.append("0" + sec);
        else
            sb.append(sec);

        return sb.toString();
    }
}
