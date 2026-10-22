package dev.kthreadd.modules.combat;

/** One owner for inventory/rotation actions during a module tick; the same owner may finish its burst. */
public final class CombatActions {
    private static String owner;
    private CombatActions() {}
    public static void beginTick() { owner=null; }
    public static boolean claim(String requested) {
        if(owner!=null && !owner.equals(requested)) return false;
        owner=requested; return true;
    }
    public static boolean available(String requested) { return owner==null || owner.equals(requested); }
}
