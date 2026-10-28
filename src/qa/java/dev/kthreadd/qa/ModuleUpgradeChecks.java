package dev.kthreadd.qa;
import dev.kthreadd.KthreaddClient;
import dev.kthreadd.modules.combat.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Blocks;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
/** Actual container support and bow transactions, plus lifecycle checks for protection ownership. */
public final class ModuleUpgradeChecks {
    private final MinecraftClient mc=MinecraftClient.getInstance();
    private final Consumer<String> out;
    private CompletableFuture<?> future;
    private int phase,wait;
    private final BlockPos support=new BlockPos(1,199,3),target=support.up();
    public ModuleUpgradeChecks(Consumer<String> out){this.out=out;}
    public boolean tick(){
        if(++wait>240)throw new AssertionError("Module upgrade fixture timed out at phase "+phase);
        var modules=KthreaddClient.INSTANCE.modules;
        switch(phase){
            case 0->{
                var uuid=mc.player.getUuid();future=mc.getServer().submit(()->{
                    var world=mc.getServer().getOverworld();var player=mc.getServer().getPlayerManager().getPlayer(uuid);
                    player.teleport(world,.5,200,.5,0,0);player.getInventory().clear();player.getInventory().setStack(0,new ItemStack(Items.BOW));
                    player.getInventory().setStack(1,new ItemStack(Items.OBSIDIAN,32));player.getInventory().setStack(9,new ItemStack(Items.ARROW,32));
                    player.getInventory().selectedSlot=0;player.currentScreenHandler.sendContentUpdates();
                    world.setBlockState(support,Blocks.CHEST.getDefaultState());world.setBlockState(target,Blocks.AIR.getDefaultState());
                });next();
            }
            case 1->{
                if(!future.isDone())return false;future.join();
                if(!mc.player.getMainHandStack().isOf(Items.BOW)||!mc.player.getInventory().getStack(1).isOf(Items.OBSIDIAN)||!mc.world.getBlockState(support).isOf(Blocks.CHEST))return false;
                CombatActions.beginTick();check(CombatSupport.place(target,Items.OBSIDIAN,true),"Placement helper attempts a block using a chest support",out);next();
            }
            case 2->{
                if(!mc.world.getBlockState(target).isOf(Blocks.OBSIDIAN))return false;
                check(mc.currentScreen==null,"Chest-supported placement reaches the server without opening the chest",out);
                check(!mc.player.isSneaking()&&mc.player.getInventory().selectedSlot==0,"Placement restores prior sneak and local selected-slot state",out);
                mc.player.setYaw(0);mc.player.setPitch(0);modules.get("FastBow").setting("ChargeTicks").set(20);modules.get("FastBow").setEnabled(true);
                mc.options.useKey.setPressed(true);mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND);next();
            }
            case 3->{
                if(wait<12){for(var entity:mc.world.getEntities())if(entity instanceof PersistentProjectileEntity)throw new AssertionError("FastBow released before configured charge");return false;}
                check(mc.player.isUsingItem(),"FastBow retains the bow beyond the old three-tick release",out);next();
            }
            case 4->{
                boolean arrow=false;for(var entity:mc.world.getEntities())if(entity instanceof PersistentProjectileEntity){arrow=true;break;}if(!arrow)return false;
                mc.options.useKey.setPressed(false);modules.get("FastBow").setEnabled(false);
                check(true,"Configured full-charge FastBow produces a real server projectile",out);
                var chain=modules.get("AntiChainPop");var feet=modules.get("AutoFeetObby");feet.setEnabled(false);chain.setEnabled(true);
                mc.getNetworkHandler().onEntityStatus(new EntityStatusS2CPacket(mc.player,(byte)35));
                check(feet.enabled(),"AntiChainPop acquires temporary surround after a local pop",out);
                chain.setEnabled(false);check(!feet.enabled(),"Disabling AntiChainPop releases its owned surround",out);
                feet.setEnabled(true);chain.setEnabled(true);mc.getNetworkHandler().onEntityStatus(new EntityStatusS2CPacket(mc.player,(byte)35));chain.setEnabled(false);
                check(feet.enabled(),"AntiChainPop preserves a surround enabled independently",out);feet.setEnabled(false);
                writeInventory(modules);return true;
            }
        }
        return false;
    }
    private static void writeInventory(dev.kthreadd.ModuleManager modules){
        var rows=new ArrayList<Map<String,Object>>();
        for(var module:modules.all){var row=new LinkedHashMap<String,Object>();row.put("name",module.name);row.put("category",module.category.name());row.put("description",module.description);row.put("settings",module.settings.stream().map(s->Map.of("name",s.name,"min",s.min,"max",s.max,"step",s.step)).toList());rows.add(row);}
        try{java.nio.file.Files.writeString(java.nio.file.Path.of("qa-results/module-inventory.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));}catch(java.io.IOException e){throw new AssertionError(e);}
    }
    private void next(){phase++;wait=0;}
    private static void check(boolean ok,String label,Consumer<String> out){if(!ok)throw new AssertionError(label);out.accept("PASS "+label);}
}
