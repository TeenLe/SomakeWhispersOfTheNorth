package com.somake.wotn.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record AlchemyPotionConfiguration(List<AlchemyRune> runes) {
    private static final Codec<List<AlchemyRune>> RUNES_CODEC = Codec.STRING.listOf().xmap(
            ids -> ids.stream().map(AlchemyRune::fromId).filter(java.util.Objects::nonNull).toList(),
            values -> values.stream().map(AlchemyRune::id).toList());
    public static final AlchemyPotionConfiguration DEFAULT = new AlchemyPotionConfiguration(List.of());
    public static final Codec<AlchemyPotionConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RUNES_CODEC.optionalFieldOf("runes", List.of()).forGetter(AlchemyPotionConfiguration::runes))
            .apply(instance, AlchemyPotionConfiguration::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyPotionConfiguration> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.runes().size());
                value.runes().forEach(rune -> buf.writeUtf(rune.id(), 48));
            }, buf -> {
                int size = buf.readVarInt();
                if (size < 0 || size > 3) throw new IllegalArgumentException("Invalid alchemy rune count");
                List<AlchemyRune> runes = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    AlchemyRune rune = AlchemyRune.fromId(buf.readUtf(48));
                    if (rune != null) runes.add(rune);
                }
                return new AlchemyPotionConfiguration(runes);
            });

    public AlchemyPotionConfiguration {
        runes = runes.stream().sorted(Comparator.comparingInt(Enum::ordinal)).toList();
    }

    public boolean isValidFor(String family, int tier) {
        if (tier < 1 || tier > 3 || occupiedSlots() > slotCapacity(tier)) return false;
        int path = 0;
        for (AlchemyRune rune : runes) {
            if (!rune.family().equals(family)) return false;
            if (!rune.repeatable() && count(rune) > 1) return false;
            if (rune.prerequisite() != null && (tier < 3 || !has(rune.prerequisite()))) return false;
            if (rune.path() != 0) {
                if (path != 0 && path != rune.path()) return false;
                path = rune.path();
            }
        }
        return true;
    }

    public boolean has(AlchemyRune rune) {
        return runes.contains(rune);
    }

    public int count(AlchemyRune rune) {
        return (int)runes.stream().filter(candidate -> candidate == rune).count();
    }

    public int occupiedSlots() {
        return runes.stream().mapToInt(AlchemyRune::slots).sum();
    }

    public AlchemyPotionConfiguration equip(AlchemyRune rune, String family, int tier) {
        if (!rune.family().equals(family)) return this;
        List<AlchemyRune> updated = new ArrayList<>(runes);
        if (!rune.repeatable()) {
            if (updated.contains(rune)) return this;
            if (rune.prerequisite() != null && !updated.contains(rune.prerequisite())) return this;
        }
        if (rune.path() != 0) {
            updated.removeIf(existing -> existing.family().equals(family)
                    && existing.path() != 0 && existing.path() != rune.path());
        }
        updated.add(rune);
        AlchemyPotionConfiguration candidate = new AlchemyPotionConfiguration(updated);
        return candidate.isValidFor(family, tier) ? candidate : this;
    }

    public AlchemyPotionConfiguration unequip(AlchemyRune rune) {
        List<AlchemyRune> updated = new ArrayList<>(runes);
        int index = updated.lastIndexOf(rune);
        if (index < 0) return this;
        updated.remove(index);
        updated.removeIf(candidate -> candidate.prerequisite() == rune);
        return new AlchemyPotionConfiguration(updated);
    }

    public int durationTicks(String family, int tier) {
        AlchemyRune duration = AlchemyRune.durationRune(family);
        return (120 + (duration == null ? 0 : count(duration) * 60)) * 20;
    }

    public int potency(String family) {
        AlchemyRune potency = AlchemyRune.potencyRune(family);
        return potency == null ? 0 : count(potency);
    }

    public double iceDamageMultiplier(int tier) {
        return 1.0D + (tier * 5 + potency("niflheim") * 15) / 100.0D;
    }

    public double fireDamageReduction(int tier) {
        return Math.min(0.8D, (5 + tier * 5) / 100.0D);
    }

    public double mistRadius() {
        return 3.5D + potency("niflheim") * 0.35D;
    }

    public int whiteoutFreezeThreshold() {
        return 5;
    }

    public int absoluteZeroFreezeThreshold() {
        return 5;
    }

    public double fenrirAttackDamageBonus(int tier) {
        return (tier * 5 + potency("fenrir") * 6) / 100.0D;
    }

    public double fenrirAttackSpeedBonus(int tier) {
        return (2 + tier * 2 + potency("fenrir") * 4) / 100.0D;
    }

    public double idunnHealingBonus(int tier) {
        return (tier * 5 + potency("idunn") * 6) / 100.0D;
    }

    public double idunnMaxHealthBonus(int tier) {
        return tier * 2.0D + potency("idunn") * 2.0D;
    }

    public double venomDamageBonus(int tier) {
        return (tier * 5 + potency("jormungandr") * 15) / 100.0D;
    }

    public double poisonResistance(int tier) {
        return Math.min(0.8D, (5 + tier * 5 + potency("jormungandr") * 15) / 100.0D);
    }

    public int maximumVenomStacks(int tier) {
        return tier + 1;
    }

    public Special special(String family) {
        if (family.equals("niflheim")) {
            if (has(AlchemyRune.WHITEOUT)) return Special.WHITEOUT;
            if (has(AlchemyRune.FROST_MIST)) return Special.FROST_MIST;
            if (has(AlchemyRune.ABSOLUTE_ZERO)) return Special.ABSOLUTE_ZERO;
            if (has(AlchemyRune.RIME_MARK)) return Special.RIME_MARK;
        } else if (family.equals("fenrir")) {
            if (has(AlchemyRune.WILD_HUNT)) return Special.WILD_HUNT;
            if (has(AlchemyRune.PREDATORY_INSTINCT)) return Special.PREDATORY_INSTINCT;
            if (has(AlchemyRune.CHAINBREAKER)) return Special.CHAINBREAKER;
            if (has(AlchemyRune.FERAL_BLOOD)) return Special.FERAL_BLOOD;
        } else if (family.equals("idunn")) {
            if (has(AlchemyRune.GOLDEN_BLOOM)) return Special.GOLDEN_BLOOM;
            if (has(AlchemyRune.ORCHARDS_GRACE)) return Special.ORCHARDS_GRACE;
            if (has(AlchemyRune.IDUNNS_PROMISE)) return Special.IDUNNS_PROMISE;
            if (has(AlchemyRune.RENEWAL_SEED)) return Special.RENEWAL_SEED;
        } else if (family.equals("jormungandr")) {
            if (has(AlchemyRune.SERPENT_RUPTURE)) return Special.SERPENT_RUPTURE;
            if (has(AlchemyRune.CORROSIVE_FANGS)) return Special.CORROSIVE_FANGS;
            if (has(AlchemyRune.WORLD_COIL)) return Special.WORLD_COIL;
            if (has(AlchemyRune.SERPENTINE_MIASMA)) return Special.SERPENTINE_MIASMA;
        }
        return Special.NONE;
    }

    public static int slotCapacity(int tier) {
        return tier;
    }

    public enum Special {
        NONE,
        FROST_MIST, WHITEOUT, RIME_MARK, ABSOLUTE_ZERO,
        PREDATORY_INSTINCT, WILD_HUNT, FERAL_BLOOD, CHAINBREAKER,
        ORCHARDS_GRACE, GOLDEN_BLOOM, RENEWAL_SEED, IDUNNS_PROMISE,
        CORROSIVE_FANGS, SERPENT_RUPTURE, SERPENTINE_MIASMA, WORLD_COIL
    }
}
