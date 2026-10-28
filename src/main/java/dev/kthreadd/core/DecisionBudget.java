package dev.kthreadd.core;
/** Hard evaluation-count bound plus a monotonic wall-clock deadline. Cached results do not consume it. */
public final class DecisionBudget {
    private final int maximum;
    private final long deadline;
    private int used;
    public DecisionBudget(int maximum,long durationNs,long startedNs){this.maximum=Math.max(1,maximum);deadline=startedNs+Math.max(1,durationNs);}
    public boolean acquire(long now){if(exhausted(now))return false;used++;return true;}
    public boolean exhausted(long now){return used>=maximum||now>=deadline;}
    public int used(){return used;}
}
