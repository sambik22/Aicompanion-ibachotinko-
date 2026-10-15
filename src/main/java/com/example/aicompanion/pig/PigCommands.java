package com.example.aicompanion.pig;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Pig;

import java.util.List;

/**
 * /aipig mode <follow|stay|guard|forage|wander> — режим для всіх твоїх свиней поруч
 * /aipig call    — покликати всіх своїх свиней до себе
 * /aipig list    — список твоїх свиней і їхні режими
 * /aipig release — відпустити всіх своїх свиней поруч
 */
public final class PigCommands {

    private static final double RANGE = 128.0;

    private PigCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aipig")
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(PigMode.ids(), b))
                                .executes(c -> setMode(c.getSource(), StringArgumentType.getString(c, "mode")))))
                .then(Commands.literal("call").executes(c -> call(c.getSource())))
                .then(Commands.literal("list").executes(c -> list(c.getSource())))
                .then(Commands.literal("release").executes(c -> release(c.getSource()))));
    }

    private static int setMode(CommandSourceStack source, String id) throws CommandSyntaxException {
        PigMode mode = PigMode.byId(id);
        if (mode == null) {
            source.sendFailure(Component.literal("Невідомий режим: " + id + ". Є: " + String.join(", ", PigMode.ids())));
            return 0;
        }
        List<Pig> pigs = ownedPigs(source.getPlayerOrException());
        pigs.forEach(pig -> PigBrain.setMode(pig, mode));
        source.sendSuccess(() -> Component.literal("Свиней: " + pigs.size() + ". Режим: " + mode.title()), false);
        return pigs.size();
    }

    private static int call(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<Pig> pigs = ownedPigs(player);
        for (Pig pig : pigs) {
            pig.getNavigation().stop();
            pig.snapTo(player.getX(), player.getY(), player.getZ(), pig.getYRot(), pig.getXRot());
            PigBrain.setMode(pig, PigMode.FOLLOW);
        }
        source.sendSuccess(() -> Component.literal("Прибігло свиней: " + pigs.size()), false);
        return pigs.size();
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<Pig> pigs = ownedPigs(player);
        if (pigs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Поруч немає твоїх свиней. Приручи свиню золотою морквою!"), false);
            return 0;
        }
        for (Pig pig : pigs) {
            String line = String.format("%s — %s (%.0f бл.)",
                    pig.getName().getString(), PigBrain.getMode(pig).title(), pig.distanceTo(player));
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return pigs.size();
    }

    private static int release(CommandSourceStack source) throws CommandSyntaxException {
        List<Pig> pigs = ownedPigs(source.getPlayerOrException());
        pigs.forEach(PigBrain::release);
        source.sendSuccess(() -> Component.literal("Відпущено свиней: " + pigs.size()), false);
        return pigs.size();
    }

    private static List<Pig> ownedPigs(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        return level.getEntitiesOfClass(Pig.class, player.getBoundingBox().inflate(RANGE),
                pig -> PigBrain.isOwnedBy(pig, player));
    }
}
