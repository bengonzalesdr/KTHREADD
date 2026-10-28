package dev.kthreadd.modules.combat;

/** One owner for inventory/rotation actions during a module tick; the same owner may finish its burst. */
public final class CombatActions {
    private static String owner;
    private static Object world;
    private static long tick=Long.MIN_VALUE;
    private CombatActions() {}
    public static void beginTick() { owner=null; }
    public static void beginWindow(Object currentWorld,long currentTick) {
        if(world!=currentWorld||tick!=currentTick){owner=null;world=currentWorld;tick=currentTick;}
    }
    public static boolean claim(String requested) {
        if(owner!=null && !owner.equals(requested)) return false;
        owner=requested; return true;
    }
    public static boolean available(String requested) { return owner==null || owner.equals(requested); }
}
