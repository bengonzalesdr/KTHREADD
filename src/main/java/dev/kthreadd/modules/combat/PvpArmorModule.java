package dev.kthreadd.modules.combat;
import dev.kthreadd.core.*;
import dev.kthreadd.modules.InventoryModules;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.enchantment.*;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import static dev.kthreadd.modules.combat.CombatSupport.mc;
public final class PvpArmorModule extends ClientModule {
 private long next;
 public PvpArmorModule(){super("AutoArmor",Category.COMBAT,"Ranks armor by protection, toughness, blast/protection enchantments and remaining durability. Swaps old armor into the original source slot.");number("Delay",100,50,1000,50);flag("AvoidBinding",true);flag("PreferBlastProtection",true);flag("PreserveElytra",true);}
 public void tick(){if(!InventoryModules.ready()||mc.player.isDead()||System.currentTimeMillis()<next)return;
  for(var type:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
   var worn=mc.player.getEquippedStack(type);if(EnchantmentHelper.hasBindingCurse(worn)||flag("PreserveElytra")&&worn.isOf(Items.ELYTRA))continue;
   int best=-1;double value=score(worn);for(int i=0;i<36;i++){var candidate=mc.player.getInventory().getStack(i);if(!(candidate.getItem() instanceof ArmorItem armor)||armor.getSlotType()!=type||flag("AvoidBinding")&&EnchantmentHelper.hasBindingCurse(candidate))continue;double n=score(candidate);if(n>value+0.001){value=n;best=i;}}
   if(best<0)continue;if(!CombatActions.claim("AutoArmor"))return;int source=InventoryModules.slot(best),sync=mc.player.playerScreenHandler.syncId;
   mc.interactionManager.clickSlot(sync,source,0,SlotActionType.PICKUP,mc.player);mc.interactionManager.clickSlot(sync,8-type.getEntitySlotId(),0,SlotActionType.PICKUP,mc.player);
   if(!mc.player.playerScreenHandler.getCursorStack().isEmpty())mc.interactionManager.clickSlot(sync,source,0,SlotActionType.PICKUP,mc.player);
   next=System.currentTimeMillis()+(long)value("Delay");return;
  }
 }
 private double score(ItemStack s){
  if(!(s.getItem() instanceof ArmorItem a))return -1000;
  double durability=(double)(s.getMaxDamage()-s.getDamage())/Math.max(1,s.getMaxDamage());if(durability<=0)return -1000;
  int armor=0,protection=0,explosionProtection=0;float toughness=0;
  for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
   ItemStack part=slot==a.getSlotType()?s:mc.player.getEquippedStack(slot);
   if(part.getItem() instanceof ArmorItem material){armor+=material.getProtection();toughness+=material.getToughness();}
   int generic=EnchantmentHelper.getLevel(Enchantments.PROTECTION,part);protection+=generic;explosionProtection+=generic+2*EnchantmentHelper.getLevel(Enchantments.BLAST_PROTECTION,part);
  }
  float crystal=net.minecraft.entity.DamageUtil.getInflictedDamage(net.minecraft.entity.DamageUtil.getDamageLeft(40,armor,toughness),Math.min(20,explosionProtection));
  float melee=net.minecraft.entity.DamageUtil.getInflictedDamage(net.minecraft.entity.DamageUtil.getDamageLeft(10,armor,toughness),Math.min(20,protection));
  double crystalWeight=flag("PreferBlastProtection")?.85:.35;
  return -(crystal*crystalWeight+melee*(1-crystalWeight))+durability*.01;
 }
}
