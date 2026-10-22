package dev.kthreadd.mixin;
import dev.kthreadd.KthreaddClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
    private void kthreadd$pushable(CallbackInfoReturnable<Boolean> cir) { var c=KthreaddClient.INSTANCE; if(c!=null && (Object)this==MinecraftClient.getInstance().player && c.modules.on("NoPush")) cir.setReturnValue(false); }
    @Inject(method="pushAwayFrom",at=@At("HEAD"),cancellable=true)
    private void kthreadd$push(Entity other,CallbackInfo ci) { var c=KthreaddClient.INSTANCE; if(c!=null && (Object)this==MinecraftClient.getInstance().player && c.modules.on("NoPush")) ci.cancel(); }
}
