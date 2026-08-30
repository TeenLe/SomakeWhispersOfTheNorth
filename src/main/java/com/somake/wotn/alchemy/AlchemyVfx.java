package com.somake.wotn.alchemy;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.particle.AlchemyMoteParticleData;
import com.somake.wotn.particle.ImpactRingParticleData.RingBehavior;
import com.somake.wotn.particle.ParticleHelper;
import com.somake.wotn.particle.RunicGlyphParticleData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class AlchemyVfx {
    public static final int FENRIR = 0;
    public static final int JORMUNGANDR = 1;
    public static final int IDUNN = 2;
    public static final int NIFLHEIM = 3;

    public static final Identifier GLYPH_FENRIR = glyphId("fenrir");
    public static final Identifier GLYPH_JORMUNGANDR = glyphId("jormungandr");
    public static final Identifier GLYPH_IDUNN = glyphId("idunn");
    public static final Identifier GLYPH_NIFLHEIM = glyphId("niflheim");
    public static final Identifier GLYPH_CHAINBREAKER = glyphId("chainbreaker");
    public static final Identifier GLYPH_BLOOM = glyphId("bloom");
    public static final Identifier GLYPH_RUPTURE = glyphId("rupture");

    private static final Identifier MOTE_FENRIR = moteId("fang");
    private static final Identifier MOTE_JORMUNGANDR = moteId("scale");
    private static final Identifier MOTE_IDUNN = moteId("leaf");
    private static final Identifier MOTE_NIFLHEIM = moteId("rime");

    private static final float[] FENRIR_CYAN = {0.08F, 0.72F, 0.91F};
    private static final float[] FENRIR_INK = {0.015F, 0.055F, 0.16F};
    private static final float[][] COLORS = {
            FENRIR_CYAN,
            {0.52F, 0.95F, 0.20F},
            {1.0F, 0.78F, 0.24F},
            {0.38F, 0.88F, 1.0F}
    };

    public static void activation(ServerLevel level, LivingEntity entity, int family, int tier) {
        float[] color = COLORS[family];
        float radius = 1.35F + tier * 0.2F;
        float[] shadow = family == FENRIR ? FENRIR_INK : color;
        ParticleHelper.spawnImpactRing(level, entity.getX(), entity.getY() + 0.05D, entity.getZ(),
                family == FENRIR ? shadow[0] : shadow[0] * 0.45F,
                family == FENRIR ? shadow[1] : shadow[1] * 0.45F,
                family == FENRIR ? shadow[2] : shadow[2] * 0.45F,
                0.9F, 1.0F, radius, 16, RingBehavior.SHRINK);
        ParticleHelper.spawnImpactRing(level, entity.getX(), entity.getY() + 0.07D, entity.getZ(),
                color[0], color[1], color[2], 0.92F, 1.0F, radius, 18, RingBehavior.GROW);
        glyph(level, entity, familyGlyph(family), 24, 1.15F + tier * 0.12F,
                color[0], color[1], color[2], 0.95F, 0.08F, 0.055F, true);

        int count = 10 + tier * 3;
        for (int index = 0; index < count; index++) {
            double angle = Mth.TWO_PI * index / count;
            double moteRadius = 0.65D + (index % 3) * 0.18D;
            double x = entity.getX() + Math.cos(angle) * moteRadius;
            double z = entity.getZ() + Math.sin(angle) * moteRadius;
            double y = entity.getY() + 0.2D + (index % 4) * entity.getBbHeight() * 0.18D;
            double speed = family == FENRIR ? 0.13D : 0.07D;
            spawnMote(level, entity, family, AlchemyMoteParticleData.FREE, 24 + tier * 2,
                    0.18F + tier * 0.018F, moteColor(family, index), 0.95F, 0.0F, 0.0F, 0.0F,
                    (float)angle, x, y, z,
                    Math.cos(angle) * speed, 0.045D + (index % 2) * 0.025D,
                    Math.sin(angle) * speed);
        }
        level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.55F, switch (family) {
                    case FENRIR -> 0.72F;
                    case JORMUNGANDR -> 0.88F;
                    case IDUNN -> 1.25F;
                    default -> 1.5F;
                });
    }

    public static void aura(ServerLevel level, LivingEntity entity, int family, int tier, int potency, int stacks,
            long expiresAt, int durationTicks) {
        long now = level.getServer().overworld().getGameTime();
        if (expiresAt <= now) return;
        int fullCount = Math.min(5, 2 + tier / 2 + Math.min(1, potency / 2) + Math.min(1, stacks / 3));
        int count = taperedAuraCount(fullCount, expiresAt - now, durationTicks);
        float direction = family == JORMUNGANDR || family == IDUNN ? 1.0F : -1.0F;
        for (int index = 0; index < count; index++) {
            float phase = (float)(Mth.TWO_PI * index / count + entity.tickCount * 0.13D);
            float vertical = (index - (count - 1) * 0.5F) * 0.18F;
            spawnMote(level, entity, family, AlchemyMoteParticleData.ORBIT, 34,
                    0.12F + tier * 0.012F + Math.min(0.035F, stacks * 0.006F), moteColor(family, index),
                    0.72F + Math.min(0.2F, stacks * 0.035F),
                    entity.getBbWidth() * (0.65F + index * 0.045F),
                    direction * (0.12F + index * 0.012F), vertical, phase,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    private static int taperedAuraCount(int fullCount, long remainingTicks, int durationTicks) {
        double remainingFraction = Mth.clamp(
                remainingTicks / (double)Math.max(1, durationTicks), 0.0D, 1.0D);
        double taper = Mth.clamp(remainingFraction / 0.5D, 0.0D, 1.0D);
        return Mth.clamp(1 + (int)Math.floor((fullCount - 1) * taper + 0.5D), 1, fullCount);
    }

    public static void mark(ServerLevel level, LivingEntity target, int family, Identifier glyph,
            int stacks, int threshold) {
        float[] color = COLORS[family];
        float intensity = Mth.clamp(stacks / (float)Math.max(1, threshold), 0.2F, 1.0F);
        glyph(level, target, glyph, 18, 0.7F + intensity * 0.42F,
                color[0], color[1], color[2], 0.76F + intensity * 0.2F,
                target.getBbHeight() + 0.35F, family == JORMUNGANDR ? -0.09F : 0.07F, false);
        int count = Math.min(6, 2 + stacks);
        for (int index = 0; index < count; index++) {
            double angle = Mth.TWO_PI * index / count;
            double radius = target.getBbWidth() * 0.7D + 0.2D;
            spawnMote(level, target, family, AlchemyMoteParticleData.CONVERGE, 18,
                    0.11F + intensity * 0.035F, moteColor(family, index), 0.85F,
                    (float)radius, 0.0F, 0.0F,
                    (float)angle,
                    target.getX() + Math.cos(angle) * radius,
                    target.getY() + target.getBbHeight() * (0.35D + index % 3 * 0.18D),
                    target.getZ() + Math.sin(angle) * radius,
                    0.0D, 0.0D, 0.0D);
        }
    }

    public static void majorProc(ServerLevel level, LivingEntity center, int family, Identifier glyph,
            float radius) {
        float[] color = COLORS[family];
        if (family == FENRIR) {
            ParticleHelper.spawnImpactRing(level, center.getX(), center.getY() + 0.06D, center.getZ(),
                    FENRIR_INK[0], FENRIR_INK[1], FENRIR_INK[2], 0.9F, 1.0F,
                    radius * 1.08F, 20, RingBehavior.GROW_THEN_SHRINK);
        }
        ParticleHelper.spawnImpactRing(level, center.getX(), center.getY() + 0.08D, center.getZ(),
                color[0], color[1], color[2], 0.95F, 1.0F, radius, 18, RingBehavior.GROW_THEN_SHRINK);
        glyph(level, center, glyph, 24, Math.max(1.0F, radius * 0.36F),
                color[0], color[1], color[2], 1.0F, 0.12F, 0.11F, true);
        int count = 14;
        for (int index = 0; index < count; index++) {
            double angle = Mth.TWO_PI * index / count;
            spawnMote(level, center, family, AlchemyMoteParticleData.FREE, 26, 0.18F,
                    moteColor(family, index), 0.95F, 0.0F, 0.0F, 0.0F, (float)angle,
                    center.getX(), center.getY() + center.getBbHeight() * 0.45D, center.getZ(),
                    Math.cos(angle) * 0.18D, 0.035D + (index % 3) * 0.03D,
                    Math.sin(angle) * 0.18D);
        }
        level.playSound(null, center.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.75F, 0.75F + family * 0.16F);
    }

    public static void link(ServerLevel level, LivingEntity source, LivingEntity target, int family, int count) {
        float[] color = COLORS[family];
        Vec3 from = source.position().add(0.0D, source.getBbHeight() * 0.55D, 0.0D);
        Vec3 toward = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D).subtract(from);
        for (int index = 0; index < count; index++) {
            float progress = (index + 1.0F) / (count + 1.0F);
            Vec3 point = from.add(toward.scale(progress)).add(0.0D,
                    Math.sin(progress * Math.PI) * 0.55D, 0.0D);
            spawnMote(level, target, family, AlchemyMoteParticleData.CONVERGE,
                    20 + index, 0.14F, moteColor(family, index), 0.9F, 0.0F, 0.0F, 0.0F,
                    index * 0.8F, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static float[] moteColor(int family, int index) {
        return family == FENRIR && index % 3 == 0 ? FENRIR_INK : COLORS[family];
    }

    private static void glyph(ServerLevel level, LivingEntity target, Identifier glyph, int lifetime, float scale,
            float red, float green, float blue, float alpha, float verticalOffset, float spin,
            boolean horizontal) {
        RunicGlyphParticleData data = new RunicGlyphParticleData(glyph, target.getId(), lifetime, scale,
                red, green, blue, alpha, verticalOffset, spin, horizontal);
        level.sendParticles(data, target.getX(), target.getY() + verticalOffset, target.getZ(),
                0, 0.0D, 0.0D, 0.0D, 1.0D);
    }

    private static void spawnMote(ServerLevel level, LivingEntity target, int family, int mode,
            int lifetime, float scale, float[] color, float alpha, float radius, float angularSpeed,
            float verticalOffset, float phase, double x, double y, double z,
            double motionX, double motionY, double motionZ) {
        AlchemyMoteParticleData data = new AlchemyMoteParticleData(
                familyMote(family), mode, target.getId(), lifetime, scale,
                color[0], color[1], color[2], alpha, radius, angularSpeed, verticalOffset, phase, true);
        level.sendParticles(data, x, y, z, 0, motionX, motionY, motionZ, 1.0D);
    }

    private static Identifier familyGlyph(int family) {
        return switch (family) {
            case FENRIR -> GLYPH_FENRIR;
            case JORMUNGANDR -> GLYPH_JORMUNGANDR;
            case IDUNN -> GLYPH_IDUNN;
            case NIFLHEIM -> GLYPH_NIFLHEIM;
            default -> throw new IllegalArgumentException("Unknown alchemy family: " + family);
        };
    }

    private static Identifier glyphId(String name) {
        return Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "runic_glyph/" + name);
    }

    private static Identifier familyMote(int family) {
        return switch (family) {
            case FENRIR -> MOTE_FENRIR;
            case JORMUNGANDR -> MOTE_JORMUNGANDR;
            case IDUNN -> MOTE_IDUNN;
            case NIFLHEIM -> MOTE_NIFLHEIM;
            default -> throw new IllegalArgumentException("Unknown alchemy family: " + family);
        };
    }

    private static Identifier moteId(String name) {
        return Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "alchemy_mote/" + name);
    }

    private AlchemyVfx() {
    }
}
