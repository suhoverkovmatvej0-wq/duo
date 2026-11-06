package com.duochaos.event;

import com.duochaos.DuoChaosMod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.monster.VindicatorEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

/** Случайные события мира: раз в 10–15 минут. Работают в Верхнем мире. */
public class ChaosEvents {
    public static final List<String> NAMES = Arrays.asList("chickens", "diamonds", "gravity", "slimes", "treasure", "random", "stop");
    private static final String[] POOL = {"chickens", "diamonds", "gravity", "slimes", "treasure"};
    private static final Random R = new Random();

    private static int countdown = newDelay();
    private static String active = null;
    private static int ticksLeft = 0;
    private static final List<ChickenEntity> chickens = new ArrayList<>();
    private static final List<ZombieEntity> hungry = new ArrayList<>();

    private static int newDelay() {
        return 12000 + R.nextInt(6001); // 10..15 минут
    }

    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent e) {
        countdown = newDelay();
        active = null;
        chickens.clear();
        hungry.clear();
        Missions.reset();
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.world.isRemote || e.world.getDimensionKey() != World.OVERWORLD) return;
        ServerWorld w = (ServerWorld) e.world;
        List<ServerPlayerEntity> ps = w.getPlayers();
        if (ps.isEmpty()) return;

        Missions.tick(w, ps);

        if (active == null) {
            if (--countdown <= 0) {
                start(POOL[R.nextInt(POOL.length)], w, ps);
            }
            return;
        }
        tickActive(w, ps);
    }

    /** Для команды. */
    public static boolean force(String name, ServerWorld w) {
        List<ServerPlayerEntity> ps = w.getPlayers();
        if (name.equals("stop")) {
            if (active != null) finish(w, ps);
            return true;
        }
        if (ps.isEmpty()) return false;
        if (active != null) finish(w, ps);
        if (name.equals("random")) name = POOL[R.nextInt(POOL.length)];
        if (!Arrays.asList(POOL).contains(name)) return false;
        start(name, w, ps);
        return true;
    }

    // ================= СТАРТ =================
    private static void start(String name, ServerWorld w, List<ServerPlayerEntity> ps) {
        active = name;
        int extra = Math.min((int) (w.getDayTime() / 24000L) / 8, 3); // чем позже — тем жёстче
        extraMobs(w, ps);
        switch (name) {
            case "chickens": {
                ticksLeft = 1200;
                ChaosUtil.tell(ps, "§6[Хаос] §eНашествие куриц! Они быстрые и бьют с отбрасыванием — держитесь вместе!");
                for (ServerPlayerEntity p : ps) {
                    for (int i = 0; i < 4 + extra; i++) {
                        Entity en = spawnNear(w, p, EntityType.CHICKEN, 12, 20);
                        if (en instanceof ChickenEntity) {
                            ChickenEntity c = (ChickenEntity) en;
                            c.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
                            c.enablePersistence();
                            c.setCustomName(new StringTextComponent("Злая курица"));
                            chickens.add(c);
                        }
                    }
                }
                break;
            }
            case "diamonds": {
                ticksLeft = 600;
                ChaosUtil.tell(ps, "§6[Хаос] §bАлмазный дождь! Но вместе с ним пришли голодные монстры...");
                break;
            }
            case "gravity": {
                ticksLeft = 1200;
                ChaosUtil.tell(ps, "§6[Хаос] §dПеревёрнутый мир! 60 секунд гравитация тянет ВВЕРХ.");
                break;
            }
            case "slimes": {
                ticksLeft = 1200;
                ChaosUtil.tell(ps, "§6[Хаос] §aСлизневая эпидемия! Слизни растут и размножаются.");
                for (ServerPlayerEntity p : ps) {
                    for (SlimeEntity s : w.getEntitiesWithinAABB(SlimeEntity.class, p.getBoundingBox().grow(64))) {
                        s.setSlimeSize(Math.min(Math.max(s.getSlimeSize(), 2) + 2, 4), true);
                    }
                    for (int i = 0; i < 2; i++) {
                        Entity en = spawnNear(w, p, EntityType.SLIME, 8, 16);
                        if (en instanceof SlimeEntity) ((SlimeEntity) en).setSlimeSize(3, true);
                    }
                }
                break;
            }
            default: { // treasure
                ticksLeft = 20;
                spawnTreasure(w, ps, extra);
                break;
            }
        }
    }

    private static void extraMobs(ServerWorld w, List<ServerPlayerEntity> ps) {
        if (R.nextInt(100) < 40) {
            ServerPlayerEntity p = ps.get(R.nextInt(ps.size()));
            spawnNear(w, p, DuoChaosMod.KILLER_DUCK.get(), 14, 22);
            p.sendStatusMessage(new StringTextComponent("§eГде-то рядом слышно подозрительное кряканье..."), false);
        }
        if (R.nextInt(100) < 40) {
            ServerPlayerEntity p = ps.get(R.nextInt(ps.size()));
            spawnNear(w, p, DuoChaosMod.THIEF.get(), 14, 22);
            p.sendStatusMessage(new StringTextComponent("§eКто-то крадётся поблизости... берегите вещи!"), false);
        }
    }

    // ================= ТИК =================
    private static void tickActive(ServerWorld w, List<ServerPlayerEntity> ps) {
        ticksLeft--;
        switch (active) {
            case "chickens":
                if (w.getGameTime() % 4 == 0) chickenTick(w);
                break;
            case "diamonds":
                diamondTick(w, ps);
                break;
            case "gravity":
                for (ServerPlayerEntity p : ps) {
                    for (LivingEntity e : w.getEntitiesWithinAABB(LivingEntity.class, p.getBoundingBox().grow(40),
                            x -> x instanceof PlayerEntity || x instanceof MobEntity)) {
                        if (e.getPosY() > 200) continue;
                        Vector3d m = e.getMotion();
                        e.setMotion(m.x, Math.min(m.y + 0.13D, 0.18D), m.z);
                        e.fallDistance = 0;
                        e.velocityChanged = true;
                    }
                }
                break;
            case "slimes":
                if (ticksLeft % 200 == 0) slimeBreed(w, ps);
                break;
            default:
                break;
        }
        if (ticksLeft <= 0) finish(w, ps);
    }

    private static void chickenTick(ServerWorld w) {
        chickens.removeIf(c -> !c.isAlive());
        for (ChickenEntity c : chickens) {
            PlayerEntity p = w.getClosestPlayer(c, 48.0D);
            if (p == null) continue;
            c.getNavigator().tryMoveToEntityLiving(p, 1.8D);
            CompoundNBT d = c.getPersistentData();
            int cd = d.getInt("cd");
            if (cd > 0) d.putInt("cd", cd - 1);
            if (c.getDistanceSq(p) < 6.25D && cd <= 0) {
                p.attackEntityFrom(DamageSource.causeMobDamage(c), 2.0F);
                p.applyKnockback(1.6F, c.getPosX() - p.getPosX(), c.getPosZ() - p.getPosZ());
                p.velocityChanged = true;
                d.putInt("cd", 6);
            }
        }
    }

    private static void diamondTick(ServerWorld w, List<ServerPlayerEntity> ps) {
        if (ticksLeft % 20 == 0) {
            for (ServerPlayerEntity p : ps) {
                ItemStack st = R.nextInt(10) < 7 ? new ItemStack(Items.DIAMOND) : new ItemStack(Items.EMERALD);
                ItemEntity ie = new ItemEntity(w, p.getPosX() + (R.nextDouble() - 0.5) * 12, p.getPosY() + 18,
                        p.getPosZ() + (R.nextDouble() - 0.5) * 12, st);
                w.addEntity(ie);
            }
        }
        if (ticksLeft % 200 == 0) {
            hungry.removeIf(z -> !z.isAlive());
            if (hungry.size() < 8) {
                for (ServerPlayerEntity p : ps) {
                    Entity en = spawnNear(w, p, EntityType.ZOMBIE, 10, 16);
                    if (en instanceof ZombieEntity) {
                        ZombieEntity z = (ZombieEntity) en;
                        z.setCustomName(new StringTextComponent("Голодный монстр"));
                        z.addPotionEffect(new EffectInstance(Effects.SPEED, 600, 0));
                        hungry.add(z);
                    }
                }
            }
        }
    }

    private static void slimeBreed(ServerWorld w, List<ServerPlayerEntity> ps) {
        List<SlimeEntity> all = new ArrayList<>();
        for (ServerPlayerEntity p : ps) all.addAll(w.getEntitiesWithinAABB(SlimeEntity.class, p.getBoundingBox().grow(64)));
        if (all.isEmpty() || all.size() >= 14) return;
        for (int i = 0; i < Math.min(3, all.size()); i++) {
            SlimeEntity src = all.get(R.nextInt(all.size()));
            SlimeEntity s = EntityType.SLIME.create(w);
            if (s == null) continue;
            s.setSlimeSize(2, true);
            ChaosUtil.place(w, s, src.getPosX() + (R.nextDouble() - 0.5), src.getPosY(), src.getPosZ() + (R.nextDouble() - 0.5));
        }
    }

    // ================= СОКРОВИЩЕ =================
    private static void spawnTreasure(ServerWorld w, List<ServerPlayerEntity> ps, int extra) {
        ServerPlayerEntity p = ps.get(R.nextInt(ps.size()));
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = R.nextDouble() * Math.PI * 2;
            double r = 45 + R.nextInt(26);
            BlockPos pos = ChaosUtil.surface(w, p.getPosX() + Math.cos(a) * r, p.getPosZ() + Math.sin(a) * r);
            if (w.getBlockState(pos.down()).getMaterial().isLiquid() || !w.isAirBlock(pos)) continue;

            w.setBlockState(pos, Blocks.CHEST.getDefaultState());
            TileEntity te = w.getTileEntity(pos);
            if (te instanceof ChestTileEntity) fillChest((ChestTileEntity) te);

            for (int g = 0; g < 1 + (extra > 1 ? 1 : 0); g++) {
                VindicatorEntity v = EntityType.VINDICATOR.create(w);
                if (v == null) continue;
                v.setCustomName(new StringTextComponent("Страж сокровищ"));
                v.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60.0D);
                v.setHealth(60.0F);
                v.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.IRON_AXE));
                v.addPotionEffect(new EffectInstance(Effects.GLOWING, 20 * 60 * 10, 0, true, false));
                v.addPotionEffect(new EffectInstance(Effects.RESISTANCE, 20 * 60 * 10, 0, true, false));
                v.enablePersistence();
                ChaosUtil.place(w, v, pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5 + g);
            }
            ChaosUtil.tell(ps, "§6[Хаос] §eПоявился сундук с легендарным лутом! Координаты: §f" + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                    + " §e— его охраняет Страж сокровищ.");
            return;
        }
        ChaosUtil.tell(ps, "§6[Хаос] §7Сокровища спрятались слишком глубоко... в этот раз ничего.");
    }

    private static void fillChest(ChestTileEntity chest) {
        List<ItemStack> loot = new ArrayList<>();
        loot.add(new ItemStack(Items.DIAMOND, 4 + R.nextInt(5)));
        loot.add(new ItemStack(Items.EMERALD, 6 + R.nextInt(10)));
        loot.add(new ItemStack(Items.GOLDEN_APPLE, 2 + R.nextInt(3)));
        loot.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        loot.add(new ItemStack(Items.NETHERITE_SCRAP, 2));
        loot.add(new ItemStack(DuoChaosMod.SLIME_BOMB.get(), 4));
        List<RegistryObject<Item>> rare = Arrays.asList(DuoChaosMod.RAINBOW_SWORD, DuoChaosMod.THUNDER_HAMMER,
                DuoChaosMod.TELEPORT_STAFF, DuoChaosMod.GRAPPLING_HOOK);
        loot.add(new ItemStack(rare.get(R.nextInt(rare.size())).get()));
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 27; i++) slots.add(i);
        Collections.shuffle(slots, R);
        for (int i = 0; i < loot.size(); i++) chest.setInventorySlotContents(slots.get(i), loot.get(i));
    }

    // ================= КОНЕЦ =================
    private static void finish(ServerWorld w, List<ServerPlayerEntity> ps) {
        String done = active;
        active = null;
        countdown = newDelay();
        for (ChickenEntity c : chickens) c.remove();
        chickens.clear();
        if ("gravity".equals(done)) {
            for (ServerPlayerEntity p : ps) p.addPotionEffect(new EffectInstance(Effects.SLOW_FALLING, 600, 0));
            ChaosUtil.tell(ps, "§6[Хаос] §7Гравитация вернулась. Вам дали Медленное падение — приземляйтесь аккуратно.");
        } else if (!"treasure".equals(done)) {
            ChaosUtil.tell(ps, "§6[Хаос] §7Событие закончилось. Следующее — через 10–15 минут.");
        }
    }

    // ================= ПОМОЩНИКИ =================
    public static Entity spawnNear(ServerWorld w, PlayerEntity p, EntityType<?> type, int minR, int maxR) {
        double a = R.nextDouble() * Math.PI * 2;
        double r = minR + R.nextInt(maxR - minR + 1);
        BlockPos pos = ChaosUtil.surface(w, p.getPosX() + Math.cos(a) * r, p.getPosZ() + Math.sin(a) * r);
        Entity e = type.create(w);
        if (e == null) return null;
        ChaosUtil.place(w, e, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return e;
    }
}
