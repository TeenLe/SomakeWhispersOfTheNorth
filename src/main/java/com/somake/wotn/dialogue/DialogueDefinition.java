package com.somake.wotn.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record DialogueDefinition(String speaker, String role, String start, Map<String, Node> nodes) {
    public static final Codec<DialogueDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("speaker").forGetter(DialogueDefinition::speaker),
            Codec.STRING.optionalFieldOf("role", "").forGetter(DialogueDefinition::role),
            Codec.STRING.fieldOf("start").forGetter(DialogueDefinition::start),
            Codec.unboundedMap(Codec.STRING, Node.CODEC).fieldOf("nodes").forGetter(DialogueDefinition::nodes))
            .apply(instance, DialogueDefinition::new));

    public record Node(String text, List<Response> responses) {
        public static final Codec<Node> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("text").forGetter(Node::text),
                Response.CODEC.listOf().optionalFieldOf("responses", List.of()).forGetter(Node::responses))
                .apply(instance, Node::new));
    }

    public record Response(String id, String text, Optional<String> next, Optional<String> action,
            int requiredMastery, boolean requiresLeviathan, boolean showWhenUnavailable) {
        public static final Codec<Response> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Response::id),
                Codec.STRING.fieldOf("text").forGetter(Response::text),
                Codec.STRING.optionalFieldOf("next").forGetter(Response::next),
                Codec.STRING.optionalFieldOf("action").forGetter(Response::action),
                Codec.INT.optionalFieldOf("required_mastery", 0).forGetter(Response::requiredMastery),
                Codec.BOOL.optionalFieldOf("requires_leviathan", false).forGetter(Response::requiresLeviathan),
                Codec.BOOL.optionalFieldOf("show_when_unavailable", true).forGetter(Response::showWhenUnavailable))
                .apply(instance, Response::new));
    }
}
