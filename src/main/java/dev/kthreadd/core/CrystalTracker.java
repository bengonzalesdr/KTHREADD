package dev.kthreadd.core;

import java.util.HashMap;
import java.util.Map;

/** Main-thread-only tracking: local acceptance is not treated as a server confirmation. */
public final class CrystalTracker<P> {
    private final Map<P, Long> placements = new HashMap<>();
    private final Map<Integer, P> crystals = new HashMap<>();
    private final Map<Integer, Long> attacks = new HashMap<>();
    private int confirmed, expired;
    private long lastConfirmationMs;
    public void placed(P base, long now) { placements.put(base, now); }
    public void cancelPlacement(P base) { placements.remove(base); }
    public java.util.Set<P> reservedBases() {
        var bases=new java.util.HashSet<P>(placements.keySet());bases.addAll(crystals.values());return java.util.Set.copyOf(bases);
    }
    public java.util.Set<Integer> activeAttacks(long now,long timeout) {
        var ids=new java.util.HashSet<Integer>();attacks.forEach((id,time)->{if(now-time<timeout)ids.add(id);});return java.util.Set.copyOf(ids);
    }
    public boolean spawned(P base, int id, long now) {
        Long sent = placements.remove(base);
        if (sent == null) return false;
        crystals.put(id, base); confirmed++; lastConfirmationMs = Math.max(0, now - sent); return true;
    }
    public void removed(int id) { crystals.remove(id); attacks.remove(id); }
    public void attacked(int id, long now) { attacks.put(id, now); }
    public boolean canAttack(int id, long now, long timeout) {
        Long sent = attacks.get(id); return sent == null || now - sent >= timeout;
    }
    public boolean blocked(P base, long now, long timeout) {
        Long sent = placements.get(base);
        return sent != null && now - sent < timeout || crystals.containsValue(base);
    }
    public void expire(long now, long timeout) {
        placements.entrySet().removeIf(e -> { if (now - e.getValue() < timeout) return false; expired++; return true; });
        // AttackRetry is independent of placement expiry. Entity unload/removal or clear releases these entries.
    }
    public int pending() { return placements.size(); }
    public int confirmed() { return confirmed; }
    public int expired() { return expired; }
    public long lastConfirmationMs() { return lastConfirmationMs; }
    public void clear() { placements.clear(); crystals.clear(); attacks.clear(); confirmed = expired = 0; lastConfirmationMs = 0; }
}
