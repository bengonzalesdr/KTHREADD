package dev.kthreadd.qa;
import dev.kthreadd.KthreaddClient;
import dev.kthreadd.core.*;
import dev.kthreadd.modules.combat.CrystalCombatModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.*;
import java.util.*;
import java.util.function.Consumer;
/** Controlled client fixture: zero-damage target, many crystals, no authoritative attack expected. */
public final class EngineUpgradeChecks {
    public static void run(Consumer<String> passed){
        var mc=MinecraftClient.getInstance();var modules=KthreaddClient.INSTANCE.modules;
        var engine=(CrystalCombatModule)modules.get("CrystalAura");
        engine.setEnabled(false);
        Map<String,Double> saved=new HashMap<>();for(var setting:engine.settings)if(!setting.isText())saved.put(setting.name,setting.get());
        List<Integer> inserted=new ArrayList<>();
        try{
            var target=new OtherClientPlayerEntity(mc.world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BudgetFixture"));
            target.setId(-21000);target.setPosition(mc.player.getPos().add(2,0,0));target.prevX=target.getX();target.prevY=target.getY();target.prevZ=target.getZ();
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,1200,4));mc.world.addEntity(target);inserted.add(target.getId());
            engine.setting("Players").set(1);engine.setting("Hostile").set(0);engine.setting("Place").set(0);engine.setting("Break").set(1);
            engine.setting("ReactOnSpawn").set(0);engine.setting("MaxDamageEvaluations").set(32);engine.setting("MaxDecisionMs").set(25);
            for(int i=0;i<64;i++){
                var crystal=new EndCrystalEntity(EntityType.END_CRYSTAL,mc.world);crystal.setId(-21100-i);
                double angle=i*Math.PI*2/64;crystal.setPosition(mc.player.getPos().add(Math.cos(angle)*2,0,Math.sin(angle)*2));mc.world.addEntity(crystal);inserted.add(crystal.getId());
            }
            engine.setEnabled(true);engine.tick();
            check(engine.damageEvaluations()>0&&engine.damageEvaluations()<=32,"Crystal evaluation work obeys a 32-calculation cap in a 64-crystal fixture",passed);
            check(engine.sentAttacks()==0,"Budget exhaustion and zero target damage do not produce an unsafe attack",passed);
            engine.setEnabled(false);
            CombatPresets.apply(modules.all,CombatPresets.changes("balanced"));
            check(!engine.enabled()&&engine.flag("AntiSuicide")&&engine.flag("ProtectFriends")&&engine.value("AttackSpeed")==20,"Balanced CPvP preset applies validated settings without changing module toggles",passed);
        }finally{
            engine.setEnabled(false);for(int id:inserted)mc.world.removeEntity(id,Entity.RemovalReason.DISCARDED);
            for(var setting:engine.settings)if(saved.containsKey(setting.name))setting.set(saved.get(setting.name));
        }
    }
    private static void check(boolean ok,String label,Consumer<String> out){if(!ok)throw new AssertionError(label);out.accept("PASS "+label);}
}
