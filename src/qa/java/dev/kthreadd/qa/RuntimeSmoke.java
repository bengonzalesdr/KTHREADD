package dev.kthreadd.qa;

import dev.kthreadd.*;
import dev.kthreadd.core.*;
import dev.kthreadd.gui.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.*;
import net.minecraft.world.*;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.gen.*;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.item.*;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

/** Separate dev-only mod. Never bundled in the release JAR. */
public final class RuntimeSmoke implements ClientModInitializer {
    private PvpRuntimeChecks pvpChecks;
    private int stage,ticks,stageAt;
    private long started;
    private final List<String> report=new ArrayList<>();
    private final Path output=Path.of("qa-results");
    public void onInitializeClient() {
        KthreaddClient.LOG.info("Runtime QA initializer active");
        started=System.currentTimeMillis();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ticks++;
            if(ticks%100==0) KthreaddClient.LOG.info("Runtime QA stage {} screen {}",stage,mc.currentScreen==null?"null":mc.currentScreen.getClass().getSimpleName());
            try {
                if(System.currentTimeMillis()-started>240_000) throw new AssertionError("Runtime QA timed out at stage "+stage);
                if(stage!=8 && ticks-stageAt<20) return;
                run(mc);
            } catch(Throwable e) {
                e.printStackTrace(); report.add("FAIL stage "+stage+": "+e); write(); mc.scheduleStop(); stage=999;
            }
        });
    }
    private void next() { stage++; stageAt=ticks; }
    private void check(boolean condition,String result) { if(!condition) throw new AssertionError(result); report.add("PASS "+result); }
    private void screenshot(MinecraftClient mc,String name) throws Exception {
        Files.createDirectories(output); try(var image=ScreenshotRecorder.takeScreenshot(mc.getFramebuffer())) { image.writeTo(output.resolve(name+".png")); }
    }
    private void run(MinecraftClient mc) throws Exception {
        var c=KthreaddClient.INSTANCE;
        switch(stage) {
            case 0 -> {
                if(mc.currentScreen!=null && mc.currentScreen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) { mc.setScreen(new TitleScreen()); return; }
                if(!(mc.currentScreen instanceof TitleScreen) || mc.getOverlay()!=null) return;
                check(c!=null && c.modules.all.size()>20,"Fabric client initialization and module registration");
                c.load("qa-initial-"+System.currentTimeMillis());
                KeyBinding.onKeyPressed(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_DELETE)); next();
            }
            case 1 -> {
                check(mc.currentScreen instanceof ClickGuiScreen,"Fabric Delete key opens KTHREADD ClickGui");
                try(var image=ScreenshotRecorder.takeScreenshot(mc.getFramebuffer())) {
                    int px=(int)(10.0*image.getWidth()/mc.getWindow().getScaledWidth());
                    int py=(int)(64.0*image.getHeight()/mc.getWindow().getScaledHeight());
                    int pixel=image.getColor(px,py);
                    check((pixel&255)==179 && ((pixel>>8)&255)==0 && ((pixel>>16)&255)==46,"Rendered KTHREADD GUI header preserves Ingros crimson RGB 179/0/46");
                }
                screenshot(mc,"01-clickgui");
                ((TextFieldWidget)mc.currentScreen.children().stream().filter(e -> e instanceof TextFieldWidget).findFirst().orElseThrow()).setText("Sprint"); next();
            }
            case 2 -> {
                check(!c.modules.on("Sprint"),"Sprint initially disabled");
                mc.currentScreen.mouseClicked(18,89,0);
                check(c.modules.on("Sprint"),"Filtered GUI module click toggles Sprint");
                mc.currentScreen.mouseClicked(18,89,2); mc.currentScreen.keyPressed(GLFW.GLFW_KEY_K,0,0);
                check(c.modules.get("Sprint").key==GLFW.GLFW_KEY_K,"GUI key capture assigns module bind");
                mc.currentScreen.mouseClicked(18,66,0); mc.currentScreen.mouseDragged(18,2,0,0,-64); mc.currentScreen.mouseReleased(18,2,0);
                check(c.profile.panels.get("MOVEMENT").y*mc.getWindow().getScaledHeight()>=61.9,"Dragging a category into the search strip clamps its persisted header to a reachable position"); c.profile.panels.clear();
                c.saveAs("qa-roundtrip"); c.modules.get("Sprint").setEnabled(false); c.load("qa-roundtrip");
                check(c.modules.on("Sprint") && c.modules.get("Sprint").key==GLFW.GLFW_KEY_K,"Runtime JSON save/load retains toggle and bind");
                c.modules.get("Sprint").setEnabled(false); c.modules.get("Sprint").key=-1;
                mc.setScreen(new TitleScreen());
                var rules=new GameRules(); rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false,null); rules.get(GameRules.DO_MOB_SPAWNING).set(false,null);
                mc.createIntegratedServerLoader().createAndStart("KTHREADD-QA-"+System.currentTimeMillis(),new LevelInfo("KTHREADD isolated QA",GameMode.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,DataConfiguration.SAFE_MODE),new GeneratorOptions(12345L,false,false),WorldPresets::createDemoOptions,new TitleScreen()); next();
            }
            case 3 -> {
                if(mc.player==null || mc.world==null || mc.getServer()==null) return;
                mc.setScreen(null); check(c.activeProfile.startsWith("server-"),"Integrated world selects isolated per-world profile");
                var uuid=mc.player.getUuid(); mc.getServer().execute(() -> {
                    var player=mc.getServer().getPlayerManager().getPlayer(uuid);
                    if(player!=null) { player.getInventory().setStack(9,new ItemStack(Items.TOTEM_OF_UNDYING)); player.getInventory().setStack(10,new ItemStack(Items.DIAMOND_HELMET)); player.getInventory().setStack(11,new ItemStack(Items.GOLDEN_APPLE)); player.getInventory().setStack(12,new ItemStack(Items.END_CRYSTAL)); player.currentScreenHandler.sendContentUpdates(); }
                });
                c.modules.get("AutoTotem").setEnabled(true); next();
            }
            case 4 -> {
                if(ticks-stageAt<60) return;
                check(mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING),"AutoTotem moves real integrated-server inventory to offhand"); c.modules.get("AutoTotem").setEnabled(false);
                c.modules.get("AutoArmor").setEnabled(true); next();
            }
            case 5 -> {
                if(ticks-stageAt<60) return;
                check(mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).isOf(Items.DIAMOND_HELMET),"AutoArmor equips real integrated-server helmet"); c.modules.get("AutoArmor").setEnabled(false);
                c.modules.get("AutoOffhandGap").setEnabled(true); stage=50; stageAt=ticks;
            }
            case 50 -> {
                if(ticks-stageAt<40) return;
                check(mc.player.getOffHandStack().isOf(Items.GOLDEN_APPLE),"AutoOffhandGap equips golden apple through real inventory transaction");
                c.modules.get("AutoOffhandGap").setEnabled(false); c.modules.get("AutoOffhandCrystal").setEnabled(true); stage=51; stageAt=ticks;
            }
            case 51 -> {
                if(ticks-stageAt<40) return;
                check(mc.player.getOffHandStack().isOf(Items.END_CRYSTAL),"AutoOffhandCrystal equips crystal through real inventory transaction");
                var uuid=mc.player.getUuid(); var server=mc.getServer(); server.execute(() -> { var p=server.getPlayerManager().getPlayer(uuid); p.getHungerManager().setFoodLevel(6); p.setHealth(6); }); stage=52; stageAt=ticks;
            }
            case 52 -> {
                check(mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING),"Offhand utility falls back to totem at low health with AutoTotem disabled");
                c.modules.get("AutoOffhandCrystal").setEnabled(false); stage=53; stageAt=ticks;
            }
            case 53 -> {
                for(String name:List.of("FullBright","SafeWalk","Step","NoSlowDown","NoWeather","EntityESP","StorageESP","Tracers","Nametags","Search","Waypoints","HighwayNavigation")) c.modules.get(name).setEnabled(true);
                for(int i=0;i<2;i++) {
                    var dummy=new net.minecraft.client.network.OtherClientPlayerEntity(mc.world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RenderQA"+i));
                    dummy.setId(-900-i); dummy.setPosition(mc.player.getX()+3+i*2,mc.player.getY(),mc.player.getZ()+3); mc.world.addEntity(dummy);
                }
                var chest=mc.player.getBlockPos().add(2,0,2); var server=mc.getServer(); server.execute(() -> server.getOverworld().setBlockState(chest,net.minecraft.block.Blocks.CHEST.getDefaultState()));
                c.profile.waypoints.add(new Profile.Waypoint("QA",mc.world.getRegistryKey().getValue().toString(),mc.player.getX()+4,mc.player.getY(),mc.player.getZ()));
                mc.setScreen(new HudEditorScreen(c)); stage=6; stageAt=ticks;
            }
            case 6 -> {
                check(c.hud.bounds.containsKey("Coordinates"),"HUD editor renders measured components in real world"); screenshot(mc,"02-hud-editor");
                check(c.hud.bounds.containsKey("TargetHUD") && c.hud.bounds.containsKey("Armor") && c.hud.bounds.containsKey("Inventory"),"TargetHUD armor and inventory elements render in real world");
                check(net.minecraft.client.render.LightmapTextureManager.getBrightness(mc.world.getDimension(),0)==1F,"Fullbright mixin changes actual lightmap brightness");
                check(mc.player.getStepHeight()==1F,"Step mixin changes actual local player step height");
                c.modules.get("Step").setEnabled(false); check(mc.player.getStepHeight()==0.6F,"Disabling Step restores vanilla step height"); c.modules.get("Step").setEnabled(true);
                check(mc.world.getRainGradient(1)==0 && mc.world.getThunderGradient(1)==0,"NoWeather suppresses actual client precipitation gradients");
                var b=c.hud.bounds.get("Watermark"); mc.currentScreen.mouseClicked(b.x()+3,b.y()+3,0); mc.currentScreen.mouseDragged(b.x()+55,b.y()+45,0,52,42); mc.currentScreen.mouseReleased(0,0,0);
                check(c.profile.hud.containsKey("Watermark"),"HUD editor drag updates persisted position");
                c.notify("Storage cluster in chunk 123, 456");
                c.modules.privacy.setEnabled(true); mc.currentScreen.close(); next();
            }
            case 7 -> {
                check(!c.hud.bounds.containsKey("Coordinates") && !c.hud.bounds.containsKey("Waypoints"),"Privacy suppresses coordinate/waypoint HUD in real world"); screenshot(mc,"03-privacy-world");
                check(c.notices.stream().noneMatch(n -> n.text().contains("123, 456")),"Enabling privacy clears already queued coordinate notifications");
                report.add("PASS World rendering ran with two QA player nametags, entity boxes, tracers, storage, Search and waypoints enabled together without a render failure");
                var initial=new net.minecraft.util.math.Vec3d(0.1,0.2,0.3); mc.player.setVelocity(initial); c.modules.get("NoVelocity").setEnabled(true);
                mc.getNetworkHandler().onEntityVelocityUpdate(new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(mc.player.getId(),new net.minecraft.util.math.Vec3d(2,3,3)));
                check(mc.player.getVelocity().squaredDistanceTo(initial)<0.000001,"NoVelocity hook preserves local velocity against inbound entity packet at zero scale");
                c.modules.get("NoVelocity").setEnabled(false); mc.getNetworkHandler().onEntityVelocityUpdate(new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(mc.player.getId(),new net.minecraft.util.math.Vec3d(2,3,3)));
                check(mc.player.getVelocity().x==2,"Disabling NoVelocity restores vanilla inbound velocity application"); mc.player.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
                c.modules.get("NoPush").setEnabled(true); check(!mc.player.isPushable(),"NoPush changes actual local pushability"); c.modules.get("NoPush").setEnabled(false); check(mc.player.isPushable(),"Disabling NoPush restores pushability");
                c.modules.get("NoLag").setting("ParticlesPerTick").set(0); c.modules.get("NoLag").setEnabled(true);
                check(mc.particleManager.addParticle(net.minecraft.particle.ParticleTypes.SMOKE,mc.player.getX(),mc.player.getY(),mc.player.getZ(),0,0,0)==null,"NoLag zero budget suppresses actual particle creation");
                c.modules.get("NoLag").setEnabled(false); check(mc.particleManager.addParticle(net.minecraft.particle.ParticleTypes.SMOKE,mc.player.getX(),mc.player.getY(),mc.player.getZ(),0,0,0)!=null,"Disabling NoLag restores particle creation");
                var dummy=(net.minecraft.entity.player.PlayerEntity)mc.world.getEntityById(-900);
                c.modules.get("TotemPopCounter").setEnabled(true); mc.getNetworkHandler().onEntityStatus(new net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket(dummy,(byte)35));
                check(c.totemPops.getOrDefault(dummy.getUuid(),0)==1,"TotemPopCounter observes real entity-status handler");
                mc.getNetworkHandler().sendChatCommand("kthread friend add RenderQA0"); check(c.isFriend("renderqa0"),"Client friend command updates local case-insensitive friend list");
                c.modules.get("AntiFriendHit").setEnabled(true); float cooldown=mc.player.getAttackCooldownProgress(0); mc.interactionManager.attackEntity(mc.player,dummy); check(mc.player.getAttackCooldownProgress(0)==cooldown,"AntiFriendHit cancels friend attack before cooldown reset");
                c.save(); check(Files.exists(mc.runDirectory.toPath().resolve("config/kthreadd/profiles/"+c.activeProfile+".json")),"Runtime profile saved in isolated game directory");
                pvpChecks=new PvpRuntimeChecks(report::add);stage=8;stageAt=ticks;
            }
            case 8 -> {if(pvpChecks.tick()){report.add("LIMIT: Targeted local-server checks; remote-server PvP, anti-cheat compatibility and full original feature parity are not verified.");write();mc.scheduleStop();stage=999;}}
        }
    }
    private void write() { try { Files.createDirectories(output); Files.write(output.resolve("runtime-smoke.txt"),report); } catch(Exception e) { e.printStackTrace(); } }
}
