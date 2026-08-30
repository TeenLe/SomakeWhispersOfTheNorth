package com.somake.wotn.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.somake.wotn.entity.FenrirEntity;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.dialogue.DialogueManager;
import net.minecraft.resources.Identifier;
import com.somake.wotn.skilltree.ForgeSessionManager;
import com.somake.wotn.alchemy.AlchemyManager;
import com.somake.wotn.registry.ModItems;
import com.somake.wotn.skilltree.WeaponSkillData;
import java.util.Comparator;
import java.util.Locale;

public final class WotnCommands {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("wotn")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("skills")
                        .executes(context -> {
                            ForgeSessionManager.INSTANCE.open(context.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("maximize")
                                .executes(context -> {
                                    var player = context.getSource().getPlayerOrException();
                                    var axe = player.getMainHandItem().is(ModItems.LEVIATHAN_AXE.get())
                                            ? player.getMainHandItem()
                                            : player.getOffhandItem().is(ModItems.LEVIATHAN_AXE.get())
                                                    ? player.getOffhandItem()
                                                    : net.minecraft.world.item.ItemStack.EMPTY;
                                    if (axe.isEmpty()) {
                                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                                "message.wotn.leviathan.maximize.no_axe"));
                                        return 0;
                                    }
                                    WeaponSkillData.setProgress(axe,
                                            com.somake.wotn.skilltree.WeaponSkillProgress.maximized());
                                    player.getInventory().setChanged();
                                    player.containerMenu.broadcastChanges();
                                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                            "message.wotn.leviathan.maximized"));
                                    return 1;
                                })))
                .then(Commands.literal("alchemy")
                        .executes(context -> {
                            AlchemyManager.INSTANCE.open(context.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("reset")
                                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                                .then(Commands.literal("confirm")
                                        .executes(context -> {
                                            AlchemyManager.INSTANCE.resetProgress(
                                                    context.getSource().getPlayerOrException());
                                            return 1;
                                        })))
                        .then(Commands.literal("unlock_all")
                                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                                .then(Commands.literal("confirm")
                                        .executes(context -> {
                                            AlchemyManager.INSTANCE.unlockAll(
                                                    context.getSource().getPlayerOrException());
                                            return 1;
                                        }))))
                .then(Commands.literal("dialogue")
                        .then(Commands.literal("blacksmith")
                                .executes(context -> DialogueManager.INSTANCE.start(
                                        context.getSource().getPlayerOrException(),
                                         Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "blacksmith"))
                                                ? 1 : 0))
                        .then(Commands.literal("alchemist")
                                .executes(context -> DialogueManager.INSTANCE.start(
                                        context.getSource().getPlayerOrException(),
                                        Identifier.fromNamespaceAndPath(WhispersOfTheNorth.MODID, "alchemist"))
                                                ? 1 : 0)))
                .then(Commands.literal("fenrir")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("mouth")
                                .then(Commands.literal("show")
                                        .executes(context -> setMouthDebug(context.getSource(), true)))
                                .then(Commands.literal("hide")
                                        .executes(context -> setMouthDebug(context.getSource(), false)))
                                .then(Commands.literal("get")
                                        .executes(context -> reportMouthOffset(context.getSource())))
                                .then(Commands.literal("reset")
                                        .executes(context -> setMouthOffset(context.getSource(),
                                                FenrirEntity.DEFAULT_MOUTH_FORWARD,
                                                FenrirEntity.DEFAULT_MOUTH_UP,
                                                FenrirEntity.DEFAULT_MOUTH_SIDE, false)))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("forward", DoubleArgumentType.doubleArg(-16.0D, 16.0D))
                                                .then(Commands.argument("up", DoubleArgumentType.doubleArg(-16.0D, 16.0D))
                                                        .then(Commands.argument("side", DoubleArgumentType.doubleArg(-16.0D, 16.0D))
                                                                .executes(context -> setMouthOffset(context.getSource(),
                                                                        DoubleArgumentType.getDouble(context, "forward"),
                                                                        DoubleArgumentType.getDouble(context, "up"),
                                                                        DoubleArgumentType.getDouble(context, "side"), false))))))
                                .then(Commands.literal("nudge")
                                        .then(Commands.argument("forward", DoubleArgumentType.doubleArg(-4.0D, 4.0D))
                                                .then(Commands.argument("up", DoubleArgumentType.doubleArg(-4.0D, 4.0D))
                                                        .then(Commands.argument("side", DoubleArgumentType.doubleArg(-4.0D, 4.0D))
                                                                .executes(context -> setMouthOffset(context.getSource(),
                                                                        DoubleArgumentType.getDouble(context, "forward"),
                                                                        DoubleArgumentType.getDouble(context, "up"),
                                                                        DoubleArgumentType.getDouble(context, "side"), true)))))))));
    }

    private static int setMouthDebug(CommandSourceStack source, boolean enabled) {
        FenrirEntity fenrir = findNearestFenrir(source);
        if (fenrir == null) return 0;
        fenrir.setMouthDebugEnabled(enabled);
        source.sendSuccess(() -> Component.literal(enabled
                ? "Fenrir mouth debug enabled. Use /wotn fenrir mouth set <forward> <up> <side>."
                : "Fenrir mouth debug disabled."), false);
        if (enabled) reportMouthOffset(source, fenrir);
        return 1;
    }

    private static int setMouthOffset(CommandSourceStack source, double forward, double up, double side,
            boolean relative) {
        FenrirEntity fenrir = findNearestFenrir(source);
        if (fenrir == null) return 0;
        float resolvedForward = (float) (relative ? fenrir.getMouthDebugForward() + forward : forward);
        float resolvedUp = (float) (relative ? fenrir.getMouthDebugUp() + up : up);
        float resolvedSide = (float) (relative ? fenrir.getMouthDebugSide() + side : side);
        fenrir.setMouthDebugOffset(resolvedForward, resolvedUp, resolvedSide);
        fenrir.setMouthDebugEnabled(true);
        reportMouthOffset(source, fenrir);
        return 1;
    }

    private static int reportMouthOffset(CommandSourceStack source) {
        FenrirEntity fenrir = findNearestFenrir(source);
        if (fenrir == null) return 0;
        reportMouthOffset(source, fenrir);
        return 1;
    }

    private static void reportMouthOffset(CommandSourceStack source, FenrirEntity fenrir) {
        String values = String.format(Locale.ROOT, "forward=%.3f, up=%.3f, side=%.3f",
                fenrir.getMouthDebugForward(), fenrir.getMouthDebugUp(), fenrir.getMouthDebugSide());
        source.sendSuccess(() -> Component.literal("Fenrir mouth offset: " + values), false);
    }

    private static FenrirEntity findNearestFenrir(CommandSourceStack source) {
        var origin = source.getPosition();
        FenrirEntity fenrir = source.getLevel().getEntitiesOfClass(FenrirEntity.class,
                AABB.ofSize(origin, 128.0D, 128.0D, 128.0D), entity -> entity.isAlive()).stream()
                .min(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(origin)))
                .orElse(null);
        if (fenrir == null) {
            source.sendFailure(Component.literal("No living Fenrir found within 64 blocks."));
        }
        return fenrir;
    }

    private WotnCommands() {
    }
}
