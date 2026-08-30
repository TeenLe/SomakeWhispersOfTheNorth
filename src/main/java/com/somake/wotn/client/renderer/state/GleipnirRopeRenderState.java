package com.somake.wotn.client.renderer.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.Vec3;

public final class GleipnirRopeRenderState extends BlockEntityRenderState {
    public boolean visible;
    public boolean waking;
    public float wakeProgress;
    public float animationTime;
    public Vec3 start = Vec3.ZERO;
    public Vec3 end = Vec3.ZERO;
}
