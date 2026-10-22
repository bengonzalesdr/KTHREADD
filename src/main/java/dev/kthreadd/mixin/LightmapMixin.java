package dev.kthreadd.mixin;
import dev.kthreadd.KthreaddClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LightmapTextureManager.class)
public abstract class LightmapMixin {
    @Inject(method="getBrightness",at=@At("HEAD"),cancellable=true)
    private static void kthreadd$brightness(DimensionType dimension,int light,CallbackInfoReturnable<Float> cir) {
        var c=KthreaddClient.INSTANCE;
        if(c!=null && c.modules.on("FullBright")) cir.setReturnValue(1F);
    }
}
