package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public record SelectLeviathanSkillPayload(UUID sessionId, int slot, int skillId) implements CustomPacketPayload {
    public static final Type<SelectLeviathanSkillPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "select_leviathan_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectLeviathanSkillPayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> {
                buf.writeUUID(value.sessionId());
                buf.writeVarInt(value.slot());
                buf.writeVarInt(value.skillId());
            }, buf -> new SelectLeviathanSkillPayload(buf.readUUID(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
