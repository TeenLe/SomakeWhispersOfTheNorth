package com.somake.wotn.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.somake.wotn.skill.LeviathanAxeSkill;
import com.somake.wotn.skill.LeviathanImbueSkill;
import com.somake.wotn.skill.LeviathanIceSpikesSkill;
import com.somake.wotn.registry.ModDataComponents;
import com.somake.wotn.registry.ModItems;
import com.somake.wotn.skilltree.LeviathanSkillTree;
import com.somake.wotn.dialogue.DialogueManager;
import com.somake.wotn.skilltree.ForgeSessionManager;
import com.somake.wotn.alchemy.AlchemyManager;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToClient(CameraShakePayload.TYPE, CameraShakePayload.STREAM_CODEC);
        registrar.playToClient(LeviathanAxeCooldownPayload.TYPE, LeviathanAxeCooldownPayload.STREAM_CODEC);
        registrar.playToClient(LeviathanImbueStatePayload.TYPE, LeviathanImbueStatePayload.STREAM_CODEC);
        registrar.playToClient(LeviathanIceSpikesCooldownPayload.TYPE, LeviathanIceSpikesCooldownPayload.STREAM_CODEC);
        registrar.playToClient(IceSpikesSlamPayload.TYPE, IceSpikesSlamPayload.STREAM_CODEC);
        registrar.playToClient(OpenLeviathanSkillsPayload.TYPE, OpenLeviathanSkillsPayload.STREAM_CODEC);
        registrar.playToClient(UpdateForgeSessionPayload.TYPE, UpdateForgeSessionPayload.STREAM_CODEC);
        registrar.playToClient(ShowDialoguePayload.TYPE, ShowDialoguePayload.STREAM_CODEC);
        registrar.playToClient(CloseDialoguePayload.TYPE, CloseDialoguePayload.STREAM_CODEC);
        registrar.playToClient(OpenAlchemyPayload.TYPE, OpenAlchemyPayload.STREAM_CODEC);
        registrar.playToClient(UpdateAlchemyPayload.TYPE, UpdateAlchemyPayload.STREAM_CODEC);
        registrar.playToServer(ActivateLeviathanSlotPayload.TYPE, ActivateLeviathanSlotPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (payload.slot() != 1 && payload.slot() != 2) return;
                    var player = (net.minecraft.server.level.ServerPlayer) context.player();
                    var axe = player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get())
                            ? player.getMainHandItem() : player.getOffhandItem();
                    if (!axe.is(ModItems.LEVIATHAN_AXE.get())) return;
                    int skillId = payload.slot() == 1
                            ? axe.getOrDefault(ModDataComponents.LEVIATHAN_PRIMARY_SKILL.get(), 3)
                            : axe.getOrDefault(ModDataComponents.LEVIATHAN_SECONDARY_SKILL.get(), 0);
                    switch (skillId) {
                        case 1 -> LeviathanImbueSkill.activate(player);
                        case 2 -> LeviathanIceSpikesSkill.activate(player);
                        case 3 -> LeviathanAxeSkill.activate(player);
                        default -> { }
                    }
                });
        registrar.playToServer(SelectLeviathanSkillPayload.TYPE, SelectLeviathanSkillPayload.STREAM_CODEC,
                (payload, context) -> {
                    ForgeSessionManager.INSTANCE.equip((net.minecraft.server.level.ServerPlayer) context.player(),
                            payload.sessionId(), payload.slot(), payload.skillId());
                });
        registrar.playToServer(UnlockSkillNodePayload.TYPE, UnlockSkillNodePayload.STREAM_CODEC,
                (payload, context) -> {
                    ForgeSessionManager.INSTANCE.unlock((net.minecraft.server.level.ServerPlayer) context.player(),
                            payload.sessionId(), payload.nodeId());
                });
        registrar.playToServer(SelectForgeWeaponPayload.TYPE, SelectForgeWeaponPayload.STREAM_CODEC,
                (payload, context) -> ForgeSessionManager.INSTANCE.select(
                        (net.minecraft.server.level.ServerPlayer) context.player(),
                        payload.sessionId(), payload.weaponId()));
        registrar.playToServer(CloseForgeSessionPayload.TYPE, CloseForgeSessionPayload.STREAM_CODEC,
                (payload, context) -> ForgeSessionManager.INSTANCE.close(
                        (net.minecraft.server.level.ServerPlayer) context.player(), payload.sessionId()));
        registrar.playToServer(SelectDialogueResponsePayload.TYPE, SelectDialogueResponsePayload.STREAM_CODEC,
                (payload, context) -> DialogueManager.INSTANCE.select(
                        (net.minecraft.server.level.ServerPlayer) context.player(),
                        payload.sessionId(), payload.responseId()));
        registrar.playToServer(CloseDialogueRequestPayload.TYPE, CloseDialogueRequestPayload.STREAM_CODEC,
                (payload, context) -> DialogueManager.INSTANCE.close(
                        (net.minecraft.server.level.ServerPlayer) context.player(), payload.sessionId()));
        registrar.playToServer(SelectAlchemyFormulaPayload.TYPE, SelectAlchemyFormulaPayload.STREAM_CODEC,
                (payload, context) -> AlchemyManager.INSTANCE.select(
                        (net.minecraft.server.level.ServerPlayer) context.player(),
                        payload.sessionId(), payload.formulaId()));
        registrar.playToServer(AlchemyActionPayload.TYPE, AlchemyActionPayload.STREAM_CODEC,
                (payload, context) -> AlchemyManager.INSTANCE.act(
                        (net.minecraft.server.level.ServerPlayer) context.player(),
                        payload.sessionId(), payload.formulaId(), payload.itemId(), payload.action()));
        registrar.playToServer(CloseAlchemySessionPayload.TYPE, CloseAlchemySessionPayload.STREAM_CODEC,
                (payload, context) -> AlchemyManager.INSTANCE.close(
                        (net.minecraft.server.level.ServerPlayer) context.player(), payload.sessionId()));
    }
}
