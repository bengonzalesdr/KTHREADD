package dev.kthreadd.modules;
import dev.kthreadd.*;
import dev.kthreadd.core.*;
import dev.kthreadd.modules.combat.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import static dev.kthreadd.modules.combat.CombatSupport.mc;

public final class PvpModules {
 private static boolean chainOwn;private static long chainUntil;private static net.minecraft.client.network.ClientPlayerEntity chainPlayer;
 public static void chainPop(){var m=KthreaddClient.INSTANCE.modules.get("AutoFeetObby");if(m.enabled())return;chainOwn=true;chainPlayer=mc.player;chainUntil=System.currentTimeMillis()+5000;m.setEnabled(true);}
 public static void register(ModuleManager manager){
manager.add(new ClientModule("AntiChainPop",Category.COMBAT,"Temporarily enables AutoFeetObby after a local totem pop; preserves a surround that was already enabled."));
manager.add(new ClientModule("AutoCrystalArmor",Category.COMBAT,"Runs protection-ranked armor selection when a loaded crystal is nearby; shares the AutoArmor settings and transaction guard."){ {number("Range",10,1,16,1);} public void tick(){if(manager.on("AutoArmor"))return;for(var e:mc.world.getEntities())if(e instanceof net.minecraft.entity.decoration.EndCrystalEntity&&e.squaredDistanceTo(mc.player)<=value("Range")*value("Range")){manager.get("AutoArmor").tick();return;}}});
net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{if(!chainOwn)return;var feet=manager.get("AutoFeetObby");if(mc.world==null||mc.player!=chainPlayer){chainOwn=false;chainPlayer=null;return;}boolean complete=true;for(Direction d:Direction.Type.HORIZONTAL)if(mc.world.getBlockState(mc.player.getBlockPos().offset(d)).isReplaceable())complete=false;if(!feet.enabled()||complete||System.currentTimeMillis()>=chainUntil){chainOwn=false;chainPlayer=null;feet.setEnabled(false);}});
manager.add(new CrystalCombatModule("CrystalAura")); manager.add(new KillAuraModule()); manager.add(trap("AutoTrap",false)); manager.add(new SelfTrapModule());
manager.add(new ClientModule("Aimbot",Category.COMBAT,"Rotation service: Packet sends look updates; Client also turns the camera."){{mode("Mode","Packet","Client");}});
manager.add(new ClientModule("Criticals",Category.COMBAT,"Attempts critical hits before attacks through modern movement packets."){{mode("Mode","Packet","Jump");}});
manager.add(new ClientModule("FastCrystal",Category.COMBAT,"Removes the local use delay only while holding end crystals."));
manager.add(new ClientModule("FastBow",Category.COMBAT,"Releases a held bow after three use ticks using the normal interaction manager."){public void tick(){if(mc.player.isUsingItem()&&mc.player.getActiveItem().getItem() instanceof BowItem&&mc.player.getItemUseTime()>=3)mc.interactionManager.stopUsingItem(mc.player);}});
}
 private static ClientModule trap(String name,boolean feet){return new ClientModule(name,Category.COMBAT,"Places a reachable cage around the nearest non-friend player using normal placement."){
  private String announced="";
  {number(feet?"Distance":"range",5,1,10,0.5);number("BlocksPerTick",4,1,10,1);flag("Rotate",true);flag(feet?"Toggles":"toggleMode",!feet);if(!feet){mode("Mode","Full","Tall");flag("announceUsage",true);flag("EChests",false);}}
  public void tick(){if(!InventoryModules.ready())return;var targets=CombatSupport.players(value(feet?"Distance":"range"));if(targets.isEmpty())return;var target=targets.get(0);if(!feet&&flag("announceUsage")&&!announced.equals(target.getName().getString())){announced=target.getName().getString();KthreaddClient.INSTANCE.notify("AutoTrap: "+announced);}var base=target.getBlockPos();List<BlockPos> positions=new ArrayList<>();for(int y=0;y<=(feet?0:2);y++)for(Direction d:Direction.Type.HORIZONTAL)positions.add(base.offset(d).up(y));if(!feet){positions.add(base.north().up(3));positions.add(base.up(3));if(setting("Mode").integer()==1)positions.add(base.up(4));}int placed=0;boolean complete=true;for(var pos:positions)if(mc.world.getBlockState(pos).isReplaceable()){complete=false;if(placed<value("BlocksPerTick")&&CombatSupport.place(pos,!feet&&flag("EChests")?Items.ENDER_CHEST:Items.OBSIDIAN,flag("Rotate")))placed++;}if(complete&&flag(feet?"Toggles":"toggleMode"))setEnabled(false);}
  protected void onDisable(){announced="";}
 };}
}
