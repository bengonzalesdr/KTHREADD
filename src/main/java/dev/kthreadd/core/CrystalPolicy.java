package dev.kthreadd.core;

/** Pure decision policy, shared by placement and breaking. Damage is measured in health points. */
public final class CrystalPolicy {
    private CrystalPolicy() {}
    public static double score(double damage, double self, double health, double selfWeight, boolean lethalPriority) {
        return Math.min(damage, health) - self * selfWeight + (lethalPriority && damage >= health ? 100 : 0);
    }
    public static boolean safe(double damage, double committed, double maximum, double health, double margin, boolean antiSuicide) {
        double total = damage + committed;
        return Double.isFinite(total) && total <= maximum && (!antiSuicide || total < health - margin);
    }
}
