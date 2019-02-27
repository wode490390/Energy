package cn.wode490390.nukkit.energy;

import cn.nukkit.Player;
import cn.nukkit.PlayerFood;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.permission.Permissible;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.NukkitRunnable;

public class Energy extends PluginBase implements Listener {

    private static final String ENERGY_IGNORE = "energy.ignore";

    private int consumption = 1;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(this, this);
        String t = "consumption-per-attack";
        try {
            this.consumption = this.getConfig().getInt(t);
        } catch (Exception e) {
            this.logLoadException(t);
        }
        int tick = 40;
        t = "restore-interval";
        try {
            tick = this.getConfig().getInt(t);
        } catch (Exception e) {
            this.logLoadException(t);
        }
        if (tick < 1) {
            tick = 1;
        }
        int temp = 1;
        t = "restore-energy";
        try {
            temp = this.getConfig().getInt(t);
        } catch (Exception e) {
            this.logLoadException(t);
        }
        final int restore = temp;
        new NukkitRunnable() {
            @Override
            public void run() {
                getServer().getOnlinePlayers().values().stream().filter((player) -> (!player.hasPermission(ENERGY_IGNORE) && player.isSurvival())).map((player) -> player.getFoodData()).forEach((food) -> {
                    int level = food.getLevel();
                    int max = food.getMaxLevel();
                    if (level != max) {
                        level += restore;
                        if (level > max) {
                            level = max;
                        }
                        food.setLevel(level);
                        food.sendFoodLevel();
                    }
                });
            }
        }.runTaskTimerAsynchronously(this, 0, tick);
        new MetricsLite(this);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player) {
            Player player = (Player) damager;
            if (!player.hasPermission(ENERGY_IGNORE) && player.isSurvival()) {
                PlayerFood food = player.getFoodData();
                int level = food.getLevel();
                if (level <= 0) {
                    event.setCancelled();
                } else {
                    level -= this.consumption;
                    if (level < 0) {
                        level = 0;
                    }
                    food.setLevel(level);
                    food.sendFoodLevel();
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (event.getCause() == EntityDamageEvent.DamageCause.HUNGER && entity instanceof Permissible && !((Permissible) entity).hasPermission(ENERGY_IGNORE)) {
            event.setCancelled();
        }
    }

    private void logLoadException(String t) {
        this.getLogger().alert("An error occurred while reading the configuration '" + t + "'. Use the default value.");
    }
}
