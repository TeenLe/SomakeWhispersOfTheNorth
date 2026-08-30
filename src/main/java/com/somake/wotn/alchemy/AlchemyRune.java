package com.somake.wotn.alchemy;

import java.util.List;

import net.minecraft.resources.Identifier;

public enum AlchemyRune {
    GLACIAL_DURATION("niflheim", "glacial_duration", 1, null, true, 0),
    GLACIAL_POWER("niflheim", "glacial_power", 1, null, true, 0),
    FROST_MIST("niflheim", "frost_mist", 2, null, false, 1),
    WHITEOUT("niflheim", "whiteout", 1, FROST_MIST, false, 1),
    RIME_MARK("niflheim", "rime_mark", 2, null, false, 2),
    ABSOLUTE_ZERO("niflheim", "absolute_zero", 1, RIME_MARK, false, 2),

    FERAL_DURATION("fenrir", "feral_duration", 1, null, true, 0),
    FERAL_POWER("fenrir", "feral_power", 1, null, true, 0),
    PREDATORY_INSTINCT("fenrir", "predatory_instinct", 2, null, false, 1),
    WILD_HUNT("fenrir", "wild_hunt", 1, PREDATORY_INSTINCT, false, 1),
    FERAL_BLOOD("fenrir", "feral_blood", 2, null, false, 2),
    CHAINBREAKER("fenrir", "chainbreaker", 1, FERAL_BLOOD, false, 2),

    GOLDEN_DURATION("idunn", "golden_duration", 1, null, true, 0),
    GOLDEN_POWER("idunn", "golden_power", 1, null, true, 0),
    ORCHARDS_GRACE("idunn", "orchards_grace", 2, null, false, 1),
    GOLDEN_BLOOM("idunn", "golden_bloom", 1, ORCHARDS_GRACE, false, 1),
    RENEWAL_SEED("idunn", "renewal_seed", 2, null, false, 2),
    IDUNNS_PROMISE("idunn", "idunns_promise", 1, RENEWAL_SEED, false, 2),

    SERPENTINE_DURATION("jormungandr", "serpentine_duration", 1, null, true, 0),
    SERPENTINE_POWER("jormungandr", "serpentine_power", 1, null, true, 0),
    CORROSIVE_FANGS("jormungandr", "corrosive_fangs", 2, null, false, 1),
    SERPENT_RUPTURE("jormungandr", "serpent_rupture", 1, CORROSIVE_FANGS, false, 1),
    SERPENTINE_MIASMA("jormungandr", "serpentine_miasma", 2, null, false, 2),
    WORLD_COIL("jormungandr", "world_coil", 1, SERPENTINE_MIASMA, false, 2);

    private final String family;
    private final String id;
    private final int slots;
    private final AlchemyRune prerequisite;
    private final boolean repeatable;
    private final int path;

    AlchemyRune(String family, String id, int slots, AlchemyRune prerequisite, boolean repeatable, int path) {
        this.family = family;
        this.id = id;
        this.slots = slots;
        this.prerequisite = prerequisite;
        this.repeatable = repeatable;
        this.path = path;
    }

    public String family() {
        return family;
    }

    public String id() {
        return id;
    }

    public int slots() {
        return slots;
    }

    public AlchemyRune prerequisite() {
        return prerequisite;
    }

    public boolean repeatable() {
        return repeatable;
    }

    public int path() {
        return path;
    }

    public boolean special() {
        return path != 0;
    }

    public Identifier textureId() {
        String texture = family + "_" + slots + "_slot" + (special() ? "_special" : "");
        return Identifier.fromNamespaceAndPath(com.somake.wotn.WhispersOfTheNorth.MODID,
                "item/runes/" + texture);
    }

    public Identifier itemId() {
        return Identifier.fromNamespaceAndPath(com.somake.wotn.WhispersOfTheNorth.MODID,
                "rune_" + id);
    }

    public String translationKey() {
        return "item.wotn.rune_" + id;
    }

    public String descriptionKey() {
        return "tooltip.wotn.rune." + id;
    }

    public static AlchemyRune fromId(String id) {
        for (AlchemyRune rune : values()) if (rune.id.equals(id)) return rune;
        return null;
    }

    public static List<AlchemyRune> ordered() {
        return List.of(values());
    }

    public static List<AlchemyRune> forFamily(String family) {
        return ordered().stream().filter(rune -> rune.family.equals(family)).toList();
    }

    public static AlchemyRune durationRune(String family) {
        return switch (family) {
            case "niflheim" -> GLACIAL_DURATION;
            case "fenrir" -> FERAL_DURATION;
            case "idunn" -> GOLDEN_DURATION;
            case "jormungandr" -> SERPENTINE_DURATION;
            default -> null;
        };
    }

    public static AlchemyRune potencyRune(String family) {
        return switch (family) {
            case "niflheim" -> GLACIAL_POWER;
            case "fenrir" -> FERAL_POWER;
            case "idunn" -> GOLDEN_POWER;
            case "jormungandr" -> SERPENTINE_POWER;
            default -> null;
        };
    }
}
