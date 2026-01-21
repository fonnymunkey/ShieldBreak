package shieldbreak.handlers;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TooltipHandler {
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	@SideOnly(Side.CLIENT)
	public void onItemTooltip(ItemTooltipEvent event) {
		if(event.isCanceled()) return;
		ItemStack itemStack = event.getItemStack();
		if(!(itemStack.getItem().isShield(itemStack, null) || itemStack.getItem() instanceof ItemShield)) return;
		
		Float shieldProtectionOverride = itemStack.getItem().getRegistryName() != null ? ModConfig.server.shieldingPowerOverrides.get(itemStack.getItem().getRegistryName().toString()) : null;
		float shieldProtection;
		if(shieldProtectionOverride != null) shieldProtection = shieldProtectionOverride;
		else {
			float shieldDurability = (float)((itemStack.getMaxDamage() > 0) ? itemStack.getMaxDamage() : ModConfig.server.unbreakableShieldDurability);
			shieldProtection = MathHelper.clamp(shieldDurability/ModConfig.server.damageDurabilityScaling, ModConfig.server.damageMinimumThreshold, ModConfig.server.damageMaximumThreshold);
		}
		double shieldProtectionRounded = ((double)((int)(shieldProtection * 100))) / 100D;
		
        event.getToolTip().add(TextFormatting.GREEN + I18n.format("tooltip.shieldbreak.shieldingpower") + " " + shieldProtectionRounded + TextFormatting.RESET);
	}
}