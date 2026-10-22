package dev.kthreadd.mixin.pvp;
import dev.kthreadd.KthreaddClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MinecraftClient.class)
public abstract class PvpClientMixin {
 @Shadow private int itemUseCooldown;
 @Inject(method="doItemUse",at=@At("HEAD"))private void kthreadd$pvp$fastCrystal(CallbackInfo ci){var c=KthreaddClient.INSTANCE;var p=MinecraftClient.getInstance().player;if(c!=null&&c.modules!=null&&c.modules.on("FastCrystal")&&p!=null&&p.isHolding(Items.END_CRYSTAL))itemUseCooldown=0;}
 @Inject(method="tick",at=@At("HEAD"))private void kthreadd$pvp$fastTick(CallbackInfo ci){var c=KthreaddClient.INSTANCE;var p=MinecraftClient.getInstance().player;if(c!=null&&c.modules!=null&&c.modules.on("FastCrystal")&&p!=null&&p.isHolding(Items.END_CRYSTAL))itemUseCooldown=0;}
}
