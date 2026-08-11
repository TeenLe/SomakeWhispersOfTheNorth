package com.somake.wotn.skilltree;

import java.util.List;

public final class LeviathanSkillTree {
    public static final int ROOT = 0;
    public static final int THROW = 1;
    public static final int QUICK_RETURN = 2;
    public static final int IMBUE = 3;
    public static final int DEEP_FREEZE = 4;
    public static final int ICE_SPIKES = 5;
    public static final int GLACIAL_REACH = 6;

    public enum Type {
        ROOT, ACTIVE, PASSIVE, MODIFIER
    }

    public record Node(int id, String key, Type type, int parentId, int cost, int requiredMastery,
            int treeX, int treeY, int skillId, boolean implemented) {
        public boolean canUnlock(WeaponSkillProgress progress) {
            return implemented && !progress.isUnlocked(id)
                    && progress.isUnlocked(parentId) && progress.points() >= cost
                    && progress.masteryLevel() >= requiredMastery;
        }
    }

    public static final List<Node> NODES = List.of(
            new Node(ROOT, "root", Type.ROOT, ROOT, 0, 1, 0, 72, 0, true),
            new Node(THROW, "throw", Type.ACTIVE, ROOT, 0, 1, 0, 32, 0, true),
            new Node(QUICK_RETURN, "quick_return", Type.MODIFIER, THROW, 2, 3, -76, -8, 0, false),
            new Node(IMBUE, "imbue", Type.ACTIVE, THROW, 1, 2, -50, -54, 1, true),
            new Node(DEEP_FREEZE, "deep_freeze", Type.PASSIVE, IMBUE, 2, 5, -86, -94, 0, false),
            new Node(ICE_SPIKES, "spikes", Type.ACTIVE, THROW, 2, 3, 50, -54, 2, true),
            new Node(GLACIAL_REACH, "glacial_reach", Type.MODIFIER, ICE_SPIKES, 2, 5, 86, -94, 0, false));

    public static Node byId(int id) {
        return NODES.stream().filter(node -> node.id() == id).findFirst().orElse(null);
    }

    private LeviathanSkillTree() {
    }
}
