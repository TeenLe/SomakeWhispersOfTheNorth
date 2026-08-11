package com.somake.wotn.client.model;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

public class IceSpikeModel extends Model<Unit> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "ice_spike"), "main");

    public IceSpikeModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.5F, -39.0F, -3.5F, 7.0F, 39.0F, 7.0F, new CubeDeformation(-0.15F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.08F, 0.0F, -0.07F));
        root.addOrReplaceChild("left_shard",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-2.5F, -24.0F, -2.5F, 5.0F, 24.0F, 5.0F, new CubeDeformation(-0.12F)),
                PartPose.offsetAndRotation(-5.0F, 24.0F, 1.0F, -0.12F, 0.28F, -0.32F));
        root.addOrReplaceChild("right_shard",
                CubeListBuilder.create().texOffs(52, 0)
                        .addBox(-2.0F, -19.0F, -2.0F, 4.0F, 19.0F, 4.0F, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(4.5F, 24.0F, -2.0F, 0.16F, -0.24F, 0.38F));
        root.addOrReplaceChild("front_shard",
                CubeListBuilder.create().texOffs(68, 0)
                        .addBox(-1.75F, -15.0F, -1.75F, 3.5F, 15.0F, 3.5F, new CubeDeformation(-0.08F)),
                PartPose.offsetAndRotation(1.0F, 24.0F, 4.5F, 0.42F, 0.1F, 0.08F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
