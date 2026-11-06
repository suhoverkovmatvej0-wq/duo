package com.duochaos.entity;

import com.duochaos.DuoChaosMod;
import com.duochaos.event.ChaosUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.HorizontalFaceBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.SilverfishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;

/**
 * Король Хаоса. 4 фазы по здоровью:
 * 1 (100-75%) молот + мелкие монстры; 2 (75-50%) безумие: молнии, паутина, магма;
 * 3 (50-25%) клоны (настоящий — тот, у кого идёт полоса здоровья); 4 (<25%) гигант, ломает арену.
 * При 10% здоровья игроки разделяются по камерам: надо ОДНОВРЕМЕННО нажать кнопки в обеих.
 */
public class ChaosKingEntity extends MonsterEntity {
    private final ServerBossInfo bossInfo =
            new ServerBossInfo(this.getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.NOTCHED_10);
    private boolean clone;
    private int phase = 0;
    private boolean separationDone;
    private boolean separated;
    private int sepTicks;
    private final List<BlockPos> buttons = new ArrayList<>();
    private final List<BlockPos> cells = new ArrayList<>();
    private final Map<UUID, Vector3d> returnPos = new HashMap<>();

    public ChaosKingEntity(EntityType<? extends ChaosKingEntity> type, World world) {
        super(type, world);
        this.experienceValue = 200;
        this.enablePersistence();
    }

    public boolean isClone() {
        return clone;
    }

