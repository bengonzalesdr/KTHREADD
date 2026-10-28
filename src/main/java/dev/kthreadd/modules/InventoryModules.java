package dev.kthreadd.modules;

import dev.kthreadd.*;
import dev.kthreadd.core.*;
import dev.kthreadd.modules.combat.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.*;
import net.minecraft.enchantment.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import java.util.*;

public final class InventoryModules {
    private static int offhandTicks;
    private static Object offhandPlayer;
    private static OffhandPolicy.Choice previousChoice=OffhandPolicy.Choice.NONE;
    private static final MinecraftClient mc=MinecraftClient.getInstance();
    private static Object threatPlayer;
    private static Object threatWorld;
    private static int threatAge=-1;
    private static double threat;
    public static boolean ready() { return mc.player!=null && mc.world!=null && mc.player.isAlive() && mc.interactionManager!=null && mc.currentScreen==null && mc.player.currentScreenHandler==mc.player.playerScreenHandler && mc.player.currentScreenHandler.getCursorStack().isEmpty(); }
    public static int slot(int inventorySlot) { return inventorySlot<9?inventorySlot+36:inventorySlot; }
    /** Two strongest loaded-crystal hits, cached for this player tick. Bounds stacked-crystal reserve. */
    public static double crystalThreat() {
        if(mc.player==null || mc.world==null)return 0;
        if(threatPlayer==mc.player && threatWorld==mc.world && threatAge==mc.player.age)return threat;
        threatPlayer=mc.player;threatWorld=mc.world;threatAge=mc.player.age;
        List<EndCrystalEntity> crystals=mc.world.getEntitiesByClass(EndCrystalEntity.class,mc.player.getBoundingBox().expand(12),e->e.isAlive() && e.squaredDistanceTo(mc.player)<=144);
        // Nearest candidates bound expensive exposure raycasts even in crystal spam scenes.
        crystals.sort(Comparator.comparingDouble(e->e.squaredDistanceTo(mc.player)));
        double[] hits=new double[Math.min(16,crystals.size())];
        for(int i=0;i<hits.length;i++)hits[i]=CombatSupport.damage(crystals.get(i).getPos(),mc.player);
        return threat=CombatInventoryPolicy.crystalThreat(hits);
    }
    private static boolean crystalDanger(ModuleManager manager) {
        var totem=manager.get("AutoTotem");
        return mc.player!=null && totem.enabled() && totem.flag("CrystalDanger") && crystalThreat()>0 && crystalThreat()>=mc.player.getHealth()+mc.player.getAbsorptionAmount()-totem.value("SafetyMargin");
    }
    public static boolean emergencyProtectionNeeded() {
        var client=KthreaddClient.INSTANCE;
        if(client==null || client.modules==null || mc.player==null || mc.world==null)return true;
        if(mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))return false;
        var totem=client.modules.get("AutoTotem");
        var gap=client.modules.get("AutoOffhandGap");var crystal=client.modules.get("AutoOffhandCrystal");
        double health=mc.player.getHealth()+mc.player.getAbsorptionAmount();
        return totem.enabled() && (health<=totem.value("Health") || mc.player.isFallFlying() || crystalDanger(client.modules))
            || gap.enabled() && (health<gap.value("Health") || gap.flag("CrystalCheck") && nearbyCrystal())
            || crystal.enabled() && (health<crystal.value("Health") || crystal.flag("CrystalCheck") && nearbyCrystal());
    }
    private static boolean nearbyCrystal() {
        return !mc.world.getEntitiesByClass(EndCrystalEntity.class,mc.player.getBoundingBox().expand(6),e->e.isAlive()&&e.squaredDistanceTo(mc.player)<36).isEmpty();
    }
    public static boolean armorCritical() {
        if(mc.player==null)return false;
        for(var type:new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD,net.minecraft.entity.EquipmentSlot.CHEST,net.minecraft.entity.EquipmentSlot.LEGS,net.minecraft.entity.EquipmentSlot.FEET}){
            var worn=mc.player.getEquippedStack(type);
            if(worn.isDamageable() && (worn.getMaxDamage()-worn.getDamage())*10<=worn.getMaxDamage())return true;
            if(worn.isEmpty())for(int i=0;i<36;i++)if(mc.player.getInventory().getStack(i).getItem() instanceof ArmorItem armor && armor.getSlotType()==type)return true;
        }
        return false;
    }
    public static void register(ModuleManager manager) {
        manager.add(new ClientModule("AutoTotem",Category.COMBAT,"Protects the offhand against low health, gliding and two-hit crystal threats; leaves active screens and cursors alone.") {
            { number("Health",20,1,22,1); number("Delay",2,1,10,1); flag("CrystalDanger",true); number("SafetyMargin",2,0,10,0.5); }
            public void tick() { /* One centralized offhand transaction in tickOffhand. */ }
        });
        manager.add(new PvpArmorModule());
        manager.add(new ClientModule("AutoTool",Category.PLAYER,"Selects a suitable hotbar mining tool and restores the previous slot unless you select another item.") {
            private int previous=-1,selected=-1; private Object player;
            { number("MinDurability",3,1,100,1); flag("Restore",true); }
            public void tick() {
                if(player!=mc.player){previous=selected=-1;player=mc.player;}
                if(previous>=0 && mc.player!=null && mc.player.getInventory().selectedSlot!=selected)previous=selected=-1;
                if(!ready() || mc.player.isUsingItem() || !mc.options.attackKey.isPressed() || !(mc.crosshairTarget instanceof BlockHitResult hit)) { restore(); return; }
                var state=mc.world.getBlockState(hit.getBlockPos());
                if(state.isAir() || state.getHardness(mc.world,hit.getBlockPos())<0){restore();return;}
                int best=mc.player.getInventory().selectedSlot; double score=-1;
                for(int i=0;i<9;i++) {
                    var stack=mc.player.getInventory().getStack(i);
                    if(stack.isDamageable() && stack.getMaxDamage()-stack.getDamage()<value("MinDurability"))continue;
                    double speed=stack.getMiningSpeedMultiplier(state);
                    int efficiency=EnchantmentHelper.getLevel(Enchantments.EFFICIENCY,stack);
                    if(speed>1 && efficiency>0)speed+=efficiency*efficiency+1;
                    if(state.isToolRequired()&&!stack.isSuitableFor(state))speed*=.01;
                    if(speed>score || speed==score && i==mc.player.getInventory().selectedSlot){score=speed;best=i;}
                }
                if(best!=mc.player.getInventory().selectedSlot && CombatActions.claim("AutoTool")) {
                    if(previous<0)previous=mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot=selected=best;
                }
            }
            private void restore() {
                if(previous>=0 && mc.player!=null && mc.player==player && mc.player.getInventory().selectedSlot==selected && flag("Restore"))mc.player.getInventory().selectedSlot=previous;
                previous=selected=-1;
            }
            protected void onDisable() { restore();player=null; }
        });
        manager.add(new ClientModule("ChestStealer",Category.OTHER,"Quick-moves receivable container stacks one at a time, including merges into a full inventory; never opens containers.") {
            private int ticks,index; private Object handler;
            { number("Delay",4,2,20,1); }
            public void tick() {
                if(mc.player==null || mc.interactionManager==null || !(mc.player.currentScreenHandler instanceof net.minecraft.screen.GenericContainerScreenHandler h) || mc.currentScreen==null || !h.getCursorStack().isEmpty()) { ticks=0;handler=null;return; }
                if(handler!=h){handler=h;ticks=index=0;}
                if(++ticks<value("Delay"))return;ticks=0;
                int count=h.getRows()*9;
                for(int n=0;n<count;n++) {
                    int i=(index+n)%count;var stack=h.getSlot(i).getStack();
                    if(stack.isEmpty() || !h.getSlot(i).canTakeItems(mc.player) || !canReceive(stack))continue;
                    if(!CombatActions.claim("ChestStealer"))return;
                    mc.interactionManager.clickSlot(h.syncId,i,0,SlotActionType.QUICK_MOVE,mc.player);index=(i+1)%count;return;
                }
            }
            protected void onDisable(){ticks=index=0;handler=null;}
        });
    }
    private static boolean canReceive(ItemStack stack) {
        for(int i=0;i<36;i++) {
            var existing=mc.player.getInventory().getStack(i);
            if(existing.isEmpty() || CombatInventoryPolicy.stackRoom(ItemStack.canCombine(existing,stack),existing.getCount(),existing.getMaxCount(),mc.player.getInventory().getMaxCountPerStack()))return true;
        }
        return false;
    }
    private static Item desiredItem(OffhandPolicy.Choice choice) {
        return switch(choice){case TOTEM->Items.TOTEM_OF_UNDYING;case GAP->Items.GOLDEN_APPLE;case CRYSTAL->Items.END_CRYSTAL;default->Items.AIR;};
    }
    private static boolean matches(ItemStack stack,Item wanted){return stack.isOf(wanted)||wanted==Items.GOLDEN_APPLE && stack.isOf(Items.ENCHANTED_GOLDEN_APPLE);}
    private static int inventoryItem(Item wanted){for(int i=0;i<36;i++)if(matches(mc.player.getInventory().getStack(i),wanted))return i;return -1;}
    public static void tickOffhand(ModuleManager manager) {
        if(!ready()) { offhandTicks=0;previousChoice=OffhandPolicy.Choice.NONE;offhandPlayer=null;return; }
        if(offhandPlayer!=mc.player){offhandPlayer=mc.player;offhandTicks=0;previousChoice=OffhandPolicy.Choice.NONE;}
        var totem=manager.get("AutoTotem");var gap=manager.get("AutoOffhandGap");var crystal=manager.get("AutoOffhandCrystal");
        boolean danger=crystalDanger(manager);
        boolean nearby=((gap.enabled()&&gap.flag("CrystalCheck"))||(crystal.enabled()&&crystal.flag("CrystalCheck"))) && nearbyCrystal();
        double threshold=Math.max(gap.enabled()?gap.value("Health"):0,crystal.enabled()?crystal.value("Health"):0);
        double health=mc.player.getHealth()+mc.player.getAbsorptionAmount();
        var choice=OffhandPolicy.choose(totem.enabled(),health,totem.value("Health"),mc.player.isFallFlying(),gap.enabled(),crystal.enabled(),threshold,nearby);
        if(danger)choice=OffhandPolicy.Choice.TOTEM;
        if(choice!=previousChoice){offhandTicks=0;previousChoice=choice;}
        if(choice==OffhandPolicy.Choice.NONE){offhandTicks=0;return;}
        Item item=desiredItem(choice);
        if(matches(mc.player.getOffHandStack(),item)){offhandTicks=0;return;}
        int source=inventoryItem(item);
        var fallback=CombatInventoryPolicy.fallback(CombatInventoryPolicy.ItemChoice.valueOf(choice.name()),source>=0,mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING),inventoryItem(Items.TOTEM_OF_UNDYING)>=0,totem.enabled());
        if(fallback==CombatInventoryPolicy.ItemChoice.KEEP){offhandTicks=0;return;}
        if(fallback==CombatInventoryPolicy.ItemChoice.TOTEM && source<0) { item=Items.TOTEM_OF_UNDYING;source=inventoryItem(item); }
        boolean urgent=choice==OffhandPolicy.Choice.TOTEM;
        ClientModule owner=urgent&&totem.enabled()?totem:gap.enabled()?gap:crystal;
        if(++offhandTicks<owner.value("Delay")&&!danger)return;
        if(source<0 || !CombatActions.claim("Offhand"))return;
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,slot(source),40,SlotActionType.SWAP,mc.player);offhandTicks=0;
    }
}
