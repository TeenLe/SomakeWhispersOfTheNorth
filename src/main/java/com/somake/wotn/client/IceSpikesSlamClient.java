package com.somake.wotn.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.somake.wotn.client.animation.IceSpikesSlamPose;
import com.somake.wotn.client.renderer.state.SlamRenderStateExtension;
import com.somake.wotn.network.IceSpikesSlamPayload;
import com.somake.wotn.registry.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

public final class IceSpikesSlamClient {
    private static final Map<Integer, SlamAnimation> ACTIVE = new HashMap<>();

    public static void start(IceSpikesSlamPayload payload) {
        ACTIVE.put(payload.casterId(), new SlamAnimation(payload.startGameTime(),
                payload.rightArm() ? HumanoidArm.RIGHT : HumanoidArm.LEFT));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE.clear();
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        Iterator<SlamAnimation> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            if (gameTime - iterator.next().startGameTime >= IceSpikesSlamPose.DURATION_TICKS) iterator.remove();
        }
    }

    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!(event.getRenderState() instanceof SlamRenderStateExtension extension)) return;
        extension.wotn$setSlam(false, 0.0F, HumanoidArm.RIGHT);
        if (!(event.getRenderState() instanceof AvatarRenderState avatarState)) return;
        SlamAnimation animation = ACTIVE.get(avatarState.id);
        if (animation == null) return;
        float age = getAge(animation, event.getPartialTick());
        if (age >= 0.0F && age < IceSpikesSlamPose.DURATION_TICKS) {
            extension.wotn$setSlam(true, age, animation.arm);
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !event.getItemStack().is(ModItems.LEVIATHAN_AXE.get())) return;
        SlamAnimation animation = ACTIVE.get(minecraft.player.getId());
        if (animation == null) return;
        InteractionHand expectedHand = animation.arm == minecraft.player.getMainArm()
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (event.getHand() != expectedHand) return;
        float age = getAge(animation, event.getPartialTick());
        if (age >= 0.0F && age < IceSpikesSlamPose.DURATION_TICKS) {
            IceSpikesSlamPose.applyFirstPerson(event.getPoseStack(), age, animation.arm == HumanoidArm.RIGHT);
        }
    }

    private static float getAge(SlamAnimation animation, float partialTick) {
        var level = Minecraft.getInstance().level;
        return level == null ? IceSpikesSlamPose.DURATION_TICKS
                : level.getGameTime() - animation.startGameTime + partialTick;
    }

    private record SlamAnimation(long startGameTime, HumanoidArm arm) {}
    private IceSpikesSlamClient() {}
}
