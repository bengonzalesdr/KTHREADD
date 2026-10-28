package dev.kthreadd.modules.combat;
import dev.kthreadd.KthreaddClient;
import dev.kthreadd.core.*;
import dev.kthreadd.modules.InventoryModules;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import java.util.*;
import static dev.kthreadd.modules.combat.CombatSupport.mc;

/** Independent KTHREADD crystal engine with bounded exposure sampling and separate place/break timing. */
public final class CrystalCombatModule extends ClientModule {
    private long nextBreak,nextPlace;
    private final CrystalTracker<BlockPos> tracker = new CrystalTracker<>();
    private final PlacementFeedback<BlockPos> feedback=new PlacementFeedback<>(256);
    private final Map<Integer,MotionHistory> motion=new HashMap<>();
    private DecisionBudget budget;
    private int budgetStops;
    private final VertexConsumerProvider.Immediate visualBuffer=VertexConsumerProvider.immediate(new BufferBuilder(512));
    private final Map<DamageKey, Float> damageCache = new HashMap<>();
    private final Map<Integer, BlockPos> observed = new HashMap<>();
    private Object worldIdentity;
    private final CombatJournal journal=new CombatJournal(4096);
    private final Map<String,Integer> rejected=new TreeMap<>();
    private final Map<Integer,Vec3d> predictionCache=new HashMap<>();
    private boolean evaluating;
    private int focusId=-1, reactiveAttacks;
    private long focusUntil;
    private long sessionStart=now();
    private int reactiveEntity=-1;
    private long reactionWindow=-1;
    private int reactionScans;
    private int calculations, cacheHits, placementsSent, attacksSent;
    private double lastEvaluationMs, lastTargetDamage, lastSelfDamage;
    private String status = "Idle";
    private record DamageKey(Vec3d center, int entity, Vec3d offset) {}
    private record Evaluation(double score, float damage, float self, int target) {}
    private record Candidate(BlockPos pos, Evaluation evaluation) {}
    private BlockPos visual;
    private long visualUntil;
    public CrystalCombatModule(String name) {
        super(name,Category.COMBAT,"Crystal automation with sampled world exposure, difficulty, armor, toughness, enchantment and resistance damage evaluation. Original packet timing differs in 1.20.4.");
        mode("BreakMode","Smart","Always");mode("PlaceMode","Most","Nearest");
        number("PlaceRange",4,0,6,0.25);number("BreakRange",4,0,6,0.25);number("WallsRange",3,0,6,0.25);
        number("MinimumDamage",4,0,36,0.5);number("MaxSelfDamage",4,0,36,0.5);number("Faceplace",8,0,36,0.5);
        number("PlaceDelay",1,0,50,1);number("AttackSpeed",20,0,20,1);number("RetryDelay",500,50,3000,50);
        flag("MultiPlace",false);number("MultiPlaceSpeed",2,1,10,1);flag("AutoSwitch",true);flag("AntiSuicide",true);flag("AntiWeakness",true);
        flag("PauseWhileEating",true);flag("PauseIfHittingBlock",false);flag("ShowRotations",false);
        flag("Players",true);flag("Hostile",false);flag("Animals",false);flag("Tamed",false);flag("ProtectFriends",true);
        flag("Place",true);flag("Break",true);number("EnemyRange",10,1,16,1);number("CandidatesPerTick",32,1,128,1);
        flag("LethalPriority",true); number("SelfDamageWeight",0.75,0,3,0.25);
        number("SafetyMargin",1,0.5,10,0.5); number("ArmorFaceplace",10,0,100,5);
        flag("AdaptiveRetry",true); number("AttackRetry",250,100,2000,50);
        number("PredictionTicks",1,0,3,0.5);
        flag("Diagnostics",false); flag("FightRecorder",true); flag("ReactOnSpawn",true);
        number("TargetLockMs",750,0,3000,50); flag("ReservePendingDamage",true);
        number("MaxDecisionMs",8,1,25,.5);number("MaxDamageEvaluations",192,32,1024,32);
        number("BreakCandidates",64,8,256,8);number("MaxTargets",4,1,12,1);
        flag("LatencyPrediction",true);flag("RetryBackoff",true);number("MinimumTradeRatio",1,0,3,.25);
        flag("Render",true);number("Red",234,0,255,5);number("Green",38,0,255,5);number("Blue",38,0,255,5);number("Alpha",153,0,255,5);
        WorldRenderEvents.LAST.register(ctx -> {
            if(!enabled() || !flag("Render") || visual==null || System.currentTimeMillis()>visualUntil || mc.world==null || ctx.matrixStack()==null) return;
            var stack=ctx.matrixStack(); var camera=ctx.camera().getPos(); stack.push();
            var layer=dev.kthreadd.render.EspRenderLayers.lines(1,true);var consumer=visualBuffer; WorldRenderer.drawBox(stack,consumer.getBuffer(layer),new Box(visual).offset(-camera.x,-camera.y,-camera.z), (float)value("Red")/255,(float)value("Green")/255,(float)value("Blue")/255,(float)value("Alpha")/255); consumer.draw(); stack.pop();
        });
    }
    private static long now() { return System.nanoTime() / 1_000_000L; }
    protected void onEnable() { journal.clear(); sessionStart=now(); reactiveAttacks=0; focusId=-1; }
    protected void onDisable() {
        visual=null; nextBreak=nextPlace=0; tracker.clear(); feedback.clear();motion.clear(); observed.clear(); damageCache.clear();
        placementsSent=attacksSent=0; worldIdentity=null; status="Idle"; focusId=-1; predictionCache.clear();
    }
    public String diagnostics() {
        return String.format(java.util.Locale.ROOT,
            "CA %s | sent %d/%d confirmed %d pending %d timeout %d | dmg %.1f/self %.1f | %.2fms evaluations %d cache %d ack %dms",
            status,placementsSent,attacksSent,tracker.confirmed(),tracker.pending(),tracker.expired(),
            lastTargetDamage,lastSelfDamage,lastEvaluationMs,calculations,cacheHits,tracker.lastConfirmationMs());
    }
    public int confirmedPlacements() { return tracker.confirmed(); }
    public int sentAttacks() { return attacksSent; }
    public int reactiveAttacks() { return reactiveAttacks; }
    public int budgetStops(){return budgetStops;}
    public int damageEvaluations(){return calculations;}
    public void deferred(String reason) { status=reason;record("skip",reason,0); }
    private void record(String type,String reason,double value) { if(flag("FightRecorder"))journal.add(now()-sessionStart,type,reason,value); }
    public String exportReport() throws java.io.IOException {
        var directory=net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("kthreadd-reports");
        java.nio.file.Files.createDirectories(directory);
        var path=directory.resolve("combat-"+System.currentTimeMillis()+"-"+System.nanoTime()+".json");
        var report=new java.util.LinkedHashMap<String,Object>();
        report.put("format",1);report.put("version",net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("kthreadd").orElseThrow().getMetadata().getVersion().getFriendlyString());report.put("summary",diagnostics());
        report.put("budgetStops",budgetStops);report.put("backoffPositions",feedback.size());report.put("reactiveAttacks",reactiveAttacks);report.put("droppedEvents",journal.dropped());
        report.put("counts",journal.counts());report.put("events",journal.events());
        java.nio.file.Files.writeString(path,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        return path.toAbsolutePath().toString();
    }
    public void entityLoaded(net.minecraft.entity.Entity entity) {
        if (!enabled() || !(entity instanceof EndCrystalEntity)) return;
        ensureWorld();
        BlockPos base=entity.getBlockPos().down(); observed.put(entity.getId(),base);
        expirePending(now());
        if(tracker.spawned(base,entity.getId(),now())){feedback.confirmed(base,tracker.lastConfirmationMs());record("confirm","placement",tracker.lastConfirmationMs());}
        if(flag("ReactOnSpawn") && flag("Break") && !evaluating && mc.isOnThread()) {
            if(now()<nextBreak) { deferred("Break cooldown");return; }
            long window=now()/50;if(window!=reactionWindow) {reactionWindow=window;reactionScans=0;}
            if(reactionScans++>=4) {deferred("Reaction budget");return;}
            if(mc.player!=null)CombatActions.beginWindow(mc.world,mc.player.age);
            if(!CombatActions.available("CrystalAura")) { deferred("Action owner");return; }
            if(InventoryModules.emergencyProtectionNeeded() || mc.player!=null && (mc.player.getHealth()+mc.player.getAbsorptionAmount()<=8 || InventoryModules.armorCritical())) { deferred("Emergency protection");return; }
            try { reactiveEntity=entity.getId();runDecision(); }
            catch(RuntimeException error) { KthreaddClient.LOG.error("Disabling CrystalAura after reactive failure",error);setEnabled(false); }
            finally { reactiveEntity=-1; }
        }
    }
    public void entityUnloaded(net.minecraft.entity.Entity entity) {
        motion.remove(entity.getId());
        if(observed.remove(entity.getId())!=null)record("confirm","removal",0);
        tracker.removed(entity.getId());
    }
    private void ensureWorld() {
        if (worldIdentity != mc.world) {
            tracker.clear(); feedback.clear();motion.clear(); observed.clear(); damageCache.clear(); nextBreak=nextPlace=0;
            worldIdentity=mc.world; focusId=-1; predictionCache.clear();
        }
    }
    private long retryTimeout() {
        long floor=(long)value("RetryDelay");
        if (!flag("AdaptiveRetry") || mc.player==null || mc.getNetworkHandler()==null) return floor;
        var entry=mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return Math.max(feedback.suggestedTimeout(floor),Math.min(3000,(entry==null?0:entry.getLatency())*2L+150));
    }
    private void expirePending(long time){
        long timeout=retryTimeout();
        for(var base:tracker.expiringBases(time,timeout)){if(flag("RetryBackoff"))feedback.failed(base,time);record("unconfirmed","placement timeout",timeout);}
        tracker.expire(time,timeout);
    }
    public void tick() { runDecision(); }
    private void runDecision() {
        if(evaluating)return;
        evaluating=true;long started=System.nanoTime();budget=new DecisionBudget((int)value("MaxDamageEvaluations"),(long)(value("MaxDecisionMs")*1_000_000),started); calculations=cacheHits=0; damageCache.clear();predictionCache.clear();rejected.clear();
        try { update(); } finally {
            lastEvaluationMs=(System.nanoTime()-started)/1_000_000.0;evaluating=false;
            rejected.forEach((reason,count)->record("rejected candidates",reason,count));
            record("decision",status,lastEvaluationMs);
        }
    }
    private void update() {
        ensureWorld(); status="Waiting";
        if(!InventoryModules.ready() || mc.world==null || mc.player.isDead()) return;
        long time=now();expirePending(time);
        CombatActions.beginWindow(mc.world,mc.player.age);
        for(var player:mc.world.getPlayers())motion.computeIfAbsent(player.getId(),id->new MotionHistory()).sample(mc.player.age,player.getX(),player.getY(),player.getZ());
        observed.entrySet().removeIf(e -> { if(mc.world.getEntityById(e.getKey())!=null)return false; tracker.removed(e.getKey());return true; });
        if(flag("PauseWhileEating") && mc.player.isUsingItem()) { status="Using item"; return; }
        if(flag("PauseIfHittingBlock") && mc.options.attackKey.isPressed() && mc.crosshairTarget instanceof BlockHitResult) { status="Mining"; return; }
        List<LivingEntity> targets=targets();
        if(targets.isEmpty()) { status="No target"; return; }
        status="Scanning";

        if(flag("Break") && time>=nextBreak) {
            EndCrystalEntity best=null; Evaluation bestEvaluation=null;
            var available=new ArrayList<EndCrystalEntity>();
            for(var entity:mc.world.getEntities())if(entity instanceof EndCrystalEntity crystal && crystal.isAlive() && (reactiveEntity<0 || entity.getId()==reactiveEntity)
                && mc.player.getEyePos().squaredDistanceTo(crystal.getPos())<=value("BreakRange")*value("BreakRange"))available.add(crystal);
            available.sort(Comparator.comparingDouble(c->mc.player.getEyePos().squaredDistanceTo(c.getPos())));
            int inspected=0;
            for(var crystal:available) {
                if(inspected++>=value("BreakCandidates") || budget.exhausted(System.nanoTime()))break;
                if(!tracker.canAttack(crystal.getId(),time,Math.max((long)value("AttackRetry"),retryTimeout()/2))) continue;
                double distance=mc.player.getEyePos().distanceTo(crystal.getPos());
                if(distance>value("BreakRange") || !mc.player.canSee(crystal) && distance>value("WallsRange")) continue;
                Evaluation evaluation=evaluate(crystal.getPos(),targets,setting("BreakMode").text().equals("Always"));
                if(evaluation!=null && (bestEvaluation==null || evaluation.score()>bestEvaluation.score()
                    || evaluation.score()==bestEvaluation.score() && crystal.getId()<best.getId())) { best=crystal; bestEvaluation=evaluation; }
            }
            if(best!=null) {
                int weapon=-1;
                if(CombatSupport.weaknessRequiresWeapon() && flag("AntiWeakness")) {
                    weapon=CombatSupport.weapon();
                    if(weapon<0 || !CombatSupport.weaponCanBreakCrystal(weapon)) { status="Need weapon"; return; }
                }
                if(!CombatActions.claim("CrystalAura")) { deferred("Action owner");return; }
                int old=mc.player.getInventory().selectedSlot;
                try {
                    if(weapon>=0) mc.player.getInventory().selectedSlot=weapon;
                    CombatSupport.look(best.getPos(),flag("ShowRotations"));
                    mc.interactionManager.attackEntity(mc.player,best); mc.player.swingHand(Hand.MAIN_HAND);
                    tracker.attacked(best.getId(),time); attacksSent++;
                    if(reactiveEntity>=0)reactiveAttacks++;
                    lastTargetDamage=bestEvaluation.damage(); lastSelfDamage=bestEvaluation.self(); status="Break"; focusId=bestEvaluation.target();focusUntil=time+(long)value("TargetLockMs");
                    record("attack",reactiveEntity>=0?"spawn reaction":"scheduled",lastTargetDamage);
                    nextBreak=time+Math.max(50,420-(long)value("AttackSpeed")*20);
                } finally { mc.player.getInventory().selectedSlot=old; }
            }
        }
        if(reactiveEntity>=0) { if(time<nextBreak && !status.equals("Break"))status="Break cooldown";return; }
        if(!flag("Place") || time<nextPlace) { if(!status.equals("Break"))status="Place cooldown";return; }
        Hand hand=mc.player.getOffHandStack().isOf(Items.END_CRYSTAL)?Hand.OFF_HAND:Hand.MAIN_HAND;
        int slot=CombatSupport.find(Items.END_CRYSTAL);
        if(hand==Hand.MAIN_HAND && slot<0) { status="No crystals"; return; }
        if(!flag("AutoSwitch") && hand==Hand.MAIN_HAND && !mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;
        double range=value("PlaceRange"); int radius=(int)Math.ceil(range);
        List<BlockPos> positions=new ArrayList<>();
        for(var pos:BlockPos.iterate(mc.player.getBlockPos().add(-radius,-radius,-radius),mc.player.getBlockPos().add(radius,radius,radius))) {
            if(tracker.blocked(pos,time,retryTimeout()) || flag("RetryBackoff")&&!feedback.ready(pos,time)) continue;
            var state=mc.world.getBlockState(pos);
            if(!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) continue;
            Vec3d center=Vec3d.ofBottomCenter(pos.up());
            if(mc.player.getEyePos().distanceTo(center)>range || !mc.world.isAir(pos.up())
                || !mc.world.getOtherEntities(null,new Box(pos.up()).stretch(0,1,0)).isEmpty()) continue;
            positions.add(pos.toImmutable());
        }
        // Stable focus first, with forecast position used for shortlisting; every candidate still gets full safety checks.
        var focus=targets.get(0);var forecast=focus.getPos().add(prediction(focus));
        positions.sort(Comparator.comparingDouble(p -> Vec3d.ofBottomCenter(p.up()).squaredDistanceTo(forecast)));
        if(targets.size()>1) {
            var alternatives=new ArrayList<>(positions);
            alternatives.sort(Comparator.comparingDouble(p->targets.stream().mapToDouble(t->t.squaredDistanceTo(Vec3d.ofBottomCenter(p.up()))).min().orElse(Double.MAX_VALUE)));
            var diversified=new LinkedHashSet<BlockPos>();int primary=0,other=0;
            while(diversified.size()<value("CandidatesPerTick") && (primary<positions.size() || other<alternatives.size())) {
                for(int i=0;i<3&&primary<positions.size();i++)diversified.add(positions.get(primary++));
                if(other<alternatives.size())diversified.add(alternatives.get(other++));
            }
            positions=new ArrayList<>(diversified);
        }
        List<Candidate> selected=new ArrayList<>(); int evaluated=0;
        for(var pos:positions) {
            if(evaluated++>=value("CandidatesPerTick") || budget.exhausted(System.nanoTime())) break;
            var hit=mc.world.raycast(new RaycastContext(mc.player.getEyePos(),Vec3d.ofCenter(pos).add(0,0.49,0),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,mc.player));
            if(hit.getType()!=HitResult.Type.MISS && !hit.getBlockPos().equals(pos) && mc.player.getEyePos().distanceTo(Vec3d.ofBottomCenter(pos.up()))>value("WallsRange"))continue;
            Evaluation evaluation=evaluate(Vec3d.ofBottomCenter(pos.up()),targets,false);
            if(evaluation!=null) selected.add(new Candidate(pos,evaluation));
        }
        if(setting("PlaceMode").text().equals("Most")) selected.sort(Comparator.comparingDouble((Candidate c)->c.evaluation().score()).reversed());
        int remaining=flag("MultiPlace")?2:1;
        for(var candidate:selected) {
            if(remaining==0) break;
            BlockPos base=candidate.pos(); Vec3d center=Vec3d.ofBottomCenter(base.up());
            Evaluation evaluation=evaluate(center,targets,false);
            if(evaluation==null) continue;
            if(!CombatActions.claim("CrystalAura")) { deferred("Action owner");return; }
            Vec3d point=Vec3d.ofCenter(base).add(0,0.5,0);
            CombatSupport.look(point,flag("ShowRotations")); int old=mc.player.getInventory().selectedSlot;
            try {
                if(hand==Hand.MAIN_HAND) mc.player.getInventory().selectedSlot=slot;
                // Track before sending, so a spawn delivered after the send can confirm this attempt.
                tracker.placed(base,time);
                var result=mc.interactionManager.interactBlock(mc.player,hand,new BlockHitResult(point,Direction.UP,base,false));
                placementsSent++; remaining--;
                nextPlace=time+Math.max((long)value("PlaceDelay")*50,flag("MultiPlace")?(long)value("MultiPlaceSpeed")*50:50);
                if(result.isAccepted()) {
                    record("place","pending",evaluation.damage());
                    mc.player.swingHand(hand); visual=base; visualUntil=System.currentTimeMillis()+500;
                    lastTargetDamage=evaluation.damage(); lastSelfDamage=evaluation.self(); status="Place pending";focusId=evaluation.target();focusUntil=time+(long)value("TargetLockMs");
                } else { tracker.cancelPlacement(base);if(flag("RetryBackoff"))feedback.failed(base,time);status="Place declined";record("skip","interaction declined",0); }
            } finally { mc.player.getInventory().selectedSlot=old; }
        }
    }
    private List<LivingEntity> targets() {
        List<LivingEntity> targets=new ArrayList<>();
        for(var entity:mc.world.getEntities()) if(entity instanceof LivingEntity target && target!=mc.player && target.isAlive()
            && !target.isSpectator() && target.distanceTo(mc.player)<value("EnemyRange")) {
            if(target instanceof PlayerEntity p) { if(!flag("Players") || p.isCreative() || CombatSupport.friend(p)) continue; }
            else if(target instanceof TameableEntity pet && pet.isTamed()) { if(!flag("Tamed")) continue; }
            else if(target instanceof HostileEntity) { if(!flag("Hostile")) continue; }
            else if(target instanceof AnimalEntity) { if(!flag("Animals")) continue; } else continue;
            targets.add(target);
        }
        targets.sort(Comparator.comparingDouble(t -> t.squaredDistanceTo(mc.player)));
        if(!targets.isEmpty()) {
            LivingEntity focused=null;
            if(now()<focusUntil)for(var target:targets)if(target.getId()==focusId) {focused=target;break;}
            if(focused==null) { focused=targets.get(0);focusId=focused.getId();focusUntil=now()+(long)value("TargetLockMs"); }
            targets.remove(focused);targets.add(0,focused);
        } else focusId=-1;
        return targets.size()>value("MaxTargets")?new ArrayList<>(targets.subList(0,(int)value("MaxTargets"))):targets;
    }
    private float damage(Vec3d center,LivingEntity entity) {
        return damage(center,entity,Vec3d.ZERO);
    }
    private float damage(Vec3d center,LivingEntity entity,Vec3d offset) {
        DamageKey key=new DamageKey(center,entity.getId(),offset); Float known=damageCache.get(key);
        if(known!=null) { cacheHits++; return known; }
        if(!budget.acquire(System.nanoTime())){budgetStops++;return Float.NaN;}
        calculations++; float damage=CombatSupport.damage(center,entity,offset); damageCache.put(key,damage); return damage;
    }
    private Vec3d prediction(LivingEntity entity) {
        return predictionCache.computeIfAbsent(entity.getId(),id->computePrediction(entity));
    }
    private Vec3d computePrediction(LivingEntity entity) {
        if(value("PredictionTicks")==0 || !(entity instanceof PlayerEntity)) return Vec3d.ZERO;
        var history=motion.get(entity.getId());if(history==null)return Vec3d.ZERO;
        double horizon=value("PredictionTicks");
        if(flag("LatencyPrediction") && mc.getNetworkHandler()!=null){var entry=mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());if(entry!=null)horizon=Math.min(3,horizon+entry.getLatency()/100.0);}
        var forecast=history.predict(horizon);Vec3d offset=new Vec3d(forecast.x(),forecast.y(),forecast.z());
        // Reject paths crossing block collisions, including narrow walls between endpoints.
        for(int step=1;step<=4;step++) if(!mc.world.isSpaceEmpty(entity,entity.getBoundingBox().offset(offset.multiply(step/4.0)))) return Vec3d.ZERO;
        return offset;
    }
    private float defensiveDamage(Vec3d center,LivingEntity entity) {
        return Math.max(damage(center,entity),damage(center,entity,prediction(entity)));
    }
    private boolean faceplace(LivingEntity target) {
        if(target.getHealth()+target.getAbsorptionAmount()<=value("Faceplace")) return true;
        if(!(target instanceof PlayerEntity) || value("ArmorFaceplace")<=0) return false;
        for(var armor:target.getArmorItems()) if(armor.isEmpty() || armor.isDamageable()
            && (armor.getMaxDamage()-armor.getDamage())*100.0/armor.getMaxDamage()<=value("ArmorFaceplace")) return true;
        return false;
    }
    private Evaluation evaluate(Vec3d center,List<LivingEntity> targets,boolean any) {
        float self=defensiveDamage(center,mc.player);
        float committed=reservedDamage(center,mc.player);
        if(!CrystalPolicy.safe(self,committed,value("MaxSelfDamage"),mc.player.getHealth()+mc.player.getAbsorptionAmount(),value("SafetyMargin"),flag("AntiSuicide"))) {rejected.merge("self/pending damage",1,Integer::sum);return null;}
        if(flag("ProtectFriends")) for(var friend:mc.world.getPlayers()) if(CombatSupport.friend(friend) && friend.squaredDistanceTo(center)<=144) {
            if(!CrystalPolicy.safe(defensiveDamage(center,friend),reservedDamage(center,friend),value("MaxSelfDamage"),friend.getHealth()+friend.getAbsorptionAmount(),value("SafetyMargin"),true)) {rejected.merge("friend damage",1,Integer::sum);return null;}
        }
        Evaluation best=null;
        for(var target:targets) {
            float damage=Math.min(damage(center,target),damage(center,target,prediction(target))); float health=target.getHealth()+target.getAbsorptionAmount();
            boolean lethal=flag("LethalPriority") && damage>=health;
            if(!Float.isFinite(damage) || damage<=0 || !(damage>=value("MinimumDamage") || lethal || damage>=1 && faceplace(target) || any)) continue;
            if(!lethal && self>0 && damage/self<value("MinimumTradeRatio")){rejected.merge("poor damage trade",1,Integer::sum);continue;}
            double score=CrystalPolicy.score(damage,self,health,value("SelfDamageWeight"),flag("LethalPriority"));
            if(best==null || score>best.score()) best=new Evaluation(score,damage,self,target.getId());
        }
        if(best==null)rejected.merge("target damage",1,Integer::sum);
        return best;
    }
    private float reservedDamage(Vec3d except,LivingEntity entity) {
        if(!flag("ReservePendingDamage"))return 0;
        Set<Vec3d> centers=new HashSet<>();
        for(var base:tracker.reservedBases())centers.add(Vec3d.ofBottomCenter(base.up()));
        for(int id:tracker.activeAttacks(now(),Math.max(retryTimeout(),(long)value("AttackRetry")))) {
            var crystal=mc.world.getEntityById(id);if(crystal!=null)centers.add(crystal.getPos());
        }
        float total=0;for(var center:centers)if(center.squaredDistanceTo(except)>0.001)total+=defensiveDamage(center,entity);
        return total;
    }
}
