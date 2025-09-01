package me.wethink.weGuardian.gui;

import me.wethink.weGuardian.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;


public class MenuItem {

    private final Material material;
    private final List<Integer> slots;
    private final String name;
    private final List<String> lore;
    private final String skullOwner;
    private final String action;
    private final String reason;
    private final String duration;
    private final boolean customInput;

    public MenuItem(Material material, List<Integer> slots, String name, List<String> lore, 
                   String skullOwner, String action, String reason, String duration, boolean customInput) {
        this.material = material;
        this.slots = slots;
        this.name = name;
        this.lore = lore;
        this.skullOwner = skullOwner;
        this.action = action;
        this.reason = reason;
        this.duration = duration;
        this.customInput = customInput;
    }


    public static MenuItem fromConfig(ConfigurationSection config, String targetPlayer) {
        if (config == null) return null;

        String materialName = config.getString("material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }


        List<Integer> slots = new ArrayList<>();
        if (config.contains("slot")) {
            slots.add(config.getInt("slot"));
        }
        if (config.contains("slots")) {
            slots.addAll(config.getIntegerList("slots"));
        }
        if (slots.isEmpty()) {
            return null;
        }

        String name = config.getString("name", "Item");
        name = MessageUtils.colorize(name.replace("{player}", targetPlayer));

        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("lore")) {
            lore.add(MessageUtils.colorize(line.replace("{player}", targetPlayer)));
        }

        String skullOwner = null;
        if (material == Material.PLAYER_HEAD && config.contains("skull-owner")) {
            skullOwner = config.getString("skull-owner", targetPlayer).replace("{player}", targetPlayer);
        }

        String action = config.getString("action", "");
        String reason = config.getString("reason", config.getString("default-reason", ""));
        String duration = config.getString("duration", config.getString("time", ""));
        boolean customInput = config.getBoolean("custom", false) || config.getBoolean("custom-reason", false);

        return new MenuItem(material, slots, name, lore, skullOwner, action, reason, duration, customInput);
    }


    public ItemStack getItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(name));

            List<Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(Component.text(line));
            }
            meta.lore(componentLore);

            if (material == Material.PLAYER_HEAD && skullOwner != null && meta instanceof SkullMeta) {
                SkullMeta skullMeta = (SkullMeta) meta;
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(skullOwner));
                meta = skullMeta;
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public Material getMaterial() {
        return material;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getSkullOwner() {
        return skullOwner;
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public String getDuration() {
        return duration;
    }

    public boolean isCustomInput() {
        return customInput;
    }


    public boolean isReasonItem() {
        return action.equals("reason") || action.startsWith("reason_") || 
               !reason.isEmpty() || isCommonReasonAction();
    }


    public boolean isDurationItem() {
        return action.equals("duration") || action.startsWith("duration_") || 
               action.startsWith("time_") || !duration.isEmpty() || 
               action.matches("\\d+[hdwmy]");
    }


    public boolean isConfirmAction() {
        return action.equals("confirm") || action.equals("execute") || action.equals("apply");
    }

    public boolean isBackAction() {
        return action.equals("back") || action.equals("cancel") || action.equals("return");
    }


    public boolean isCloseAction() {
        return action.equals("close") || action.equals("exit");
    }


    private boolean isCommonReasonAction() {
        return action.equals("cheating") || action.equals("griefing") || 
               action.equals("toxicity") || action.equals("spam") || 
               action.equals("advertising") || action.equals("exploiting") ||
               action.equals("doxxing") || action.equals("harassment") ||
               action.equals("inappropriate");
    }
}
