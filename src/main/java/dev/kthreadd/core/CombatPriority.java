package dev.kthreadd.core;
public final class CombatPriority {
    private CombatPriority() {}
    public static String first(boolean lowHealth,boolean armorCritical) {
        return lowHealth||armorCritical?"AutoArmor":"CrystalAura";
    }
}
