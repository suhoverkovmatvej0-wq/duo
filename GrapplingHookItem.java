package com.duochaos.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/** ПКМ на блок в пределах 40 м — рывок к нему. */
public class GrapplingHookItem extends Item {
    private static final double RANGE = 40.0D;

    public GrapplingHookItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d end = eye.add(player.getLookVec().scale(RANGE));
        BlockRayTraceResult hit = world.rayTraceBlocks(new RayTraceContext(eye, end,
                RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, player));
        if (hit.getType() != RayTraceResult.Type.BLOCK) {
            return ActionResult.resultFail(stack);
        }
        Vector3d dir = hit.getHitVec().subtract(eye);
        double dist = dir.length();
        Vector3d motion = dir.normalize().scale(Math.min(2.2D, 0.8D + dist * 0.06D)).add(0, 0.25D, 0);
        player.setMotion(motion);
        player.fallDistance = 0;
        player.velocityChanged = true;

        if (!world.isRemote) {
            ServerWorld sw = (ServerWorld) world;
            int steps = (int) Math.min(20, dist);
            for (int i = 1; i <= steps; i++) {
                Vector3d p = eye.add(dir.scale(i / (double) steps));
                sw.spawnParticle(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(),
                    SoundEvents.ENTITY_FISHING_BOBBER_THROW, SoundCategory.PLAYERS, 1F, 0.8F);
            stack.damageItem(1, player, pl -> pl.sendBreakAnimation(hand));
        }
        player.getCooldownTracker().setCooldown(this, 20);
        return ActionResult.func_233538_a_(stack, world.isRemote());
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("ПКМ на блок: рывок к нему (до 40 блоков)").mergeStyle(TextFormatting.GRAY));
    }
}