    public void makeClone() {
        this.clone = true;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(24.0D);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0D);
        this.setHealth(24.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, false));
    }

    // ---------- Удар молотом ----------
    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && target instanceof LivingEntity) {
            ((LivingEntity) target).applyKnockback(1.4F,
                    MathHelper.sin(this.rotationYaw * 0.017453292F), -MathHelper.cos(this.rotationYaw * 0.017453292F));
            if (!this.world.isRemote) {
                ((ServerWorld) this.world).spawnParticle(ParticleTypes.EXPLOSION, target.getPosX(), target.getPosY() + 1, target.getPosZ(), 1, 0, 0, 0, 0);
            }
        }
        return hit;
    }

    // ---------- Основной цикл ----------
    @Override
    public void livingTick() {
        super.livingTick();
        if (this.world.isRemote || !(this.world instanceof ServerWorld)) return;
        ServerWorld sw = (ServerWorld) this.world;

        if (clone) {
            if (this.ticksExisted > 1200) this.remove();
            return;
        }

        float ratio = this.getHealth() / this.getMaxHealth();
        bossInfo.setPercent(ratio);

        if (separated) {
            tickSeparation(sw);
            return;
        }

        int newPhase = ratio > 0.75F ? 1 : ratio > 0.5F ? 2 : ratio > 0.25F ? 3 : 4;
        if (newPhase != phase) {
            phase = newPhase;
            onPhase(sw);
        }
        if (!separationDone && ratio <= 0.10F) {
            startSeparation(sw);
            return;
        }

        int t = this.ticksExisted;
        switch (phase) {
            case 1:
                if (t % 160 == 0) summonMinions(sw, 2);
                break;
            case 2:
                if (t % 100 == 0) madness(sw);
                if (t % 200 == 0) summonMinions(sw, 1);
                break;
            case 3:
                if (t % 400 == 0 && countClones(sw) < 2) spawnClones(sw, 2 - countClones(sw));
                if (t % 300 == 0) swapWithClone(sw);
                if (t % 200 == 0) summonMinions(sw, 1);
                break;
            default:
                if (t % 40 == 0) crumble(sw);
                if (t % 60 == 0) {
                    sw.createExplosion(this, getPosX() + (rand.nextDouble() - 0.5) * 16, getPosY(),
                            getPosZ() + (rand.nextDouble() - 0.5) * 16, 2.0F, Explosion.Mode.NONE);
                }
                break;
        }
    }

    private List<PlayerEntity> players(ServerWorld sw, double r) {
        return sw.getEntitiesWithinAABB(PlayerEntity.class, this.getBoundingBox().grow(r));
    }

    private void onPhase(ServerWorld sw) {
        switch (phase) {
            case 1:
                ChaosUtil.tell(players(sw, 60), "§5[Король Хаоса] §fКто посмел войти в моё царство?!");
                break;
            case 2:
                ChaosUtil.tell(players(sw, 60), "§5[Король Хаоса] §cФАЗА БЕЗУМИЯ! §fПол под вами уже не безопасен...");
                break;
            case 3:
                ChaosUtil.tell(players(sw, 60), "§5[Король Хаоса] §eФАЗА КЛОНОВ! §fНайдите настоящего — у него идёт полоса здоровья.");
                spawnClones(sw, 2);
                break;
            default:
                ChaosUtil.tell(players(sw, 60), "§5[Король Хаоса] §4ФИНАЛ! §fЯ РАЗНЕСУ ВАШУ АРЕНУ!");
                this.addPotionEffect(new EffectInstance(Effects.STRENGTH, 99999, 1, true, false));
                this.addPotionEffect(new EffectInstance(Effects.SPEED, 99999, 0, true, false));
                for (ChaosKingEntity c : clones(sw)) c.remove();
                break;
        }
    }

    // ---------- Фаза 1 / общие помощники ----------
    private void summonMinions(ServerWorld sw, int n) {
        if (sw.getEntitiesWithinAABB(SilverfishEntity.class, this.getBoundingBox().grow(24)).size() >= 6) return;
        for (int i = 0; i < n; i++) {
            SilverfishEntity s = EntityType.SILVERFISH.create(sw);
            if (s == null) continue;
            ChaosUtil.place(sw, s, getPosX() + (rand.nextDouble() - 0.5) * 8, getPosY(), getPosZ() + (rand.nextDouble() - 0.5) * 8);
        }
    }

    // ---------- Фаза 2 ----------
    private void madness(ServerWorld sw) {
        for (PlayerEntity p : players(sw, 40)) {
            ChaosUtil.strike(sw, p.getPosX() + (rand.nextDouble() - 0.5) * 8, p.getPosY(),
                    p.getPosZ() + (rand.nextDouble() - 0.5) * 8, 6.0F, this, true);
            BlockPos bp = p.getPosition();
            if (sw.isAirBlock(bp)) sw.setBlockState(bp, Blocks.COBWEB.getDefaultState());
        }
        int fy = MathHelper.floor(getPosY()) - 1;
        for (int i = 0; i < 6; i++) {
            BlockPos f = new BlockPos(getPosX() + (rand.nextInt(21) - 10), fy, getPosZ() + (rand.nextInt(21) - 10));
            BlockState s = sw.getBlockState(f);
            if (s.getBlock() == Blocks.OBSIDIAN || s.getBlock() == Blocks.CRYING_OBSIDIAN) {
                sw.setBlockState(f, Blocks.MAGMA_BLOCK.getDefaultState());
            }
        }
    }

    // ---------- Фаза 3 ----------
    private List<ChaosKingEntity> clones(ServerWorld sw) {
        return sw.getEntitiesWithinAABB(ChaosKingEntity.class, this.getBoundingBox().grow(64), e -> e.clone && e.isAlive());
    }

    private int countClones(ServerWorld sw) {
        return clones(sw).size();
    }

    private void spawnClones(ServerWorld sw, int n) {
        for (int i = 0; i < n; i++) {
            ChaosKingEntity c = DuoChaosMod.CHAOS_KING.get().create(sw);
            if (c == null) continue;
            c.makeClone();
            ChaosUtil.place(sw, c, getPosX() + (i == 0 ? 4 : -4), getPosY(), getPosZ() + 3);
        }
    }

    private void swapWithClone(ServerWorld sw) {
        List<ChaosKingEntity> list = clones(sw);
        if (list.isEmpty()) return;
        ChaosKingEntity c = list.get(rand.nextInt(list.size()));
        Vector3d a = this.getPositionVec();
        Vector3d b = c.getPositionVec();
        this.setPositionAndUpdate(b.x, b.y, b.z);
        c.setPositionAndUpdate(a.x, a.y, a.z);
    }

    // ---------- Фаза 4 ----------
    private void crumble(ServerWorld sw) {
        int fy = MathHelper.floor(getPosY()) - 1;
        for (int i = 0; i < 8; i++) {
            for (int dy = 0; dy < 2; dy++) {
                BlockPos f = new BlockPos(getPosX() + (rand.nextInt(29) - 14), fy - dy, getPosZ() + (rand.nextInt(29) - 14));
                BlockState s = sw.getBlockState(f);
                if (s.getBlock() == Blocks.OBSIDIAN || s.getBlock() == Blocks.CRYING_OBSIDIAN || s.getBlock() == Blocks.MAGMA_BLOCK) {
                    sw.setBlockState(f, Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    // ---------- Разделение при 10% ----------
    private void startSeparation(ServerWorld sw) {
        List<PlayerEntity> ps = players(sw, 48);
        if (ps.isEmpty()) {
            separationDone = true;
            return;
        }
        separationDone = true;
        separated = true;
        sepTicks = 0;
        this.setInvulnerable(true);
        BlockPos base = this.getPosition();
        int i = 0;
        for (PlayerEntity p : ps) {
            if (!(p instanceof ServerPlayerEntity)) continue;
            int sign = (i % 2 == 0) ? 1 : -1;
            BlockPos o = new BlockPos(base.getX() + sign * (50 + (i / 2) * 14), 60, base.getZ());
            buildCell(sw, o);
            returnPos.put(p.getUniqueID(), p.getPositionVec());
            ((ServerPlayerEntity) p).teleport(sw, o.getX() + 0.5, o.getY() + 1, o.getZ() + 0.5, p.rotationYaw, 0F);
            i++;
        }
        ChaosUtil.tell(ps, "§5[Король Хаоса] §cВы разделены! §fВ каждой камере есть каменная кнопка — нажмите их ОДНОВРЕМЕННО (кнопка держится 1 секунду)!");
    }

    private void buildCell(ServerWorld sw, BlockPos o) {
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 0; dy <= 4; dy++) {
                    boolean shell = Math.abs(dx) == 3 || Math.abs(dz) == 3 || dy == 0 || dy == 4;
                    BlockState s = shell ? wall : Blocks.AIR.getDefaultState();
                    if (dy == 4 && ((dx == 0 && dz == 0) || (Math.abs(dx) == 2 && Math.abs(dz) == 2))) {
                        s = Blocks.SEA_LANTERN.getDefaultState();
                    }
                    sw.setBlockState(o.add(dx, dy, dz), s);
                }
            }
        }
        BlockPos btn = o.add(-2, 2, 0);
        sw.setBlockState(btn, Blocks.STONE_BUTTON.getDefaultState()
                .with(HorizontalFaceBlock.FACE, AttachFace.WALL)
                .with(HorizontalBlock.HORIZONTAL_FACING, Direction.EAST));
        buttons.add(btn);
        cells.add(o);
    }

    private void tickSeparation(ServerWorld sw) {
        sepTicks++;
        boolean all = !buttons.isEmpty();
        for (BlockPos b : buttons) {
            BlockState s = sw.getBlockState(b);
            if (!(s.getBlock() == Blocks.STONE_BUTTON && s.get(AbstractButtonBlock.POWERED))) {
                all = false;
                break;
            }
        }
        boolean lost = false;
        for (UUID id : returnPos.keySet()) {
            PlayerEntity p = sw.getPlayerByUuid(id);
            if (p == null || !p.isAlive()) lost = true;
        }
        if (all || lost || sepTicks > 12000) endSeparation(sw, true);
    }

    private void endSeparation(ServerWorld sw, boolean teleportBack) {
        if (teleportBack) {
            for (Map.Entry<UUID, Vector3d> en : returnPos.entrySet()) {
                PlayerEntity p = sw.getPlayerByUuid(en.getKey());
                if (p instanceof ServerPlayerEntity && p.isAlive()) {
                    Vector3d v = en.getValue();
                    ((ServerPlayerEntity) p).teleport(sw, v.x, v.y, v.z, p.rotationYaw, 0F);
                    p.sendStatusMessage(new net.minecraft.util.text.StringTextComponent("§aВы вырвались! Добейте Короля!"), false);
                }
            }
        }
        for (BlockPos o : cells) {
            for (int dx = -3; dx <= 3; dx++)
                for (int dz = -3; dz <= 3; dz++)
                    for (int dy = 0; dy <= 4; dy++)
                        sw.setBlockState(o.add(dx, dy, dz), Blocks.AIR.getDefaultState());
        }
        cells.clear();
        buttons.clear();
        returnPos.clear();
        separated = false;
        this.setInvulnerable(false);
    }

    // ---------- Смерть и награда ----------
    @Override
    public void onDeath(DamageSource src) {
        if (!this.world.isRemote && this.world instanceof ServerWorld) {
            ServerWorld sw = (ServerWorld) this.world;
            if (clone) {
                sw.spawnParticle(ParticleTypes.POOF, getPosX(), getPosY() + 1, getPosZ(), 20, 0.3, 0.6, 0.3, 0.05);
            } else {
                for (ChaosKingEntity c : clones(sw)) c.remove();
                if (separated) endSeparation(sw, true);
                this.entityDropItem(new ItemStack(DuoChaosMod.CHAOS_CROWN.get()));
                this.entityDropItem(new ItemStack(DuoChaosMod.KING_HAMMER.get()));
                this.entityDropItem(new ItemStack(Items.NETHERITE_INGOT, 2));
                this.entityDropItem(new ItemStack(Items.DIAMOND, 8));
                this.entityDropItem(new ItemStack(DuoChaosMod.CHAOS_SIGIL.get(), 2));
                ChaosUtil.tell(players(sw, 80), "§6Король Хаоса повержен! §fЗаберите награду. Печать Хаоса вернёт вас домой.");
            }
        }
        super.onDeath(src);
    }

    // ---------- Полоса босса ----------
    @Override
    public void addTrackingPlayer(ServerPlayerEntity player) {
        super.addTrackingPlayer(player);
        if (!clone) bossInfo.addPlayer(player);
    }

    @Override
    public void removeTrackingPlayer(ServerPlayerEntity player) {
        super.removeTrackingPlayer(player);
        bossInfo.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable ITextComponent name) {
        super.setCustomName(name);
        bossInfo.setName(this.getDisplayName());
    }

    @Override
    public void writeAdditional(CompoundNBT tag) {
        super.writeAdditional(tag);
        tag.putBoolean("Clone", clone);
        tag.putBoolean("SepDone", separationDone);
    }

    @Override
    public void readAdditional(CompoundNBT tag) {
        super.readAdditional(tag);
        clone = tag.getBoolean("Clone");
        separationDone = tag.getBoolean("SepDone");
        if (separationDone) this.setInvulnerable(false);
        if (this.hasCustomName()) bossInfo.setName(this.getDisplayName());
    }
}
