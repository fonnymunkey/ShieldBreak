package shieldbreak.mixin.vanilla;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.CooldownTracker;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shieldbreak.handlers.ModConfig;
import shieldbreak.util.IEntityPlayerMixin;
import shieldbreak.util.PotionEntry;

import java.util.List;

@Mixin(EntityPlayer.class)
public abstract class EntityPlayerMixin extends EntityLivingBase implements IEntityPlayerMixin {
	
	@Shadow public abstract CooldownTracker getCooldownTracker();
	
	public EntityPlayerMixin(World worldIn) {
		super(worldIn);
	}
	
	@Unique
	private float shieldbreak$currentAttackDamage = 0.0F;
	
	@Unique
	public void shieldbreak$setCurrentDamage(float damage) {
		this.shieldbreak$currentAttackDamage = damage;
	}
	
	@Inject(
			method = "blockUsingShield",
			at = @At("HEAD"),
			cancellable = true
	)
	private void shieldbreak_vanillaEntityPlayer_blockUsingShield(EntityLivingBase attacker, CallbackInfo ci) {
		ci.cancel();
		
		ItemStack playerItem = this.isHandActive() ? this.getActiveItemStack() : ItemStack.EMPTY;
		if(!(playerItem.getItem().isShield(playerItem, this) || playerItem.getItem() instanceof ItemShield)) return;//If shield item is broken after damage, block attack but don't do additional effects
		
		//Weapon breaks shields
		ItemStack attackerItem = attacker.getHeldItem(attacker.getActiveHand());
		if(!attackerItem.isEmpty() && attackerItem.getItem().canDisableShield(attackerItem, playerItem, this, attacker)) {
			float chance = ModConfig.server.shieldBypassChance + (EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, attackerItem)*0.05F);
			if(this.world.rand.nextFloat() < chance) {//Shield broken by shieldbreak weapon
				this.getCooldownTracker().setCooldown(playerItem.getItem(), ModConfig.server.shieldBypassCooldown);
				this.resetActiveHand();
				this.world.setEntityState(this, (byte)30);
				shieldbreak$doShieldPotionEffects(attacker, this, false);
				return;
			}
		}
		
		int parryTicks = this.getItemInUseMaxCount();
		Float shieldProtectionOverride = playerItem.getItem().getRegistryName() != null ? ModConfig.server.shieldingPowerOverrides.get(playerItem.getItem().getRegistryName().toString()) : null;
		float shieldProtection;
		if(shieldProtectionOverride != null) shieldProtection = shieldProtectionOverride;
		else {
			float shieldDurability = (float)((playerItem.getMaxDamage() > 0) ? playerItem.getMaxDamage() : ModConfig.server.unbreakableShieldDurability);
			shieldProtection = MathHelper.clamp(shieldDurability/ModConfig.server.damageDurabilityScaling, ModConfig.server.damageMinimumThreshold, ModConfig.server.damageMaximumThreshold);
		}
		float knockbackPower = ModConfig.server.knockbackNormal;
		boolean parry = false;
		boolean broken = false;
		
		//Parry
		if(parryTicks < (ModConfig.server.parryTickRange + ModConfig.server.shieldRaiseTickDelay)) {
			this.world.playSound(null, this.getPosition(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 0.3F);
			knockbackPower = ModConfig.server.knockbackParry;
			parry = true;
		}
		//Shield broken by damage
		else if(this.shieldbreak$currentAttackDamage > shieldProtection) {
			this.getCooldownTracker().setCooldown(playerItem.getItem(), (int)Math.max(ModConfig.server.cooldownTicksMinimum, Math.min(ModConfig.server.cooldownTicksMaximum, ((this.shieldbreak$currentAttackDamage - shieldProtection) * ModConfig.server.cooldownTicksScaling))));
			this.resetActiveHand();
			this.world.setEntityState(this, (byte)30);
			knockbackPower = ModConfig.server.knockbackBreak;
			broken = true;
		}
		//Normal block
		else {
			this.world.setEntityState(this, (byte)29);
		}
		
		attacker.knockBack(this, knockbackPower, this.posX - attacker.posX, this.posZ - attacker.posZ);
		if(attacker instanceof EntityPlayerMP) {
			((EntityPlayerMP)attacker).connection.sendPacket(new SPacketEntityVelocity(attacker));
		}
		if(parry || broken) shieldbreak$doShieldPotionEffects(attacker, this, parry);
		
		//Causes slight momentary visual wierdness with shield after hit, but seems to fix gui exploit?
		if(ModConfig.server.alwaysResetActiveHand) this.resetActiveHand();
	}
	
	@Unique
	private static void shieldbreak$doShieldPotionEffects(EntityLivingBase attacker, EntityLivingBase defender, boolean parry) {
		List<PotionEntry> attackerEffect;
		List<PotionEntry> defenderEffect;
		
		if(parry) {
			attackerEffect = ModConfig.server.getEffectAttackerParry();
			defenderEffect = ModConfig.server.getEffectDefenderParry();
		}
		else {
			attackerEffect = ModConfig.server.getEffectAttackerBreak();
			defenderEffect = ModConfig.server.getEffectDefenderBreak();
		}
		
		if(attackerEffect != null) {
			for(PotionEntry entry : attackerEffect) {
				attacker.addPotionEffect(new PotionEffect(entry.getPotion(), entry.getDuration(), entry.getAmplifier()));
			}
		}
		if(defenderEffect != null) {
			for(PotionEntry entry : defenderEffect) {
				defender.addPotionEffect(new PotionEffect(entry.getPotion(), entry.getDuration(), entry.getAmplifier()));
			}
		}
	}
}