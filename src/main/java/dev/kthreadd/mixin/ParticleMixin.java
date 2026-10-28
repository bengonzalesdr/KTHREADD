package dev.kthreadd.mixin;
import dev.kthreadd.KthreaddClient;
import net.minecraft.client.particle.*;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ParticleManager.class)
public abstract class ParticleMixin {
    @Inject(method="addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",at=@At("HEAD"),cancellable=true)
    private void kthreadd$budget(ParticleEffect effect,double x,double y,double z,double vx,double vy,double vz,CallbackInfoReturnable<Particle> cir) {
        var c=KthreaddClient.INSTANCE; var mc=net.minecraft.client.MinecraftClient.getInstance();
        if(c==null || !c.modules.on("NoLag")) return;
        var module=c.modules.get("NoLag"); double range=module.value("MaxDistance");
        if((mc.player!=null && mc.player.squaredDistanceTo(x,y,z)>range*range) || c.particlesThisTick++>=module.value("ParticlesPerTick")) cir.setReturnValue(null);
    }
}
