package com.example.aicompanion.morph;

import com.example.aicompanion.network.MorphSyncPacket;
import com.example.aicompanion.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class MorphCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext ctx) {

        // /morphplayer <target> <entity>
        dispatcher.register(
                Commands.literal("morphplayer")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("entity", ResourceLocationArgument.id())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                                                BuiltInRegistries.ENTITY_TYPE.keySet(), b))
                                        .executes(c -> {
                                            ServerPlayer target = EntityArgument.getPlayer(c, "target");
                                            ResourceLocation id = ResourceLocationArgument.getId(c, "entity");

                                            // ENTITY_TYPE — "defaulted" реєстр: get() для невідомого id
                                            // повернув би свиню, тому перевіряємо через getOptional
                                            if (BuiltInRegistries.ENTITY_TYPE.getOptional(id).isEmpty()) {
                                                c.getSource().sendFailure(
                                                        Component.literal("Невідомий моб: " + id)
                                                );
                                                return 0;
                                            }

                                            MorphData.setMorphServer(target.getUUID(), id);
                                            NetworkHandler.sendToAll(new MorphSyncPacket(target.getUUID(), id));

                                            c.getSource().sendSuccess(
                                                    () -> Component.literal("Гравець " +
                                                            target.getName().getString() +
                                                            " тепер виглядає як " + id),
                                                    true
                                            );
                                            return 1;
                                        })
                                )
                        )
        );

        // /morphclear <target>
        dispatcher.register(
                Commands.literal("morphclear")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(c -> {
                                    ServerPlayer target = EntityArgument.getPlayer(c, "target");

                                    MorphData.clearMorphServer(target.getUUID());
                                    NetworkHandler.sendToAll(new MorphSyncPacket(target.getUUID(), null));

                                    c.getSource().sendSuccess(
                                            () -> Component.literal("Морф гравця " +
                                                    target.getName().getString() + " скинуто"),
                                            true
                                    );
                                    return 1;
                                })
                        )
        );
    }
}
