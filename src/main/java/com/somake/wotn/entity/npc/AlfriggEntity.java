package com.somake.wotn.entity.npc;

import com.somake.wotn.WhispersOfTheNorth;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class AlfriggEntity extends DialogueNpcEntity {
    private static final Identifier DIALOGUE_ID = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "blacksmith");

    public AlfriggEntity(EntityType<? extends AlfriggEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected Identifier dialogueId() {
        return DIALOGUE_ID;
    }
}
