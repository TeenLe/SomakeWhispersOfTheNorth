package com.somake.wotn.client.animation.runtime;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.joml.Vector3f;

public final class BedrockHumanoidRig {
    private final Map<String, Bone> bones;

    private BedrockHumanoidRig(Map<String, Bone> bones) {
        this.bones = Map.copyOf(bones);
    }

    public static BedrockHumanoidRig parse(JsonObject root) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.isEmpty()) throw new JsonParseException("Missing minecraft:geometry");
        JsonArray boneArray = geometries.get(0).getAsJsonObject().getAsJsonArray("bones");
        Map<String, Bone> bones = new HashMap<>();
        for (var element : boneArray) {
            JsonObject object = element.getAsJsonObject();
            String name = object.get("name").getAsString();
            String parent = object.has("parent") ? object.get("parent").getAsString() : null;
            JsonArray pivotArray = object.getAsJsonArray("pivot");
            Vector3f pivot = new Vector3f(-pivotArray.get(0).getAsFloat(),
                    24.0F - pivotArray.get(1).getAsFloat(), pivotArray.get(2).getAsFloat());
            bones.put(name, new Bone(name, parent, pivot));
        }
        return new BedrockHumanoidRig(bones);
    }

    public Bone bone(String name) {
        return this.bones.get(name);
    }

    public record Bone(String name, String parent, Vector3f pivot) {}
}
