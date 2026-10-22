package dev.kthreadd.modules;

import dev.kthreadd.*;
import dev.kthreadd.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Hand;

public final class InventoryModules {
    private static int offhandTicks;
    private static final MinecraftClient mc=MinecraftClient.getInstance();
    public static boolean ready() { return mc.player!=null && mc.interactionManager!=null && mc.currentScreen==null && mc.player.currentScreenHandler==mc.player.playerScreenHandler && mc.player.currentScreenHandler.getCursorStack().isEmpty(); }
    public static int slot(int inventorySlot) { return inventorySlot<9?inventorySlot+36:inventorySlot; }
    private static boolean crystalDanger(ModuleManager manager) {
        var totem=manager.get("AutoTotem");
        if(mc.player==null || mc.world==null || !totem.enabled() || !totem.flag("CrystalDanger"))return false;
        double health=mc.player.getHealth()+mc.player.getAbsorptionAmount();
        for(var entity:mc.world.getEntities()) if(entity instanceof net.minecraft.entity.decoration.EndCrystalEntity && entity.squaredDistanceTo(mc.player)<=144
            && dev.kthreadd.modules.combat.CombatSupport.damage(entity.getPos(),mc.player)>=health-totem.value("SafetyMargin"))return true;
        return false;
    }
    public static boolean emergencyProtectionNeeded() {
        var client=KthreaddClient.INSTANCE;
        if(client==null || client.modules==null || mc.player==null || mc.world==null)return true;
        if(mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))return false;
        var totem=client.modules.get("AutoTotem");
        return totem.enabled() && (mc.player.getHealth()+mc.player.getAbsorptionAmount()<=totem.value("Health") || mc.player.isFallFlying() || crystalDanger(client.modules));
    }
    public static boolean armorCritical() {
        if(mc.player==null)return false;
        for(var stack:mc.player.getArmorItems())if(stack.isDamageable() && (stack.getMaxDamage()-stack.getDamage())*10<=stack.getMaxDamage())return true;
        return false;
    }
    public static void register(ModuleManager manager) {
        manager.add(new ClientModule("AutoTotem",Category.COMBAT,"Moves a totem to the offhand with normal inventory swap; waits while a screen/cursor is active.") {
            private int delay;
            { number("Health",20,1,22,1); number("Delay",2,1,10,1); flag("CrystalDanger",true); number("SafetyMargin",2,0,10,0.5); }
            public void tick() {
                // Centralized in tickOffhand: one decision/transaction per delay across all offhand modules.
            }
        });
        manager.add(new dev.kthreadd.modules.combat.PvpArmorModule());
        manager.add(new ClientModule("AutoTool",Category.PLAYER,"Selects the best hotbar tool for the targeted block while mining; restores the prior slot on release.") {
            private int previous=-1;
            public void tick() {
                if(!ready() || !mc.options.attackKey.isPressed() || !(mc.crosshairTarget instanceof BlockHitResult hit)) { restore(); return; }
                var state=mc.world.getBlockState(hit.getBlockPos()); int best=mc.player.getInventory().selectedSlot; float speed=1;
                for(int i=0;i<9;i++) { var stack=mc.player.getInventory().getStack(i); float v=stack.getMiningSpeedMultiplier(state); if(v>speed && (!stack.isDamageable() || stack.getMaxDamage()-stack.getDamage()>2)) { speed=v; best=i; } }
                if(best!=mc.player.getInventory().selectedSlot) { if(previous<0) previous=mc.player.getInventory().selectedSlot; mc.player.getInventory().selectedSlot=best; }
            }
            private void restore() { if(previous>=0 && mc.player!=null) { mc.player.getInventory().selectedSlot=previous; previous=-1; } }
            protected void onDisable() { restore(); }
        });
        manager.add(new ClientModule("ChestStealer",Category.OTHER,"Quick-moves container items one at a time while its screen is open; no automatic opening.") {
            private int ticks;
            { number("Delay",4,2,20,1); }
            public void tick() {
                if(mc.interactionManager==null || !(mc.player.currentScreenHandler instanceof net.minecraft.screen.GenericContainerScreenHandler h) || !h.getCursorStack().isEmpty() || ++ticks<value("Delay")) return;
                ticks=0; if(mc.player.getInventory().getEmptySlot()<0) return;
                for(int i=0;i<h.getRows()*9;i++) if(!h.getSlot(i).getStack().isEmpty()) { mc.interactionManager.clickSlot(h.syncId,i,0,SlotActionType.QUICK_MOVE,mc.player); return; }
            }
        });
    }
    public static void tickOffhand(ModuleManager manager) {
        if(!ready()) { offhandTicks=0; return; }
        var totem=manager.get("AutoTotem"); var gap=manager.get("AutoOffhandGap"); var crystal=manager.get("AutoOffhandCrystal");
        boolean danger=crystalDanger(manager);
        boolean nearby=false;
        if((gap.enabled() && gap.flag("CrystalCheck")) || (crystal.enabled() && crystal.flag("CrystalCheck")))
            for(var e:mc.world.getEntities()) if(e instanceof net.minecraft.entity.decoration.EndCrystalEntity && e.squaredDistanceTo(mc.player)<36) { nearby=true; break; }
        double threshold=gap.enabled()?gap.value("Health"):crystal.value("Health");
        var choice=OffhandPolicy.choose(totem.enabled(),mc.player.getHealth()+mc.player.getAbsorptionAmount(),totem.value("Health"),mc.player.isFallFlying(),gap.enabled(),crystal.enabled(),threshold,nearby);
        if(danger) choice=OffhandPolicy.Choice.TOTEM;
        if(choice==OffhandPolicy.Choice.NONE) { offhandTicks=0; return; }
        boolean totemRequested=totem.enabled() && (mc.player.getHealth()+mc.player.getAbsorptionAmount()<=totem.value("Health") || mc.player.isFallFlying());
        ClientModule owner=totemRequested||danger?totem:gap.enabled()?gap:crystal;
        if(++offhandTicks<owner.value("Delay") && !danger) return; offhandTicks=0;
        Item item=switch(choice) { case TOTEM -> Items.TOTEM_OF_UNDYING; case GAP -> Items.GOLDEN_APPLE; case CRYSTAL -> Items.END_CRYSTAL; default -> Items.AIR; };
        if(mc.player.getOffHandStack().isOf(item)) return;
        for(int i=0;i<36;i++) if(mc.player.getInventory().getStack(i).isOf(item)) { if(!dev.kthreadd.modules.combat.CombatActions.claim("Offhand"))return; mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,slot(i),40,SlotActionType.SWAP,mc.player); return; }
    }
}
