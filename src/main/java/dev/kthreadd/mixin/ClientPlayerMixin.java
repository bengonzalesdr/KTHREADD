package dev.kthreadd.mixin;
import dev.kthreadd.KthreaddClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerMixin {
    @ModifyExpressionValue(method="tickMovement",at=@At(value="CONSTANT",args="floatValue=0.2"))
    private float kthreadd$noSlow(float original) {
        var c=KthreaddClient.INSTANCE;
        return c!=null && c.modules.on("NoSlowDown") ? 1F : original;
    }
}
