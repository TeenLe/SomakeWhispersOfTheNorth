package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;

import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectAlchemyFormulaPayload(UUID sessionId, String formulaId) implements CustomPacketPayload {
    public static final Type<SelectAlchemyFormulaPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "select_alchemy_formula"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectAlchemyFormulaPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUUID(value.sessionId());
                buf.writeUtf(value.formulaId(), 128);
            }, buf -> new SelectAlchemyFormulaPayload(buf.readUUID(), buf.readUtf(128)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
