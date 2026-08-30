package com.somake.wotn.client.renderer;

import com.somake.wotn.entity.npc.DialogueNpcEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public final class DialogueNpcRenderer<T extends DialogueNpcEntity>
        extends HumanoidMobRenderer<T, AvatarRenderState, PlayerModel> {
    private final Identifier texture;
    private final PlayerSkin skin;

    public DialogueNpcRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.texture = texture;
        this.skin = PlayerSkin.insecure(
                new ClientAsset.ResourceTexture(texture, texture),
                null,
                null,
                PlayerModelType.WIDE);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(T entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = this.skin;
        state.id = entity.getId();
        state.isSpectator = false;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showCape = false;
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return this.texture;
    }
}
