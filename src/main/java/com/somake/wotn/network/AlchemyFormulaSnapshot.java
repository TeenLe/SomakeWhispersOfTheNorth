package com.somake.wotn.network;

import com.somake.wotn.alchemy.AlchemyPotionConfiguration;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record AlchemyFormulaSnapshot(String id, String family, int tier, ItemStack result,
        String hiddenTitle, String hint, String description, String role, boolean beneficial,
        int analysisTicks, State state, int studiedUnits, int totalStudyUnits,
        List<IngredientSnapshot> studyIngredients, List<IngredientSnapshot> brewingIngredients,
        List<ModifierSnapshot> modifiers, AlchemyPotionConfiguration runeConfiguration) {
    private static final int MAX_INGREDIENTS = 16;
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyFormulaSnapshot> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.id(), 128);
                buf.writeUtf(value.family(), 64);
                buf.writeVarInt(value.tier());
                ItemStack.STREAM_CODEC.encode(buf, value.result());
                buf.writeUtf(value.hiddenTitle(), 256);
                buf.writeUtf(value.hint(), 256);
                buf.writeUtf(value.description(), 256);
                buf.writeUtf(value.role(), 256);
                buf.writeBoolean(value.beneficial());
                buf.writeVarInt(value.analysisTicks());
                buf.writeVarInt(value.state().ordinal());
                buf.writeVarInt(value.studiedUnits());
                buf.writeVarInt(value.totalStudyUnits());
                writeIngredients(buf, value.studyIngredients());
                writeIngredients(buf, value.brewingIngredients());
                if (value.modifiers().size() > 3) throw new IllegalArgumentException("Too many alchemy modifiers");
                buf.writeVarInt(value.modifiers().size());
                value.modifiers().forEach(modifier -> ModifierSnapshot.STREAM_CODEC.encode(buf, modifier));
                AlchemyPotionConfiguration.STREAM_CODEC.encode(buf, value.runeConfiguration());
            }, buf -> {
                String id = buf.readUtf(128);
                String family = buf.readUtf(64);
                int tier = buf.readVarInt();
                ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                String hiddenTitle = buf.readUtf(256);
                String hint = buf.readUtf(256);
                String description = buf.readUtf(256);
                String role = buf.readUtf(256);
                boolean beneficial = buf.readBoolean();
                int analysisTicks = buf.readVarInt();
                int stateId = buf.readVarInt();
                int studiedUnits = buf.readVarInt();
                int totalStudyUnits = buf.readVarInt();
                if (tier < 1 || tier > 3 || analysisTicks < 0 || studiedUnits < 0 || totalStudyUnits < 1
                        || stateId < 0 || stateId >= State.values().length) {
                    throw new IllegalArgumentException("Invalid alchemy formula snapshot");
                }
                return new AlchemyFormulaSnapshot(id, family, tier, result, hiddenTitle, hint,
                        description, role, beneficial, analysisTicks, State.values()[stateId],
                        studiedUnits, totalStudyUnits, readIngredients(buf), readIngredients(buf),
                        readModifiers(buf), AlchemyPotionConfiguration.STREAM_CODEC.decode(buf));
            });

    public AlchemyFormulaSnapshot {
        result = result.copy();
        studyIngredients = List.copyOf(studyIngredients);
        brewingIngredients = List.copyOf(brewingIngredients);
        modifiers = List.copyOf(modifiers);
    }

    private static void writeIngredients(RegistryFriendlyByteBuf buf, List<IngredientSnapshot> ingredients) {
        if (ingredients.size() > MAX_INGREDIENTS) throw new IllegalArgumentException("Too many alchemy ingredients");
        buf.writeVarInt(ingredients.size());
        ingredients.forEach(ingredient -> IngredientSnapshot.STREAM_CODEC.encode(buf, ingredient));
    }

    private static List<IngredientSnapshot> readIngredients(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_INGREDIENTS) throw new IllegalArgumentException("Invalid alchemy ingredient count");
        List<IngredientSnapshot> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(IngredientSnapshot.STREAM_CODEC.decode(buf));
        return List.copyOf(result);
    }

    private static List<ModifierSnapshot> readModifiers(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 3) throw new IllegalArgumentException("Invalid alchemy modifier count");
        List<ModifierSnapshot> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(ModifierSnapshot.STREAM_CODEC.decode(buf));
        return List.copyOf(result);
    }

    public enum State {
        HIDDEN,
        STUDYING,
        ANALYZING,
        LEARNED
    }

    public record IngredientSnapshot(ItemStack stack, int required, int contributed, int owned,
            boolean revealed, boolean analyzing) {
        public static final StreamCodec<RegistryFriendlyByteBuf, IngredientSnapshot> STREAM_CODEC = StreamCodec.of(
                (buf, value) -> {
                    ItemStack.STREAM_CODEC.encode(buf, value.stack());
                    buf.writeVarInt(value.required());
                    buf.writeVarInt(value.contributed());
                    buf.writeVarInt(value.owned());
                    buf.writeBoolean(value.revealed());
                    buf.writeBoolean(value.analyzing());
                }, buf -> new IngredientSnapshot(ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean()));

        public IngredientSnapshot {
            stack = stack.copyWithCount(1);
            if (required < 1 || contributed < 0 || owned < 0) {
                throw new IllegalArgumentException("Invalid alchemy ingredient snapshot");
            }
        }
    }

    public record ModifierSnapshot(int requiredTier, String nameKey, String descriptionKey, boolean unlocked) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ModifierSnapshot> STREAM_CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeVarInt(value.requiredTier());
                    buf.writeUtf(value.nameKey(), 256);
                    buf.writeUtf(value.descriptionKey(), 256);
                    buf.writeBoolean(value.unlocked());
                }, buf -> new ModifierSnapshot(buf.readVarInt(), buf.readUtf(256), buf.readUtf(256), buf.readBoolean()));
    }
}
