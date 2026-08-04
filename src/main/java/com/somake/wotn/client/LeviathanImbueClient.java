package com.somake.wotn.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.network.ActivateLeviathanImbuePayload;
import com.somake.wotn.network.LeviathanImbueStatePayload;
import com.somake.wotn.registry.ModItems;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class LeviathanImbueClient {
    private static final int ACTIVATION_ANIMATION_TICKS = 28;
    private static final Identifier HUD_LAYER = Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "leviathan_imbue_hud");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "textures/gui/ice_skill.png");
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "skills"));
    private static final KeyMapping ACTIVATE = new KeyMapping("key.wotn.leviathan_imbue", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    private static int activeTicks;
    private static int cooldownTicks;
    private static int deniedPulseTicks;
    private static int readyPulseTicks;
    private static boolean wasCoolingDown;
    private static int activationAnimationTicks;
    private static int endingAnimationTicks;
    private static boolean wasActive;

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE);
    }

    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, LeviathanImbueClient::renderHud);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            activeTicks = cooldownTicks = deniedPulseTicks = readyPulseTicks = 0;
            activationAnimationTicks = endingAnimationTicks = 0;
            wasCoolingDown = false;
            wasActive = false;
            return;
        }
        while (ACTIVATE.consumeClick()) {
            if (minecraft.player != null && isHoldingAxe()) {
                if (cooldownTicks > 0) deniedPulseTicks = 14;
                ClientPacketDistributor.sendToServer(ActivateLeviathanImbuePayload.INSTANCE);
            }
        }
        if (!minecraft.isPaused()) {
            if (activeTicks > 0) activeTicks--;
            if (cooldownTicks > 0) cooldownTicks--;
        }
        boolean active = activeTicks > 0;
        if (wasActive && !active) {
            endingAnimationTicks = 8;
            playEndingSound(minecraft);
        }
        wasActive = active;
        boolean cooling = cooldownTicks > 0;
        if (wasCoolingDown && !cooling) readyPulseTicks = 14;
        wasCoolingDown = cooling;
        if (deniedPulseTicks > 0) deniedPulseTicks--;
        if (readyPulseTicks > 0) readyPulseTicks--;

        if (activationAnimationTicks > 0) {
            spawnActivationVortex(minecraft, activationAnimationTicks);
            activationAnimationTicks--;
        }
        if (endingAnimationTicks > 0) {
            spawnEndingBurst(minecraft, endingAnimationTicks);
            endingAnimationTicks--;
        }

        if (activeTicks > 0 && activationAnimationTicks <= 0 && minecraft.player != null
                && minecraft.player.tickCount % 2 == 0) {
            var player = minecraft.player;
            double side = player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get()) ? -0.42D : 0.42D;
            double yaw = player.getYRot() * Mth.DEG_TO_RAD;
            double x = player.getX() + Math.cos(yaw) * side;
            double z = player.getZ() + Math.sin(yaw) * side;
            player.level().addParticle(ParticleTypes.SNOWFLAKE, x, player.getEyeY() - 0.55D, z, 0, 0.015D, 0);
            if (player.tickCount % 4 == 0) {
                player.level().addParticle(new DustColorTransitionOptions(0x35D8FF, 0xF2FFFF, 0.65F), x, player.getEyeY() - 0.55D, z, 0, 0, 0);
            }
        }
    }

    public static void applyState(LeviathanImbueStatePayload payload) {
        boolean started = payload.activeTicks() > 0 && activeTicks <= 0 && !payload.denied();
        activeTicks = Math.max(0, payload.activeTicks());
        cooldownTicks = Math.max(0, payload.cooldownTicks());
        deniedPulseTicks = payload.denied() ? 14 : 0;
        wasCoolingDown = cooldownTicks > 0;
        wasActive = activeTicks > 0;
        if (started) {
            activationAnimationTicks = ACTIVATION_ANIMATION_TICKS;
        }
    }

    private static void spawnActivationVortex(Minecraft minecraft, int ticksLeft) {
        if (minecraft.player == null) return;
        var player = minecraft.player;
        Vec3 target = getAxeHandPosition(player);
        float progress = 1.0F - ticksLeft / (float) ACTIVATION_ANIMATION_TICKS;
        float easedProgress = progress * progress * (3.0F - 2.0F * progress);
        double radius = Mth.lerp(easedProgress, 1.85D, 0.12D);
        double turns = 2.35D;
        for (int i = 0; i < 7; i++) {
            double angle = easedProgress * Mth.TWO_PI * turns + i * Mth.TWO_PI / 7.0D;
            double height = (i - 3) * 0.13D + Math.sin(angle * 1.15D) * (0.2D * (1.0D - easedProgress));
            Vec3 spawn = target.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            Vec3 inward = target.subtract(spawn).normalize();
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle));
            Vec3 velocity = inward.scale(0.025D + easedProgress * 0.055D)
                    .add(tangent.scale(0.045D * (1.0D - easedProgress)));
            player.level().addParticle(ParticleTypes.SNOWFLAKE, spawn.x, spawn.y, spawn.z,
                    velocity.x, velocity.y, velocity.z);
            if ((i & 1) == 0 && ticksLeft % 2 == 0) {
                player.level().addParticle(new DustColorTransitionOptions(0x28CFFF, 0xF4FFFF, 0.7F),
                        spawn.x, spawn.y, spawn.z, velocity.x, velocity.y, velocity.z);
            }
        }
        if (ticksLeft <= 5) {
            var random = player.getRandom();
            for (int i = 0; i < 3; i++) {
                player.level().addParticle(new DustColorTransitionOptions(0x6DEBFF, 0xFFFFFF, 0.85F),
                        target.x + random.nextGaussian() * 0.08D,
                        target.y + random.nextGaussian() * 0.08D,
                        target.z + random.nextGaussian() * 0.08D,
                        0.0D, 0.01D, 0.0D);
            }
        }
    }

    private static void spawnEndingBurst(Minecraft minecraft, int ticksLeft) {
        if (minecraft.player == null || ticksLeft % 2 == 0) return;
        var player = minecraft.player;
        Vec3 origin = getAxeHandPosition(player);
        var random = player.getRandom();
        BlockParticleOption ice = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());
        for (int i = 0; i < 4; i++) {
            Vec3 velocity = new Vec3(random.nextGaussian() * 0.09D,
                    0.04D + random.nextDouble() * 0.12D,
                    random.nextGaussian() * 0.09D);
            player.level().addParticle(i < 2 ? ice : ParticleTypes.SNOWFLAKE,
                    origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static Vec3 getAxeHandPosition(net.minecraft.client.player.LocalPlayer player) {
        boolean mainHand = player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get());
        double side = mainHand ? -0.42D : 0.42D;
        double yaw = player.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(player.getX() + Math.cos(yaw) * side,
                player.getEyeY() - 0.55D,
                player.getZ() + Math.sin(yaw) * side);
    }

    private static void playEndingSound(Minecraft minecraft) {
        if (minecraft.player != null) {
            minecraft.player.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_CLUSTER_BREAK, 0.28F, 1.45F);
        }
    }

    private static boolean isHoldingAxe() {
        var player = Minecraft.getInstance().player;
        return player != null && (player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get()) || player.getOffhandItem().is(ModItems.LEVIATHAN_AXE.get()));
    }

    private static void renderHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || (!isHoldingAxe() && activeTicks <= 0 && cooldownTicks <= 0)) return;
        int size = 20;
        int x = graphics.guiWidth() / 2 + 130;
        int y = graphics.guiHeight() - 24;
        float denied = deniedPulseTicks > 0 ? 0.5F + 0.5F * Mth.sin((14 - deniedPulseTicks) * 0.58F) : 0;
        float ready = readyPulseTicks > 0 ? 0.5F + 0.5F * Mth.sin((14 - readyPulseTicks) * 0.45F) : 0;
        int border = deniedPulseTicks > 0 ? 0xFFFF4949 : activeTicks > 0 ? 0xFFFFFFFF : readyPulseTicks > 0 ? 0xFFBFFBFF : cooldownTicks > 0 ? 0xFF426B78 : 0xFF79E9FF;
        graphics.fill(x - 3, y - 3, x + size + 3, y + size + 3, 0xB0101D24);
        graphics.outline(x - 2, y - 2, size + 4, size + 4, border);
        if (denied > 0) graphics.fill(x - 4, y - 4, x + size + 4, y + size + 4, ((int)(denied * 105) << 24) | 0xFF3030);
        else if (ready > 0) graphics.fill(x - 4, y - 4, x + size + 4, y + size + 4, ((int)(ready * 80) << 24) | 0x8DEBFF);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, x, y, 0, 0, size, size, 16, 16, 16, 16);
        int timer = activeTicks > 0 ? activeTicks : cooldownTicks;
        int total = activeTicks > 0 ? 160 : 400;
        float fraction = Mth.clamp((timer - deltaTracker.getGameTimeDeltaPartialTick(true)) / total, 0, 1);
        if (timer > 0) {
            int top = y + Mth.floor(size * (1 - fraction));
            graphics.fill(x, top, x + size, y + size, activeTicks > 0 ? 0x5029C9EE : 0xA0183440);
            Component seconds = Component.literal(Integer.toString(Mth.ceil(timer / 20.0F)));
            graphics.text(minecraft.font, seconds, x + size / 2 - minecraft.font.width(seconds) / 2, y + size / 2 - 4, 0xFFFFFFFF, true);
        }
        Component key = ACTIVATE.getTranslatedKeyMessage();
        graphics.text(minecraft.font, key, x + size - minecraft.font.width(key), y - 10, 0xFFBFEFFF, true);
    }

    private LeviathanImbueClient() {
    }
}
