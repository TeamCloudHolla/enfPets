package com.enforcedmc.pets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener
{
    private static YamlConfiguration db;
    
    public void onEnable() {
        Main.db = new YamlConfiguration();
        this.getConfig().options().copyDefaults(true);
        this.saveDefaultConfig();
        this.load();
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
    }
    
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (cmd.getName().equalsIgnoreCase("pet")) {
            if (sender.hasPermission("pet.create")) {
                if (args.length == 1) {
                    final boolean online = Bukkit.getPlayer(args[0]) != null;
                    if (online) {
                        final Player target = Bukkit.getPlayer(args[0]);
                        final ItemStack pet = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
                        final SkullMeta petMeta = (SkullMeta)pet.getItemMeta();
                        petMeta.setOwner("MHF_Blaze");
                        pet.setItemMeta((ItemMeta)petMeta);
                        target.getInventory().addItem(new ItemStack[] { this.setLoreAndUUID(pet) });
                        target.sendMessage(this.formatText(this.getConfig().getString("messages.pet")));
                    }
                    else {
                        sender.sendMessage(this.formatText("{player} is offline.").replace("{player}", args[0]));
                    }
                }
                else if (args.length == 2) {
                    if (sender instanceof Player) {
                        if (!args[0].equalsIgnoreCase("lvl")) {
                            if (!args[0].equalsIgnoreCase("level")) {
                                sender.sendMessage(this.formatText("&b- &cInsufficient arguments, try: &7/pet <player> &cor &7/pet <level/lvl> <level>"));
                                return true;
                            }
                        }
                        try {
                            final int level = Integer.valueOf(args[1]);
                            final Player p = (Player)sender;
                            if (p.getItemInHand().hasItemMeta() && p.getItemInHand().getItemMeta().hasLore() && p.getItemInHand().getItemMeta().getDisplayName().startsWith(this.formatText("&0&dExperience Pet"))) {
                                this.setLevel(this.getPetUUID(p.getItemInHand()), level - 1);
                                this.levelUp(p.getItemInHand());
                                this.setExpLore(p.getItemInHand(), p);
                                if (this.getLevel(this.getPetUUID(p.getItemInHand())) >= 250L) {
                                    p.sendMessage(this.formatText("&e(!) &cYour pet has reached the max level!"));
                                }
                            }
                        }
                        catch (NumberFormatException e) {
                            sender.sendMessage(this.formatText("&b- &cInsufficient arguments, try: &7/pet <player> &cor &7/pet <level/lvl> <level>"));
                        }
                    }
                    else {
                        sender.sendMessage(this.formatText("&cThe console cannot execute this command."));
                    }
                }
                else {
                    sender.sendMessage(this.formatText("&b- &cInsufficient arguments, try: &7/pet <player> &cor &7/pet <level/lvl> <level>"));
                }
            }
            else {
                sender.sendMessage(this.formatText("&cYou do not have permission to use that command."));
            }
            return true;
        }
        return false;
    }
    
    public void onDisable() {
        Main.db = null;
    }
    
    private String formatText(final String msg) {
        if (msg == null || msg.equals("")) {
            return "null";
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(false);
            if (e.getPlayer().getItemInHand().hasItemMeta() && e.getPlayer().getItemInHand().getItemMeta().hasLore() && e.getPlayer().getItemInHand().getItemMeta().hasDisplayName() && e.getPlayer().getItemInHand().getItemMeta().getDisplayName().startsWith(this.formatText("&0&dExperience Pet"))) {
                if (!this.isActive(this.getPetUUID(e.getPlayer().getItemInHand()))) {
                    this.setPetActive(this.getPetUUID(e.getPlayer().getItemInHand()), true);
                    e.getPlayer().sendMessage(this.formatText(this.getConfig().getString("messages.activate")).replace("{level}", String.valueOf(this.getLevel(this.getPetUUID(e.getPlayer().getItemInHand())))));
                    e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                }
                else {
                    this.setPetActive(this.getPetUUID(e.getPlayer().getItemInHand()), false);
                    e.getPlayer().sendMessage(this.formatText(this.getConfig().getString("messages.deactivate")));
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerExpChange(final PlayerExpChangeEvent e) {
        if (e.getAmount() > 0) {
            ItemStack[] contents;
            for (int length = (contents = e.getPlayer().getInventory().getContents()).length, j = 0; j < length; ++j) {
                final ItemStack i = contents[j];
                if (i != null && i.hasItemMeta() && i.getItemMeta().hasLore() && i.getItemMeta().hasDisplayName() && i.getItemMeta().getDisplayName().startsWith(this.formatText("&0&dExperience Pet")) && this.isActive(this.getPetUUID(i))) {
                    e.setAmount((int)(e.getAmount() * this.getMultiplier(this.getPetUUID(i))));
                    if (this.getLevel(this.getPetUUID(i)) >= 250L) {
                        return;
                    }
                    this.setExp(this.getPetUUID(i), this.getExp(this.getPetUUID(i)) + e.getAmount());
                    this.setExpLore(i, e.getPlayer());
                }
            }
        }
    }
    
    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        if (e.getPlayer().getItemInHand().hasItemMeta() && e.getPlayer().getItemInHand().getItemMeta().hasLore() && e.getPlayer().getItemInHand().getItemMeta().getDisplayName().startsWith(this.formatText("&0&dExperience Pet"))) {
            e.setCancelled(true);
        }
    }
    
    private void save() {
        try {
            Main.db.save("plugins/enfPets/petdatabase.yml");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void load() {
        if (Main.db == null) {
            Main.db = new YamlConfiguration();
        }
        try {
            Main.db.load("plugins/enfPets/petdatabase.yml");
        }
        catch (FileNotFoundException e3) {
            this.save();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InvalidConfigurationException e2) {
            e2.printStackTrace();
        }
    }
    
    private void setExp(final UUID uuid, final long amt) {
        Main.db.set(uuid.toString().concat(".pet.exp"), (Object)amt);
        this.save();
        this.load();
    }
    
    private void setExpLore(final ItemStack pet, final Player player) {
        final ItemMeta petMeta = pet.getItemMeta();
        final List<String> petLore = (List<String>)petMeta.getLore();
        double requiredXp;
        if (this.getLevel(this.getPetUUID(pet)) > 25L) {
            requiredXp = (int)(this.getLevel(this.getPetUUID(pet)) * 1.75 * (this.getLevel(this.getPetUUID(pet)) * 1.4 * 3.0) + 0.75);
        }
        else if (this.getLevel(this.getPetUUID(pet)) != 0L) {
            requiredXp = (int)(this.getLevel(this.getPetUUID(pet)) * 2.36 * 6.5 * 3.0 + 0.8);
        }
        else {
            requiredXp = 1.0;
        }
        final int xp = (int)this.getExp(this.getPetUUID(pet));
        final double barPercent = xp / requiredXp;
        if (barPercent < 1.0) {
            petLore.set(5, this.formatText("&eEXP: {xp} / {requiredxp}").replace("{xp}", String.valueOf(this.getExp(this.getPetUUID(pet)))).replace("{requiredxp}", String.valueOf((int)requiredXp)));
            final int barsToFill = (int)(barPercent * 30.0);
            String levelBar = "";
            for (int i = 0; i < barsToFill; ++i) {
                levelBar = String.valueOf(levelBar) + "§a\u258c";
            }
            for (int i = 0; i < 30 - barsToFill; ++i) {
                levelBar = String.valueOf(levelBar) + "§c\u258c";
            }
            petLore.set(6, levelBar);
            petMeta.setLore((List)petLore);
            pet.setItemMeta(petMeta);
        }
        else {
            petLore.set(5, this.formatText("&eEXP: 0 / {requiredxp}").replace("{requiredxp}", String.valueOf((int)requiredXp)));
            petLore.set(6, this.formatText("&c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c").replace("|", "\u258c"));
            petMeta.setLore((List)petLore);
            pet.setItemMeta(petMeta);
            this.levelUp(pet);
            player.sendMessage(this.formatText(this.getConfig().getString("messages.levelup")).replace("{level}", String.valueOf(this.getLevel(this.getPetUUID(pet)))));
        }
    }
    
    private void levelUp(final ItemStack pet) {
        if (this.getLevel(this.getPetUUID(pet)) < 250L) {
            this.setLevel(this.getPetUUID(pet), this.getLevel(this.getPetUUID(pet)) + 1L);
        }
        this.setExp(this.getPetUUID(pet), 0L);
        this.setMultiplier(this.getPetUUID(pet), (this.getLevel(this.getPetUUID(pet)) / 25.0f > 5.0f) ? 5.0f : (this.getLevel(this.getPetUUID(pet)) / 25.0f));
        this.setLevelInfo(pet);
    }
    
    private void setLevelInfo(final ItemStack pet) {
        final ItemMeta petMeta = pet.getItemMeta();
        final List<String> loreList = new ArrayList<String>();
        loreList.add(petMeta.getLore().get(0));
        petMeta.setDisplayName(this.formatText("&0&dExperience Pet &7[LVL {level}]".replace("{level}", String.valueOf(this.getLevel(this.getPetUUID(pet))))));
        int requiredXp;
        if (this.getLevel(this.getPetUUID(pet)) > 25L) {
            requiredXp = (int)(this.getLevel(this.getPetUUID(pet)) * 1.75 * (this.getLevel(this.getPetUUID(pet)) * 1.4 * 3.0) + 0.75);
        }
        else if (this.getLevel(this.getPetUUID(pet)) != 0L) {
            requiredXp = (int)(this.getLevel(this.getPetUUID(pet)) * 2.36 * 6.5 * 3.0 + 0.8);
        }
        else {
            requiredXp = 1;
        }
        for (final String s : this.getConfig().getStringList("lore")) {
            loreList.add(this.formatText(s).replace("{level}", this.formatText(String.valueOf(this.getLevel(this.getPetUUID(pet))))));
        }
        loreList.add(this.formatText("&eEXP: 0 / {requiredxp}".replace("{requiredxp}", String.valueOf(requiredXp))));
        loreList.add(this.formatText("&c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c").replace("|", "\u258c"));
        petMeta.setLore((List)loreList);
        pet.setItemMeta(petMeta);
    }
    
    private ItemStack setLoreAndUUID(final ItemStack pet) {
        final ItemMeta petMeta = pet.getItemMeta();
        petMeta.setDisplayName(this.formatText("&0&dExperience Pet &7[LVL 0]"));
        final String uuid = UUID.randomUUID().toString();
        String finalUUID = "";
        char[] charArray;
        for (int length = (charArray = uuid.toCharArray()).length, i = 0; i < length; ++i) {
            final char c = charArray[i];
            finalUUID = String.valueOf(finalUUID) + "§" + c;
        }
        final List<String> loreList = new ArrayList<String>();
        loreList.add(finalUUID);
        for (final String s : this.getConfig().getStringList("lore")) {
            loreList.add(this.formatText(s).replace("{level}", String.valueOf(0)));
        }
        loreList.add(this.formatText("&eEXP: 0 / {requiredxp}".replace("{requiredxp}", String.valueOf(0))));
        loreList.add(this.formatText("&c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c\u258c").replace("|", "\u258c"));
        petMeta.setLore((List)loreList);
        pet.setItemMeta(petMeta);
        this.addPet(UUID.fromString(uuid));
        return pet;
    }
    
    private void setPetActive(final UUID uuid, final boolean active) {
        Main.db.set(uuid.toString().concat(".pet.active"), (Object)active);
        this.save();
        this.load();
    }
    
    private void setLevel(final UUID uuid, final long amt) {
        Main.db.set(uuid.toString().concat(".pet.level"), (Object)((amt > 250L) ? 250L : amt));
        this.save();
        this.load();
    }
    
    public void setRequiredXp(final ItemStack pet) {
        int requiredXp;
        if (this.getLevel(this.getPetUUID(pet)) > 25L) {
            requiredXp = (int)(this.getLevel(this.getPetUUID(pet)) * 1.75 * (this.getLevel(this.getPetUUID(pet)) * 1.4 * 3.0) + 0.75);
        }
        else if (this.getLevel(this.getPetUUID(pet)) != 0L) {
            requiredXp = (int)(this.getLevel(this.getPetUUID(pet)) * 2.36 * 6.5 * 3.0 + 0.8);
        }
        else {
            requiredXp = 1;
        }
        Main.db.set(this.getPetUUID(pet).toString().concat(".pet.requiredxp"), (Object)requiredXp);
        this.save();
        this.load();
    }
    
    public int getRequiredXp(final ItemStack pet) {
        return Main.db.getInt(this.getPetUUID(pet).toString().concat(".pet.requiredxp"));
    }
    
    private void setMultiplier(final UUID uuid, final float multiplier) {
        Main.db.set(uuid.toString().concat(".pet.multiplier"), (Object)(1.0f + multiplier));
        this.save();
        this.load();
    }
    
    private boolean isActive(final UUID uuid) {
        return Main.db.getBoolean(uuid.toString().concat(".pet.active"));
    }
    
    private long getLevel(final UUID uuid) {
        return Main.db.getLong(uuid.toString().concat(".pet.level"));
    }
    
    private long getExp(final UUID uuid) {
        return Main.db.getLong(uuid.toString().concat(".pet.exp"));
    }
    
    private UUID getPetUUID(final ItemStack pet) {
        UUID uuid = null;
        if (pet.hasItemMeta() && pet.getItemMeta().hasLore()) {
            final List<String> lore = (List<String>)pet.getItemMeta().getLore();
            uuid = UUID.fromString(lore.get(0).replace("§", "").replace("&", ""));
        }
        return uuid;
    }
    
    private double getMultiplier(final UUID uuid) {
        return Main.db.getDouble(uuid.toString().concat(".pet.multiplier"));
    }
    
    private void addPet(final UUID uuid) {
        Main.db.set(uuid.toString().concat(".pet.level"), (Object)0);
        Main.db.set(uuid.toString().concat(".pet.exp"), (Object)0);
        Main.db.set(uuid.toString().concat(".pet.active"), (Object)false);
        Main.db.set(uuid.toString().concat(".pet.multiplier"), (Object)1.0);
        Main.db.set(uuid.toString().concat(".pet.requiredxp"), (Object)1);
        this.save();
        this.load();
    }
}
