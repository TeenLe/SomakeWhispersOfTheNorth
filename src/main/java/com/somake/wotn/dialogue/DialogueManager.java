package com.somake.wotn.dialogue;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.network.CloseDialoguePayload;
import com.somake.wotn.network.ShowDialoguePayload;
import com.somake.wotn.registry.ModItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.somake.wotn.skilltree.ForgeSessionManager;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.somake.wotn.alchemy.AlchemyManager;
import org.jspecify.annotations.Nullable;

public final class DialogueManager extends SimpleJsonResourceReloadListener<DialogueDefinition> {
    public static final DialogueManager INSTANCE = new DialogueManager();
    private static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "dialogues");

    private volatile Map<Identifier, DialogueDefinition> definitions = Map.of();
    private final Map<UUID, Session> sessions = new HashMap<>();

    private DialogueManager() {
        super(DialogueDefinition.CODEC, FileToIdConverter.json("dialogues"));
    }

    public static void registerReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, INSTANCE);
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        INSTANCE.sessions.remove(event.getEntity().getUUID());
    }

    @Override
    protected void apply(Map<Identifier, DialogueDefinition> preparations,
            ResourceManager resourceManager, ProfilerFiller profiler) {
        this.definitions = Map.copyOf(preparations);
        this.sessions.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            DialogueDefinition definition = this.definitions.get(session.dialogueId());
            return definition == null || !definition.nodes().containsKey(session.nodeId());
        });
        WhispersOfTheNorth.LOGGER.info("Loaded {} dialogue definitions", this.definitions.size());
    }

    public boolean start(ServerPlayer player, Identifier dialogueId) {
        return start(player, dialogueId, null);
    }

    public boolean start(ServerPlayer player, Identifier dialogueId, @Nullable Entity source) {
        DialogueDefinition definition = this.definitions.get(dialogueId);
        if (definition == null || !definition.nodes().containsKey(definition.start())) return false;
        Session session = new Session(UUID.randomUUID(), dialogueId, definition.start(),
                source == null ? null : source.getUUID());
        this.sessions.put(player.getUUID(), session);
        sendNode(player, session, definition);
        return true;
    }

    public void select(ServerPlayer player, UUID sessionId, String responseId) {
        Session session = this.sessions.get(player.getUUID());
        if (session == null || !session.id().equals(sessionId)) return;
        if (!isSourceValid(player, session)) {
            close(player, sessionId);
            return;
        }
        DialogueDefinition definition = this.definitions.get(session.dialogueId());
        if (definition == null) {
            close(player, sessionId);
            return;
        }
        DialogueDefinition.Node node = definition.nodes().get(session.nodeId());
        if (node == null) return;
        Optional<DialogueDefinition.Response> selected = node.responses().stream()
                .filter(response -> response.id().equals(responseId)).findFirst();
        if (selected.isEmpty() || !isAvailable(player, selected.get())) return;

        DialogueDefinition.Response response = selected.get();
        if (response.action().isPresent()) {
            switch (response.action().get()) {
                case "open_weapon_mastery" -> {
                    this.sessions.remove(player.getUUID());
                    PacketDistributor.sendToPlayer(player, new CloseDialoguePayload(sessionId));
                    ForgeSessionManager.INSTANCE.open(player);
                    return;
                }
                case "open_alchemy" -> {
                    this.sessions.remove(player.getUUID());
                    PacketDistributor.sendToPlayer(player, new CloseDialoguePayload(sessionId));
                    AlchemyManager.INSTANCE.open(player);
                    return;
                }
                case "close" -> {
                    close(player, sessionId);
                    return;
                }
                default -> WhispersOfTheNorth.LOGGER.warn("Unknown dialogue action: {}", response.action().get());
            }
        }
        if (response.next().isPresent() && definition.nodes().containsKey(response.next().get())) {
            Session next = new Session(session.id(), session.dialogueId(), response.next().get(),
                    session.sourceEntityId());
            this.sessions.put(player.getUUID(), next);
            sendNode(player, next, definition);
        }
    }

    public void close(ServerPlayer player, UUID sessionId) {
        Session session = this.sessions.get(player.getUUID());
        if (session == null || !session.id().equals(sessionId)) return;
        this.sessions.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new CloseDialoguePayload(sessionId));
    }

    private void sendNode(ServerPlayer player, Session session, DialogueDefinition definition) {
        DialogueDefinition.Node node = definition.nodes().get(session.nodeId());
        List<ShowDialoguePayload.Response> responses = new ArrayList<>();
        for (DialogueDefinition.Response response : node.responses()) {
            boolean available = isAvailable(player, response);
            if (!available && !response.showWhenUnavailable()) continue;
            String requirement = !available ? requirement(player, response) : "";
            responses.add(new ShowDialoguePayload.Response(response.id(), response.text(), available, requirement));
        }
        PacketDistributor.sendToPlayer(player, new ShowDialoguePayload(session.id(), definition.speaker(),
                definition.role(), node.text(), responses));
    }

    private boolean isAvailable(ServerPlayer player, DialogueDefinition.Response response) {
        return true;
    }

    private String requirement(ServerPlayer player, DialogueDefinition.Response response) {
        return "";
    }

    private boolean isSourceValid(ServerPlayer player, Session session) {
        if (session.sourceEntityId() == null) return true;
        Entity source = player.level().getEntityInAnyDimension(session.sourceEntityId());
        return source != null
                && source.isAlive()
                && source.level() == player.level()
                && player.distanceToSqr(source) <= 36.0D;
    }

    private record Session(UUID id, Identifier dialogueId, String nodeId, @Nullable UUID sourceEntityId) {
    }
}
