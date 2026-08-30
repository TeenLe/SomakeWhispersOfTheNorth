package com.somake.wotn.client.renderer;

import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.somake.wotn.block.FenrirSealBlock;
import com.somake.wotn.block.entity.FenrirSealBlockEntity;
import com.somake.wotn.client.renderer.state.GleipnirRopeRenderState;
import com.somake.wotn.entity.FenrirEntity;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class GleipnirRopeRenderer
        implements BlockEntityRenderer<FenrirSealBlockEntity, GleipnirRopeRenderState> {
    private static final int SEGMENTS = 24;
    private static final int FULL_BRIGHT = 15728880;
    private static final float STRAND_WIDTH = 0.022F;
    private static final double STRAND_SPACING = 0.075D;

    public GleipnirRopeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public GleipnirRopeRenderState createRenderState() {
        return new GleipnirRopeRenderState();
    }

    @Override
    public void extractRenderState(FenrirSealBlockEntity seal, GleipnirRopeRenderState state,
            float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(seal, state, partialTicks, cameraPosition, breakProgress);
        state.visible = false;
        if (seal.isCompleted() || !(seal.getLevel() instanceof ClientLevel level)) return;

        FenrirEntity fenrir = findFenrir(level, seal.getFenrirUuid(), seal.getBlockPos());
        if (fenrir == null || !fenrir.isEncounterLocked()) return;

        Direction facing = seal.getBlockState().getValue(FenrirSealBlock.FACING);
        Vec3 blockOrigin = Vec3.atLowerCornerOf(seal.getBlockPos());
        Vec3 direction = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vec3 worldStart = seal.getBlockPos().getCenter().add(direction.scale(0.58D));
        Vec3 worldEnd = fenrir.getPosition(partialTicks).add(0.0D, 1.25D, 0.0D);

        state.visible = true;
        state.waking = fenrir.isWaking();
        state.wakeProgress = Mth.clamp((fenrir.getWakeTicks() + partialTicks) / FenrirEntity.WAKE_DURATION,
                0.0F, 1.0F);
        state.animationTime = level.getGameTime() + partialTicks;
        state.start = worldStart.subtract(blockOrigin);
        state.end = worldEnd.subtract(blockOrigin);
    }

    @Override
    public void submit(GleipnirRopeRenderState state, PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.visible) return;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.leash(),
                (pose, buffer) -> renderRope(state, pose, buffer));
    }

    private static void renderRope(GleipnirRopeRenderState state, PoseStack.Pose pose, VertexConsumer buffer) {
        Vec3 delta = state.end.subtract(state.start);
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        Vec3 side = horizontal.lengthSqr() < 1.0E-6D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();
        Vec3 verticalSide = delta.normalize().cross(side).normalize();
        float pulse = 0.82F + 0.18F * Mth.sin(state.animationTime * 0.18F);
        float vibration = state.waking ? 0.018F + state.wakeProgress * 0.07F : 0.008F;
        float breakProgress = state.waking
                ? Mth.clamp((state.wakeProgress - 0.75F) / 0.25F, 0.0F, 1.0F)
                : 0.0F;

        for (int strand = -1; strand <= 1; strand++) {
            double strandOffset = strand * STRAND_SPACING;
            float phase = strand * 2.1F;
            for (int pass = 0; pass < 2; pass++) {
                if (pass == 0) {
                    for (int step = 0; step <= SEGMENTS; step++) {
                        addVertexPair(state, pose, buffer, side, verticalSide, strandOffset,
                                phase, vibration, breakProgress, pulse, step, false);
                    }
                } else {
                    for (int step = SEGMENTS; step >= 0; step--) {
                        addVertexPair(state, pose, buffer, side, verticalSide, strandOffset,
                                phase, vibration, breakProgress, pulse, step, true);
                    }
                }
            }
        }
    }

    private static void addVertexPair(GleipnirRopeRenderState state, PoseStack.Pose pose,
            VertexConsumer buffer, Vec3 side, Vec3 verticalSide, double strandOffset,
            float phase, float vibration, float breakProgress, float pulse, int step, boolean reverse) {
        float progress = step / (float) SEGMENTS;
        float gapHalfWidth = 0.02F + breakProgress * 0.24F;
        if (breakProgress > 0.0F && Math.abs(progress - 0.5F) < gapHalfWidth) return;

        Vec3 point = curvedPoint(state, side, verticalSide, strandOffset, phase, vibration, progress);
        Vec3 ribbon = side.scale(STRAND_WIDTH);
        float alternating = step % 2 == (reverse ? 1 : 0) ? 0.74F : 1.0F;
        int red = Mth.clamp((int) (238.0F * pulse * alternating), 0, 255);
        int green = Mth.clamp((int) (190.0F * pulse * alternating), 0, 255);
        int blue = Mth.clamp((int) (68.0F * pulse * alternating), 0, 255);

        vertex(buffer, pose.pose(), point.subtract(ribbon), red, green, blue);
        vertex(buffer, pose.pose(), point.add(ribbon), red, green, blue);
    }

    private static Vec3 curvedPoint(GleipnirRopeRenderState state, Vec3 side, Vec3 verticalSide,
            double strandOffset, float phase, float vibration, float progress) {
        Vec3 point = state.start.lerp(state.end, progress);
        double sag = -0.48D * 4.0D * progress * (1.0D - progress);
        double waveEnvelope = Math.sin(Math.PI * progress);
        double wave = Math.sin(progress * Math.PI * 8.0D + state.animationTime * 0.32D + phase)
                * vibration * waveEnvelope;
        return point.add(side.scale(strandOffset + wave))
                .add(verticalSide.scale(Math.cos(progress * Math.PI * 6.0D + phase) * vibration * 0.5D * waveEnvelope))
                .add(0.0D, sag, 0.0D);
    }

    private static void vertex(VertexConsumer buffer, Matrix4fc pose, Vec3 point,
            int red, int green, int blue) {
        buffer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, 255)
                .setLight(FULL_BRIGHT);
    }

    private static @Nullable FenrirEntity findFenrir(ClientLevel level, @Nullable UUID uuid, BlockPos sealPos) {
        FenrirEntity nearest = null;
        double nearestDistance = 96.0D * 96.0D;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof FenrirEntity fenrir)) continue;
            if (uuid != null && uuid.equals(fenrir.getUUID())) return fenrir;
            double distance = fenrir.distanceToSqr(sealPos.getCenter());
            if (distance < nearestDistance) {
                nearest = fenrir;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(FenrirSealBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(96.0D);
    }
}
