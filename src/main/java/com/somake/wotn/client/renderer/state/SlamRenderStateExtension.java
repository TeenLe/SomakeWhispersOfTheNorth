package com.somake.wotn.client.renderer.state;

import net.minecraft.world.entity.HumanoidArm;

public interface SlamRenderStateExtension {
    void wotn$setSlam(boolean active, float age, HumanoidArm arm);
    boolean wotn$isSlamActive();
    float wotn$getSlamAge();
    HumanoidArm wotn$getSlamArm();
}
