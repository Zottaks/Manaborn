package com.manaborn.commands;

import com.manaborn.spells.LearnedSpellsManager;
import com.manaborn.spells.SpellLevelManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.command.argument.EntityArgumentType;

/**
 * Команда для сброса всех изученных заклинаний и их уровней
 * Использование: /manaborn reset_spells [player]
 */
public class ResetSpellsCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, 
                                CommandRegistryAccess registryAccess, 
                                CommandManager.RegistrationEnvironment environment) {
        
        dispatcher.register(CommandManager.literal("manaborn")
            .then(CommandManager.literal("reset_spells")
                .requires(source -> source.hasPermissionLevel(2)) // Требует OP уровень 2
                .executes(context -> resetSpells(context, context.getSource().getPlayerOrThrow()))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> resetSpells(context, EntityArgumentType.getPlayer(context, "player")))
                )
            )
        );
    }
    
    private static int resetSpells(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        ServerCommandSource source = context.getSource();
        
        // ВАЖНО: LearnedSpellsManager использует глобальное хранилище для ВСЕХ игроков
        // Поэтому сброс изученных заклинаний будет для ВСЕХ игроков на сервере
        LearnedSpellsManager.clearAll();
        LearnedSpellsManager.saveToDisk(); // Сохраняем на диск
        
        // Сбрасываем уровни заклинаний для конкретного игрока
        SpellLevelManager.clearPlayer(player.getUuid());
        
        // Синхронизируем с ВСЕМИ онлайн игроками (т.к. заклинания глобальные)
        for (ServerPlayerEntity onlinePlayer : source.getServer().getPlayerManager().getPlayerList()) {
            LearnedSpellsManager.sendSync(onlinePlayer);
            SpellLevelManager.sendSync(onlinePlayer);
        }
        
        // Сообщения
        if (source.getPlayer() == player) {
            player.sendMessage(Text.literal("§c§l⚠ ВНИМАНИЕ: Все изученные заклинания сброшены для ВСЕХ игроков!"), false);
            player.sendMessage(Text.literal("§6Уровни заклинаний сброшены для тебя."), false);
        } else {
            player.sendMessage(Text.literal("§6Твои уровни заклинаний были сброшены администратором!"), false);
            source.sendFeedback(() -> Text.literal("§c§l⚠ Изученные заклинания сброшены для ВСЕХ игроков!"), true);
            source.sendFeedback(() -> Text.literal("§aУровни заклинаний игрока " + player.getName().getString() + " сброшены!"), true);
        }
        
        return 1;
    }
}
