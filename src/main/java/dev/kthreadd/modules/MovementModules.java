package dev.kthreadd.modules;

import dev.kthreadd.*;
import dev.kthreadd.core.*;
import dev.kthreadd.movement.*;
import net.minecraft.client.MinecraftClient;

public final class MovementModules {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static void register(ModuleManager manager) {
        manager.add(new ClientModule("Sprint",Category.MOVEMENT,"Sprints forward when hunger and movement state permit; releases only its own sprint request.") {
            private boolean requested;
            { flag("WhileUsingItem",false); }
            public void tick() {
                boolean allowed=MovementControl.playable() && MovementControl.controlsAllowed() && mc.player.input.movementForward>=0.8F
                    && !mc.player.isSneaking() && !mc.player.horizontalCollision && !mc.player.isTouchingWater() && !mc.player.isInLava()
                    && !mc.player.isFallFlying() && (!mc.player.isUsingItem() || flag("WhileUsingItem"))
                    && (mc.player.getHungerManager().getFoodLevel()>6 || mc.player.getAbilities().allowFlying);
                if(allowed) { mc.player.setSprinting(true); requested=true; }
                else release();
            }
            private void release() { if(requested && mc.player!=null && !MovementControl.physical(mc.options.sprintKey)) mc.player.setSprinting(false); requested=false; }
            protected void onDisable() { release(); }
        });
        manager.add(new ClientModule("AutoWalk",Category.MOVEMENT,"Holds forward while playing. Pauses in menus, when unfocused, riding or dead.") {
            private boolean held;
            public void tick() {
                boolean allowed=MovementControl.playable() && MovementControl.controlsAllowed();
                if(allowed) { mc.options.forwardKey.setPressed(true); held=true; }
                else release();
            }
            private void release() { if(held) MovementControl.restore(mc.options.forwardKey); held=false; }
            protected void onDisable() { release(); }
        });
        manager.add(new ClientModule("Speed",Category.MOVEMENT,"Ground speed control; yields to flight, gliding and FastIce on ice. Servers may correct movement.") {
            { number("Speed",0.32,0.1,0.8,0.02); }
            public void tick() { if(MovementControl.owns(manager,name)) move(value("Speed"),false); }
        });
        manager.add(new ClientModule("ElytraFly",Category.MOVEMENT,"Controls existing gliding with jump/sneak altitude. Flight has priority; no packet bypass.") {
            { number("Speed",0.8,0.1,2,0.1); number("Vertical",0.3,0.05,1,0.05); number("Sink",0.02,0,0.2,0.01); }
            public void tick() {
                if(!MovementControl.owns(manager,name)) return;
                move(value("Speed"),true);
                var v=mc.player.getVelocity();
                int vertical=(mc.options.jumpKey.isPressed()?1:0)-(mc.options.sneakKey.isPressed()?1:0);
                mc.player.setVelocity(v.x,vertical==0?-value("Sink"):vertical*value("Vertical"),v.z);
            }
        });
        manager.add(new ClientModule("Step",Category.MOVEMENT,"Raises local step height during ordinary grounded movement; no packet stepping.") {{ number("Height",1,0.6,2,0.2); }});
        manager.add(new ClientModule("Safewalk",Category.MOVEMENT,"Vanilla grounded edge clipping; pauses in flight, liquids and while riding.") {{ flag("WhileJumping",false); }});
        manager.add(new ClientModule("NoSlowDown",Category.MOVEMENT,"Removes only the local active-item movement multiplier; server correction remains possible.") {{ flag("WhileSneaking",true); }});
        manager.add(new ClientModule("Yaw",Category.MOVEMENT,"Aligns yaw to the nearest angle while moving; can also lock while idle.") {
            { number("Increment",45,15,90,15); flag("OnlyMoving",true); }
            public void tick() {
                if(MovementControl.playable() && MovementControl.controlsAllowed() && (!flag("OnlyMoving") || mc.player.input.movementForward!=0 || mc.player.input.movementSideways!=0)) {
                    float yaw=(float)(Math.round(mc.player.getYaw()/value("Increment"))*value("Increment"));
                    mc.player.setYaw(yaw); mc.player.setHeadYaw(yaw);
                }
            }
        });
        manager.add(new ClientModule("AutoRespawn",Category.OTHER,"Requests ordinary respawn once per death after Delay ticks.") {
            private final MovementPolicy.RespawnDelay delay=new MovementPolicy.RespawnDelay();
            { number("Delay",20,1,100,1); }
            public void tick() { if(delay.tick(mc.player.isDead(),(int)value("Delay"))) mc.player.requestRespawn(); }
            protected void onDisable() { delay.reset(); }
            protected void onEnable() { delay.reset(); }
        });
    }
    public static void move(double speed, boolean stop) {
        if(mc.player==null) return;
        var v=MovementPolicy.horizontal(mc.player.input.movementForward,mc.player.input.movementSideways,mc.player.getYaw(),speed);
        if(!stop && v[0]==0 && v[1]==0) return;
        mc.player.setVelocity(v[0],mc.player.getVelocity().y,v[1]);
    }
}
