package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenAlchemyPayload(UUID sessionId, List<AlchemyFormulaSnapshot> formulas,
        List<AlchemyRuneSnapshot> runes,
        String selectedFormulaId, String activeResearchId, int remainingTicks,
        String messageKey) implements CustomPacketPayload {
    private static final int MAX_FORMULAS = 64;
    public static final Type<OpenAlchemyPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "open_alchemy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAlchemyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUUID(value.sessionId());
                if (value.formulas().size() > MAX_FORMULAS) throw new IllegalArgumentException("Too many alchemy formulas");
                buf.writeVarInt(value.formulas().size());
                value.formulas().forEach(formula -> AlchemyFormulaSnapshot.STREAM_CODEC.encode(buf, formula));
                buf.writeVarInt(value.runes().size());
                value.runes().forEach(rune -> AlchemyRuneSnapshot.STREAM_CODEC.encode(buf, rune));
                buf.writeUtf(value.selectedFormulaId(), 128);
                buf.writeUtf(value.activeResearchId(), 128);
                buf.writeVarInt(value.remainingTicks());
                buf.writeUtf(value.messageKey(), 256);
            }, buf -> {
                UUID sessionId = buf.readUUID();
                int count = buf.readVarInt();
                if (count < 0 || count > MAX_FORMULAS) throw new IllegalArgumentException("Invalid alchemy formula count");
                List<AlchemyFormulaSnapshot> formulas = new ArrayList<>(count);
                for (int i = 0; i < count; i++) formulas.add(AlchemyFormulaSnapshot.STREAM_CODEC.decode(buf));
                int runeCount = buf.readVarInt();
                if (runeCount < 0 || runeCount > 32) throw new IllegalArgumentException("Invalid alchemy rune count");
                List<AlchemyRuneSnapshot> runes = new ArrayList<>(runeCount);
                for (int i = 0; i < runeCount; i++) runes.add(AlchemyRuneSnapshot.STREAM_CODEC.decode(buf));
                return new OpenAlchemyPayload(sessionId, List.copyOf(formulas), List.copyOf(runes), buf.readUtf(128),
                        buf.readUtf(128), buf.readVarInt(), buf.readUtf(256));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
