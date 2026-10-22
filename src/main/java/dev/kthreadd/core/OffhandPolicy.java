package dev.kthreadd.core;
public final class OffhandPolicy {
    private OffhandPolicy() {}
    public enum Choice { NONE, TOTEM, GAP, CRYSTAL }
    public static Choice choose(boolean totem,double health,double threshold,boolean gliding,boolean gap,boolean crystal,double utilityHealth,boolean nearbyCrystal) {
        if(!totem && !gap && !crystal) return Choice.NONE;
        if((totem && (health<=threshold || gliding)) || ((gap || crystal) && (health<utilityHealth || nearbyCrystal))) return Choice.TOTEM;
        if(gap) return Choice.GAP;
        if(crystal) return Choice.CRYSTAL;
        return Choice.NONE;
    }
}
