package shieldbreak.handlers;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Level;
import shieldbreak.core.ShieldBreak;
import shieldbreak.util.PotionEntry;

import javax.annotation.Nullable;
import java.util.Arrays;

@Config(modid = ShieldBreak.MODID)
public class ModConfig {
	
	@Config.Comment("Server Config")
	@Config.Name("Server")
	public static final ServerConfig server = new ServerConfig();
	
	public static class ServerConfig{

		private PotionEntry attackerParry = null;
		private PotionEntry defenderParry = null;
		private PotionEntry attackerBreak = null;
		private PotionEntry defenderBreak = null;
		@Config.Comment("Minimum amount of damage any shield will withstand before chance of cooldown.")
		@Config.Name("Damage Minimum Threshold")
		public float damageMinimumThreshold= 1.0F;
		
		@Config.Comment("Maximum amount of damage any shield will withstand before chance of cooldown.")
		@Config.Name("Damage Maximum Threshold")
		public float damageMaximumThreshold= 20.0F;
		
		@Config.Comment("Scaling for how much damage a shield will withstand before chance of cooldown based on max durability. (Durability/This = Damage)")
		@Config.Name("Damage Durability Scaling")
		public float damageDurabilityScaling= 100.0F;
		
		@Config.Comment("Minimum amount of cooldown ticks a shield break will cause.")
		@Config.Name("Cooldown Minimum Ticks")
		public int cooldownTicksMinimum= 10;
		
		@Config.Comment("Maximum amount of cooldown ticks a shield break will cause.")
		@Config.Name("Cooldown Maximum Ticks")
		public int cooldownTicksMaximum= 200;
		
		@Config.Comment("Multiplier for cooldown ticks per point of damage.")
		@Config.Name("Cooldown Ticks Scaling")
		public float cooldownTicksScaling= 10.0f;
		
		@Config.Comment("Range for how long the shield can be held up and still parry the attack. (Counted after the initial shield delay, if any)")
		@Config.Name("Parry Tick Range")
		public int parryTickRange= 10;
		
		@Config.Comment("How much durability a shield should have for the purpose of scaling if it's max durability is 0.")
		@Config.Name("Shield Durability Fallback")
		public int unbreakableShieldDurability= 3000;
		
		@Config.Comment("How many ticks should it take when raising a shield for it to become active.")
		@Config.Name("Shield Raise Tick Delay")
		public int shieldRaiseTickDelay= 5;
		
		@Config.Comment("Base chance for a weapon that can break shields to ignore shield protection and cause a cooldown. (Ex. Axe and Halberd)")
		@Config.Name("Shield Bypass Chance")
		public float shieldBypassChance= 0.25F;
		
		@Config.Comment("How long should the cooldown be if a weapon that can break shields bypasses the shield. (Ex. Axe and Halberd)")
		@Config.Name("Shield Bypass Cooldown")
		public int shieldBypassCooldown= 100;
		
		@Config.Comment("Always reset active hand after shield hit? (Fixes exploit with using handheld gui's to attack while shielding)")
		@Config.Name("Always Reset Active Hand")
		public boolean alwaysResetActiveHand= true;

		@Config.Comment("Potion Effect to apply to an attacker on a parry. (Potion,Duration,Amplifier)")
		@Config.Name("Potion Effect Attacker Parry")
		public String potionEffectAttackerParry = "";

		@Config.Comment("Potion Effect to apply to a defender on a parry. (Potion,Duration,Amplifier)")
		@Config.Name("Potion Effect Defender Parry")
		public String potionEffectDefenderParry = "";

		@Config.Comment("Potion Effect to apply to an attacker on a break. (Potion,Duration,Amplifier)")
		@Config.Name("Potion Effect Attacker Break")
		public String potionEffectAttackerBreak = "";

		@Config.Comment("Potion Effect to apply to a defender on a break. (Potion,Duration,Amplifier)")
		@Config.Name("Potion Effect Defender Break")
		public String potionEffectDefenderBreak = "";

		public void resetEffectCache() {
			attackerParry=null;
			attackerBreak=null;
			defenderParry=null;
			defenderBreak=null;
		}

		@Nullable
		public PotionEntry getEffectAttackerParry() {
			if(attackerParry!=null) return attackerParry;
			if(!potionEffectAttackerParry.isEmpty()) {
				attackerParry = getPotionEffectFromString(cleanEntry(potionEffectAttackerParry));
				return attackerParry;
			}
			return null;
		}
		@Nullable
		public PotionEntry getEffectDefenderParry() {
			if(defenderParry!=null) return defenderParry;
			if(!potionEffectDefenderParry.isEmpty()) {
				defenderParry = getPotionEffectFromString(cleanEntry(potionEffectDefenderParry));
				return defenderParry;
			}
			return null;
		}
		@Nullable
		public PotionEntry getEffectAttackerBreak() {
			if(attackerBreak!=null) return attackerBreak;
			if(!potionEffectAttackerBreak.isEmpty()) {
				attackerBreak = getPotionEffectFromString(cleanEntry(potionEffectAttackerBreak));
				return attackerBreak;
			}
			return null;
		}
		@Nullable
		public PotionEntry getEffectDefenderBreak() {
			if(defenderBreak!=null) return defenderBreak;
			if(!potionEffectDefenderBreak.isEmpty()) {
				defenderBreak = getPotionEffectFromString(cleanEntry(potionEffectDefenderBreak));
				return defenderBreak;
			}
			return null;
		}

		@Nullable
		private PotionEntry getPotionEffectFromString(String[] entry) {
			try {
				Potion potion = Potion.getPotionFromResourceLocation(entry[0]);
				if(potion==null) {
					ShieldBreak.logger.log(Level.WARN, "Invalid potion name: " + entry[0]);
					return null;
				}
				int duration = Integer.parseInt(entry[1]);
				int amplifier = Integer.parseInt(entry[2]);
				return new PotionEntry(potion, duration, amplifier);
			}
			catch(Exception ex) {
				ShieldBreak.logger.log(Level.WARN, "Failed to parse config entry: " + ex);
				return null;
			}
		}

		private String[] cleanEntry(String entry) {
			return Arrays.stream(entry.split(",")).map(String::trim).toArray(String[]::new);
		}
	}
	
	@Mod.EventBusSubscriber(modid = ShieldBreak.MODID)
	private static class EventHandler{
		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if(event.getModID().equals(ShieldBreak.MODID)) {
				ModConfig.server.resetEffectCache();
				ConfigManager.sync(ShieldBreak.MODID, Config.Type.INSTANCE);
			}
		}
	}
}
