package dev.kthreadd.modules;

import dev.kthreadd.*;
import dev.kthreadd.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

public final class CombatModules {
    private static final MinecraftClient mc=MinecraftClient.getInstance();
    public static void register(ModuleManager manager) {
        manager.add(new ClientModule("AutoFeetObby",Category.COMBAT,"Places hotbar obsidian beside the feet using reachable support faces. No packet bypass or inventory spoofing.") {
            private int ticks;
            { number("Delay",3,1,20,1); }
            public void tick() {
                if(!InventoryModules.ready() || !mc.player.isOnGround() || ++ticks<value("Delay")) return; ticks=0;
                int slot=find(Items.OBSIDIAN); if(slot<0) return;
                for(Direction direction:Direction.Type.HORIZONTAL) {
                    BlockPos target=mc.player.getBlockPos().offset(direction);
                    if(!mc.world.getBlockState(target).isReplaceable() || !mc.world.getOtherEntities(null,new Box(target)).isEmpty()) continue;
                    for(Direction face:Direction.values()) {
                        BlockPos support=target.offset(face);
                        if(mc.world.getBlockState(support).isAir() || !mc.world.getBlockState(support).isSolidBlock(mc.world,support)) continue;
                        Vec3d point=Vec3d.ofCenter(support).add(Vec3d.of(face.getOpposite().getVector()).multiply(0.5));
                        if(mc.player.getEyePos().squaredDistanceTo(point)>16) continue;
                        place(slot,new BlockHitResult(point,face.getOpposite(),support,false)); return;
                    }
                }
            }
        });

    }
    private static int find(net.minecraft.item.Item item) { for(int i=0;i<9;i++) if(mc.player.getInventory().getStack(i).isOf(item)) return i; return -1; }
    private static void place(int slot,BlockHitResult hit) {
        if(!dev.kthreadd.modules.combat.CombatActions.claim("AutoFeetObby"))return;
        int old=mc.player.getInventory().selectedSlot;
        try { mc.player.getInventory().selectedSlot=slot; if(mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,hit).isAccepted()) mc.player.swingHand(Hand.MAIN_HAND); }
        finally { mc.player.getInventory().selectedSlot=old; }
    }
}
