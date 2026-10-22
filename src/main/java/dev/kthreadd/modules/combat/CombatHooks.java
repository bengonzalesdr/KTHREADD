package dev.kthreadd.modules.combat;
import dev.kthreadd.*;
import net.minecraft.entity.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import static dev.kthreadd.modules.combat.CombatSupport.mc;
public final class CombatHooks {
 private static long previous;private static float tps=20;
 public static float tps(){return tps;}
 public static void time(){long now=System.nanoTime();if(previous>0)tps=(float)Math.max(1,Math.min(20,20_000_000_000d/(now-previous)));previous=now;}
 public static void critical(Entity target){critical(target,false);}
 public static void critical(Entity target,boolean requested){var c=KthreaddClient.INSTANCE;if(c==null||c.modules==null||(!requested&&!c.modules.on("Criticals"))||mc.player==null||!(target instanceof LivingEntity)||!mc.player.isOnGround()||mc.player.isTouchingWater()||mc.player.isInLava()||mc.player.hasVehicle()||mc.player.isClimbing())return;
  if(c.modules.get("Criticals").setting("Mode").integer()==1){mc.player.jump();return;}var p=mc.player;for(double y:new double[]{0.0625,0})p.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(),p.getY()+y,p.getZ(),false));
 }
}
