package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;
    public volatile boolean frozen = false;
    private long elapsed;
    private Vector<int[]> sets;
    private int[] set;
    public volatile boolean isLegalSet;
    private List<Integer> cardsOnTable;
    public volatile boolean resetTokens = false;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.set = new int[3];
        isLegalSet = false;
        sets = new Vector<>();
        elapsed = env.config.turnTimeoutMillis;
        cardsOnTable = new LinkedList<>();
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (env.config.turnTimeoutMillis >= 0) {
            BlockingQueue<Thread> currThread = new LinkedBlockingQueue<>();
            for (int i = 0; i < players.length; i++) {
                currThread.add(new Thread(players[i]));
            }
            int firstIteration = 0;
            while (!shouldFinish()) {
                frozen = true;
                if (firstIteration == 0) {
                    for (Thread a : currThread)
                        a.start();
                    firstIteration++;
                }
                frozen = false;
                placeCardsOnTable();
                if (env.config.hints) {
                    table.hints();
                }
                timerLoop();
                updateTimerDisplay(true);
                removeAllCardsFromTable();
            }
            env.ui.removeTokens();
            if(env.util.findSets(deck, 1).size() == 0)
                announceWinners();
        }
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did
     * not time out.
     */
    private void timerLoop() {
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        while (elapsed > 0 && env.util.findSets(cardsOnTable, 1).size() != 0 && !terminate) {
            updateTimerDisplay(false);
            sleepUntilWokenOrTimeout();
            checkSet();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        resetTokens = true;
        frozen = true;
        terminate = true;

        for (int i = players.length-1; i>=0; i--) {
            players[i].terminate();
        }
        if(env.util.findSets(deck, 1).size() != 0) // xButtonPressed
            env.ui.dispose();
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */

    private void removeCardsFromTable() { // calls it iff this.set is legal set
        frozen = true;
        if (isLegalSet) {
            if (table.cardToSlot[set[0]] != null && table.cardToSlot[set[1]] != null
                    && table.cardToSlot[set[2]] != null) {
                for (int i = 0; i < players.length; i++) // backend modification
                {
                    players[i].removeToken(table.cardToSlot[set[0]]);
                    players[i].removeToken(table.cardToSlot[set[1]]);
                    players[i].removeToken(table.cardToSlot[set[2]]);
                }
                env.ui.removeTokens(table.cardToSlot[set[0]]); // frontend modification
                table.removeCard(table.cardToSlot[set[0]]);

                env.ui.removeTokens(table.cardToSlot[set[1]]);
                table.removeCard(table.cardToSlot[set[1]]);

                env.ui.removeTokens(table.cardToSlot[set[2]]);
                table.removeCard(table.cardToSlot[set[2]]);

                cardsOnTable.remove((Integer) set[0]);
                cardsOnTable.remove((Integer) set[1]);
                cardsOnTable.remove((Integer) set[2]);

                isLegalSet = false;
            }
        }
        frozen = false;

    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        frozen = true;
        int slot = tableHasPlace();
        Collections.shuffle(deck);
        while (!deck.isEmpty() && slot != -1) {
            int card = deck.get(0);
            deck.remove(0);
            cardsOnTable.add(card);
            table.placeCard(card, slot);
            slot = tableHasPlace();
        }
        frozen = false;
    }

    private int tableHasPlace() {
        for (int i = 0; i < env.config.tableSize; i++) {
            if (table.slotToCard[i] == null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some
     * purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        try {

            Thread.sleep(1);

        } catch (InterruptedException ignored) {
            ignored.printStackTrace();
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */

    private void updateTimerDisplay(boolean reset) {
        elapsed = reshuffleTime - System.currentTimeMillis();
        if (!reset && elapsed > 0) {
            if (elapsed > env.config.turnTimeoutWarningMillis) {
                env.ui.setCountdown(elapsed, false);
            } else {
                env.ui.setCountdown(elapsed, true);
            }
        } else {
            env.ui.setCountdown(0, false);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        frozen = true;
        for (Player player : players) {
            player.resetTokens();
        }

        for (int i = 0; i < env.config.tableSize; i++) {
            if (table.slotToCard[i] != null) {
                deck.add(table.slotToCard[i]);
                cardsOnTable.remove((Integer) table.slotToCard[i]);
            }
            table.removeCard(i);
        }
        frozen = false;
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        elapsed = System.currentTimeMillis();
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int[] scores = new int[players.length];
        for (int i = 0; i < players.length; i++) {
            scores[i] = players[i].score();
        }
        Arrays.sort(scores);
        int numWinners = 1;
        for (int i = scores.length - 2; i >= 0; i--) {
            if (scores[i] < scores[i + 1])
                break;
            else
                numWinners++;
        }
        int max = scores[scores.length - 1];
        int[] winners = new int[numWinners];
        int counter = 0;
        for (int i = 0; i < players.length && counter < numWinners; i++) {
            int score = players[i].score();
            if (score == max) {
                winners[counter] = players[i].id;
            }
        }

        this.env.ui.announceWinner(winners);

        // try {
        //     Thread.sleep(env.config.endGamePauseMillies);
        // } catch (InterruptedException ignored) {}
        terminate();
    }

    public void addSet(int[] tmp) {
        sets.add(tmp);
    }

    public void checkSet() {

        while (!sets.isEmpty()) {
            int[] tmp = sets.remove(0);

            for (int i = 0; i < tmp.length - 1; i++) {
                set[i] = tmp[i];
            }
            Player player = players[tmp[3]];

            if (this.env.util.testSet(set)) {
                isLegalSet = true;
                reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
                removeCardsFromTable();
                placeCardsOnTable();
                player.point = true;
                for (int i = 0; i < sets.size(); i++) {
                    Player p = players[sets.get(i)[3]];
                    if (sets.get(i)[0] == set[0] || sets.get(i)[0] == set[1] || sets.get(i)[0] == set[2]
                            || sets.get(i)[1] == set[0] || sets.get(i)[1] == set[1] || sets.get(i)[1] == set[2]
                            || sets.get(i)[2] == set[0] || sets.get(i)[2] == set[1] || sets.get(i)[2] == set[2]) {
                        sets.remove(i);
                        p.isDeleted = true;
                    }
                }
            } else {
                isLegalSet = false;
                player.penalty = true;
            }

        }
    }

}
