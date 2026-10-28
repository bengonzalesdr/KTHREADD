package dev.kthreadd.core;
import java.util.*;
/** Explicit, reversible tuning presets. Applying one changes settings, never secretly enables modules. */
public final class CombatPresets {
    public record Change(String module,String setting,double value) {}
    private CombatPresets() {}
    public static List<Change> changes(String name){
        String selected=name.toLowerCase(Locale.ROOT);
        if(!List.of("balanced","aggressive","defensive").contains(selected))throw new IllegalArgumentException("Use balanced, aggressive or defensive");
        boolean aggressive=selected.equals("aggressive"),defensive=selected.equals("defensive");
        var changes=new ArrayList<Change>();
        changes.add(new Change("CrystalAura","MinimumDamage",aggressive?3:4));
        changes.add(new Change("CrystalAura","MaxSelfDamage",defensive?3:aggressive?5:4));
        changes.add(new Change("CrystalAura","SafetyMargin",defensive?2:1));
        changes.add(new Change("CrystalAura","MinimumTradeRatio",defensive?1.5:aggressive?.75:1));
        changes.add(new Change("CrystalAura","AttackSpeed",20));changes.add(new Change("CrystalAura","PlaceDelay",1));
        changes.add(new Change("CrystalAura","CandidatesPerTick",48));changes.add(new Change("CrystalAura","MaxDamageEvaluations",256));
        changes.add(new Change("CrystalAura","MaxDecisionMs",8));changes.add(new Change("CrystalAura","PredictionTicks",1));
        changes.add(new Change("CrystalAura","MultiPlace",0));changes.add(new Change("CrystalAura","AntiSuicide",1));
        changes.add(new Change("CrystalAura","ProtectFriends",1));changes.add(new Change("CrystalAura","ReservePendingDamage",1));
        changes.add(new Change("CrystalAura","ReactOnSpawn",1));changes.add(new Change("CrystalAura","RetryBackoff",1));
        changes.add(new Change("AutoTotem","Health",defensive?18:14));changes.add(new Change("AutoTotem","CrystalDanger",1));
        changes.add(new Change("AutoTotem","Delay",1));
        return List.copyOf(changes);
    }
    public static void apply(Collection<ClientModule> modules,List<Change> changes){
        Map<Setting,Double> validated=new LinkedHashMap<>();
        for(var change:changes){
            var module=modules.stream().filter(m->m.name.equalsIgnoreCase(change.module())).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown module "+change.module()));
            var setting=module.setting(change.setting());
            if(setting.isText()||setting.isMode()||!Double.isFinite(change.value())||change.value()<setting.min||change.value()>setting.max)throw new IllegalArgumentException("Invalid preset value for "+change.module()+" / "+change.setting());
            validated.put(setting,change.value());
        }
        validated.forEach(Setting::set);
    }
}
