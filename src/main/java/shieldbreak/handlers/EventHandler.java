package shieldbreak.handlers;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;
import shieldbreak.core.ShieldBreak;
import shieldbreak.util.PotionEntry;

public class EventHandler
{
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingAttack(LivingAttackEvent event) {
		if(event.getEntityLiving().world.isRemote || event.isCanceled() || !(event.getEntityLiving() instanceof EntityPlayer) || !(event.getSource().getTrueSource() instanceof EntityLivingBase) || event.getAmount() <= 0.0F) return;
		
		World world = event.getEntityLiving().world;
		DamageSource damageSource = event.getSource();
		EntityPlayer player = (EntityPlayer)event.getEntityLiving();
		EntityLivingBase attacker = (EntityLivingBase)damageSource.getTrueSource();
		Float damageAmount = event.getAmount();
		
		if(!canPlayerBlockDamageSource(player, damageSource)) return;
		
		ItemStack playerItem = player.isHandActive() ? player.getActiveItemStack() : ItemStack.EMPTY;
		if(!(playerItem.getItem() instanceof ItemShield)) return;//Technically already checked for blocking, but who knows conflicts
		
		ItemStack attackerItem = attacker.getHeldItem(attacker.getActiveHand());
		
		event.setCanceled(true);
		
		if(damageShield(player, playerItem, event.getAmount())) return;//If shield breaks, dont damage player, but also dont knockback attacker

		if(!attackerItem.isEmpty() && attackerItem.getItem().canDisableShield(attackerItem, playerItem, player, attacker)) {
			float chance = ModConfig.server.shieldBypassChance + (EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, attackerItem)*0.05F);
			if(world.rand.nextFloat() < chance) {//Shield broken by shieldbreak weapon
				player.getCooldownTracker().setCooldown(playerItem.getItem(), ModConfig.server.shieldBypassCooldown);
				player.resetActiveHand();
				world.setEntityState(player, (byte)30);
				doPotionEffects(attacker, player, false);
				return;
			}
		}
		
		int parryTicks = player.getItemInUseMaxCount();
		float shieldDurability = (float)((playerItem.getMaxDamage() > 0) ? playerItem.getMaxDamage() : ModConfig.server.unbreakableShieldDurability);
		float shieldProtection = Math.max(ModConfig.server.damageMinimumThreshold, Math.min(ModConfig.server.damageMaximumThreshold, shieldDurability/ModConfig.server.damageDurabilityScaling));
		float knockbackPower = 0.5F;
		boolean parry = false;
		boolean broken = false;

