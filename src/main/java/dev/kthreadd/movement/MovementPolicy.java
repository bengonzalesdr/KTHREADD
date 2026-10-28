package dev.kthreadd.movement;

/** Pure movement decisions, independent of module tick order and the game runtime. */
public final class MovementPolicy {
    private MovementPolicy() {}
    public static String owner(boolean flight, boolean glide, boolean speed, boolean ice, boolean strafe, boolean ground, boolean onIce) {
        if (flight) return "Flight";
        if (glide) return "ElytraFly";
        if (ground && onIce && ice) return "FastIce";
        if (ground && speed) return "Speed";
        if (!ground && strafe) return "Strafe";
        return "";
    }
    public static double[] horizontal(double forward, double sideways, double yawDegrees, double speed) {
        double length=Math.hypot(forward,sideways);
        if(length<0.01 || !Double.isFinite(length) || !Double.isFinite(speed) || !Double.isFinite(yawDegrees)) return new double[]{0,0};
        double yaw=Math.toRadians(yawDegrees), f=forward/length, s=sideways/length;
        return new double[]{(-Math.sin(yaw)*f+Math.cos(yaw)*s)*speed,(Math.cos(yaw)*f+Math.sin(yaw)*s)*speed};
    }
    public static boolean rescue(boolean alive, boolean spectator, boolean riding, boolean flying, double y, int bottom, double vertical, double margin) {
        return alive && !spectator && !riding && !flying && y<bottom+margin && vertical<0;
    }
    public static final class RespawnDelay {
        private int ticks;
        private boolean requested;
        public boolean tick(boolean dead,int delay) {
            if(!dead) { reset(); return false; }
            if(requested || ++ticks<Math.max(1,delay)) return false;
            requested=true; return true;
        }
        public void reset() { ticks=0; requested=false; }
    }
}
