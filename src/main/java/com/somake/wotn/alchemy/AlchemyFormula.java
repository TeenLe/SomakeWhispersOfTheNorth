package com.somake.wotn.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Optional;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record AlchemyFormula(String family, int tier, Identifier result, int resultCount,
        int analysisTicks, List<Cost> studyIngredients, List<Cost> brewingIngredients,
        Optional<String> prerequisite, String hiddenTitle, String hint,
        String description, String role, boolean beneficial) {
    public static final Codec<AlchemyFormula> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("family").forGetter(AlchemyFormula::family),
            Codec.intRange(1, 3).fieldOf("tier").forGetter(AlchemyFormula::tier),
            Identifier.CODEC.fieldOf("result").forGetter(AlchemyFormula::result),
            Codec.intRange(1, 16).optionalFieldOf("result_count", 1).forGetter(AlchemyFormula::resultCount),
            Codec.intRange(20, Integer.MAX_VALUE).fieldOf("analysis_ticks")
                    .forGetter(AlchemyFormula::analysisTicks),
            Cost.CODEC.listOf().fieldOf("study_ingredients").forGetter(AlchemyFormula::studyIngredients),
            Cost.CODEC.listOf().fieldOf("brewing_ingredients").forGetter(AlchemyFormula::brewingIngredients),
            Codec.STRING.optionalFieldOf("prerequisite").forGetter(AlchemyFormula::prerequisite),
            Codec.STRING.fieldOf("hidden_title").forGetter(AlchemyFormula::hiddenTitle),
            Codec.STRING.fieldOf("hint").forGetter(AlchemyFormula::hint),
            Codec.STRING.fieldOf("description").forGetter(AlchemyFormula::description),
            Codec.STRING.fieldOf("role").forGetter(AlchemyFormula::role),
            Codec.BOOL.optionalFieldOf("beneficial", true).forGetter(AlchemyFormula::beneficial))
            .apply(instance, AlchemyFormula::new));

    public AlchemyFormula {
        studyIngredients = mergeCosts(studyIngredients);
        brewingIngredients = mergeCosts(brewingIngredients);
    }

    public int totalStudyUnits() {
        return studyIngredients.stream().mapToInt(Cost::count).sum();
    }

    public Optional<Cost> studyCost(String itemId) {
        return studyIngredients.stream().filter(cost -> cost.item().toString().equals(itemId)).findFirst();
    }

    private static List<Cost> mergeCosts(List<Cost> costs) {
        LinkedHashMap<CostIdentity, Integer> merged = new LinkedHashMap<>();
        costs.forEach(cost -> merged.merge(new CostIdentity(cost.item(), cost.components()), cost.count(), Integer::sum));
        return merged.entrySet().stream()
                .map(entry -> new Cost(entry.getKey().item(), entry.getKey().components(), entry.getValue()))
                .toList();
    }

    private record CostIdentity(Identifier item, DataComponentPatch components) {
    }

    public record Cost(Identifier item, DataComponentPatch components, int count) {
        public static final Codec<Cost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("item").forGetter(Cost::item),
                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                        .forGetter(Cost::components),
                Codec.intRange(1, 64).fieldOf("count").forGetter(Cost::count))
                .apply(instance, Cost::new));

        public boolean matches(ItemStack stack) {
            Item expectedItem = BuiltInRegistries.ITEM.getValue(item);
            if (expectedItem == null || !stack.is(expectedItem.builtInRegistryHolder())) return false;
            return components.entrySet().stream()
                    .allMatch(entry -> componentMatches(stack, entry.getKey(), entry.getValue()));
        }

        public ItemStack displayStack() {
            Item itemValue = BuiltInRegistries.ITEM.getValue(item);
            if (itemValue == null) return ItemStack.EMPTY;
            ItemStack stack = new ItemStack(itemValue);
            stack.applyComponents(components);
            return stack;
        }

        private static <T> boolean componentMatches(ItemStack stack, DataComponentType<T> type,
                Optional<?> expected) {
            T actual = stack.get(type);
            return expected.isPresent() ? expected.get().equals(actual) : actual == null;
        }
    }
}
