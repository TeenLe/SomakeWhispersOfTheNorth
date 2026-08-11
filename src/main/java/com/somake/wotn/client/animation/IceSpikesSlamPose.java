package com.somake.wotn.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.somake.wotn.client.animation.runtime.BedrockHumanoidAnimation;
import com.somake.wotn.client.animation.runtime.HumanoidAnimationResources;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class IceSpikesSlamPose {
    public static final int DURATION_TICKS = 16;
    public static final int IMPACT_TICK = 9;

    private static final float[] TIMES = {0.0F, 5.0F, 9.0F, 11.0F, 16.0F};
    private static final boolean[] CATMULL_SEGMENTS = {true, true, false, true};
    private static final float[] FP_X_ROT = {0.0F, 78.0F, -36.0F, -31.0F, 0.0F};
    private static final float[] FP_Y_ROT = {0.0F, 34.0F, -12.0F, -10.0F, 0.0F};
    private static final float[] FP_Z_ROT = {0.0F, -20.0F, 8.0F, 7.0F, 0.0F};
    private static final float[] FP_X = {0.0F, 0.24F, 0.08F, 0.07F, 0.0F};
    private static final float[] FP_Y = {0.0F, -0.52F, 0.38F, 0.34F, 0.0F};
    private static final float[] FP_Z = {0.0F, -0.32F, -0.18F, -0.2F, 0.0F};

    public static void applyThirdPerson(HumanoidModel<?> model, float age, HumanoidArm arm) {
        BedrockHumanoidAnimation animation = HumanoidAnimationResources.INSTANCE.slam();
        var rig = HumanoidAnimationResources.INSTANCE.slamRig();
        if (animation != null && rig != null) {
            animation.apply(model, age, arm, rig);
        }
    }

    public static void applyFirstPerson(PoseStack poseStack, float age, boolean rightArm) {
        float clampedAge = Mth.clamp(age, 0.0F, DURATION_TICKS);
        float side = rightArm ? 1.0F : -1.0F;
        poseStack.translate(
                sample(FP_X, clampedAge) * side,
                sample(FP_Y, clampedAge),
                sample(FP_Z, clampedAge));
        poseStack.mulPose(Axis.XP.rotationDegrees(sample(FP_X_ROT, clampedAge)));
        poseStack.mulPose(Axis.YP.rotationDegrees(sample(FP_Y_ROT, clampedAge) * side));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sample(FP_Z_ROT, clampedAge) * side));
        if (clampedAge >= IMPACT_TICK && clampedAge <= 11.0F) {
            poseStack.translate(0.0F, Mth.sin((clampedAge - IMPACT_TICK) * Mth.PI) * 0.035F, 0.0F);
        }
    }

    private static float sample(float[] values, float age) {
        if (age <= TIMES[0]) return values[0];
        int last = TIMES.length - 1;
        if (age >= TIMES[last]) return values[last];
        int segment = 0;
        while (segment + 1 < TIMES.length && age > TIMES[segment + 1]) segment++;
        float progress = (age - TIMES[segment]) / (TIMES[segment + 1] - TIMES[segment]);
        if (!CATMULL_SEGMENTS[segment]) return Mth.lerp(progress, values[segment], values[segment + 1]);
        float p0 = values[Math.max(0, segment - 1)];
        float p1 = values[segment];
        float p2 = values[segment + 1];
        float p3 = values[Math.min(last, segment + 2)];
        float t2 = progress * progress;
        float t3 = t2 * progress;
        return 0.5F * (2.0F * p1 + (-p0 + p2) * progress
                + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
                + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3);
    }

    private IceSpikesSlamPose() {}
}
