package com.somake.wotn.network;

import com.somake.wotn.WhispersOfTheNorth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShowDialoguePayload(UUID sessionId, String speaker, String role, String text,
        List<Response> responses) implements CustomPacketPayload {
    private static final int MAX_RESPONSES = 16;
    public static final Type<ShowDialoguePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "show_dialogue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShowDialoguePayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUUID(value.sessionId());
                buf.writeUtf(value.speaker(), 256);
                buf.writeUtf(value.role(), 256);
                buf.writeUtf(value.text(), 8192);
                int count = Math.min(value.responses().size(), MAX_RESPONSES);
                buf.writeVarInt(count);
                for (int i = 0; i < count; i++) {
                    Response response = value.responses().get(i);
                    buf.writeUtf(response.id(), 128);
                    buf.writeUtf(response.text(), 1024);
                    buf.writeBoolean(response.available());
                    buf.writeUtf(response.requirement(), 256);
                }
            }, buf -> {
                UUID sessionId = buf.readUUID();
                String speaker = buf.readUtf(256);
                String role = buf.readUtf(256);
                String text = buf.readUtf(8192);
                int count = buf.readVarInt();
                if (count < 0 || count > MAX_RESPONSES) {
                    throw new IllegalArgumentException("Invalid dialogue response count: " + count);
                }
                List<Response> responses = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    responses.add(new Response(buf.readUtf(128), buf.readUtf(1024),
                            buf.readBoolean(), buf.readUtf(256)));
                }
                return new ShowDialoguePayload(sessionId, speaker, role, text, List.copyOf(responses));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Response(String id, String text, boolean available, String requirement) {
    }
}
