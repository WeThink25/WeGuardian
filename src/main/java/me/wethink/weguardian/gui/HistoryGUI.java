package me.wethink.weguardian.gui;

import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.Punishment;
import me.wethink.weguardian.util.MessageUtil;
import me.wethink.weguardian.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;


public class HistoryGUI extends FastInv {


    private static ItemStack GLASS_PANE;
    private static ItemStack PREV_PAGE;
    private static ItemStack NEXT_PAGE;
    private static ItemStack BACK_BUTTON;


    public static void initializeIcons() {
        GLASS_PANE = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();

        PREV_PAGE = new ItemBuilder(Material.ARROW)
                .name(MessageUtil.colorize("&a← Previous Page"))
                .lore("", MessageUtil.colorize("&e▶ Click to go back"))
                .build();

        NEXT_PAGE = new ItemBuilder(Material.ARROW)
                .name(MessageUtil.colorize("&aNext Page →"))
                .lore("", MessageUtil.colorize("&e▶ Click to continue"))
                .build();

        BACK_BUTTON = new ItemBuilder(Material.BARRIER)
                .name(MessageUtil.colorize("&c&lBack to Punish"))
                .lore("", MessageUtil.colorize("&e▶ Click to go back"))
                .build();
    }

    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 17,
            19, 20, 21, 22, 23, 24, 25, 26,
            28, 29, 30, 31, 32, 33, 34, 35,
            37, 38, 39, 40, 41, 42, 43, 44
    };

    private static final int ITEMS_PER_PAGE = ITEM_SLOTS.length;


    private final WeGuardian plugin;
    private final Player staff;
    private final OfflinePlayer target;
    private final List<Punishment> history;
    private final int page;

    private ItemStack headItem;
    private final List<ItemStack> punishmentItems = new ArrayList<>();

    public HistoryGUI(WeGuardian plugin, Player staff, OfflinePlayer target, List<Punishment> history) {
        this(plugin, staff, target, history, 0);
    }

    public HistoryGUI(WeGuardian plugin, Player staff, OfflinePlayer target, List<Punishment> history, int page) {
        super(54, MessageUtil.colorize("&d&lHistory &8» &e" + target.getName() + " &7(Page " + (page + 1) + ")"));

        this.plugin = plugin;
        this.staff = staff;
        this.target = target;
        this.history = history;
        this.page = page;
    }


    public void build() {
        long activeCount = history.stream().filter(Punishment::isActive).count();
        headItem = new ItemBuilder(Material.PLAYER_HEAD)
                .name(MessageUtil.colorize("&e" + target.getName()))
                .lore(
                        "",
                        MessageUtil.colorize("&7Total punishments: &f" + history.size()),
                        MessageUtil.colorize("&7Active: &f" + activeCount))
                .build();

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, history.size());

        for (int i = startIndex; i < endIndex; i++) {
            punishmentItems.add(createPunishmentItem(history.get(i)));
        }

        populateInventory();
    }



    private void populateInventory() {
        for (int i = 0; i < 9; i++) {
            setItem(i, GLASS_PANE);
            setItem(45 + i, GLASS_PANE);
        }

        setItem(4, headItem);

        for (int i = 0; i < punishmentItems.size() && i < ITEM_SLOTS.length; i++) {
            setItem(ITEM_SLOTS[i], punishmentItems.get(i));
        }

        if (page > 0) {
            setItem(45, PREV_PAGE, e -> openPageAsync(page - 1));
        }

        int maxPages = (int) Math.ceil((double) history.size() / ITEMS_PER_PAGE);
        if (page < maxPages - 1) {
            setItem(53, NEXT_PAGE, e -> openPageAsync(page + 1));
        }

        setItem(49, BACK_BUTTON, e -> PunishmentGUI.openAsync(plugin, staff, target));
    }


    private void openPageAsync(int newPage) {
        staff.closeInventory();
        plugin.getSchedulerManager().runAsync(() -> {
            HistoryGUI gui = new HistoryGUI(plugin, staff, target, history, newPage);
            gui.build();
            plugin.getSchedulerManager().runForEntity(staff, gui::open);
        });
    }


    private ItemStack createPunishmentItem(Punishment punishment) {
        Material material = getMaterialForType(punishment.getType());

        String statusColor = punishment.isActive() ? "&a" : "&c";
        String status = punishment.isActive() ? (punishment.isExpired() ? "Expired" : "Active") : "Removed";

        List<String> lore = new ArrayList<>(12);
        lore.add("");
        lore.add(MessageUtil
                .colorize("&7Type: " + punishment.getType().getColor() + punishment.getType().getDisplayName()));
        lore.add(MessageUtil
                .colorize("&7Reason: &f" + (punishment.getReason() != null ? punishment.getReason() : "None")));
        lore.add(MessageUtil.colorize("&7By: &f" + punishment.getStaffName()));
        lore.add(MessageUtil.colorize("&7Date: &f" + TimeUtil.formatDate(punishment.getCreatedAt())));
        lore.add("");

        if (punishment.getType().isTemporary()) {
            lore.add(MessageUtil.colorize("&7Duration: &f" + TimeUtil.formatRemaining(punishment.getExpiresAt())));
        } else {
            lore.add(MessageUtil.colorize("&7Duration: &fPermanent"));
        }

        lore.add(MessageUtil.colorize("&7Status: " + statusColor + status));

        if (!punishment.isActive() && punishment.getRemovedByName() != null) {
            lore.add("");
            lore.add(MessageUtil.colorize("&7Removed by: &f" + punishment.getRemovedByName()));
            lore.add(MessageUtil.colorize("&7Removed at: &f" + TimeUtil.formatDate(punishment.getRemovedAt())));
        }

        return new ItemBuilder(material)
                .name(MessageUtil.colorize(punishment.getType().getColor() + "&l" +
                        punishment.getType().getDisplayName() + " &7#" + punishment.getId()))
                .lore(lore.toArray(new String[0]))
                .build();
    }


    private static Material getMaterialForType(me.wethink.weguardian.model.PunishmentType type) {
        return switch (type) {
            case BAN -> Material.RED_WOOL;
            case TEMPBAN -> Material.ORANGE_WOOL;
            case BANIP -> Material.RED_CONCRETE;
            case TEMPBANIP -> Material.ORANGE_CONCRETE;
            case MUTE -> Material.YELLOW_WOOL;
            case TEMPMUTE -> Material.LIME_WOOL;
            case MUTEIP -> Material.YELLOW_CONCRETE;
            case TEMPMUTEIP -> Material.LIME_CONCRETE;
            case KICK -> Material.LIGHT_BLUE_WOOL;
        };
    }

    public void open() {
        open(staff);
    }


    public static void openAsync(WeGuardian plugin, Player staff, OfflinePlayer target) {
        staff.sendMessage(MessageUtil.toComponent("&7Loading punishment history..."));

        plugin.getPunishmentManager().getHistory(target.getUniqueId())
                .thenAccept(history -> {
                    HistoryGUI gui = new HistoryGUI(plugin, staff, target, history);
                    gui.build();
                    plugin.getSchedulerManager().runForEntity(staff, gui::open);
                });
    }
}