		//Parry
		if(parryTicks < (ModConfig.server.parryTickRange+ModConfig.server.shieldRaiseTickDelay)) {
			world.playSound(null, player.getPosition(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 0.3F);
			knockbackPower = 1.0F;
			parry = true;
		}
		else if(damageAmount > shieldProtection) {//Shield broken by damage
			player.getCooldownTracker().setCooldown(playerItem.getItem(), (int)Math.max(ModConfig.server.cooldownTicksMinimum, Math.min(ModConfig.server.cooldownTicksMaximum, ((damageAmount-shieldProtection)*ModConfig.server.cooldownTicksScaling))));
			player.resetActiveHand();
			world.setEntityState(player, (byte)30);
			broken = true;
		}
		else {//Normal block
			world.setEntityState(player, (byte)29);
		}
		
		if(!damageSource.isProjectile()) {
			attacker.knockBack(player, knockbackPower, player.posX - attacker.posX, player.posZ - attacker.posZ);
			if(attacker instanceof EntityPlayerMP) {
				//System.out.println("Attacker packet sent");
				((EntityPlayerMP)attacker).connection.sendPacket(new SPacketEntityVelocity(attacker));
			}
			if(parry || broken) doPotionEffects(attacker, player, parry);
		}
		
		//Causes slight momentary visual wierdness with shield after hit, but seems to fix gui exploit?
		if(ModConfig.server.alwaysResetActiveHand) player.resetActiveHand();
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	@SideOnly(Side.CLIENT)
	public void onItemTooltip(ItemTooltipEvent event) {
		if(event.isCanceled() || !(event.getItemStack().getItem() instanceof ItemShield)) return;
		
		ItemStack itemStack = event.getItemStack();
		float shieldDurability = (float)((itemStack.getMaxDamage() > 0) ? itemStack.getMaxDamage() : ModConfig.server.unbreakableShieldDurability);
		float shieldProtection = Math.max(ModConfig.server.damageMinimumThreshold, Math.min(ModConfig.server.damageMaximumThreshold, shieldDurability/ModConfig.server.damageDurabilityScaling));
		double shieldProtectionRounded = ((double)((int)(shieldProtection*100)))/100D;
		
        event.getToolTip().add(TextFormatting.GREEN + "Shielding Power: " + shieldProtectionRounded + TextFormatting.RESET);
	}

	private void doPotionEffects(EntityLivingBase attacker, EntityLivingBase defender, boolean parry) {
		PotionEntry attackerEffect;
		PotionEntry defenderEffect;

		if(parry) {
			attackerEffect = ModConfig.server.getEffectAttackerParry();
			defenderEffect = ModConfig.server.getEffectDefenderParry();
		}
		else {
			attackerEffect = ModConfig.server.getEffectAttackerBreak();
			defenderEffect = ModConfig.server.getEffectDefenderBreak();
		}

		if(attackerEffect!=null) {
			attacker.addPotionEffect(new PotionEffect(attackerEffect.getPotion(), attackerEffect.getDuration(), attackerEffect.getAmplifier()));
		}
		if(defenderEffect!=null) {
			defender.addPotionEffect(new PotionEffect(defenderEffect.getPotion(), defenderEffect.getDuration(), defenderEffect.getAmplifier()));
		}
	}
	
	private boolean canPlayerBlockDamageSource(EntityPlayer playerIn, DamageSource damageSourceIn)
    {
        if(!damageSourceIn.isUnblockable() && isActiveItemStackBlockingWithDelay(playerIn)) {
            Vec3d vec3d = damageSourceIn.getDamageLocation();
            if(vec3d != null) {
                Vec3d vec3d1 = playerIn.getLook(1.0F);
                Vec3d vec3d2 = vec3d.subtractReverse(new Vec3d(playerIn.posX, playerIn.posY, playerIn.posZ)).normalize();
                vec3d2 = new Vec3d(vec3d2.x, 0.0D, vec3d2.z);

                if(vec3d2.dotProduct(vec3d1) < 0.0D) return true;
            }
        }
        return false;
    }
	
	private boolean isActiveItemStackBlockingWithDelay(EntityPlayer playerIn)
    {
        if(playerIn.isHandActive() && !playerIn.getActiveItemStack().isEmpty()) {
            Item item = playerIn.getActiveItemStack().getItem();

            if(item.getItemUseAction(playerIn.getActiveItemStack()) != EnumAction.BLOCK) return false;
            else return playerIn.getItemInUseMaxCount() >= ModConfig.server.shieldRaiseTickDelay;
        }
        else return false;
    }
	
	private boolean damageShield(EntityPlayer playerIn, ItemStack shieldIn, float damage)
    {
		ItemStack shieldCopy = shieldIn.copy();
        int i = Math.max(1, (int)damage);
        shieldIn.damageItem(i, playerIn);

        if(playerIn.getActiveItemStack().isEmpty()) {
            EnumHand hand = playerIn.getActiveHand();
            ForgeEventFactory.onPlayerDestroyItem(playerIn, shieldCopy, hand);

            playerIn.setItemStackToSlot(hand.equals(EnumHand.MAIN_HAND) ? EntityEquipmentSlot.MAINHAND : EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
            playerIn.resetActiveHand();
            playerIn.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + playerIn.world.rand.nextFloat() * 0.4F);
            
            return true;
        }
        return false;
    }
}
