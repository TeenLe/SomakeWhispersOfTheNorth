package com.somake.wotn.mixin.client;

import com.somake.wotn.client.renderer.state.SlamRenderStateExtension;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements SlamRenderStateExtension {
    @Unique private boolean wotn$slamActive;
    @Unique private float wotn$slamAge;
    @Unique private HumanoidArm wotn$slamArm = HumanoidArm.RIGHT;
    @Override public void wotn$setSlam(boolean active, float age, HumanoidArm arm) {
        this.wotn$slamActive = active;
        this.wotn$slamAge = age;
        this.wotn$slamArm = arm;
    }
    @Override public boolean wotn$isSlamActive() { return this.wotn$slamActive; }
    @Override public float wotn$getSlamAge() { return this.wotn$slamAge; }
    @Override public HumanoidArm wotn$getSlamArm() { return this.wotn$slamArm; }
}
