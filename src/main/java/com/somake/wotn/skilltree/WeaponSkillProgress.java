package com.somake.wotn.skilltree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record WeaponSkillProgress(int points, int unlockedMask, int masteryLevel, int masteryXp) {
    public static final int MAX_MASTERY_LEVEL = 10;
    public static final int DEFAULT_UNLOCKED = bit(LeviathanSkillTree.ROOT)
            | bit(LeviathanSkillTree.THROW);
    public static final WeaponSkillProgress DEFAULT = new WeaponSkillProgress(0, DEFAULT_UNLOCKED, 1, 0);

    public static final Codec<WeaponSkillProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("points", 0).forGetter(WeaponSkillProgress::points),
            Codec.INT.optionalFieldOf("unlocked", DEFAULT_UNLOCKED).forGetter(WeaponSkillProgress::unlockedMask),
            Codec.INT.optionalFieldOf("mastery_level", 1).forGetter(WeaponSkillProgress::masteryLevel),
            Codec.INT.optionalFieldOf("mastery_xp", 0).forGetter(WeaponSkillProgress::masteryXp))
            .apply(instance, WeaponSkillProgress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponSkillProgress> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.points());
                buf.writeVarInt(value.unlockedMask());
                buf.writeVarInt(value.masteryLevel());
                buf.writeVarInt(value.masteryXp());
            },
            buf -> new WeaponSkillProgress(buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt()));

    public WeaponSkillProgress {
        points = Math.max(0, points);
        unlockedMask |= DEFAULT_UNLOCKED;
        masteryLevel = Math.clamp(masteryLevel, 1, MAX_MASTERY_LEVEL);
        masteryXp = masteryLevel >= MAX_MASTERY_LEVEL ? 0 : Math.max(0, masteryXp);
    }

    public boolean isUnlocked(int nodeId) {
        return (unlockedMask & bit(nodeId)) != 0;
    }

    public WeaponSkillProgress unlock(LeviathanSkillTree.Node node) {
        return new WeaponSkillProgress(points - node.cost(), unlockedMask | bit(node.id()),
                masteryLevel, masteryXp);
    }

    public MasteryGain addMasteryXp(int amount) {
        int level = masteryLevel;
        int xp = masteryXp + Math.max(0, amount);
        int gainedLevels = 0;
        int gainedPoints = 0;
        while (level < MAX_MASTERY_LEVEL && xp >= xpRequired(level)) {
            xp -= xpRequired(level);
            level++;
            gainedLevels++;
            gainedPoints += pointsForLevel(level);
        }
        if (level >= MAX_MASTERY_LEVEL) xp = 0;
        return new MasteryGain(new WeaponSkillProgress(points + gainedPoints, unlockedMask, level, xp),
                gainedLevels, gainedPoints);
    }

    public static WeaponSkillProgress maximized() {
        int unlocked = DEFAULT_UNLOCKED;
        for (LeviathanSkillTree.Node node : LeviathanSkillTree.NODES) {
            if (node.implemented()) unlocked |= bit(node.id());
        }
        return new WeaponSkillProgress(0, unlocked, MAX_MASTERY_LEVEL, 0);
    }

    public static int xpRequired(int level) {
        return 100 + (Math.max(1, level) - 1) * 75;
    }

    public static int pointsForLevel(int level) {
        return level == 5 || level == 10 ? 2 : 1;
    }

    public record MasteryGain(WeaponSkillProgress progress, int levelsGained, int pointsGained) {
    }

    private static int bit(int nodeId) {
        return 1 << nodeId;
    }
}
