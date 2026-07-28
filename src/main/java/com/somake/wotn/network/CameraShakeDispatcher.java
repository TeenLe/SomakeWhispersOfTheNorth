package com.somake.wotn.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CameraShakeDispatcher {
    private CameraShakeDispatcher() {
    }

    public static void shake(ServerLevel level, Vec3 origin, float radius, float magnitude, int durationTicks) {
        CameraShakePayload payload = new CameraShakePayload(
                origin.x, origin.y, origin.z, radius, magnitude, durationTicks);
        PacketDistributor.sendToPlayersNear(
                level, null, origin.x, origin.y, origin.z, radius, payload);
    }
}
