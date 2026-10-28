package dev.kthreadd.modules;

import dev.kthreadd.*;
import dev.kthreadd.core.*;
import dev.kthreadd.modules.combat.CombatSupport;
import net.minecraft.item.Items;
import net.minecraft.util.math.*;
import static dev.kthreadd.modules.combat.CombatSupport.mc;

public final class CombatModules {
    public static void register(ModuleManager manager) {
        manager.add(new ClientModule("AutoFeetObby",Category.COMBAT,"Builds a supported obsidian surround, including floor support when needed; uses normal reachable placements.") {
            private int ticks;
            { number("Delay",3,1,20,1); flag("FloorSupport",true); flag("Rotate",true); }
            public void tick() {
                if(!InventoryModules.ready() || !mc.player.isOnGround() || mc.player.isUsingItem()){ticks=0;return;}
                if(++ticks<value("Delay"))return;ticks=0;
                BlockPos base=mc.player.getBlockPos();
                for(Direction direction:Direction.Type.HORIZONTAL) {
                    BlockPos target=base.offset(direction);
                    if(!mc.world.getBlockState(target).isReplaceable())continue;
                    if(CombatSupport.place(target,Items.OBSIDIAN,flag("Rotate")))return;
                    if(flag("FloorSupport") && mc.world.getBlockState(target.down()).isReplaceable() && CombatSupport.place(target.down(),Items.OBSIDIAN,flag("Rotate")))return;
                }
            }
            protected void onDisable(){ticks=0;}
        });
    }
}
