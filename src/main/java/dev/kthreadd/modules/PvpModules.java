package dev.kthreadd.modules;

import dev.kthreadd.*;
import dev.kthreadd.core.*;
import dev.kthreadd.modules.combat.*;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.*;
import java.util.*;
import static dev.kthreadd.modules.combat.CombatSupport.mc;

public final class PvpModules {
    private static boolean chainOwn;
    private static int chainUntil;
    private static net.minecraft.client.network.ClientPlayerEntity chainPlayer;
    public static void chainPop() {
        var client=KthreaddClient.INSTANCE;
        if(client==null || client.modules==null || mc.player==null)return;
        var feet=client.modules.get("AutoFeetObby");
        // A second pop renews the protection window rather than being ignored.
        if(chainOwn && chainPlayer==mc.player){chainUntil=mc.player.age+100;return;}
        if(feet.enabled())return;
        chainOwn=true;chainPlayer=mc.player;chainUntil=mc.player.age+100;feet.setEnabled(true);
    }
    private static void releaseChain(ModuleManager manager) {
        if(chainOwn)manager.get("AutoFeetObby").setEnabled(false);
        chainOwn=false;chainPlayer=null;chainUntil=0;
    }
    public static void register(ModuleManager manager) {
        manager.add(new ClientModule("AntiChainPop",Category.COMBAT,"Temporarily enables AutoFeetObby after a local pop, renews on repeated pops and releases only the surround it enabled.") {
            protected void onDisable(){releaseChain(manager);}
        });
        manager.add(new ClientModule("AutoCrystalArmor",Category.COMBAT,"Runs protection-ranked armor selection around live nearby crystals; shares AutoArmor settings and transaction guard.") {
            { number("Range",10,1,16,1); }
            public void tick() {
                if(!InventoryModules.ready() || manager.on("AutoArmor"))return;
                double range=value("Range");
                if(!mc.world.getEntitiesByClass(EndCrystalEntity.class,mc.player.getBoundingBox().expand(range),e->e.isAlive()&&e.squaredDistanceTo(mc.player)<=range*range).isEmpty())manager.get("AutoArmor").tick();
            }
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(!chainOwn)return;
            var feet=manager.get("AutoFeetObby");
            if(mc.world==null || mc.player!=chainPlayer || !manager.on("AntiChainPop")){releaseChain(manager);return;}
            boolean complete=true;
            for(Direction direction:Direction.Type.HORIZONTAL)if(mc.world.getBlockState(mc.player.getBlockPos().offset(direction)).isReplaceable())complete=false;
            if(!feet.enabled() || complete || mc.player.age>=chainUntil)releaseChain(manager);
        });
        manager.add(new CrystalCombatModule("CrystalAura"));manager.add(new KillAuraModule());manager.add(trap("AutoTrap",false));manager.add(new SelfTrapModule());
        manager.add(new ClientModule("Aimbot",Category.COMBAT,"Shared combat rotation service: Packet sends look updates; Client also turns the camera.") {{mode("Mode","Packet","Client");}});
        manager.add(new ClientModule("Criticals",Category.COMBAT,"Guards melee critical attempts: Packet uses bounded movement offsets; Jump waits for descent before attacking.") {{mode("Mode","Packet","Jump");}});
        manager.add(new ClientModule("FastCrystal",Category.COMBAT,"Removes local use delay while actively using a held crystal with no screen open."));
        manager.add(new ClientModule("FastBow",Category.COMBAT,"Releases the held bow at the configured charge using normal item interaction; full charge is 20 ticks.") {
            {number("ChargeTicks",20,3,40,1);}
            public void tick() {
                if(!InventoryModules.ready() || !mc.player.isUsingItem() || !(mc.player.getActiveItem().getItem() instanceof BowItem))return;
                boolean usable=mc.player.getAbilities().creativeMode || !mc.player.getProjectileType(mc.player.getActiveItem()).isEmpty();
                if(CombatInventoryPolicy.releaseBow(mc.player.getItemUseTime(),(int)value("ChargeTicks"),usable) && CombatActions.claim("FastBow"))mc.interactionManager.stopUsingItem(mc.player);
            }
        });
    }
    private static ClientModule trap(String name,boolean feet) {
        return new ClientModule(name,Category.COMBAT,"Builds a supported cage around the nearest non-friend, with a stable target and bounded normal placements.") {
            private String announced="";private UUID targetId;
            {number(feet?"Distance":"range",5,1,10,0.5);number("BlocksPerTick",4,1,10,1);flag("Rotate",true);flag(feet?"Toggles":"toggleMode",!feet);if(!feet){mode("Mode","Full","Tall");flag("announceUsage",true);flag("EChests",false);}}
            public void tick() {
                if(!InventoryModules.ready() || mc.player.isUsingItem())return;
                var targets=CombatSupport.players(value(feet?"Distance":"range"));
                if(targets.isEmpty()){targetId=null;announced="";return;}
                var target=targets.stream().filter(p->p.getUuid().equals(targetId)).findFirst().orElse(targets.get(0));targetId=target.getUuid();
                if(!feet && flag("announceUsage") && !announced.equals(target.getName().getString())) {announced=target.getName().getString();KthreaddClient.INSTANCE.notify("AutoTrap: "+announced);}
                var base=target.getBlockPos();List<BlockPos> positions=new ArrayList<>();
                // Bottom ring first gives higher ring/roof placements a real support path.
                for(int y=0;y<=(feet?0:2);y++)for(Direction direction:Direction.Type.HORIZONTAL)positions.add(base.offset(direction).up(y));
                if(!feet){positions.add(base.north().up(3));positions.add(base.up(3));if(setting("Mode").integer()==1){positions.add(base.north().up(4));positions.add(base.up(4));}}
                Item material=!feet && flag("EChests") && CombatSupport.find(Items.ENDER_CHEST)>=0?Items.ENDER_CHEST:Items.OBSIDIAN;
                int placed=0;boolean complete=true;
                for(var pos:positions) {
                    if(!mc.world.getBlockState(pos).isReplaceable())continue;
                    complete=false;
                    if(placed>=value("BlocksPerTick"))continue;
                    if(CombatSupport.place(pos,material,flag("Rotate")))placed++;
                    else if(pos.getY()==base.getY() && mc.world.getBlockState(pos.down()).isReplaceable() && CombatSupport.place(pos.down(),material,flag("Rotate")))placed++;
                }
                // Completion is read from world state, never inferred from an accepted interaction.
                if(complete && flag(feet?"Toggles":"toggleMode"))setEnabled(false);
            }
            protected void onDisable(){announced="";targetId=null;}
        };
    }
}
