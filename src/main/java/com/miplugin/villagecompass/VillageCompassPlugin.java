package com.pingumc.villagecompass;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.StructureType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType; // ¡Faltaba este import!
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable; // ¡Faltaba este import!

import java.util.Arrays;

public class VillageCompassPlugin extends JavaPlugin implements Listener {

    private NamespacedKey recipeKey;
    private NamespacedKey customItemKey;

    @Override
    public void onEnable() {
        recipeKey = new NamespacedKey(this, "village_compass");
        customItemKey = new NamespacedKey(this, "is_village_compass");
        
        getServer().getPluginManager().registerEvents(this, this);
        registerVillageCompassRecipe();
        
        startTranslationTask();
        
        getLogger().info("¡Village Compass Pro con traducción en tiempo real activado!");
    }

    private void registerVillageCompassRecipe() {
        ItemStack villageCompass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) villageCompass.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text("Village Compass", NamedTextColor.GREEN));
            meta.lore(Arrays.asList(Component.text("Right-click to calibrate", NamedTextColor.GRAY)));
            
            // Ponemos la marca invisible que identificará a nuestra brújula para siempre
            meta.getPersistentDataContainer().set(customItemKey, PersistentDataType.BOOLEAN, true);
            villageCompass.setItemMeta(meta);
        }

        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, villageCompass);
        recipe.addIngredient(Material.COMPASS);
        recipe.addIngredient(Material.EMERALD);

        Bukkit.addRecipe(recipe);
    }

    private void startTranslationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Recorremos todos los jugadores conectados al servidor
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String locale = player.getLocale().toLowerCase();
                    
                    // Revisamos todo su inventario
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.COMPASS) {
                            CompassMeta meta = (CompassMeta) item.getItemMeta();
                            
                            // Si tiene nuestra marca invisible, actualizamos su idioma (¡SIN CALIBRAR!)
                            if (meta != null && meta.getPersistentDataContainer().has(customItemKey, PersistentDataType.BOOLEAN)) {
                                String finalName;
                                String loreText;

                                // Si la brújula ya tiene coordenadas guardadas (ya se usó)
                                if (meta.hasLodestone() && meta.getLodestone() != null) {
                                    Location loc = meta.getLodestone();
                                    if (locale.startsWith("es")) {
                                        finalName = "Brújula de Aldeas";
                                        loreText = "Calibrada en X: " + loc.getBlockX() + " Z: " + loc.getBlockZ();
                                    } else if (locale.startsWith("ja")) {
                                        finalName = "村のコンパス";
                                        loreText = "位置 X: " + loc.getBlockX() + " Z: " + loc.getBlockZ();
                                    } else if (locale.startsWith("zh")) {
                                        finalName = "村庄指南针";
                                        loreText = "已定位 X: " + loc.getBlockX() + " Z: " + loc.getBlockZ();
                                    } else {
                                        finalName = "Village Compass";
                                        loreText = "Calibrated at X: " + loc.getBlockX() + " Z: " + loc.getBlockZ();
                                    }
                                } else {
                                    // Si es nueva recién salida de la mesa de crafteo
                                    if (locale.startsWith("es")) {
                                        finalName = "Brújula de Aldeas";
                                        loreText = "Click derecho para calibrar";
                                    } else if (locale.startsWith("ja")) {
                                        finalName = "村のコンパス";
                                        loreText = "右クリックで位置を特定";
                                    } else if (locale.startsWith("zh")) {
                                        finalName = "村庄指南针";
                                        loreText = "右键点击以定位";
                                    } else {
                                        finalName = "Village Compass";
                                        loreText = "Right-click to calibrate";
                                    }
                                }

                                // Aplicamos la traducción silenciosa
                                meta.displayName(Component.text(finalName, NamedTextColor.GREEN));
                                meta.lore(Arrays.asList(Component.text(loreText, NamedTextColor.GRAY)));
                                item.setItemMeta(meta);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L); // 20L = 20 ticks = 1 segundo
    }

    // --- LOGICA DEL CLICK DERECHO (Solo busca y calibra) ---
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.COMPASS) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                CompassMeta meta = (CompassMeta) item.getItemMeta();
                
                // Comprobamos la marca invisible
                if (meta != null && meta.getPersistentDataContainer().has(customItemKey, PersistentDataType.BOOLEAN)) {
                    event.setCancelled(true);
                    Location playerLoc = player.getLocation();
                    String locale = player.getLocale().toLowerCase();
                    
                    // Buscamos la estructura (Solo ocurre al hacer click, consume cero recursos en el bucle)
                    Location villageLoc = player.getWorld().locateNearestStructure(playerLoc, StructureType.VILLAGE, 5000, false);
                    
                    if (villageLoc != null) {
                        meta.setLodestone(villageLoc);
                        meta.setLodestoneTracked(false);
                        item.setItemMeta(meta); // Guardamos la localización inmediatamente
                        
                        // Mensaje de éxito instantáneo según idioma
                        String successMessage;
                        if (locale.startsWith("es")) successMessage = "¡Aldea localizada con éxito!";
                        else if (locale.startsWith("ja")) successMessage = "附近の村を検出しました！";
                        else if (locale.startsWith("zh")) successMessage = "成功定位到村庄！";
                        else successMessage = "Village successfully located!";
                        
                        player.sendMessage(Component.text(successMessage, NamedTextColor.GREEN));
                    } else {
                        // Mensaje de error
                        String errorMessage;
                        if (locale.startsWith("es")) errorMessage = "No se encontraron aldeas cerca.";
                        else if (locale.startsWith("ja")) errorMessage = "近くに村は見つかりませんでした。";
                        else if (locale.startsWith("zh")) errorMessage = "未找到附近的村庄。";
                        else errorMessage = "No villages found nearby.";
                        
                        player.sendMessage(Component.text(errorMessage, NamedTextColor.RED));
                    }
                }
            }
        }
    }
}