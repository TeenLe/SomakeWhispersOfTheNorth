package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectForgeWeaponPayload(UUID sessionId, UUID weaponId) implements CustomPacketPayload {
    public static final Type<SelectForgeWeaponPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "select_forge_weapon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectForgeWeaponPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUUID(value.sessionId()); buf.writeUUID(value.weaponId()); },
            buf -> new SelectForgeWeaponPayload(buf.readUUID(), buf.readUUID()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
