package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public record UnlockSkillNodePayload(UUID sessionId, int nodeId) implements CustomPacketPayload {
    public static final Type<UnlockSkillNodePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "unlock_skill_node"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockSkillNodePayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUUID(value.sessionId()); buf.writeVarInt(value.nodeId()); },
            buf -> new UnlockSkillNodePayload(buf.readUUID(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
