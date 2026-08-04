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

public class IceShellModel extends Model<Unit> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "ice_shell"), "main");

    public IceShellModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition upperCrown = root.addOrReplaceChild("upper_crown", CubeListBuilder.create(), PartPose.offset(8.0F, -4.6F, 0.0F));
        upperCrown.addOrReplaceChild("upper_east", CubeListBuilder.create().texOffs(32, 48).addBox(0.1F, -6.0F, -8.0F, 0.0F, 7.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, -3.0F, 0.0F, 0.0F, 0.0F, 0.3054F));
        upperCrown.addOrReplaceChild("upper_west", CubeListBuilder.create().texOffs(32, 48).addBox(0.0F, -6.0F, -8.0F, 0.0F, 7.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.2F, -3.0F, 0.0F, -3.1416F, 0.0F, 2.8362F));
        upperCrown.addOrReplaceChild("upper_south", CubeListBuilder.create().texOffs(64, 11).addBox(-8.0F, -6.0F, 0.0F, 16.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, -3.0F, 8.2F, -0.3054F, 0.0F, 0.0F));
        upperCrown.addOrReplaceChild("upper_north", CubeListBuilder.create().texOffs(64, 11).addBox(-8.0F, -6.0F, 0.0F, 16.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, -3.0F, -8.2F, 2.8362F, 0.0F, -3.1416F));

        PartDefinition sideCrystals = root.addOrReplaceChild("side_crystals", CubeListBuilder.create(), PartPose.offset(0.0012F, -8.3768F, 0.0F));
        sideCrystals.addOrReplaceChild("crystal_ne", CubeListBuilder.create().texOffs(64, 11).addBox(8.0F, -8.5F, 11.0F, 16.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(64, 11).addBox(-8.0F, -8.5F, 11.0F, 16.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.2535F, 8.3768F, -13.3026F, 0.7854F, 0.0F, 1.5708F));
        sideCrystals.addOrReplaceChild("crystal_nw", CubeListBuilder.create().texOffs(64, 11).mirror().addBox(-8.0F, -8.5F, 11.0F, 16.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false).texOffs(64, 11).mirror().addBox(-24.0F, -8.5F, 11.0F, 16.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(2.2512F, 8.3768F, -13.3026F, 0.7854F, 0.0F, -1.5708F));
        sideCrystals.addOrReplaceChild("crystal_se", CubeListBuilder.create().texOffs(64, 11).addBox(-8.0F, -8.5F, -11.0F, 16.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(64, 11).addBox(8.0F, -8.5F, -11.0F, 16.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.2535F, 8.3768F, 13.3026F, -0.7854F, 0.0F, 1.5708F));
        sideCrystals.addOrReplaceChild("crystal_sw", CubeListBuilder.create().texOffs(64, 11).mirror().addBox(-24.0F, -8.5F, -11.0F, 16.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false).texOffs(64, 11).mirror().addBox(-8.0F, -8.5F, -11.0F, 16.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(2.2512F, 8.3768F, 13.3026F, -0.7854F, 0.0F, -1.5708F));

        PartDefinition lowerCrown = root.addOrReplaceChild("lower_crown", CubeListBuilder.create(), PartPose.offset(8.0F, 26.0F, 0.0F));
        lowerCrown.addOrReplaceChild("lower_east", CubeListBuilder.create().texOffs(32, 48).addBox(0.2F, -6.0F, -8.0F, 0.0F, 7.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, -3.0F, 0.0F, 0.0F, 0.0F, 0.3054F));
        lowerCrown.addOrReplaceChild("lower_west", CubeListBuilder.create().texOffs(32, 48).addBox(0.1F, -6.0F, -8.0F, 0.0F, 7.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.2F, -3.0F, 0.0F, -3.1416F, 0.0F, 2.8362F));
        lowerCrown.addOrReplaceChild("lower_south", CubeListBuilder.create().texOffs(64, 11).addBox(-8.0F, -6.0F, 0.1F, 16.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, -3.0F, 8.2F, -0.3054F, 0.0F, 0.0F));
        lowerCrown.addOrReplaceChild("lower_north", CubeListBuilder.create().texOffs(64, 11).addBox(-8.0F, -6.0F, 0.1F, 16.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, -3.0F, -8.2F, 2.8362F, 0.0F, -3.1416F));

        PartDefinition lowerCrystals = root.addOrReplaceChild("lower_crystals", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 8.0F));
        lowerCrystals.addOrReplaceChild("lower_side_e", CubeListBuilder.create().texOffs(0, 48).addBox(0.0F, -11.0F, -8.0F, 0.0F, 11.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, 0.0F, -8.0F, 3.1416F, 0.0F, -2.9671F));
        lowerCrystals.addOrReplaceChild("lower_side_w", CubeListBuilder.create().texOffs(0, 48).addBox(0.0F, -11.0F, -8.0F, 0.0F, 11.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, 0.0F, -8.0F, 0.0F, 0.0F, -0.1745F));
        lowerCrystals.addOrReplaceChild("lower_side_s", CubeListBuilder.create().texOffs(64, 0).addBox(-8.0F, -11.0F, 0.0F, 16.0F, 11.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1745F, 0.0F, 0.0F));
        lowerCrystals.addOrReplaceChild("lower_side_n", CubeListBuilder.create().texOffs(64, 0).addBox(-8.0F, -11.0F, 0.0F, 16.0F, 11.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -16.0F, 2.9671F, 0.0F, -3.1416F));

        root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -32.0F, -8.0F, 16.0F, 32.0F, 16.0F, new CubeDeformation(0.35F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
