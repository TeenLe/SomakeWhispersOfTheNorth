package com.somake.wotn.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.somake.wotn.skill.LeviathanAxeSkill;
import com.somake.wotn.skill.LeviathanImbueSkill;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(CameraShakePayload.TYPE, CameraShakePayload.STREAM_CODEC);
        registrar.playToClient(LeviathanAxeCooldownPayload.TYPE, LeviathanAxeCooldownPayload.STREAM_CODEC);
        registrar.playToClient(LeviathanImbueStatePayload.TYPE, LeviathanImbueStatePayload.STREAM_CODEC);
        registrar.playToServer(ActivateLeviathanAxePayload.TYPE, ActivateLeviathanAxePayload.STREAM_CODEC,
                (payload, context) -> LeviathanAxeSkill.activate((net.minecraft.server.level.ServerPlayer) context.player()));
        registrar.playToServer(ActivateLeviathanImbuePayload.TYPE, ActivateLeviathanImbuePayload.STREAM_CODEC,
                (payload, context) -> LeviathanImbueSkill.activate((net.minecraft.server.level.ServerPlayer) context.player()));
    }
}
