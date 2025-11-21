package shieldbreak.mixin.vanilla;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shieldbreak.handlers.ModConfig;
import shieldbreak.util.IEntityPlayerMixin;

@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseMixin {
	
	@ModifyConstant(
			method = "isActiveItemStackBlocking",
			constant = @Constant(intValue = 5)
	)
	private int shieldbreak_vanillaEntityLivingBase_isActiveItemStackBlocking(int constant) {
		return ModConfig.server.shieldRaiseTickDelay;
	}
	
	@Inject(
			method = "attackEntityFrom",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;canBlockDamageSource(Lnet/minecraft/util/DamageSource;)Z")
	)
	private void shieldbreak_vanillaEntityLivingBase_attackEntityFrom(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if(this instanceof IEntityPlayerMixin) {
			((IEntityPlayerMixin)this).shieldbreak$setCurrentDamage(amount);
		}
	}
}