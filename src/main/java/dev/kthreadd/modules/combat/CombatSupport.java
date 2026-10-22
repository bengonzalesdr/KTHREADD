package dev.kthreadd.modules.combat;
import dev.kthreadd.KthreaddClient;
import dev.kthreadd.modules.InventoryModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.explosion.Explosion;
import dev.kthreadd.core.ExplosionMath;
import java.util.*;
public final class CombatSupport {
    public static final MinecraftClient mc=MinecraftClient.getInstance();
    private CombatSupport() {}
    public static boolean friend(PlayerEntity p) { return KthreaddClient.INSTANCE.isFriend(p.getGameProfile().getName()); }
    public static List<PlayerEntity> players(double range) { return mc.world.getPlayers().stream().filter(p -> p!=mc.player && p.isAlive() && !p.isSpectator() && !p.isCreative() && !friend(p) && p.squaredDistanceTo(mc.player)<=range*range).sorted(Comparator.comparingDouble(p -> p.squaredDistanceTo(mc.player))).map(p -> (PlayerEntity)p).toList(); }
    public static int find(Item item) { for(int i=0;i<9;i++) if(mc.player.getInventory().getStack(i).isOf(item)) return i; return -1; }
    public static int weapon() { int best=-1; double score=0; for(int i=0;i<9;i++) { var item=mc.player.getInventory().getStack(i).getItem(); double s=item instanceof SwordItem sword?sword.getAttackDamage():item instanceof MiningToolItem tool?tool.getAttackDamage():0; if(s>score) { score=s; best=i; } } return best; }
    public static void look(Vec3d pos,boolean client) {
        Vec3d d=pos.subtract(mc.player.getEyePos()); float yaw=(float)Math.toDegrees(Math.atan2(d.z,d.x))-90,pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));
        var aimbot=KthreaddClient.INSTANCE.modules.get("Aimbot");
        if(client || aimbot!=null && aimbot.enabled() && aimbot.setting("Mode").text().equals("Client")) { mc.player.setYaw(yaw); mc.player.setPitch(pitch); }
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw,pitch,mc.player.isOnGround()));
    }
    public static boolean place(BlockPos target,Item item,boolean rotate) {
        int slot=find(item); if(slot<0 || !mc.world.getBlockState(target).isReplaceable() || !mc.world.getOtherEntities(null,new Box(target)).isEmpty()) return false;
        for(Direction face:Direction.values()) {
            BlockPos support=target.offset(face); if(!mc.world.getBlockState(support).isSolidBlock(mc.world,support)) continue;
            Vec3d point=Vec3d.ofCenter(support).add(Vec3d.of(face.getOpposite().getVector()).multiply(0.5)); if(mc.player.getEyePos().squaredDistanceTo(point)>20.25) continue;
            if(!CombatActions.claim("BlockPlacement")) return false;
            if(rotate) look(point,false); int old=mc.player.getInventory().selectedSlot;
            try { mc.player.getInventory().selectedSlot=slot; var result=mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,new BlockHitResult(point,face.getOpposite(),support,false)); if(result.isAccepted()) { mc.player.swingHand(Hand.MAIN_HAND); return true; } }
            finally { mc.player.getInventory().selectedSlot=old; }
        }
        return false;
    }
    public static boolean hole(BlockPos pos) {
        if(!mc.world.isAir(pos) || !mc.world.isAir(pos.up()) || !mc.world.isAir(pos.up(2))) return false;
        for(Direction side:Direction.values()) { if(side==Direction.UP) continue; if(mc.world.getBlockState(pos.offset(side)).getBlock().getBlastResistance()<600) return false; } return true;
    }
    public static float damage(Vec3d center,LivingEntity entity) {
        return damage(center,entity,Vec3d.ZERO);
    }
    /** Forecast exposure against a translated bounding box without mutating the live entity/world. */
    public static float damage(Vec3d center,LivingEntity entity,Vec3d offset) {
        if(entity instanceof PlayerEntity p && p.isCreative()) return 0;
        double distance=entity.getPos().add(offset).distanceTo(center);
        if(distance>12) return 0;
        double exposure=offset.lengthSquared()<1e-8?Explosion.getExposure(center,entity):exposure(center,entity.getBoundingBox().offset(offset),entity);
        float raw=ExplosionMath.difficulty(ExplosionMath.raw(distance,exposure),mc.world.getDifficulty().getId());
        float armor=DamageUtil.getDamageLeft(raw,entity.getArmor(),(float)entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
        var resistance=entity.getStatusEffect(StatusEffects.RESISTANCE); if(resistance!=null) armor=ExplosionMath.resistance(armor,resistance.getAmplifier());
        int enchant=EnchantmentHelper.getProtectionAmount(entity.getArmorItems(),mc.world.getDamageSources().explosion(null,null));
        return DamageUtil.getInflictedDamage(armor,enchant);
    }
    private static float exposure(Vec3d center,Box box,LivingEntity entity) {
        double dx=1/(box.getLengthX()*2+1),dy=1/(box.getLengthY()*2+1),dz=1/(box.getLengthZ()*2+1);
        double ox=(1-Math.floor(1/dx)*dx)/2,oz=(1-Math.floor(1/dz)*dz)/2;
        int clear=0,total=0;
        for(double x=0;x<=1;x+=dx) for(double y=0;y<=1;y+=dy) for(double z=0;z<=1;z+=dz) {
            Vec3d sample=new Vec3d(MathHelper.lerp(x,box.minX,box.maxX)+ox,MathHelper.lerp(y,box.minY,box.maxY),MathHelper.lerp(z,box.minZ,box.maxZ)+oz);
            if(mc.world.raycast(new net.minecraft.world.RaycastContext(sample,center,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,entity)).getType()==net.minecraft.util.hit.HitResult.Type.MISS)clear++;
            total++;
        }
        return total==0?0:(float)clear/total;
    }
}
