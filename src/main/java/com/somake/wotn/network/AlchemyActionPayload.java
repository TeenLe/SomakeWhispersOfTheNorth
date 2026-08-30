package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;

import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AlchemyActionPayload(UUID sessionId, String formulaId, String itemId,
        Action action) implements CustomPacketPayload {
    public static final Type<AlchemyActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "alchemy_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUUID(value.sessionId());
                buf.writeUtf(value.formulaId(), 128);
                buf.writeUtf(value.itemId(), 128);
                buf.writeVarInt(value.action().networkId());
            }, buf -> {
                UUID sessionId = buf.readUUID();
                String formulaId = buf.readUtf(128);
                String itemId = buf.readUtf(128);
                int action = buf.readVarInt();
                return new AlchemyActionPayload(sessionId, formulaId, itemId, Action.fromNetworkId(action));
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public enum Action {
        SUBMIT_INGREDIENT(0),
        EQUIP_RUNE(1),
        UNEQUIP_RUNE(2),
        BREW_ONE(3),
        BREW_THREE(4);

        private final int networkId;

        Action(int networkId) {
            this.networkId = networkId;
        }

        public int networkId() {
            return networkId;
        }

        public static Action fromNetworkId(int networkId) {
            for (Action action : values()) {
                if (action.networkId == networkId) return action;
            }
            throw new IllegalArgumentException("Invalid alchemy action");
        }
    }
}
