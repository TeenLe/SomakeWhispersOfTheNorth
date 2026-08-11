package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpdateForgeSessionPayload(UUID sessionId, List<ForgeWeaponSnapshot> weapons,
        UUID selectedWeaponId) implements CustomPacketPayload {
    public static final Type<UpdateForgeSessionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "update_forge_session"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateForgeSessionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> OpenLeviathanSkillsPayload.STREAM_CODEC.encode(buf,
                    new OpenLeviathanSkillsPayload(value.sessionId(), value.weapons(), value.selectedWeaponId())),
            buf -> {
                OpenLeviathanSkillsPayload value = OpenLeviathanSkillsPayload.STREAM_CODEC.decode(buf);
                return new UpdateForgeSessionPayload(value.sessionId(), value.weapons(), value.selectedWeaponId());
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
