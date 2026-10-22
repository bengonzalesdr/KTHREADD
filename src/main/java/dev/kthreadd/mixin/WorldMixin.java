package dev.kthreadd.mixin;
import dev.kthreadd.KthreaddClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(method={"getRainGradient","getThunderGradient"},at=@At("HEAD"),cancellable=true)
    private void kthreadd$weather(float delta,CallbackInfoReturnable<Float> cir) {
        var c=KthreaddClient.INSTANCE;
        if(((World)(Object)this).isClient && c!=null && c.modules.on("NoWeather")) cir.setReturnValue(0F);
    }
}
