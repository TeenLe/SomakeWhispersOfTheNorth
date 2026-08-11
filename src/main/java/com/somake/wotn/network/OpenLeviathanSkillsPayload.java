package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record OpenLeviathanSkillsPayload(UUID sessionId, List<ForgeWeaponSnapshot> weapons,
        UUID selectedWeaponId) implements CustomPacketPayload {
    public static final Type<OpenLeviathanSkillsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "open_leviathan_skills"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLeviathanSkillsPayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> {
                buf.writeUUID(value.sessionId());
                buf.writeVarInt(value.weapons().size());
                for (ForgeWeaponSnapshot weapon : value.weapons()) {
                    ForgeWeaponSnapshot.STREAM_CODEC.encode(buf, weapon);
                }
                buf.writeBoolean(value.selectedWeaponId() != null);
                if (value.selectedWeaponId() != null) buf.writeUUID(value.selectedWeaponId());
            }, buf -> {
                UUID sessionId = buf.readUUID();
                int count = buf.readVarInt();
                if (count < 0 || count > 54) throw new IllegalArgumentException("Invalid forge weapon count: " + count);
                java.util.ArrayList<ForgeWeaponSnapshot> weapons = new java.util.ArrayList<>(count);
                for (int i = 0; i < count; i++) weapons.add(ForgeWeaponSnapshot.STREAM_CODEC.decode(buf));
                UUID selected = buf.readBoolean() ? buf.readUUID() : null;
                return new OpenLeviathanSkillsPayload(sessionId, List.copyOf(weapons), selected);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
