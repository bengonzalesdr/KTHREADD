package dev.kthreadd.core;
import java.util.*;
/** Bounded negative feedback for placement retries; confirmation timing is separate from network ping. */
public final class PlacementFeedback<P> {
    private record Failure(int count,long until,long last) {}
    private final int capacity;
    private final LinkedHashMap<P,Failure> failures=new LinkedHashMap<>(16,.75f,true);
    private double confirmationMs;
    public PlacementFeedback(int capacity){if(capacity<1)throw new IllegalArgumentException();this.capacity=capacity;}
    public void failed(P position,long now){
        failures.entrySet().removeIf(e->now-e.getValue().last()>30000);
        var old=failures.get(position);int count=old==null?1:Math.min(5,old.count()+1);
        failures.put(position,new Failure(count,now+(100L<<(count-1)),now));
        while(failures.size()>capacity)failures.remove(failures.keySet().iterator().next());
    }
    public void confirmed(P position,long delay){
        failures.remove(position);
        if(delay>0)confirmationMs=confirmationMs==0?delay:confirmationMs*.75+delay*.25;
    }
    public boolean ready(P position,long now){var failure=failures.get(position);return failure==null||now>=failure.until();}
    public long suggestedTimeout(long floor){return Math.max(floor,Math.min(3000,Math.max(50,Math.round(confirmationMs*1.25))));}
    public int size(){return failures.size();}
    public void clear(){failures.clear();confirmationMs=0;}
}
