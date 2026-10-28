package dev.kthreadd.modules.combat;

import dev.kthreadd.*;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import static dev.kthreadd.modules.combat.CombatSupport.mc;

public final class CombatHooks {
    private static long previous;
    private static float tps=20;
    private static Object criticalPlayer;
    private static int criticalAge=-1;
    public static float tps(){return tps;}
    public static void time() {
        long now=System.nanoTime();
        if(previous>0 && now-previous<5_000_000_000L) {
            float sample=(float)Math.max(1,Math.min(20,20_000_000_000d/(now-previous)));
            tps=tps*.75f+sample*.25f;
        }else tps=20;
        previous=now;
    }
    public static void critical(Entity target){critical(target,false);}
    public static void critical(Entity target,boolean requested){prepareAttack(target,requested);}
    /** False postpones a Jump-mode melee attack until natural descent. Crystal attacks are untouched. */
    public static boolean prepareAttack(Entity target,boolean requested) {
        var client=KthreaddClient.INSTANCE;
        if(client==null || client.modules==null || (!requested && !client.modules.on("Criticals")) || mc.player==null || !(target instanceof LivingEntity))return true;
        var p=mc.player;
        if(p.isTouchingWater() || p.isInLava() || p.hasVehicle() || p.isClimbing() || p.isFallFlying() || p.getAbilities().flying || p.isSprinting() || p.hasStatusEffect(StatusEffects.BLINDNESS) || p.hasStatusEffect(StatusEffects.SLOW_FALLING) || !CombatSupport.canAttackWithCurrentItem())return true;
        if(client.modules.get("Criticals").setting("Mode").integer()==1) {
            if(p.isOnGround()){p.jump();return false;}
            return p.fallDistance>0 && p.getVelocity().y<0;
        }
        if(!p.isOnGround() || p.getAttackCooldownProgress(0)<.9f || !mc.world.isSpaceEmpty(p,p.getBoundingBox().offset(0,.0625,0)))return true;
        // Aura and its attack mixin can both call us; send at most one offset pair per player tick.
        if(criticalPlayer==p && criticalAge==p.age)return true;
        criticalPlayer=p;criticalAge=p.age;
        for(double y:new double[]{.0625,0})p.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(),p.getY()+y,p.getZ(),false));
        return true;
    }
}
