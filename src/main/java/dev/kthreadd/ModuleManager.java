package dev.kthreadd;

import dev.kthreadd.core.*;
import dev.kthreadd.modules.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import java.util.*;

public final class ModuleManager {
    public final List<ClientModule> all = new ArrayList<>();
    private final Map<Integer, Boolean> down = new HashMap<>();
    public final ClientModule colors, gui, hud, privacy, notifications;
    public ModuleManager() {
        colors = add(new ClientModule("Colors", Category.RENDER, "Ingros-style crimson GUI theme; RGB accent and rainbow."));
        colors.configurationOnly=true;
        colors.number("Red",179,0,255,1); colors.number("Green",0,0,255,1); colors.number("Blue",46,0,255,1); colors.flag("Rainbow",false);
        gui = add(new ClientModule("ClickGui",Category.RENDER,"Delete opens category panels. Right-click expands settings; middle-click binds."));
        gui.configurationOnly=true;
        gui.number("Scale",1,0.65,1.5,0.05); gui.flag("Rounded",true); gui.flag("Animations",true); gui.flag("Tooltips",true); gui.number("Opacity",210,80,255,15);
        hud = add(new ClientModule("Overlay",Category.RENDER,"KTHREADD watermark, right aligned ArrayList, coordinates, target and inventory overlays."));
        hud.flag("Watermark",true); hud.flag("ArrayList",true); hud.flag("Coordinates",true); hud.flag("Stats",true); hud.flag("Combat",true); hud.flag("Waypoints",true); hud.flag("Shadow",true); hud.flag("TargetHUD",true); hud.flag("Armor",true); hud.flag("Inventory",false); hud.number("Red",234,0,255,1); hud.number("Green",38,0,255,1); hud.number("Blue",38,0,255,1);
        hud.setEnabled(true);
        privacy = add(new ClientModule("Privacy",Category.RENDER,"Hides this client's coordinate overlays, waypoint labels and stash location notifications."));
        notifications = add(new ClientModule("Notifications",Category.RENDER,"Local module toggle and diagnostic notifications.")); notifications.setEnabled(true);
        MovementModules.register(this); InventoryModules.register(this); CombatModules.register(this);
        IngrosModules.register(this); PvpModules.register(this);
        add(new ClientModule("FullBright",Category.RENDER,"Fullbright via lightmap brightness; no gamma setting or potion modification."));
        ClientModule esp=add(new ClientModule("EntityESP",Category.RENDER,"Outlined entity boxes. Range-capped; uses the modern world render pipeline.")); esp.number("Range",64,8,192,8); esp.flag("PlayersOnly",true); esp.flag("ThroughWalls",true); esp.number("LineWidth",1,0.5,3,0.25);
        ClientModule storage=add(new ClientModule("StorageESP",Category.RENDER,"Outlines nearby loaded chests, barrels, shulkers, hoppers and furnaces.")); storage.number("Range",48,8,96,8); storage.flag("ThroughWalls",true); storage.number("LineWidth",1,0.5,3,0.25);
        ClientModule tracers=add(new ClientModule("Tracers",Category.RENDER,"Lines from the camera to nearby players.")); tracers.number("Range",96,8,192,8); tracers.flag("ThroughWalls",true); tracers.number("LineWidth",1,0.5,3,0.25);
        ClientModule names=add(new ClientModule("Nametags",Category.RENDER,"Player names and health above their heads, visible through terrain.")); names.number("Range",64,8,128,8);
        add(new ClientModule("Waypoints",Category.RENDER,"Dimension-aware world markers. Manage with /kthread waypoint; disabled in privacy mode."));
        ClientModule stash=add(new ClientModule("StashFinder",Category.PLAYER,"Checks loaded storage counts per chunk; local notification once per session.")); stash.number("Threshold",6,2,64,1); stash.number("Range",64,16,128,16);
        ClientModule search=add(new ClientModule("Search",Category.PLAYER,"Incrementally scans nearby loaded blocks for selected ore. /kthread search <block-id>.")); search.number("Range",16,4,32,4); search.number("Budget",512,128,2048,128);
        add(new ClientModule("HighwayNavigation",Category.PLAYER,"HUD yaw/cardinal alignment and cross-track distance to cardinal/diagonal highways."));
        ClientModule weather=add(new ClientModule("NoWeather",Category.PLAYER,"Suppresses client rain/thunder rendering while enabled; server state is untouched."));
        all.sort(Comparator.comparing(m -> m.name));
    }
    public <T extends ClientModule> T add(T m) { if(get(m.name)!=null) throw new IllegalArgumentException("Duplicate module: "+m.name); all.add(m); return m; }
    public ClientModule get(String name) { return all.stream().filter(m -> m.name.equalsIgnoreCase(name)).findFirst().orElse(null); }
    public boolean on(String name) { var m=get(name); return m!=null && m.enabled(); }
    public void tick(MinecraftClient mc) {
        if (mc.player==null || mc.world==null) return;
        dev.kthreadd.modules.combat.CombatActions.beginTick();
        for (var m: all) {
            if (!m.configurationOnly && m.key >= 32 && mc.currentScreen==null) {
                boolean pressed=InputUtil.isKeyPressed(mc.getWindow().getHandle(),m.key);
                if (pressed && !down.getOrDefault(m.key,false)) m.toggle();
            }
        }
        // Offhand always runs first. Routine maintenance yields to offense unless protection is urgent.
        InventoryModules.tickOffhand(this);
        var ordered=new ArrayList<>(all);
        var crystal=get("CrystalAura"); ordered.remove(crystal); ordered.add(0,crystal);
        String first=CombatPriority.first(mc.player.getHealth()+mc.player.getAbsorptionAmount()<=8,InventoryModules.armorCritical());
        if(first.equals("AutoArmor"))for(String priority:List.of("AutoFeetObby","AutoCrystalArmor","AutoArmor")) { var m=get(priority); ordered.remove(m); ordered.add(0,m); }
        for(var m:ordered) {
            if (m.enabled() && ((m.category!=Category.COMBAT && !m.name.equals("AutoTool")) || dev.kthreadd.modules.combat.CombatActions.available(m.name))) {
                try { m.tick(); } catch (RuntimeException e) {
                    KthreaddClient.LOG.error("Disabling failed module {}",m.name,e); m.setEnabled(false);
                    KthreaddClient.INSTANCE.notify("Disabled " + m.name + ": " + e.getClass().getSimpleName());
                }
            } else if(m==crystal && m.enabled()) ((dev.kthreadd.modules.combat.CrystalCombatModule)m).deferred("Action owner");
        }
        for (var m:all) if (m.key>=32) down.put(m.key,InputUtil.isKeyPressed(mc.getWindow().getHandle(),m.key));
    }
    public void release() { for (var m:all) if (m.enabled()) { m.setEnabled(false); } down.clear(); }
    public int accent(int offset) {
        if (colors.flag("Rainbow")) return 0xff000000 | java.awt.Color.HSBtoRGB(((System.currentTimeMillis()/25+offset)%360)/360f,0.65f,1f);
        return 0xff000000 | colors.setting("Red").integer()<<16 | colors.setting("Green").integer()<<8 | colors.setting("Blue").integer();
    }
    public int hudAccent(int offset) {
        if(colors.flag("Rainbow")) return accent(offset);
        return 0xff000000 | hud.setting("Red").integer()<<16 | hud.setting("Green").integer()<<8 | hud.setting("Blue").integer();
    }
}
