package dev.kthreadd.modules;

import dev.kthreadd.*;
import dev.kthreadd.core.*;
import dev.kthreadd.movement.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import java.util.*;

public final class IngrosModules {
    private static final MinecraftClient mc=MinecraftClient.getInstance();
    public static void register(ModuleManager manager) {
        for(String name:List.of("AutoOffhandGap","AutoOffhandCrystal")) manager.add(new ClientModule(name,Category.COMBAT,"Normal offhand inventory swap above Health; falls back to a totem below Health or near crystals. AutoTotem takes priority; gap wins over crystal.") {{ number("Health",12,1,36,1); flag("CrystalCheck",true); number("Delay",2,1,10,1); }});
        manager.add(new ClientModule("AutoLog",Category.COMBAT,"Normal disconnect on multiplayer at low health without a totem. No invalid packets; disables itself after firing.") {
            private int dangerTicks;
            { number("Health",6,1,20,1); flag("CheckTotems",true); number("Delay",1,1,20,1); }
            public void tick() {
                if(mc.isInSingleplayer() || mc.getNetworkHandler()==null || !mc.player.isAlive() || mc.player.isSpectator() || mc.player.getHealth()+mc.player.getAbsorptionAmount()>value("Health")) { dangerTicks=0; return; }
                if(flag("CheckTotems")) for(int i=0;i<mc.player.getInventory().size();i++) if(mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) { dangerTicks=0; return; }
                if(++dangerTicks<value("Delay")) return;
                setEnabled(false); dangerTicks=0; mc.getNetworkHandler().getConnection().disconnect(Text.literal("KTHREADD AutoLog: low health"));
            }
            protected void onEnable() { dangerTicks=0; }
            protected void onDisable() { dangerTicks=0; }
        });
        manager.add(new ClientModule("NoVelocity",Category.COMBAT,"Scales inbound entity velocity and explosion knockback for the local player. Servers may correct motion.") {{ number("Horizontal",0,0,1,0.1); number("Vertical",0,0,1,0.1); flag("EntityVelocity",true); flag("Explosions",true); }});
        manager.add(new ClientModule("AntiFriendHit",Category.OTHER,"Cancels direct attacks on local friend names. Manage with /kthread friend."));
        manager.add(new ClientModule("NoPush",Category.MOVEMENT,"Cancels local entity collision pushing; does not alter other players or global physics."));
        manager.add(new ClientModule("Flight",Category.MOVEMENT,"Direct local flight velocity, jump/sneak for vertical motion. No packet bypass; multiplayer servers may reject it.") {
            { number("Speed",0.5,0.1,2,0.1); number("Vertical",0.3,0.05,1,0.05); }
            public void tick() {
                if(!MovementControl.owns(manager,name)) return;
                MovementModules.move(value("Speed"),true); var v=mc.player.getVelocity(); mc.player.setVelocity(v.x,((mc.options.jumpKey.isPressed()?1:0)-(mc.options.sneakKey.isPressed()?1:0))*value("Vertical"),v.z);
                mc.player.fallDistance=0;
            }

        });
        manager.add(new ClientModule("Strafe",Category.MOVEMENT,"Applies configurable horizontal control in the air. Simplified modern equivalent.") {
            { number("Speed",0.28,0.1,0.8,0.02); }
            public void tick() { if(MovementControl.owns(manager,name)) MovementModules.move(value("Speed"),false); }
        });
        manager.add(new ClientModule("FastIce",Category.MOVEMENT,"Changes only this player's horizontal movement while standing on ice; no global block friction mutation.") {
            { number("Speed",0.5,0.1,1.5,0.05); }
            public void tick() { if(MovementControl.owns(manager,name)) MovementModules.move(value("Speed"),false); }
        });
        manager.add(new ClientModule("InvWalk",Category.PLAYER,"Physical movement in inventory/container screens without text fields. Text screens, pause, death and focus loss release keys.") {
            private boolean held;
            { flag("Jump",true); flag("Sprint",true); }
            private List<net.minecraft.client.option.KeyBinding> keys() { return List.of(mc.options.forwardKey,mc.options.backKey,mc.options.leftKey,mc.options.rightKey,mc.options.jumpKey,mc.options.sprintKey); }
            public void tick() {
                boolean allowed=MovementControl.playable() && mc.isWindowFocused() && !mc.isPaused() && mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> && mc.currentScreen.children().stream().noneMatch(e -> e instanceof TextFieldWidget);
                if(!allowed) { if(held) release(); return; }
                for(var key:keys()) key.setPressed((key!=mc.options.jumpKey || flag("Jump")) && (key!=mc.options.sprintKey || flag("Sprint")) && MovementControl.physical(key)); held=true;
            }
            private void release() { for(var key:keys()) MovementControl.restore(key); held=false; }
            protected void onDisable() { if(held) release(); }
        });
        manager.add(new ClientModule("AntiVoid",Category.PLAYER,"Attempts upward motion near the world bottom. Partial local rescue only, not guaranteed protection from void death.") {
            { number("Margin",2,1,16,1); number("Boost",0.5,0.1,1,0.1); }
            public void tick() { if(!manager.on("Flight") && MovementPolicy.rescue(mc.player.isAlive(),mc.player.isSpectator(),mc.player.hasVehicle(),mc.player.getAbilities().flying || mc.player.isFallFlying(),mc.player.getY(),mc.world.getBottomY(),mc.player.getVelocity().y,value("Margin"))) mc.player.setVelocity(mc.player.getVelocity().x,value("Boost"),mc.player.getVelocity().z); }
        });
        manager.add(new ClientModule("NoBossBar",Category.RENDER,"Hides vanilla boss bars while enabled."));
        manager.add(new ClientModule("NoHurtcam",Category.OTHER,"Suppresses the hurt camera tilt while enabled."));
        manager.add(new ClientModule("NoLag",Category.OTHER,"Limits client particle spawning to a configurable per-tick budget. Does not delete entities or items.") {{ number("ParticlesPerTick",64,0,256,16); number("MaxDistance",128,16,256,16); }});
        manager.add(new ClientModule("TotemPopCounter",Category.OTHER,"Counts observed totem-use entity statuses during this connection; local notifications and TargetHUD count.") {{ flag("Notify",true); flag("Self",true); }});
    }
}
