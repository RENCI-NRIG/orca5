package net.exogeni.orca.util;

import java.util.Timer;
import java.util.TimerTask;

public class OrcaTimer {
    private Timer timer;
    private static OrcaTimer instance = new OrcaTimer();
    
    private OrcaTimer() {
        timer = new Timer("util.OrcaTimer", true);
    }
    
    public static TimerTask schedule(IOrcaTimerQueue queue, IOrcaTimerTask task, long delay) {
        TimerTask t = new InternalTimerTask(queue, task);
        instance.timer.schedule(t, delay);
        return t;
    }
    
    static class InternalTimerTask extends TimerTask {
        IOrcaTimerQueue queue;
        IOrcaTimerTask task;
        
        public InternalTimerTask(IOrcaTimerQueue queue, IOrcaTimerTask task) {
            this.queue = queue;
            this.task = task;
        }
        
        public void run() {
            queue.queueTimer(task);
        }
    }
}
