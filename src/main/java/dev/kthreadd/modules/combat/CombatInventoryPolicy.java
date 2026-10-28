package dev.kthreadd.modules.combat;

/** Deterministic combat inventory decisions; independent of game/client initialization. */
public final class CombatInventoryPolicy {
    private CombatInventoryPolicy() {}
    public enum ItemChoice { KEEP, TOTEM, GAP, CRYSTAL }
    public static double crystalThreat(double[] damage) {
        double highest=0, second=0;
        for(double hit:damage) if(Double.isFinite(hit) && hit>0) {
            if(hit>=highest) { second=highest; highest=hit; }
            else second=Math.max(second,hit);
        }
        return highest+second;
    }
    public static ItemChoice fallback(ItemChoice wanted, boolean available, boolean holdingTotem, boolean hasTotem, boolean fallbackEnabled) {
        if(available)return wanted;
        if(holdingTotem || !fallbackEnabled || !hasTotem)return ItemChoice.KEEP;
        return ItemChoice.TOTEM;
    }
    public static boolean stackRoom(boolean matching, int count, int itemLimit, int inventoryLimit) {
        return matching && count < Math.min(itemLimit, inventoryLimit);
    }
    public static boolean releaseBow(int ticks, int charge, boolean usable) { return usable && ticks>=Math.max(3,charge); }
    public static double durabilityPenalty(double fraction) { return Math.max(0, .15-fraction)*30; }
    public static boolean canAttack(int weakness, int strength, double attackDamage) {
        return attackDamage+(strength<0?0:3*(strength+1))-(weakness<0?0:4*(weakness+1))>0;
    }
}
