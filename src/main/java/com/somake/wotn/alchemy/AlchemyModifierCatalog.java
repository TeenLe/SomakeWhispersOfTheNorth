package com.somake.wotn.alchemy;

import java.util.List;

public final class AlchemyModifierCatalog {
    public static List<Modifier> forFamily(String family) {
        return switch (family) {
            case "jormungandr" -> List.of(
                    modifier(family, 1, "concentrated_toxin"),
                    modifier(family, 2, "serpents_grip"),
                    modifier(family, 3, "world_coil"));
            case "fenrir" -> List.of(
                    modifier(family, 1, "raging_blood"),
                    modifier(family, 2, "hunters_pulse"),
                    modifier(family, 3, "alpha_howl"));
            case "niflheim" -> List.of(
                    modifier(family, 1, "dense_frost"),
                    modifier(family, 2, "winter_veil"),
                    modifier(family, 3, "stillness_of_hel"));
            case "idunn" -> List.of(
                    modifier(family, 1, "concentrated_nectar"),
                    modifier(family, 2, "golden_renewal"),
                    modifier(family, 3, "orchards_grace"));
            default -> List.of();
        };
    }

    private static Modifier modifier(String family, int tier, String key) {
        String prefix = "screen.wotn.alchemy.modifier." + family + "." + key;
        return new Modifier(tier, prefix, prefix + ".description");
    }

    public record Modifier(int requiredTier, String nameKey, String descriptionKey) {
    }

    private AlchemyModifierCatalog() {
    }
}
