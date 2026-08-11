package com.somake.wotn.command;

import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.dialogue.DialogueManager;
import net.minecraft.resources.Identifier;
import com.somake.wotn.skilltree.ForgeSessionManager;

public final class WotnCommands {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("wotn")
                .then(Commands.literal("skills")
                        .executes(context -> {
                            ForgeSessionManager.INSTANCE.open(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("dialogue")
                        .then(Commands.literal("blacksmith")
                                .executes(context -> DialogueManager.INSTANCE.start(
                                        context.getSource().getPlayerOrException(),
                                        Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "blacksmith"))
                                                ? 1 : 0))));
    }

    private WotnCommands() {
    }
}
