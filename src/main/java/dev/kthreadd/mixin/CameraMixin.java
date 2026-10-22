package dev.kthreadd.mixin;
import dev.kthreadd.KthreaddClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class CameraMixin {
    @Inject(method="tiltViewWhenHurt",at=@At("HEAD"),cancellable=true)
    private void kthreadd$hurt(MatrixStack matrices,float delta,CallbackInfo ci) { var c=KthreaddClient.INSTANCE; if(c!=null && c.modules.on("NoHurtcam")) ci.cancel(); }
}
