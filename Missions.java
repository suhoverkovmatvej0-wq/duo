package com.duochaos.event;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.block.Blocks;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** «Друг или враг?»: раз в 3 игровых дня один случайный игрок получает тайное задание. */
public class Missions {
    private static final Random R = new Random();
    private static UUID holder;
    private static int type = -1;
    private static int kills;
    private static boolean active;

    public static void reset() {
        holder = null;
        type = -1;
        kills = 0;
        active = false;
    }

    public static void tick(ServerWorld w, List<ServerPlayerEntity> ps) {
        if (!active && w.getDayTime() % 72000L == 6000L) assign(w, ps);
        if (active && type == 0 && w.getGameTime() % 40 == 0) {
            PlayerEntity p = w.getPlayerByUuid(holder);
            if (p instanceof ServerPlayerEntity && countItem(p, Items.DIAMOND) >= 10) complete((ServerPlayerEntity) p);
        }
    }

    public static void assign(ServerWorld w, List<ServerPlayerEntity> ps) {
        if (ps.isEmpty()) return;
        ServerPlayerEntity p = ps.get(R.nextInt(ps.size()));
        holder = p.getUniqueID();
        type = R.nextInt(4);
        kills = 0;
        active = true;
        String[] text = {
                "Раздобудь 10 алмазов — и чтобы друг ничего не заподозрил.",
                "Лично убей 20 монстров. Друг не должен помогать.",
                "Незаметно поставь блок TNT рядом с другом (до 10 блоков).",
                "Приручи любое животное и спрячь его на базе."};
        p.sendStatusMessage(new StringTextComponent("§4[Тайное задание] §c" + text[type] + " §7Никому не говори!"), false);
    }

    private static int countItem(PlayerEntity p, net.minecraft.item.Item item) {
        int n = 0;
        for (ItemStack s : p.inventory.mainInventory) if (s.getItem() == item) n += s.getCount();
        return n;
    }

    private static void complete(ServerPlayerEntity p) {
        active = false;
        ItemStack reward;
        switch (R.nextInt(3)) {
            case 0: reward = new ItemStack(Items.NETHERITE_SCRAP, 4); break;
            case 1: reward = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE); break;
            default: reward = new ItemStack(Items.EMERALD_BLOCK, 4); break;
        }
        if (!p.inventory.addItemStackToInventory(reward)) p.dropItem(reward, false);
        p.sendStatusMessage(new StringTextComponent("§aЗадание выполнено! Награда твоя. Друг ничего не узнает... если не найдёт улики."), false);
        for (ServerPlayerEntity o : p.getServer().getPlayerList().getPlayers()) {
            if (o != p) o.sendStatusMessage(new StringTextComponent("§7Вы чувствуете, что кто-то из вас что-то скрывает... Ищите улики!"), false);
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!active || type != 1) return;
        if (e.getSource().getTrueSource() instanceof ServerPlayerEntity && e.getEntityLiving() instanceof IMob) {
            ServerPlayerEntity p = (ServerPlayerEntity) e.getSource().getTrueSource();
            if (p.getUniqueID().equals(holder) && ++kills >= 20) complete(p);
        }
    }

    @SubscribeEvent
    public static void onTnt(BlockEvent.EntityPlaceEvent e) {
        if (!active || type != 2 || !(e.getEntity() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getEntity();
        if (!p.getUniqueID().equals(holder) || e.getPlacedBlock().getBlock() != Blocks.TNT || !(e.getWorld() instanceof World)) return;
        for (PlayerEntity o : ((World) e.getWorld()).getPlayers()) {
            if (o != p && o.getDistanceSq(e.getPos().getX(), e.getPos().getY(), e.getPos().getZ()) < 100.0D) {
                complete(p);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent e) {
        if (!active || type != 3 || !(e.getTamer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getTamer();
        if (p.getUniqueID().equals(holder)) complete(p);
    }
}
