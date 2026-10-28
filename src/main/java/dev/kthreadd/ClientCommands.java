package dev.kthreadd;

import dev.kthreadd.core.*;
import com.mojang.brigadier.arguments.*;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public final class ClientCommands {
    public static void register(KthreaddClient c) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access) -> dispatcher.register(literal("kthread")
            .executes(ctx -> reply(ctx.getSource(),"Delete: ClickGui | Right Shift: HUD editor | /kthread toggle/bind/profile/waypoint/privacy/search/friend"))
            .then(literal("combatstats").executes(ctx -> reply(ctx.getSource(),((dev.kthreadd.modules.combat.CrystalCombatModule)c.modules.get("CrystalAura")).diagnostics())))
            .then(literal("preset").then(argument("name",StringArgumentType.word()).executes(ctx->{
                try {CombatPresets.apply(c.modules.all,CombatPresets.changes(StringArgumentType.getString(ctx,"name")));c.changed();return reply(ctx.getSource(),"Applied CPvP settings. Module toggles are unchanged; enable CrystalAura / AutoTotem / AutoArmor as desired.");}
                catch(IllegalArgumentException error){return reply(ctx.getSource(),error.getMessage());}
            })))
            .then(literal("set").then(argument("module",StringArgumentType.word()).then(argument("setting",StringArgumentType.string()).then(argument("value",StringArgumentType.greedyString()).executes(ctx->{
                try{
                    var module=c.modules.get(StringArgumentType.getString(ctx,"module"));if(module==null)return reply(ctx.getSource(),"Unknown module");
                    var setting=module.setting(StringArgumentType.getString(ctx,"setting"));String value=StringArgumentType.getString(ctx,"value");
                    if(setting.isText())setting.setText(value);
                    else if(setting.isMode()){var old=setting.text();setting.setText(value);if(!setting.text().equalsIgnoreCase(value))throw new IllegalArgumentException("Invalid mode; current mode: "+old);}
                    else{double number=value.equalsIgnoreCase("on")||value.equalsIgnoreCase("true")?1:value.equalsIgnoreCase("off")||value.equalsIgnoreCase("false")?0:Double.parseDouble(value);if(!Double.isFinite(number)||number<setting.min||number>setting.max)throw new IllegalArgumentException("Value must be between "+setting.min+" and "+setting.max);setting.set(number);}
                    c.changed();return reply(ctx.getSource(),module.name+" / "+setting.name+": "+setting.display());
                }catch(IllegalArgumentException|java.util.NoSuchElementException error){return reply(ctx.getSource(),"Invalid setting/value: "+error.getMessage());}
            })))) )
            .then(literal("combatreport").executes(ctx -> {
                try { return reply(ctx.getSource(),"Saved combat report: "+((dev.kthreadd.modules.combat.CrystalCombatModule)c.modules.get("CrystalAura")).exportReport()); }
                catch(java.io.IOException error) { return reply(ctx.getSource(),"Could not save combat report: "+error.getMessage()); }
            }))
            .then(literal("friend")
                .then(literal("list").executes(ctx -> reply(ctx.getSource(),String.join(", ",c.profile.friends))))
                .then(literal("add").then(argument("name",StringArgumentType.word()).executes(ctx -> {
                    String name=StringArgumentType.getString(ctx,"name"); if(!name.matches("[A-Za-z0-9_]{1,16}")) return reply(ctx.getSource(),"Use a Minecraft player name (1-16 letters/digits/_)");
                    if(!c.isFriend(name)) c.profile.friends.add(name.toLowerCase(java.util.Locale.ROOT)); c.changed(); return reply(ctx.getSource(),"Friend added: "+name);
                })))
                .then(literal("remove").then(argument("name",StringArgumentType.word()).executes(ctx -> { c.profile.friends.removeIf(n -> n.equalsIgnoreCase(StringArgumentType.getString(ctx,"name"))); c.changed(); return reply(ctx.getSource(),"Friend removed"); }))))
            .then(literal("toggle").then(argument("module",StringArgumentType.word()).executes(ctx -> {
                var m=c.modules.get(StringArgumentType.getString(ctx,"module")); if(m==null) return reply(ctx.getSource(),"Unknown module"); m.toggle(); return reply(ctx.getSource(),m.name+": "+m.enabled());
            })))
            .then(literal("privacy").executes(ctx -> { c.modules.privacy.toggle(); return reply(ctx.getSource(),"Privacy: "+c.privacy()); }))
            .then(literal("bind").then(argument("module",StringArgumentType.word()).then(argument("keycode",IntegerArgumentType.integer(-1,348)).executes(ctx -> {
                var m=c.modules.get(StringArgumentType.getString(ctx,"module")); if(m==null) return reply(ctx.getSource(),"Unknown module"); m.key=IntegerArgumentType.getInteger(ctx,"keycode"); c.changed(); return reply(ctx.getSource(),"Bound "+m.name+" to GLFW "+m.key+" (-1 clears)");
            }))))
            .then(literal("profile")
                .then(literal("list").executes(ctx -> { try { return reply(ctx.getSource(),"Active: "+c.activeProfile+" | "+String.join(", ",c.store.list())); } catch(Exception e) { return reply(ctx.getSource(),e.getMessage()); } }))
                .then(literal("save").then(argument("name",StringArgumentType.word()).executes(ctx -> { try { c.saveAs(StringArgumentType.getString(ctx,"name")); return reply(ctx.getSource(),"Saved "+c.activeProfile); } catch(Exception e) { return reply(ctx.getSource(),"Save failed: "+e.getMessage()); } })))
                .then(literal("load").then(argument("name",StringArgumentType.word()).executes(ctx -> {
                    try { String name=StringArgumentType.getString(ctx,"name"); if(!c.store.list().contains(name)) return reply(ctx.getSource(),"Profile does not exist"); c.save(); c.load(name); return reply(ctx.getSource(),"Loaded "+c.activeProfile); } catch(Exception e) { return reply(ctx.getSource(),"Load failed: "+e.getMessage()); }
                }))))
            .then(literal("waypoint")
                .then(literal("add").then(argument("name",StringArgumentType.word()).executes(ctx -> {
                    var mc=MinecraftClient.getInstance(); if(mc.player==null || mc.world==null) return 0;
                    String name=StringArgumentType.getString(ctx,"name"); if(name.length()>40) return reply(ctx.getSource(),"Name limit: 40 characters");
                    c.profile.waypoints.removeIf(w -> w.name.equalsIgnoreCase(name));
                    c.profile.waypoints.add(new Profile.Waypoint(name,mc.world.getRegistryKey().getValue().toString(),mc.player.getX(),mc.player.getY(),mc.player.getZ())); c.changed(); return reply(ctx.getSource(),"Waypoint saved: "+name);
                })))
                .then(literal("remove").then(argument("name",StringArgumentType.word()).executes(ctx -> { boolean removed=c.profile.waypoints.removeIf(w -> w.name.equalsIgnoreCase(StringArgumentType.getString(ctx,"name"))); c.changed(); return reply(ctx.getSource(),removed?"Waypoint removed":"Unknown waypoint"); })))
                .then(literal("list").executes(ctx -> reply(ctx.getSource(),c.privacy()?"Waypoints hidden by privacy mode":c.profile.waypoints.stream().map(w -> w.name+" ("+w.dimension+")").collect(java.util.stream.Collectors.joining(", "))))))
            .then(literal("search").then(argument("block",StringArgumentType.word()).executes(ctx -> {
                Identifier id=Identifier.tryParse(StringArgumentType.getString(ctx,"block"));
                if(id==null || !Registries.BLOCK.containsId(id)) return reply(ctx.getSource(),"Unknown block ID");
                c.worldOverlay.searchBlock=Registries.BLOCK.get(id); c.profile.searchBlock=id.toString(); c.changed(); c.worldOverlay.reset(); return reply(ctx.getSource(),"Search target: "+id+" (enable Search)");
            })))));
    }
    private static int reply(FabricClientCommandSource source,String text) { source.sendFeedback(Text.literal("[KTHREADD] "+text)); return 1; }
}
