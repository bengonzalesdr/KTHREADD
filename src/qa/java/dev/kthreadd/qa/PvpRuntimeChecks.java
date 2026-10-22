package dev.kthreadd.qa;

import dev.kthreadd.KthreaddClient;
import dev.kthreadd.core.*;
import dev.kthreadd.modules.combat.CombatSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.*;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Actual local-server placement, explosion and melee checks. Never part of a release mod. */
public final class PvpRuntimeChecks {
    private final MinecraftClient mc=MinecraftClient.getInstance();
    private final Consumer<String> passed;
    private final ClientModule crystal=KthreaddClient.INSTANCE.modules.get("CrystalAura"),aura=KthreaddClient.INSTANCE.modules.get("KillAura");
    private CompletableFuture<?> pending;
    private int phase,wait,targetId;
    public PvpRuntimeChecks(Consumer<String> passed){this.passed=passed;}
    public boolean tick(){
        if(++wait>400)throw new AssertionError("PvP fixture timed out at phase "+phase);
        switch(phase){
            case 0->{
                for(var m:KthreaddClient.INSTANCE.modules.all)if(m.category==Category.COMBAT)m.setEnabled(false);
                mc.setScreen(null);var server=mc.getServer();var uuid=mc.player.getUuid();
                pending=server.submit(()->{
                    server.setDifficulty(Difficulty.NORMAL,true);var world=server.getOverworld();world.setTimeOfDay(18000);
                    for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)world.setBlockState(new BlockPos(x,199,z),Blocks.OBSIDIAN.getDefaultState());
                    var player=server.getPlayerManager().getPlayer(uuid);player.changeGameMode(GameMode.SURVIVAL);player.teleport(world,0.5,200,0.5,0,0);player.clearStatusEffects();player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,2400,4));player.setHealth(20);player.getHungerManager().setFoodLevel(20);player.getInventory().clear();
                    player.getInventory().setStack(0,new ItemStack(Items.DIAMOND_SWORD));player.getInventory().setStack(1,new ItemStack(Items.END_CRYSTAL,16));player.getInventory().selectedSlot=0;player.currentScreenHandler.sendContentUpdates();spawnZombie(3.5,1.5);
                });next();
            }
            case 1->{
                if(!pending.isDone())return false;pending.join();if(mc.player.squaredDistanceTo(0.5,200,0.5)>1||!(mc.world.getEntityById(targetId) instanceof ZombieEntity target)||!mc.player.hasStatusEffect(StatusEffects.RESISTANCE))return false;
                check(CombatSupport.damage(new Vec3d(3.5,200,0.5),target)>0,"Crystal damage evaluation samples a real target and world exposure");
                Vec3d explosion=new Vec3d(3.5,200,0.5); Vec3d original=target.getPos();
                float forecast=CombatSupport.damage(explosion,target,new Vec3d(0.25,0,0));
                check(Float.isFinite(forecast)&&forecast>0&&target.getPos().equals(original),"Forecast exposure evaluates a shifted box without mutating the target");
                check(CombatSupport.damage(new Vec3d(3.5,200,0.5),mc.player)==0,"Crystal damage evaluation accounts for Resistance V fixture");
                crystal.setting("Players").set(0);crystal.setting("Hostile").set(1);crystal.setting("Break").set(0);crystal.setting("Place").set(1);crystal.setEnabled(true);next();
            }
            case 2->{
                boolean present=false;for(var e:mc.world.getEntities())if(e instanceof EndCrystalEntity){present=true;break;}if(!present)return false;
                check(true,"CrystalAura places an actual server-spawned crystal using hotbar auto-switch");check(mc.player.getInventory().selectedSlot==0,"Crystal placement restores the local hotbar selection");
                check(((dev.kthreadd.modules.combat.CrystalCombatModule)crystal).confirmedPlacements()>0,"Crystal tracker confirms placement from a server entity-load event");
                crystal.setting("Place").set(0);crystal.setting("Break").set(1);next();
            }
            case 3->{
                for(var e:mc.world.getEntities())if(e instanceof EndCrystalEntity)return false;
                var target=mc.world.getEntityById(targetId);if(target instanceof ZombieEntity zombie&&zombie.isAlive()&&zombie.getHealth()>=20)return false;
                check(true,"CrystalAura breaks the server crystal and the resulting explosion damages its target");check(mc.player.getHealth()==20,"Protected fixture survives actual crystal explosion without health loss");
                check(((dev.kthreadd.modules.combat.CrystalCombatModule)crystal).sentAttacks()==1,"Confirmed crystal break requires exactly one attack in the local fixture");crystal.setEnabled(false);
                pending=mc.getServer().submit(()->spawnZombie(2.5,0.5));next();
            }
            case 4->{
                if(!pending.isDone())return false;pending.join();if(!(mc.world.getEntityById(targetId) instanceof ZombieEntity))return false;
                aura.setting("Players").set(0);aura.setting("Monsters").set(1);aura.setting("Rotation").setText("NORMAL");aura.setting("AutoSwitch").set(1);aura.setEnabled(true);next();
            }
            case 5->{
                var target=mc.world.getEntityById(targetId);if(target instanceof ZombieEntity zombie&&zombie.isAlive()&&zombie.getHealth()>=20)return false;
                aura.setEnabled(false);check(true,"KillAura performs a server-accepted melee attack against a selected hostile target");
                var uuid=mc.player.getUuid();pending=mc.getServer().submit(()->{
                    var world=mc.getServer().getOverworld();var player=mc.getServer().getPlayerManager().getPlayer(uuid);
                    player.teleport(world,0.5,200,0.5,0,0);player.clearStatusEffects();player.setHealth(20);
                    player.getInventory().setStack(5,new ItemStack(Items.TOTEM_OF_UNDYING));
                    player.getInventory().setStack(40,new ItemStack(Items.END_CRYSTAL));player.currentScreenHandler.sendContentUpdates();
                    var danger=new EndCrystalEntity(EntityType.END_CRYSTAL,world);danger.setPosition(2.5,200,0.5);world.spawnEntity(danger);
                });next();
            }
            case 6->{
                if(!pending.isDone())return false;pending.join();
                if(mc.player.hasStatusEffect(StatusEffects.RESISTANCE)||!mc.player.getOffHandStack().isOf(Items.END_CRYSTAL))return false;
                boolean danger=false;for(var entity:mc.world.getEntities())if(entity instanceof EndCrystalEntity&&CombatSupport.damage(entity.getPos(),mc.player)>=18)danger=true;
                if(!danger)return false;
                var totem=KthreaddClient.INSTANCE.modules.get("AutoTotem");totem.setting("Health").set(1);totem.setting("Delay").set(10);totem.setEnabled(true);next();
            }
            case 7->{
                if(!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))return false;
                check(mc.player.getHealth()>1,"AutoTotem responds to lethal crystal damage above its health threshold");
                check(wait<10,"Lethal crystal protection bypasses the ordinary offhand delay");
                var utility=KthreaddClient.INSTANCE.modules.get("AutoOffhandCrystal");utility.setting("Health").set(1);utility.setting("CrystalCheck").set(0);utility.setting("Delay").set(1);utility.setEnabled(true);next();
            }
            case 8->{
                if(!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))throw new AssertionError("Utility offhand displaced emergency totem during persistent crystal danger");
                if(wait<12)return false;
                check(true,"Persistent crystal danger retains the emergency totem against utility offhand requests");
                KthreaddClient.INSTANCE.modules.get("AutoOffhandCrystal").setEnabled(false);KthreaddClient.INSTANCE.modules.get("AutoTotem").setEnabled(false);
                var uuid=mc.player.getUuid();pending=mc.getServer().submit(()->{
                    var world=mc.getServer().getOverworld();var player=mc.getServer().getPlayerManager().getPlayer(uuid);
                    for(var entity:world.getEntitiesByClass(EndCrystalEntity.class,new Box(-10,190,-10,10,210,10),e->true))entity.discard();
                    player.teleport(world,0.5,200,0.5,0,0);player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,2400,4));player.setHealth(20);spawnZombie(3.5,1.5);
                });next();
            }
            case 9->{
                if(!pending.isDone())return false;pending.join();
                if(!mc.player.hasStatusEffect(StatusEffects.RESISTANCE)||mc.world.getEntityById(targetId)==null)return false;
                for(var entity:mc.world.getEntities())if(entity instanceof EndCrystalEntity)return false;
                crystal.setting("Place").set(0);crystal.setting("Break").set(1);crystal.setting("ReactOnSpawn").set(1);crystal.setEnabled(true);
                pending=mc.getServer().submit(()->{var world=mc.getServer().getOverworld();var entity=new EndCrystalEntity(EntityType.END_CRYSTAL,world);entity.setPosition(3.5,200,0.5);world.spawnEntity(entity);});next();
            }
            case 10->{
                if(!pending.isDone())return false;pending.join();
                var engine=(dev.kthreadd.modules.combat.CrystalCombatModule)crystal;
                if(engine.sentAttacks()==0)return false;
                check(engine.reactiveAttacks()==1,"Server crystal spawn triggers a guarded attack from the entity-load event");
                var target=mc.world.getEntityById(targetId);if(target instanceof ZombieEntity zombie&&zombie.getHealth()>=20)return false;
                check(true,"Reactive attack reaches the server and damages the target");
                try {
                    var report=java.nio.file.Path.of(engine.exportReport());
                    var json=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(report)).getAsJsonObject();
                    check(json.get("reactiveAttacks").getAsInt()==1&&json.getAsJsonArray("events").size()>0,"Fight report exports real reactive-attack events as local JSON");
                } catch(java.io.IOException error){throw new AssertionError(error);}
                crystal.setEnabled(false);return true;
            }
        }
        return false;
    }
    private void spawnZombie(double x,double z){var world=mc.getServer().getOverworld();var target=new ZombieEntity(EntityType.ZOMBIE,world);target.setPosition(x,200,z);target.setAiDisabled(true);target.setNoGravity(true);target.setPersistent();world.spawnEntity(target);targetId=target.getId();}
    private void next(){phase++;wait=0;}
    private void check(boolean condition,String label){if(!condition)throw new AssertionError(label);passed.accept("PASS "+label);}
}
