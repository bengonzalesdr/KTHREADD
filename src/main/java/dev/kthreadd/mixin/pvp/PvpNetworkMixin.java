package dev.kthreadd.mixin.pvp;
import dev.kthreadd.KthreaddClient;
import dev.kthreadd.modules.combat.CombatHooks;
import dev.kthreadd.modules.PvpModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayNetworkHandler.class)
public abstract class PvpNetworkMixin {
 @Inject(method="onWorldTimeUpdate",at=@At("TAIL"))private void kthreadd$pvp$tps(WorldTimeUpdateS2CPacket packet,CallbackInfo ci){CombatHooks.time();}
 @Inject(method="onEntityStatus",at=@At("TAIL"))private void kthreadd$pvp$chain(EntityStatusS2CPacket packet,CallbackInfo ci){var mc=MinecraftClient.getInstance();var c=KthreaddClient.INSTANCE;if(c!=null&&c.modules!=null&&mc.world!=null&&mc.player!=null&&packet.getStatus()==35&&packet.getEntity(mc.world)==mc.player&&c.modules.on("AntiChainPop"))PvpModules.chainPop();}
}
