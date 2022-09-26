package shieldbreak.util;

import net.minecraft.potion.Potion;

public class PotionEntry {
    private final Potion potion;
    private final int duration;
    private final int amplifier;

    public PotionEntry(Potion potion, int duration, int amplifier) {
        this.potion=potion;
        this.duration=duration;
        this.amplifier=amplifier;
    }

    public Potion getPotion() {
        return this.potion;
    }

    public int getDuration() {
        return this.duration;
    }

    public int getAmplifier() {
        return this.amplifier;
    }
}
