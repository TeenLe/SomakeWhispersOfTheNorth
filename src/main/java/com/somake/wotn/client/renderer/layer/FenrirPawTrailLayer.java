package com.somake.wotn.client.renderer.layer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.entity.FenrirEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class FenrirPawTrailLayer extends GeoRenderLayer {
    private static final String BASE_BONE = "PawTrailBaseRight";
    private static final String TIP_BONE = "PawTrailTipRight";
    private static final float EMISSION_START_TICK = 4.5F;
    private static final float EMISSION_END_TICK = 13.5F;
    private static final float TRAIL_LIFETIME_TICKS = 6.0F;
    private static final int MAX_SAMPLES = 28;
    private static final double MAX_SAMPLE_SPACING = 0.16D;
    private static final Identifier TRAIL_TEXTURE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "textures/entity/fenrir_paw_trail.png");
    private static final RenderType TRAIL_RENDER_TYPE = RenderTypes.entityTranslucentEmissive(
            TRAIL_TEXTURE, false);
    private static final DataTicket<TrailFrame> TRAIL_FRAME = DataTickets.create(
            "wotn_fenrir_paw_trail_frame", TrailFrame.class);

    private final Map<UUID, TrailHistory> histories = new HashMap<>();
    private ClientLevel activeLevel;
    private long lastCleanupTick = Long.MIN_VALUE;

    public FenrirPawTrailLayer(GeoRenderer renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(GeoAnimatable animatable, Object relatedObject, GeoRenderState state,
            float partialTick) {
        if (!(animatable instanceof FenrirEntity fenrir)
                || !(state instanceof LivingEntityRenderState livingState)) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (this.activeLevel != level) {
            this.histories.clear();
            this.activeLevel = level;
            this.lastCleanupTick = Long.MIN_VALUE;
        }

        Vec3 renderOrigin = new Vec3(livingState.x, livingState.y, livingState.z);
        float attackTime = fenrir.getClawAttackTime(partialTick);
        boolean emitting = fenrir.isPerformingClawAttack()
                && attackTime >= EMISSION_START_TICK
                && attackTime <= EMISSION_END_TICK;
        float renderAge = livingState.ageInTicks;
        state.addGeckolibData(DataTickets.POSITION, renderOrigin);
        state.addGeckolibData(TRAIL_FRAME, new TrailFrame(
                fenrir.getUUID(), fenrir.getId(), emitting, renderAge, renderOrigin));

        long gameTime = level == null ? 0L : level.getGameTime();
        TrailHistory history = this.histories.computeIfAbsent(fenrir.getUUID(), ignored -> new TrailHistory());
        history.markSeen(fenrir.getId(), emitting, renderAge, gameTime);
        if (level != null && gameTime - this.lastCleanupTick >= 20L) {
            this.lastCleanupTick = gameTime;
            this.histories.entrySet().removeIf(entry -> gameTime - entry.getValue().lastSeenGameTick > 40L);
        }
    }

    @Override
    public void preRender(RenderPassInfo renderPass, SubmitNodeCollector collector) {
        TrailFrame frame = (TrailFrame) renderPass.getGeckolibData(TRAIL_FRAME);
        if (frame == null || !frame.emitting) return;
        TrailHistory history = this.histories.get(frame.entityUuid);
        if (history == null) return;

        PositionCapture capture = new PositionCapture(history, frame.renderAge);
        renderPass.addBonePositionListener(BASE_BONE,
                (worldPosition, modelPosition, localPosition) -> capture.acceptBase(worldPosition));
        renderPass.addBonePositionListener(TIP_BONE,
                (worldPosition, modelPosition, localPosition) -> capture.acceptTip(worldPosition));
    }

    @Override
    public void submitRenderTask(RenderPassInfo renderPass, SubmitNodeCollector collector) {
        TrailFrame frame = (TrailFrame) renderPass.getGeckolibData(TRAIL_FRAME);
        if (frame == null) return;
        TrailHistory history = this.histories.get(frame.entityUuid);
        if (history == null || !frame.emitting && !history.hasLiveSamples(frame.renderAge)) return;

        PoseStack trailPose = new PoseStack();
        trailPose.last().set(renderPass.getPreRenderMatrixPose());
        collector.order(1).submitCustomGeometry(trailPose, TRAIL_RENDER_TYPE,
                (pose, buffer) -> history.render(pose, buffer, frame.renderOrigin, frame.renderAge));
    }

    private record TrailFrame(UUID entityUuid, int entityId, boolean emitting, float renderAge,
            Vec3 renderOrigin) {
    }

    private record TrailSample(Vec3 base, Vec3 tip, float age) {
    }

    private static final class PositionCapture {
        private final TrailHistory history;
        private final float renderAge;
        private Vec3 base;
        private Vec3 tip;

        private PositionCapture(TrailHistory history, float renderAge) {
            this.history = history;
            this.renderAge = renderAge;
        }

        private void acceptBase(Vec3 position) {
            this.base = position;
            this.commitIfComplete();
        }

        private void acceptTip(Vec3 position) {
            this.tip = position;
            this.commitIfComplete();
        }

        private void commitIfComplete() {
            if (this.base == null || this.tip == null) return;
            this.history.add(this.base, this.tip, this.renderAge);
            this.base = null;
            this.tip = null;
        }
    }

    private static final class TrailHistory {
        private final ArrayDeque<TrailSample> samples = new ArrayDeque<>();
        private int entityId;
        private boolean wasEmitting;
        private float lastRenderAge = Float.NEGATIVE_INFINITY;
        private long lastSeenGameTick;

        private void markSeen(int currentEntityId, boolean emitting, float renderAge, long gameTick) {
            if (this.entityId != 0 && this.entityId != currentEntityId || renderAge < this.lastRenderAge - 0.5F) {
                this.samples.clear();
            }
            if (emitting && !this.wasEmitting) {
                this.samples.clear();
            }
            this.entityId = currentEntityId;
            this.wasEmitting = emitting;
            this.lastRenderAge = renderAge;
            this.lastSeenGameTick = gameTick;
            this.prune(renderAge);
        }

        private void add(Vec3 base, Vec3 tip, float age) {
            TrailSample previous = this.samples.peekLast();
            if (previous == null) {
                this.samples.addLast(new TrailSample(base, tip, age));
                return;
            }
            if (age + 0.001F < previous.age
                    || age - previous.age > 2.5F
                    || previous.base.distanceToSqr(base) > 25.0D
                    || previous.tip.distanceToSqr(tip) > 25.0D) {
                this.samples.clear();
                this.samples.addLast(new TrailSample(base, tip, age));
                return;
            }
            if (Math.abs(age - previous.age) < 1.0E-4F
                    && previous.base.distanceToSqr(base) < 1.0E-6D
                    && previous.tip.distanceToSqr(tip) < 1.0E-6D) {
                return;
            }

            double distance = Math.max(previous.base.distanceTo(base), previous.tip.distanceTo(tip));
            int inserts = Mth.clamp((int) Math.ceil(distance / MAX_SAMPLE_SPACING), 1, 4);
            for (int index = 1; index <= inserts; index++) {
                double progress = index / (double) inserts;
                this.samples.addLast(new TrailSample(
                        previous.base.lerp(base, progress),
                        previous.tip.lerp(tip, progress),
                        Mth.lerp((float) progress, previous.age, age)));
            }
            while (this.samples.size() > MAX_SAMPLES) this.samples.removeFirst();
            this.prune(age);
        }

        private boolean hasLiveSamples(float currentAge) {
            this.prune(currentAge);
            return this.samples.size() >= 2;
        }

        private void prune(float currentAge) {
            while (!this.samples.isEmpty()
                    && currentAge - this.samples.peekFirst().age > TRAIL_LIFETIME_TICKS) {
                this.samples.removeFirst();
            }
        }

        private void render(PoseStack.Pose pose, VertexConsumer buffer, Vec3 origin, float currentAge) {
            this.prune(currentAge);
            if (this.samples.size() < 2) return;
            List<TrailSample> visible = new ArrayList<>(this.samples);
            for (int index = 1; index < visible.size(); index++) {
                TrailSample previous = visible.get(index - 1);
                TrailSample current = visible.get(index);
                float previousAlpha = alphaFor(previous, currentAge);
                float currentAlpha = alphaFor(current, currentAge);
                if (previousAlpha <= 0.01F && currentAlpha <= 0.01F) continue;

                Vec3 previousBase = previous.base.subtract(origin);
                Vec3 previousTip = previous.tip.subtract(origin);
                Vec3 currentBase = current.base.subtract(origin);
                Vec3 currentTip = current.tip.subtract(origin);
                Vec3 previousMid = previous.base.add(previous.tip).scale(0.5D);
                Vec3 currentMid = current.base.add(current.tip).scale(0.5D);
                Vec3 edge = current.tip.subtract(current.base);
                Vec3 travel = currentMid.subtract(previousMid);
                Vec3 normal = edge.cross(travel);
                if (normal.lengthSqr() < 1.0E-6D) normal = new Vec3(0.0D, 1.0D, 0.0D);
                else normal = normal.normalize();

                float previousV = (index - 1) / (float) (visible.size() - 1);
                float currentV = index / (float) (visible.size() - 1);
                vertex(buffer, pose, previousBase, 0.0F, previousV, previousAlpha, normal);
                vertex(buffer, pose, previousTip, 1.0F, previousV, previousAlpha, normal);
                vertex(buffer, pose, currentTip, 1.0F, currentV, currentAlpha, normal);
                vertex(buffer, pose, currentBase, 0.0F, currentV, currentAlpha, normal);
            }
        }

        private static float alphaFor(TrailSample sample, float currentAge) {
            float remaining = 1.0F - Mth.clamp(
                    (currentAge - sample.age) / TRAIL_LIFETIME_TICKS, 0.0F, 1.0F);
            return 0.9F * remaining * remaining * (3.0F - 2.0F * remaining);
        }

        private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vec3 position,
                float u, float v, float alpha, Vec3 normal) {
            buffer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                    .setColor(150, 238, 255, Mth.clamp((int) (alpha * 255.0F), 0, 255))
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(15728880)
                    .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        }
    }
}
