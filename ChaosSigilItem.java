package com.duochaos.item;

import com.duochaos.DuoChaosMod;
import com.duochaos.entity.ChaosKingEntity;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

/** Печать Хаоса: в обычном мире — портал в Царство Хаоса (к боссу), в Царстве — обратно домой. */
public class ChaosSigilItem extends Item {
    public ChaosSigilItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) return ActionResult.resultSuccess(stack);

        ServerPlayerEntity sp = (ServerPlayerEntity) player;
        MinecraftServer server = sp.getServer();

        // --- Домой ---
        if (world.getDimensionKey() == DuoChaosMod.CHAOS_REALM) {
            ServerWorld ow = server.getWorld(World.OVERWORLD);
            BlockPos spawn = ow.getSpawnPoint();
            ow.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
            int y = ow.getHeight(Heightmap.Type.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
            List<ServerPlayerEntity> group = world.getEntitiesWithinAABB(ServerPlayerEntity.class, sp.getBoundingBox().grow(20));
            for (ServerPlayerEntity p : group) {
                p.teleport(ow, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, p.rotationYaw, 0F);
            }
            return ActionResult.resultSuccess(stack);
        }

        // --- К Королю ---
        long day = server.getWorld(World.OVERWORLD).getDayTime() / 24000L;
        if (!player.isCreative() && day < DuoChaosMod.SIGIL_MIN_DAY) {
            sp.sendStatusMessage(new StringTextComponent("Печать молчит... Король Хаоса проснётся на " + DuoChaosMod.SIGIL_MIN_DAY
                    + "-й день (сейчас день " + (day + 1) + ").").mergeStyle(TextFormatting.RED), true);
            return ActionResult.resultFail(stack);
        }
        ServerWorld realm = server.getWorld(DuoChaosMod.CHAOS_REALM);
        if (realm == null) {
            sp.sendStatusMessage(new StringTextComponent("Царство Хаоса не найдено. Измерение появляется только в НОВОМ мире.")
                    .mergeStyle(TextFormatting.RED), false);
            return ActionResult.resultFail(stack);
        }
        List<ServerPlayerEntity> group = world.getEntitiesWithinAABB(ServerPlayerEntity.class, sp.getBoundingBox().grow(16));
        int i = 0;
        for (ServerPlayerEntity p : group) {
            p.teleport(realm, 0.5 + i * 2, 4, 0.5, 0F, 0F);
            i++;
        }
        boolean exists = !realm.getEntitiesWithinAABB(ChaosKingEntity.class,
                new AxisAlignedBB(-100, 0, -100, 100, 256, 100), e -> !e.isClone()).isEmpty();
        if (!exists) {
            ChaosKingEntity king = DuoChaosMod.CHAOS_KING.get().create(realm);
            if (king != null) {
                king.setLocationAndAngles(0.5, 4, 16.5, 180F, 0F);
                realm.addEntity(king);
            }
        }
        if (!player.isCreative()) stack.shrink(1);
        return ActionResult.resultSuccess(stack);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("ПКМ: отправиться в Царство Хаоса (с " + DuoChaosMod.SIGIL_MIN_DAY + "-го дня)").mergeStyle(TextFormatting.DARK_PURPLE));
        tooltip.add(new StringTextComponent("Берёт с собой друзей в радиусе 16 блоков").mergeStyle(TextFormatting.GRAY));
    }
}
