package dev.kthreadd.modules.combat;

import dev.kthreadd.KthreaddClient;
import dev.kthreadd.core.*;
import dev.kthreadd.modules.InventoryModules;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import java.util.*;
import static dev.kthreadd.modules.combat.CombatSupport.mc;

public final class KillAuraModule extends ClientModule {
    private int elapsed,index;
    private Entity prior;
    private int previousSlot=-1,ownedSlot=-1;
    private Object player;
    private float yaw,pitch;
    public KillAuraModule(){
        super("KillAura",Category.COMBAT,"Visible-target attacks with FOV/health/distance sorting, normal/smoothed packet rotation and modern weapon cooldowns.");
        mode("Mode","Closest","Priority","Switch");mode("SortMode","FOV","HEALTH","DISTANCE");mode("Rotation","NORMAL","SMOOTH");
        number("Range",3.4,0,6,0.1);number("TurnSpeed",25,1,180,1);flag("Criticals",false);flag("Invisibles",true);flag("Hit Delay",true);flag("TPSSync",false);
        flag("Players",true);flag("Monsters",false);
        for(String s:List.of("Neutrals","Animals","Tamed","Projectile","SwordOnly","PauseIfCrystal","PauseIfEating","AutoSwitch"))flag(s,false);
        number("Ticks",10,1,40,1);number("Iterations",1,1,10,1);
    }
    protected void onEnable(){elapsed=100;index=0;prior=null;if(mc.player!=null){yaw=mc.player.getYaw();pitch=mc.player.getPitch();}}
    protected void onDisable(){restoreWeapon();prior=null;elapsed=index=0;player=null;}
    public void tick(){
        if(player!=mc.player){player=mc.player;previousSlot=ownedSlot=-1;prior=null;elapsed=100;index=0;if(mc.player!=null){yaw=mc.player.getYaw();pitch=mc.player.getPitch();}}
        if(previousSlot>=0 && mc.player!=null && mc.player.getInventory().selectedSlot!=ownedSlot)previousSlot=ownedSlot=-1;
        elapsed=Math.min(1000,elapsed+1);
        if(!InventoryModules.ready()||flag("PauseIfEating")&&mc.player.isUsingItem()||flag("PauseIfCrystal")&&mc.player.isHolding(Items.END_CRYSTAL))return;
        List<Entity> targets=new ArrayList<>();
        for(Entity e:mc.world.getEntities())if(valid(e))targets.add(e);
        Comparator<Entity> order=switch(setting("SortMode").text()){
            case "HEALTH"->Comparator.comparingDouble(e->e instanceof LivingEntity living?living.getHealth()+living.getAbsorptionAmount():Double.MAX_VALUE);
            case "FOV"->Comparator.comparingDouble(this::fov);
            default->Comparator.comparingDouble(e->e.squaredDistanceTo(mc.player));
        };
        targets.sort(order.thenComparingDouble(e->e.squaredDistanceTo(mc.player)).thenComparingInt(Entity::getId));
        if(targets.isEmpty()){prior=null;restoreWeapon();yaw=mc.player.getYaw();pitch=mc.player.getPitch();return;}
        if(setting("Mode").text().equals("Priority")&&prior!=null&&targets.remove(prior))targets.add(0,prior);
        Entity target=targets.get(setting("Mode").text().equals("Switch")?Math.floorMod(index,targets.size()):0);
        int weapon=CombatSupport.weapon(flag("SwordOnly"));
            if(!flag("AutoSwitch") && previousSlot>=0){restoreWeapon();return;}
            if(flag("AutoSwitch") && weapon>=0 && weapon!=mc.player.getInventory().selectedSlot) {
                if(!CombatActions.claim("KillAura"))return;
                if(previousSlot<0)previousSlot=mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot=ownedSlot=weapon;
                return; // Let vanilla update held attributes/cooldown before evaluating an attack.
            }
            if(flag("SwordOnly")&&!(mc.player.getMainHandStack().getItem() instanceof SwordItem))return;
            if(!CombatSupport.canAttackWithCurrentItem())return;
            // Waiting for weapon cooldown must not reserve inventory/rotation from defensive modules.
            if(flag("Hit Delay")){
                if(mc.player.getAttackCooldownProgress(0)<1)return;
                double base=20/Math.max(0.1,mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
                if(flag("TPSSync")&&elapsed<base*20/CombatHooks.tps())return;
            }else if(elapsed<value("Ticks"))return;
            Vec3d delta=target.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
            float wantedYaw=(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90;
            float wantedPitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
            if(setting("Rotation").text().equals("SMOOTH")){
                float turn=(float)value("TurnSpeed");yaw+=MathHelper.clamp(MathHelper.wrapDegrees(wantedYaw-yaw),-turn,turn);pitch+=MathHelper.clamp(wantedPitch-pitch,-turn,turn);
            }else{yaw=wantedYaw;pitch=wantedPitch;}
            if(!CombatActions.claim("KillAura"))return;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw,pitch,mc.player.isOnGround()));
            var aimbot=KthreaddClient.INSTANCE.modules.get("Aimbot");
            if(aimbot.enabled()&&aimbot.setting("Mode").text().equals("Client")){mc.player.setYaw(yaw);mc.player.setPitch(pitch);}
            if(Math.abs(MathHelper.wrapDegrees(wantedYaw-yaw))>8||Math.abs(wantedPitch-pitch)>8)return;
            if((flag("Criticals") || KthreaddClient.INSTANCE.modules.on("Criticals")) && !CombatHooks.prepareAttack(target,flag("Criticals")))return;
            for(int i=0;i<(flag("Hit Delay")?1:value("Iterations"));i++){mc.interactionManager.attackEntity(mc.player,target);mc.player.swingHand(Hand.MAIN_HAND);}
            prior=target;elapsed=0;index++;
    }
    private void restoreWeapon(){
        if(previousSlot>=0 && mc.player!=null && mc.player==player && mc.player.getInventory().selectedSlot==ownedSlot)mc.player.getInventory().selectedSlot=previousSlot;
        previousSlot=ownedSlot=-1;
    }
    private double fov(Entity e){Vec3d d=e.getBoundingBox().getCenter().subtract(mc.player.getEyePos());return Math.abs(MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(d.z,d.x))-90-mc.player.getYaw()));}
    private boolean valid(Entity e){
        if(e==mc.player||!e.isAlive()||e.isSpectator()||e.isInvisible()&&!flag("Invisibles")||distanceToBox(e)>value("Range")||!mc.player.canSee(e))return false;
        if(e instanceof PlayerEntity p)return flag("Players")&&!p.isCreative()&&!CombatSupport.friend(p);
        if(e instanceof TameableEntity p&&p.isTamed())return flag("Tamed");
        if(e instanceof HostileEntity)return flag("Monsters");if(e instanceof AnimalEntity)return flag("Animals");if(e instanceof LivingEntity)return flag("Neutrals");
        return flag("Projectile")&&(e instanceof FireballEntity||e instanceof ShulkerBulletEntity);
    }
    private double distanceToBox(Entity e){
        var box=e.getBoundingBox();var eye=mc.player.getEyePos();
        return eye.distanceTo(new Vec3d(MathHelper.clamp(eye.x,box.minX,box.maxX),MathHelper.clamp(eye.y,box.minY,box.maxY),MathHelper.clamp(eye.z,box.minZ,box.maxZ)));
    }
}
