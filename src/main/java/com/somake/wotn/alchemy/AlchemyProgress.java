package com.somake.wotn.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record AlchemyProgress(List<String> learnedFormulas,
        List<Study> studies, Optional<Analysis> activeAnalysis) {
    public static final AlchemyProgress DEFAULT = new AlchemyProgress(
            List.of(), List.of(), Optional.empty());
    public static final com.mojang.serialization.MapCodec<AlchemyProgress> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("learned_formulas", List.of())
                    .forGetter(AlchemyProgress::learnedFormulas),
            Codec.STRING.listOf().optionalFieldOf("learned_runes", List.of())
                    .forGetter(progress -> List.of()),
            Study.CODEC.listOf().optionalFieldOf("studies", List.of()).forGetter(AlchemyProgress::studies),
            Analysis.CODEC.optionalFieldOf("active_analysis").forGetter(AlchemyProgress::activeAnalysis))
            .apply(instance, (learnedFormulas, ignoredLearnedRunes, studies, activeAnalysis) ->
                    new AlchemyProgress(learnedFormulas, studies, activeAnalysis)));
    public static final Codec<AlchemyProgress> CODEC = MAP_CODEC.codec();

    public AlchemyProgress {
        learnedFormulas = List.copyOf(learnedFormulas.stream().distinct().toList());
        studies = List.copyOf(studies);
    }

    public boolean isLearned(String formulaId) {
        return learnedFormulas.contains(formulaId);
    }

    public Optional<Study> study(String formulaId) {
        return studies.stream().filter(study -> study.formulaId().equals(formulaId)).findFirst();
    }

    public AlchemyProgress openStudy(String formulaId) {
        if (isLearned(formulaId) || study(formulaId).isPresent()) return this;
        List<Study> updated = new ArrayList<>(studies);
        updated.add(new Study(formulaId, List.of()));
        return new AlchemyProgress(learnedFormulas, updated, activeAnalysis);
    }

    public AlchemyProgress unlockAll(List<String> formulaIds) {
        return new AlchemyProgress(formulaIds, List.of(), Optional.empty());
    }

    public AlchemyProgress startAnalysis(String formulaId, String itemId, int count, long completesAt) {
        return new AlchemyProgress(learnedFormulas, studies,
                Optional.of(new Analysis(formulaId, itemId, count, completesAt)));
    }

    public AlchemyProgress finishAnalysis(String formulaId, String itemId, int count,
            int requiredCount, boolean formulaComplete) {
        List<Study> updatedStudies = new ArrayList<>();
        for (Study study : studies) {
            if (study.formulaId().equals(formulaId)) {
                updatedStudies.add(study.add(itemId, count, requiredCount));
            } else {
                updatedStudies.add(study);
            }
        }
        List<String> learned = new ArrayList<>(learnedFormulas);
        if (formulaComplete) {
            updatedStudies.removeIf(study -> study.formulaId().equals(formulaId));
            if (!learned.contains(formulaId)) learned.add(formulaId);
        }
        return new AlchemyProgress(learned, updatedStudies, Optional.empty());
    }

    public record Study(String formulaId, List<StudiedIngredient> ingredients) {
        public static final Codec<Study> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("formula").forGetter(Study::formulaId),
                StudiedIngredient.CODEC.listOf().optionalFieldOf("ingredients", List.of())
                        .forGetter(Study::ingredients))
                .apply(instance, Study::new));

        public Study {
            ingredients = List.copyOf(ingredients);
        }

        public int studied(String itemId) {
            return ingredients.stream().filter(ingredient -> ingredient.itemId().equals(itemId))
                    .mapToInt(StudiedIngredient::count).sum();
        }

        public Study add(String itemId, int amount, int maximum) {
            List<StudiedIngredient> updated = new ArrayList<>();
            boolean found = false;
            for (StudiedIngredient ingredient : ingredients) {
                if (ingredient.itemId().equals(itemId)) {
                    updated.add(new StudiedIngredient(itemId,
                            Math.min(maximum, ingredient.count() + Math.max(0, amount))));
                    found = true;
                } else {
                    updated.add(ingredient);
                }
            }
            if (!found) updated.add(new StudiedIngredient(itemId, Math.min(maximum, Math.max(0, amount))));
            return new Study(formulaId, updated);
        }
    }

    public record StudiedIngredient(String itemId, int count) {
        public static final Codec<StudiedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("item").forGetter(StudiedIngredient::itemId),
                Codec.INT.optionalFieldOf("count", 0).forGetter(StudiedIngredient::count))
                .apply(instance, StudiedIngredient::new));

        public StudiedIngredient {
            count = Math.max(0, count);
        }
    }

    public record Analysis(String formulaId, String itemId, int count, long completesAt) {
        public static final Codec<Analysis> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("formula").forGetter(Analysis::formulaId),
                Codec.STRING.fieldOf("item").forGetter(Analysis::itemId),
                Codec.INT.fieldOf("count").forGetter(Analysis::count),
                Codec.LONG.fieldOf("completes_at").forGetter(Analysis::completesAt))
                .apply(instance, Analysis::new));

        public Analysis {
            count = Math.max(1, count);
        }
    }
}
