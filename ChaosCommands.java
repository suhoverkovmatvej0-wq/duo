package com.duochaos.event;

import com.duochaos.DuoChaosMod;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** /duochaos event <chickens|diamonds|gravity|slimes|treasure|random|stop>, /duochaos mission, /duochaos thief|duck */
public class ChaosCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("duochaos")
                .requires(s -> s.hasPermissionLevel(2))
                .then(Commands.literal("event")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> ISuggestionProvider.suggest(ChaosEvents.NAMES, b))
                                .executes(c -> {
                                    ServerWorld w = c.getSource().getServer().getWorld(World.OVERWORLD);
                                    boolean ok = ChaosEvents.force(StringArgumentType.getString(c, "name"), w);
                                    c.getSource().sendFeedback(new StringTextComponent(ok ? "Готово" : "Неизвестное событие или нет игроков"), true);
                                    return 1;
                                })))
                .then(Commands.literal("mission").executes(c -> {
                    ServerWorld w = c.getSource().getServer().getWorld(World.OVERWORLD);
                    Missions.assign(w, w.getPlayers());
                    return 1;
                }))
                .then(Commands.literal("thief").executes(c -> {
                    ServerPlayerEntity p = c.getSource().asPlayer();
                    ChaosEvents.spawnNear((ServerWorld) p.world, p, DuoChaosMod.THIEF.get(), 6, 10);
                    return 1;
                }))
                .then(Commands.literal("duck").executes(c -> {
                    ServerPlayerEntity p = c.getSource().asPlayer();
                    ChaosEvents.spawnNear((ServerWorld) p.world, p, DuoChaosMod.KILLER_DUCK.get(), 8, 12);
                    return 1;
                })));
    }
}
