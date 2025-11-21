package shieldbreak.mixin.ddd;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shieldbreak.util.IEntityPlayerMixin;
import yeelp.distinctdamagedescriptions.capability.impl.DDDCombatTracker;

@Mixin(DDDCombatTracker.class)
public abstract class DDDCombatTrackerMixin {
	
	@Shadow(remap = false) public abstract EntityLivingBase getFighter();
	
	@Inject(
			method = "handleAttackStage",
			at = @At(value = "INVOKE", target = "Lyeelp/distinctdamagedescriptions/mixin/MixinASMEntityLivingBase;useBlockUsingShield(Lnet/minecraft/entity/EntityLivingBase;)V"),
			remap = false
	)
	private void shieldbreak_dddDDDCombatTracker_handleAttackStage(LivingAttackEvent evt, CallbackInfo ci) {
		if(this.getFighter() instanceof IEntityPlayerMixin) {
			((IEntityPlayerMixin)this.getFighter()).shieldbreak$setCurrentDamage(evt.getAmount());
		}
	}
}