package com.somake.wotn.mixin.client;

import com.somake.wotn.client.animation.IceSpikesSlamPose;
import com.somake.wotn.client.renderer.state.SlamRenderStateExtension;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void wotn$applyIceSpikesSlam(HumanoidRenderState state, CallbackInfo callbackInfo) {
        if (state instanceof SlamRenderStateExtension extension && extension.wotn$isSlamActive()) {
            IceSpikesSlamPose.applyThirdPerson((HumanoidModel<?>) (Object) this,
                    extension.wotn$getSlamAge(), extension.wotn$getSlamArm());
        }
    }
}
