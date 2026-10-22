package dev.kthreadd.mixin;
import dev.kthreadd.KthreaddClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayerInteractionManager.class)
public abstract class InteractionMixin {
    @Inject(method="attackEntity",at=@At("HEAD"),cancellable=true)
    private void kthreadd$friend(PlayerEntity user,Entity target,CallbackInfo ci) { var c=KthreaddClient.INSTANCE; if(c!=null && c.modules.on("AntiFriendHit") && target instanceof PlayerEntity player && c.isFriend(player.getGameProfile().getName())) ci.cancel(); }
}
