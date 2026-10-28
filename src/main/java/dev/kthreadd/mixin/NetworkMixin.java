package dev.kthreadd.mixin;
import dev.kthreadd.KthreaddClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.*;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayNetworkHandler.class)
public abstract class NetworkMixin {
    @Redirect(method="onEntityVelocityUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;setVelocityClient(DDD)V"))
    private void kthreadd$velocity(Entity entity,double x,double y,double z) {
        var c=KthreaddClient.INSTANCE;
        if(c!=null && entity==MinecraftClient.getInstance().player && c.modules.on("NoVelocity") && c.modules.get("NoVelocity").flag("EntityVelocity")) {
            var m=c.modules.get("NoVelocity"); var old=entity.getVelocity();
            entity.setVelocityClient(old.x+(x-old.x)*m.value("Horizontal"),old.y+(y-old.y)*m.value("Vertical"),old.z+(z-old.z)*m.value("Horizontal"));
        } else entity.setVelocityClient(x,y,z);
    }
    @Redirect(method="onExplosion",at=@At(value="INVOKE",target="Lnet/minecraft/client/network/ClientPlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private void kthreadd$explosion(ClientPlayerEntity player,Vec3d incoming) {
        var c=KthreaddClient.INSTANCE;
        if(c!=null && c.modules.on("NoVelocity") && c.modules.get("NoVelocity").flag("Explosions")) {
            var m=c.modules.get("NoVelocity"); var old=player.getVelocity(); var change=incoming.subtract(old);
            player.setVelocity(old.add(change.x*m.value("Horizontal"),change.y*m.value("Vertical"),change.z*m.value("Horizontal")));
        } else player.setVelocity(incoming);
    }
    @Inject(method="onEntityStatus",at=@At("TAIL"))
    private void kthreadd$pop(EntityStatusS2CPacket packet,CallbackInfo ci) {
        var c=KthreaddClient.INSTANCE; var mc=MinecraftClient.getInstance();
        if(c==null || mc.world==null || !c.modules.on("TotemPopCounter") || packet.getStatus()!=35) return;
        var entity=packet.getEntity(mc.world);
        if(entity instanceof net.minecraft.entity.player.PlayerEntity player) {
            if(player==mc.player && !c.modules.get("TotemPopCounter").flag("Self"))return;
            int count=c.totemPops.merge(player.getUuid(),1,Integer::sum); if(c.modules.get("TotemPopCounter").flag("Notify"))c.notify(player.getName().getString()+" popped "+count+" totem"+(count==1?"":"s"));
        }
    }
}
