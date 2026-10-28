package dev.kthreadd.movement;

import dev.kthreadd.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.tag.BlockTags;
import org.lwjgl.glfw.GLFW;

/** Shared guards and input ownership for local movement modules. */
public final class MovementControl {
    private static final MinecraftClient mc=MinecraftClient.getInstance();
    private MovementControl() {}
    public static boolean playable() {
        return mc.player!=null && mc.world!=null && mc.player.isAlive() && !mc.player.isSpectator() && !mc.player.hasVehicle();
    }
    public static boolean ordinary() {
        return playable() && !mc.player.isTouchingWater() && !mc.player.isInLava() && !mc.player.isClimbing() && !mc.player.getAbilities().flying && !mc.player.isFallFlying();
    }
    public static boolean controlsAllowed() { return mc.currentScreen==null && mc.isWindowFocused(); }
    public static boolean owns(ModuleManager manager,String name) {
        if(!playable() || !controlsAllowed()) return false;
        boolean ordinary=ordinary() && !mc.player.isSneaking() && (!mc.player.isUsingItem() || manager.on("NoSlowDown"));
        return name.equals(MovementPolicy.owner(manager.on("Flight"),manager.on("ElytraFly") && mc.player.isFallFlying(),
            ordinary && manager.on("Speed"),ordinary && manager.on("FastIce"),ordinary && manager.on("Strafe"),
            mc.player.isOnGround(),mc.world.getBlockState(mc.player.getBlockPos().down()).isIn(BlockTags.ICE)));
    }
    public static boolean physical(KeyBinding binding) {
        if(!mc.isWindowFocused()) return false;
        var key=InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
        if(key.getCategory()==InputUtil.Type.MOUSE) return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),key.getCode())==GLFW.GLFW_PRESS;
        if(key.getCategory()==InputUtil.Type.KEYSYM && key.getCode()!=GLFW.GLFW_KEY_UNKNOWN) return InputUtil.isKeyPressed(mc.getWindow().getHandle(),key.getCode());
        return false;
    }
    public static void restore(KeyBinding key) { key.setPressed(mc.currentScreen==null && physical(key)); }
}
