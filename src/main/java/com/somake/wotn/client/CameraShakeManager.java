package com.somake.wotn.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.somake.wotn.network.CameraShakePayload;
import com.somake.wotn.entity.GroundWaveEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class CameraShakeManager {
    private static final List<ActiveShake> ACTIVE_SHAKES = new ArrayList<>();

    private CameraShakeManager() {
    }

    public static void add(CameraShakePayload payload) {
        if (!Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())
                || !Float.isFinite(payload.radius()) || !Float.isFinite(payload.magnitude())
                || payload.radius() <= 0.0F || payload.magnitude() <= 0.0F || payload.durationTicks() <= 0) {
            return;
        }

        ACTIVE_SHAKES.add(new ActiveShake(
                new Vec3(payload.x(), payload.y(), payload.z()),
                Mth.clamp(payload.radius(), 1.0F, 64.0F),
                Mth.clamp(payload.magnitude(), 0.01F, 1.0F),
                Mth.clamp(payload.durationTicks(), 1, 40)));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            ACTIVE_SHAKES.clear();
            return;
        }

        Iterator<ActiveShake> iterator = ACTIVE_SHAKES.iterator();
        while (iterator.hasNext()) {
            ActiveShake shake = iterator.next();
            shake.age++;

            if (shake.age >= shake.durationTicks) {
                iterator.remove();
            }
        }
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        Vec3 cameraPosition = minecraft.player.getEyePosition();
        float partialTick = (float) event.getPartialTick();
        float pitchOffset = 0.0F;
        float yawOffset = 0.0F;
        float rollOffset = 0.0F;

        for (ActiveShake shake : ACTIVE_SHAKES) {
            float distanceFactor = 1.0F - Mth.clamp((float) (cameraPosition.distanceTo(shake.origin) / shake.radius), 0.0F, 1.0F);
            distanceFactor *= distanceFactor;

            float life = (shake.age + partialTick) / shake.durationTicks;
            float timeFactor = 1.0F - Mth.clamp(life, 0.0F, 1.0F);
            timeFactor *= timeFactor;
            float amplitude = shake.magnitude * distanceFactor * timeFactor;
            float time = shake.age + partialTick;

            pitchOffset += Mth.sin(time * 2.75F + shake.phase) * amplitude * 7.0F;
            yawOffset += Mth.sin(time * 3.65F + shake.phase * 1.7F) * amplitude * 4.5F;
            rollOffset += Mth.sin(time * 3.1F + shake.phase * 2.3F) * amplitude * 3.0F;
        }

        for (GroundWaveEntity wave : minecraft.level.getEntitiesOfClass(GroundWaveEntity.class,
                minecraft.player.getBoundingBox().inflate(22.0D), entity -> !entity.isSubmerging())) {
            double distance = cameraPosition.distanceTo(wave.position());
            float radius = 12.0F + wave.getVisualIntensity() * 8.0F;
            if (distance >= radius) {
                continue;
            }
            float proximity = 1.0F - Mth.clamp((float) (distance / radius), 0.0F, 1.0F);
            proximity *= proximity;
            float amplitude = (0.008F + wave.getVisualIntensity() * 0.022F) * proximity;
            if (wave.isAnticipatingEmergence()) {
                amplitude *= 1.75F;
            }
            float time = wave.tickCount + partialTick;
            float phase = wave.getVariantSeed() * 0.017F;
            pitchOffset += Mth.sin(time * 1.9F + phase) * amplitude * 6.0F;
            yawOffset += Mth.sin(time * 1.45F + phase * 1.3F) * amplitude * 3.0F;
            rollOffset += Mth.sin(time * 1.65F + phase * 1.7F) * amplitude * 4.0F;
        }

        event.setPitch(event.getPitch() + Mth.clamp(pitchOffset, -8.0F, 8.0F));
        event.setYaw(event.getYaw() + Mth.clamp(yawOffset, -6.0F, 6.0F));
        event.setRoll(event.getRoll() + Mth.clamp(rollOffset, -4.0F, 4.0F));
    }

    private static final class ActiveShake {
        private final Vec3 origin;
        private final float radius;
        private final float magnitude;
        private final int durationTicks;
        private final float phase;
        private int age;

        private ActiveShake(Vec3 origin, float radius, float magnitude, int durationTicks) {
            this.origin = origin;
            this.radius = radius;
            this.magnitude = magnitude;
            this.durationTicks = durationTicks;
            this.phase = (float) ((origin.x * 0.73D + origin.y * 1.13D + origin.z * 1.91D) % (Math.PI * 2.0D));
        }
    }
}
