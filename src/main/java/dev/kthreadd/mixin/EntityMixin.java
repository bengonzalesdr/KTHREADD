package dev.kthreadd.mixin;
import dev.kthreadd.KthreaddClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method="getStepHeight",at=@At("RETURN"),cancellable=true)
    private void kthreadd$step(CallbackInfoReturnable<Float> cir) {
        var c=KthreaddClient.INSTANCE;
        if(c!=null && (Object)this==MinecraftClient.getInstance().player && c.modules.on("Step")) cir.setReturnValue((float)c.modules.get("Step").value("Height"));
    }
}
