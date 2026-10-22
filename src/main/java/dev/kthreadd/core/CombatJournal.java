package dev.kthreadd.core;
import java.util.*;
/** Bounded, main-thread fight diagnostics. No player names, addresses or coordinates. */
public final class CombatJournal {
    public record Event(long timeMs,String type,String reason,double value) {}
    private final int capacity;
    private final ArrayDeque<Event> events=new ArrayDeque<>();
    private final Map<String,Integer> counts=new TreeMap<>();
    private long dropped;
    public CombatJournal(int capacity) { if(capacity<1)throw new IllegalArgumentException();this.capacity=capacity; }
    public void add(long time,String type,String reason,double value) {
        if(events.size()==capacity){events.removeFirst();dropped++;}
        events.addLast(new Event(time,type,reason,value));counts.merge(type+":"+reason,1,Integer::sum);
    }
    public List<Event> events(){return List.copyOf(events);}
    public Map<String,Integer> counts(){return Map.copyOf(counts);}
    public long dropped(){return dropped;}
    public void clear(){events.clear();counts.clear();dropped=0;}
}
