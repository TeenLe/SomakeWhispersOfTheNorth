package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.alchemy.AlchemyProgress;
import com.somake.wotn.alchemy.NiflheimEffectState;
import com.somake.wotn.alchemy.FenrirEffectState;
import com.somake.wotn.alchemy.IdunnEffectState;
import com.somake.wotn.alchemy.JormungandrEffectState;
import com.somake.wotn.effect.FenrirFrostState;

import java.util.function.Supplier;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES, WhispersOfTheNorth.MODID);

    public static final Supplier<AttachmentType<AlchemyProgress>> ALCHEMY_PROGRESS = ATTACHMENTS.register(
            "alchemy_progress", () -> AttachmentType.builder(() -> AlchemyProgress.DEFAULT)
                    .serialize(AlchemyProgress.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<NiflheimEffectState>> NIFLHEIM_EFFECT = ATTACHMENTS.register(
            "niflheim_effect", () -> AttachmentType.builder(() -> NiflheimEffectState.INACTIVE)
                    .serialize(NiflheimEffectState.MAP_CODEC)
                    .build());

    public static final Supplier<AttachmentType<FenrirEffectState>> FENRIR_EFFECT = ATTACHMENTS.register(
            "fenrir_effect", () -> AttachmentType.builder(() -> FenrirEffectState.INACTIVE)
                    .serialize(FenrirEffectState.MAP_CODEC)
                    .build());

    public static final Supplier<AttachmentType<IdunnEffectState>> IDUNN_EFFECT = ATTACHMENTS.register(
            "idunn_effect", () -> AttachmentType.builder(() -> IdunnEffectState.INACTIVE)
                    .serialize(IdunnEffectState.MAP_CODEC)
                    .build());

    public static final Supplier<AttachmentType<JormungandrEffectState>> JORMUNGANDR_EFFECT = ATTACHMENTS.register(
            "jormungandr_effect", () -> AttachmentType.builder(() -> JormungandrEffectState.INACTIVE)
                    .serialize(JormungandrEffectState.MAP_CODEC)
                    .build());

    public static final Supplier<AttachmentType<FenrirFrostState>> FENRIR_FROST = ATTACHMENTS.register(
            "fenrir_frost", () -> AttachmentType.builder(() -> FenrirFrostState.EMPTY)
                    .serialize(FenrirFrostState.MAP_CODEC)
                    .build());

    private ModAttachments() {
    }
}
