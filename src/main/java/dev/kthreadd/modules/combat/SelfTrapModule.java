package dev.kthreadd.modules.combat;
import dev.kthreadd.KthreaddClient;
import dev.kthreadd.core.*;
import dev.kthreadd.modules.InventoryModules;
import net.minecraft.item.Items;
import net.minecraft.util.math.*;
import java.util.*;
import static dev.kthreadd.modules.combat.CombatSupport.mc;
public final class SelfTrapModule extends ClientModule {
 private int ticks;
 public SelfTrapModule(){super("SelfTrap",Category.COMBAT,"Places a supported overhead block or cage. Smart activates when a nearby enemy is within Range; normal server placement limits apply.");number("Range",4.5,1,8,0.5);number("BlocksPerTick",2,1,8,1);number("TickDelay",2,1,10,1);mode("Mode","BLOCKOVERHEAD","TRAP");flag("Rotate",true);flag("Smart",false);flag("DisableOnPlace",false);flag("DisableCAOnPlace",false);}
 public void tick(){if(!InventoryModules.ready()||!mc.player.isOnGround()||mc.player.isUsingItem()){ticks=0;return;}if(++ticks<value("TickDelay"))return;ticks=0;if(flag("Smart")&&CombatSupport.players(value("Range")).isEmpty())return;
  BlockPos base=mc.player.getBlockPos();List<BlockPos> positions=new ArrayList<>();if(setting("Mode").integer()==1)for(int y=0;y<2;y++)for(Direction d:Direction.Type.HORIZONTAL)positions.add(base.offset(d).up(y));
  // Include a side support so overhead placement can succeed without an existing roof.
  positions.add(base.north());positions.add(base.north().up());positions.add(base.north().up(2));positions.add(base.up(2));int placed=0;
  for(var pos:positions)if(placed<value("BlocksPerTick")&&CombatSupport.place(pos,Items.OBSIDIAN,flag("Rotate"))){placed++;if(flag("DisableCAOnPlace"))KthreaddClient.INSTANCE.modules.get("CrystalAura").setEnabled(false);}
  // Stop only after the intended roof is observed; a side scaffold is not a completed self trap.
  boolean complete=!mc.world.getBlockState(base.up(2)).isReplaceable();
  if(setting("Mode").integer()==1)for(int y=0;y<2;y++)for(Direction d:Direction.Type.HORIZONTAL)if(mc.world.getBlockState(base.offset(d).up(y)).isReplaceable())complete=false;
  if(flag("DisableOnPlace")&&complete)setEnabled(false);
 }
 protected void onDisable(){ticks=0;}
}
