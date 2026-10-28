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
 private boolean kthreadd$fastCrystalReady(){
  var c=KthreaddClient.INSTANCE;var mc=MinecraftClient.getInstance();var p=mc.player;
  return c!=null&&c.modules!=null&&c.modules.on("FastCrystal")&&p!=null&&p.isAlive()&&mc.world!=null&&mc.currentScreen==null&&!p.isUsingItem()&&p.isHolding(Items.END_CRYSTAL)&&mc.options.useKey.isPressed();
 }
 @Inject(method="doItemUse",at=@At("HEAD"))private void kthreadd$pvp$fastCrystal(CallbackInfo ci){if(kthreadd$fastCrystalReady())itemUseCooldown=0;}
 @Inject(method="tick",at=@At("HEAD"))private void kthreadd$pvp$fastTick(CallbackInfo ci){if(kthreadd$fastCrystalReady())itemUseCooldown=0;}
}
