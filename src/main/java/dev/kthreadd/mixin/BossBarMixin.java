package dev.kthreadd.mixin;
import dev.kthreadd.KthreaddClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BossBarHud.class)
public abstract class BossBarMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void kthreadd$boss(DrawContext ctx,CallbackInfo ci) { var c=KthreaddClient.INSTANCE; if(c!=null && c.modules.on("NoBossBar")) ci.cancel(); }
}
