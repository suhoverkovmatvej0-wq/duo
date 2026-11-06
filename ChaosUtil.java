package com.duochaos.event;

import com.duochaos.entity.ChaosKingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

public class ChaosUtil {
    /** Молния-эффект + ручной урон по области (без поджогов и огня). */
    public static void strike(ServerWorld w, double x, double y, double z, float dmg, Entity ignore, boolean hitPlayers) {
        LightningBoltEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
        if (bolt != null) {
            bolt.moveForced(x, y, z);
            bolt.setEffectOnly(true);
            w.addEntity(bolt);
        }
        for (LivingEntity e : w.getEntitiesWithinAABB(LivingEntity.class,
                new AxisAlignedBB(x - 2.5, y - 2, z - 2.5, x + 2.5, y + 4, z + 2.5))) {
            if (e == ignore || e instanceof ChaosKingEntity || e instanceof ArmorStandEntity) continue;
            if (e instanceof PlayerEntity && !hitPlayers) continue;
            e.attackEntityFrom(DamageSource.LIGHTNING_BOLT, dmg);
        }
    }

    public static void tell(Iterable<? extends PlayerEntity> players, String msg) {
        ITextComponent c = new StringTextComponent(msg);
        for (PlayerEntity p : players) p.sendStatusMessage(c, false);
    }

    /** Поверхность в точке (подгружает чанк). */
    public static BlockPos surface(ServerWorld w, double x, double z) {
        int bx = MathHelper.floor(x);
        int bz = MathHelper.floor(z);
        w.getChunk(bx >> 4, bz >> 4);
        int y = w.getHeight(Heightmap.Type.MOTION_BLOCKING, bx, bz);
        return new BlockPos(bx, y, bz);
    }

    public static void place(ServerWorld w, Entity e, double x, double y, double z) {
        e.setLocationAndAngles(x, y, z, w.rand.nextFloat() * 360F, 0F);
        w.addEntity(e);
    }
}
