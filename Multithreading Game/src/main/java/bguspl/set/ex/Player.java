package bguspl.set.ex;

import bguspl.set.Env;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Random;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */

public class Player implements Runnable {

    public volatile ArrayBlockingQueue<Integer> q;
    public volatile int tokensNum;
    private int[] tokens;
    private final Dealer dealer;
    private boolean ischanged;
    public volatile boolean isDeleted = false;
    public volatile boolean point = false;
    public volatile boolean penalty = false;
    public volatile boolean exit = false;
    public volatile boolean standBy = false;
    private int[] set;

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    public Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate
     * key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided
     *               manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        terminate = false;
        this.env = env;
        this.dealer = dealer;
        this.table = table;
        this.id = id;
        this.human = human;
        this.q = new ArrayBlockingQueue<>(3);
        tokens = new int[env.config.tableSize];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = -1;
        }
        tokensNum = 0;
        ischanged = true;
        set = new int[4];
    }

    /**
     * The main player thread of each player starts here (main loop for the player
     * thread).
     */
    @Override

    

    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human)
            createArtificialIntelligence();
        while (!terminate) {
            // TODO implement main player loop
                if ((tokensNum == 3) && ischanged) {
                    int index = 0;
                    for (int i = 0; i < tokens.length; i++) {
                        if (tokens[i] != -1 && table.slotToCard[i] != null) {
                            set[index] = table.slotToCard[i];
                            index++;
                        }
                    }
                    set[3] = id;
                    dealer.addSet(set);

                    while (!point && !penalty && !isDeleted) {
                        if(terminate) break;
                    }
                    
                    if (point) {
                        this.point();
                    } else if (penalty) {
                        this.penalty();
                    }

                    point = false;
                    penalty = false;
                    ischanged = false;
                    isDeleted = false;
                }
                if(!human){
                    try {
                        synchronized(this){
                            notifyAll();
                            wait(); // sleep aiThread
                        }
                    } catch (InterruptedException ignored) {}
                }
                
            }
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of
     * this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it
     * is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                    while (tokensNum < 3) {
                    if(terminate)break;
                    int slot = getRandomKey();
                    keyPressed(slot);
                }
                
                try {
                    synchronized(this){
                        notifyAll();
                        wait(); // sleep aiThread
                    }
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate = true;
        playerThread.interrupt();
        aiThread.interrupt();
        synchronized(this){
            notifyAll();
        }
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
         if (table.slotToCard[slot] != null && !terminate &&  !dealer.frozen && !standBy) {
            if(q.remainingCapacity()==0 && !human){
                q.clear();
            }
            if (tokensNum < 3) {
                if (tokens[slot] != -1 && !dealer.frozen) {
                    table.removeToken(id, slot);
                    q.remove(slot);
                    tokens[slot] = -1;
                    tokensNum--;
                } else if(!dealer.frozen) {
                    table.placeToken(id, slot);
                    q.add(slot);
                    tokens[slot] = 1;
                    ischanged = true;
                    tokensNum++;
                }
            } else if (isFoundInTokens(slot) && !dealer.frozen) {
                table.removeToken(id, slot);
                q.remove(slot);
                tokens[slot] = -1;
                tokensNum--;
            }
        }
        }

    public boolean isFoundInTokens(int slot) {
        return tokens[slot] != -1;
    }

    public boolean isHuman(){
        return human;
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        standBy = true;
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        score++;
        env.ui.setScore(id, score);

        long start = System.currentTimeMillis();
        long finishFreezeTime = start + env.config.pointFreezeMillis;

        long timeleft = 0;
        while (finishFreezeTime > start) {
            timeleft = finishFreezeTime-start;
            if(timeleft >= 1000)
                env.ui.setFreeze(id, finishFreezeTime - start);
            else
                env.ui.setFreeze(id, 0);
            try {
                if (timeleft >= 1000){
                    Thread.sleep(1000);
                }else{
                    Thread.sleep(timeleft);
                }
            } catch (InterruptedException e) {
                // Handle InterruptedException if necessary
            }
            start += 1000;
        }

        tokensNum = 0;
        q.clear();

        for (int i = 0; i < env.config.tableSize; i++) {
            tokens[i] = -1;
        }
        standBy = false;
    }


    

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        standBy = true;
        long start = System.currentTimeMillis();
        long finishFreezeTime = start + env.config.penaltyFreezeMillis;

        long timeleft = 0;
        while (finishFreezeTime > start) {
            timeleft = finishFreezeTime-start;
            env.ui.setFreeze(id, finishFreezeTime - start);
            try {
                if (timeleft >= 1000){
                    Thread.sleep(1000);
                }else{
                    Thread.sleep(timeleft);
                }
            } catch (InterruptedException e) {
                // Handle InterruptedException if necessary
            }
            start += 1000;
        }
        ischanged = false;

        if (!this.isHuman()) {
            int randomSlotToRemove = new Random().nextInt(3) + 0;
            if (table.cardToSlot[set[randomSlotToRemove]] != null) {
                this.removeToken(table.cardToSlot[set[randomSlotToRemove]]); // backend remove
                table.removeToken(set[3], table.cardToSlot[set[randomSlotToRemove]]);// frontend remove
            }
        } 

        standBy = false;
    }

    public int score() {
        return score;
    }

    public static int getRandomKey() {
        // Define the range
        int min = 0;
        int max = 11;

        // Create a Random object
        Random random = new Random();

        // Generate a random number within the range
        return random.nextInt(max - min + 1) + min;
    }

    public void resetTokens() {
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = -1;
            table.removeToken(id, i);
        }
        tokensNum = 0;
        q.clear();
        point = false;
        penalty = false;
        isDeleted = false;
    }

    public void removeToken(int slot) {
        if (isFoundInTokens(slot)) {
            tokens[slot] = -1;
            q.remove(slot);
            tokensNum--;
        }
    }

    
}
