package com.somake.wotn.client.animation.runtime;

import java.io.Reader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

public final class HumanoidAnimationResources implements PreparableReloadListener {
    public static final HumanoidAnimationResources INSTANCE = new HumanoidAnimationResources();
    private static final Identifier SLAM_RESOURCE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "humanoid_animations/leviathan_ice_spikes_slam.animation.json");
    private static final Identifier SLAM_RIG_RESOURCE = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "humanoid_animations/leviathan_ice_spikes_slam.geo.json");
    private static final String SLAM_NAME = "animation.leviathan_axe.ice_spikes_slam";
    private volatile BedrockHumanoidAnimation slam;
    private volatile BedrockHumanoidRig slamRig;

    public BedrockHumanoidAnimation slam() {
        return this.slam;
    }

    public BedrockHumanoidRig slamRig() {
        return this.slamRig;
    }

    @Override
    public CompletableFuture<Void> reload(SharedState currentReload, Executor backgroundExecutor,
            PreparationBarrier barrier, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> load(currentReload.resourceManager()), backgroundExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(loaded -> {
                    if (loaded != null) {
                        this.slam = loaded.animation;
                        this.slamRig = loaded.rig;
                    }
                }, gameExecutor);
    }

    private static LoadedAnimation load(ResourceManager resourceManager) {
        try (Reader animationReader = resourceManager.openAsReader(SLAM_RESOURCE);
                Reader rigReader = resourceManager.openAsReader(SLAM_RIG_RESOURCE)) {
            BedrockHumanoidAnimation animation = BedrockHumanoidAnimation.parse(
                    JsonParser.parseReader(animationReader).getAsJsonObject(), SLAM_NAME);
            BedrockHumanoidRig rig = BedrockHumanoidRig.parse(
                    JsonParser.parseReader(rigReader).getAsJsonObject());
            WhispersOfTheNorth.LOGGER.info("Loaded humanoid animation {}", SLAM_RESOURCE);
            return new LoadedAnimation(animation, rig);
        } catch (Exception exception) {
            WhispersOfTheNorth.LOGGER.error("Failed to load humanoid animation {}", SLAM_RESOURCE, exception);
            return null;
        }
    }

    private record LoadedAnimation(BedrockHumanoidAnimation animation, BedrockHumanoidRig rig) {}

    private HumanoidAnimationResources() {}
}
