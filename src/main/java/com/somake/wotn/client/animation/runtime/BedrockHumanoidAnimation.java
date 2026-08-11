package com.somake.wotn.client.animation.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import java.util.HashSet;
import java.util.Set;

public final class BedrockHumanoidAnimation {
    private final float lengthTicks;
    private final Map<String, BoneTrack> bones;

    private BedrockHumanoidAnimation(float lengthTicks, Map<String, BoneTrack> bones) {
        this.lengthTicks = lengthTicks;
        this.bones = Map.copyOf(bones);
    }

    public static BedrockHumanoidAnimation parse(JsonObject root, String animationName) {
        JsonObject animations = requiredObject(root, "animations");
        JsonObject animation = requiredObject(animations, animationName);
        float lengthTicks = animation.get("animation_length").getAsFloat() * 20.0F;
        JsonObject boneObjects = requiredObject(animation, "bones");
        Map<String, BoneTrack> bones = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : boneObjects.entrySet()) {
            JsonObject bone = entry.getValue().getAsJsonObject();
            bones.put(entry.getKey(), new BoneTrack(
                    VectorTrack.parse(bone.get("position"), new Vector3f()),
                    VectorTrack.parse(bone.get("rotation"), new Vector3f())));
        }
        return new BedrockHumanoidAnimation(lengthTicks, bones);
    }

    public void apply(HumanoidModel<?> model, float age, HumanoidArm weaponArm, BedrockHumanoidRig rig) {
        float time = Mth.clamp(age, 0.0F, this.lengthTicks);
        Map<String, Matrix4f> worldTransforms = new HashMap<>();
        computeWorldTransform("body", time, rig, worldTransforms, new HashSet<>());
        computeWorldTransform("head", time, rig, worldTransforms, new HashSet<>());
        computeWorldTransform("right_arm", time, rig, worldTransforms, new HashSet<>());
        computeWorldTransform("left_arm", time, rig, worldTransforms, new HashSet<>());
        computeWorldTransform("right_leg", time, rig, worldTransforms, new HashSet<>());
        computeWorldTransform("left_leg", time, rig, worldTransforms, new HashSet<>());
        boolean mirrorPose = weaponArm == HumanoidArm.LEFT;
        applyMatrix(model.body, worldTransforms.get("body"), rig.bone("body"), mirrorPose);
        applyMatrix(model.head, worldTransforms.get("head"), rig.bone("head"), mirrorPose);
        model.hat.loadPose(model.head.storePose());
        if (weaponArm == HumanoidArm.RIGHT) {
            applyMatrix(model.rightArm, worldTransforms.get("right_arm"), rig.bone("right_arm"), false);
            applyMatrix(model.leftArm, worldTransforms.get("left_arm"), rig.bone("left_arm"), false);
            applyMatrix(model.rightLeg, worldTransforms.get("right_leg"), rig.bone("right_leg"), false);
            applyMatrix(model.leftLeg, worldTransforms.get("left_leg"), rig.bone("left_leg"), false);
        } else {
            applyMatrix(model.leftArm, worldTransforms.get("right_arm"), rig.bone("right_arm"), true);
            applyMatrix(model.rightArm, worldTransforms.get("left_arm"), rig.bone("left_arm"), true);
            applyMatrix(model.leftLeg, worldTransforms.get("right_leg"), rig.bone("right_leg"), true);
            applyMatrix(model.rightLeg, worldTransforms.get("left_leg"), rig.bone("left_leg"), true);
        }
    }

    private Matrix4f computeWorldTransform(String boneName, float time, BedrockHumanoidRig rig,
            Map<String, Matrix4f> cache, Set<String> visiting) {
        Matrix4f cached = cache.get(boneName);
        if (cached != null) return cached;
        if (!visiting.add(boneName)) throw new IllegalStateException("Animation bone cycle: " + boneName);
        BedrockHumanoidRig.Bone bone = rig.bone(boneName);
        if (bone == null) return new Matrix4f();
        BoneTrack track = this.bones.get(boneName);
        Vector3f position = track == null ? new Vector3f() : track.position.sample(time);
        Vector3f rotation = track == null ? new Vector3f() : track.rotation.sample(time);
        Vector3f pivot = bone.pivot();
        boolean leg = boneName.equals("right_leg") || boneName.equals("left_leg");
        float rotationX = (leg ? rotation.x : -rotation.x) * Mth.DEG_TO_RAD;
        float rotationY = (leg ? rotation.y : -rotation.y) * Mth.DEG_TO_RAD;
        float positionZ = leg ? -position.z : position.z;
        Matrix4f local = new Matrix4f()
                .translate(pivot)
                .translate(-position.x, -position.y, positionZ)
                .rotateZYX(rotation.z * Mth.DEG_TO_RAD,
                        rotationY, rotationX)
                .translate(new Vector3f(pivot).negate());
        Matrix4f world;
        if (bone.parent() == null || bone.parent().equals("root")) {
            world = local;
        } else {
            world = new Matrix4f(computeWorldTransform(bone.parent(), time, rig, cache, visiting)).mul(local);
        }
        visiting.remove(boneName);
        cache.put(boneName, world);
        return world;
    }

    private void applyMatrix(ModelPart part, Matrix4f matrix, BedrockHumanoidRig.Bone bone, boolean mirror) {
        if (matrix == null || bone == null) return;
        PartPose initial = part.getInitialPose();
        Matrix4f resolved = mirror ? mirrorMatrix(matrix) : matrix;
        Vector3f bindPivot = new Vector3f(bone.pivot());
        if (mirror) bindPivot.x = -bindPivot.x;
        Vector3f animatedPivot = resolved.transformPosition(new Vector3f(bindPivot));
        Vector3f delta = animatedPivot.sub(bindPivot, new Vector3f());
        part.x = initial.x() + delta.x;
        part.y = initial.y() + delta.y;
        part.z = initial.z() + delta.z;
        Quaternionf rotation = resolved.getUnnormalizedRotation(new Quaternionf()).normalize();
        Vector3f euler = rotation.getEulerAnglesZYX(new Vector3f());
        part.xRot = euler.x;
        part.yRot = euler.y;
        part.zRot = euler.z;
    }

    private static Matrix4f mirrorMatrix(Matrix4f matrix) {
        Matrix4f reflection = new Matrix4f().scaling(-1.0F, 1.0F, 1.0F);
        return reflection.mul(new Matrix4f(matrix)).mul(new Matrix4f().scaling(-1.0F, 1.0F, 1.0F));
    }

    private static JsonObject requiredObject(JsonObject parent, String member) {
        JsonElement value = parent.get(member);
        if (value == null || !value.isJsonObject()) throw new JsonParseException("Missing object: " + member);
        return value.getAsJsonObject();
    }

    private record BoneTrack(VectorTrack position, VectorTrack rotation) {}

    private record VectorKeyframe(float time, Vector3f pre, Vector3f post, Interpolation interpolation) {}

    private enum Interpolation { LINEAR, CATMULL_ROM, STEP }

    private static final class VectorTrack {
        private final List<VectorKeyframe> frames;
        private final Vector3f defaultValue;

        private VectorTrack(List<VectorKeyframe> frames, Vector3f defaultValue) {
            this.frames = List.copyOf(frames);
            this.defaultValue = new Vector3f(defaultValue);
        }

        static VectorTrack parse(JsonElement element, Vector3f defaultValue) {
            if (element == null || element.isJsonNull()) return new VectorTrack(List.of(), defaultValue);
            if (element.isJsonArray()) {
                Vector3f value = vector(element);
                return new VectorTrack(List.of(new VectorKeyframe(0.0F, value, value, Interpolation.LINEAR)), defaultValue);
            }
            JsonObject object = element.getAsJsonObject();
            if (object.has("vector")) {
                Vector3f value = vector(object.get("vector"));
                return new VectorTrack(List.of(new VectorKeyframe(0.0F, value, value, interpolation(object))), defaultValue);
            }
            List<VectorKeyframe> frames = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                float time = Float.parseFloat(entry.getKey()) * 20.0F;
                JsonElement frameElement = entry.getValue();
                if (frameElement.isJsonArray()) {
                    Vector3f value = vector(frameElement);
                    frames.add(new VectorKeyframe(time, value, value, Interpolation.LINEAR));
                    continue;
                }
                JsonObject frame = frameElement.getAsJsonObject();
                Vector3f base = frame.has("vector") ? vector(frame.get("vector")) : null;
                Vector3f pre = frame.has("pre") ? wrappedVector(frame.get("pre")) : base;
                Vector3f post = frame.has("post") ? wrappedVector(frame.get("post")) : base;
                if (pre == null && post == null) throw new JsonParseException("Keyframe has no vector at " + entry.getKey());
                if (pre == null) pre = new Vector3f(post);
                if (post == null) post = new Vector3f(pre);
                frames.add(new VectorKeyframe(time, pre, post, interpolation(frame)));
            }
            frames.sort(Comparator.comparingDouble(VectorKeyframe::time));
            return new VectorTrack(frames, defaultValue);
        }

        Vector3f sample(float time) {
            if (this.frames.isEmpty()) return new Vector3f(this.defaultValue);
            if (time <= this.frames.getFirst().time) return new Vector3f(this.frames.getFirst().post);
            VectorKeyframe last = this.frames.getLast();
            if (time >= last.time) return new Vector3f(last.post);
            int index = 0;
            while (index + 1 < this.frames.size() && time >= this.frames.get(index + 1).time) index++;
            VectorKeyframe from = this.frames.get(index);
            VectorKeyframe to = this.frames.get(index + 1);
            float progress = (time - from.time) / (to.time - from.time);
            Interpolation mode = from.interpolation;
            if (mode == Interpolation.STEP) return new Vector3f(from.post);
            if (mode == Interpolation.LINEAR) return new Vector3f(from.post).lerp(to.pre, progress);
            Vector3f p0 = index > 0 ? this.frames.get(index - 1).post : from.post;
            Vector3f p1 = from.post;
            Vector3f p2 = to.pre;
            Vector3f p3 = index + 2 < this.frames.size() ? this.frames.get(index + 2).pre : to.pre;
            return catmull(p0, p1, p2, p3, progress);
        }

        private static Interpolation interpolation(JsonObject object) {
            String mode = object.has("lerp_mode") ? object.get("lerp_mode").getAsString()
                    : object.has("easing") ? object.get("easing").getAsString() : "linear";
            return switch (mode.toLowerCase()) {
                case "catmullrom", "catmull_rom" -> Interpolation.CATMULL_ROM;
                case "step" -> Interpolation.STEP;
                default -> Interpolation.LINEAR;
            };
        }

        private static Vector3f wrappedVector(JsonElement element) {
            return element.isJsonObject() ? vector(element.getAsJsonObject().get("vector")) : vector(element);
        }

        private static Vector3f vector(JsonElement element) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() != 3) throw new JsonParseException("Expected three animation components");
            return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
        }

        private static Vector3f catmull(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
            return new Vector3f(catmull(p0.x, p1.x, p2.x, p3.x, t),
                    catmull(p0.y, p1.y, p2.y, p3.y, t), catmull(p0.z, p1.z, p2.z, p3.z, t));
        }

        private static float catmull(float p0, float p1, float p2, float p3, float t) {
            float t2 = t * t;
            float t3 = t2 * t;
            return 0.5F * (2.0F * p1 + (-p0 + p2) * t
                    + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
                    + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3);
        }
    }
}
