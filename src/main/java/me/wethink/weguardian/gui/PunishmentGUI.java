package me.wethink.weguardian.gui;

import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.PunishmentType;
import me.wethink.weguardian.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;


public class PunishmentGUI extends FastInv {


        private static ItemStack GLASS_PANE;
        private static ItemStack BAN_ITEM;
        private static ItemStack TEMP_BAN_ITEM;
        private static ItemStack KICK_ITEM;
        private static ItemStack MUTE_ITEM;
        private static ItemStack TEMP_MUTE_ITEM;
        private static ItemStack HISTORY_ITEM;
        private static ItemStack CLOSE_ITEM;


        public static void initializeIcons() {
                GLASS_PANE = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                                .name(" ")
                                .build();

                BAN_ITEM = new ItemBuilder(Material.BARRIER)
                                .name(MessageUtil.colorize("&c&lPermanent Ban"))
                                .lore(
                                                "",
                                                MessageUtil.colorize("&7Permanently ban this player"),
                                                MessageUtil.colorize("&7from the server."),
                                                "",
                                                MessageUtil.colorize("&cThis action cannot be undone!"),
                                                "",
                                                MessageUtil.colorize("&e▶ Click to apply"))
                                .build();

                TEMP_BAN_ITEM = new ItemBuilder(Material.CLOCK)
                                .name(MessageUtil.colorize("&6&lTemporary Ban"))
                                .lore(
                                                "",
                                                MessageUtil.colorize("&7Temporarily ban this player"),
                                                MessageUtil.colorize("&7for a set duration."),
                                                "",
                                                MessageUtil.colorize("&e▶ Click to select duration"))
                                .build();

                KICK_ITEM = new ItemBuilder(Material.LEATHER_BOOTS)
                                .name(MessageUtil.colorize("&9&lKick"))
                                .lore(
                                                "",
                                                MessageUtil.colorize("&7Kick this player from"),
                                                MessageUtil.colorize("&7the server."),
                                                "",
                                                MessageUtil.colorize("&e▶ Click to apply"))
                                .build();

                MUTE_ITEM = new ItemBuilder(Material.PAPER)
                                .name(MessageUtil.colorize("&e&lPermanent Mute"))
                                .lore(
                                                "",
                                                MessageUtil.colorize("&7Permanently mute this player"),
                                                MessageUtil.colorize("&7from chatting."),
                                                "",
                                                MessageUtil.colorize("&e▶ Click to apply"))
                                .build();

                TEMP_MUTE_ITEM = new ItemBuilder(Material.WRITABLE_BOOK)
                                .name(MessageUtil.colorize("&a&lTemporary Mute"))
                                .lore(
                                                "",
                                                MessageUtil.colorize("&7Temporarily mute this player"),
                                                MessageUtil.colorize("&7for a set duration."),
                                                "",
                                                MessageUtil.colorize("&e▶ Click to select duration"))
                                .build();

                HISTORY_ITEM = new ItemBuilder(Material.BOOK)
                                .name(MessageUtil.colorize("&d&lView History"))
                                .lore(
                                                "",
                                                MessageUtil.colorize("&7View this player's"),
                                                MessageUtil.colorize("&7punishment history."),
                                                "",
                                                MessageUtil.colorize("&e▶ Click to view"))
                                .build();

                CLOSE_ITEM = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                                .name(MessageUtil.colorize("&c&lClose"))
                                .lore("", MessageUtil.colorize("&e▶ Click to close"))
                                .build();
        }


        private static final Consumer<org.bukkit.event.inventory.InventoryClickEvent> CLOSE_HANDLER = e -> e
                        .getWhoClicked().closeInventory();


        private final WeGuardian plugin;
        private final Player staff;
        private final OfflinePlayer target;


        public PunishmentGUI(WeGuardian plugin, Player staff, OfflinePlayer target) {
                super(45, MessageUtil.colorize("&c&lPunish &8» &e" + target.getName()));

                this.plugin = plugin;
                this.staff = staff;
                this.target = target;
        }


        public void build() {
                for (int i = 0; i < 9; i++) {
                        setItem(i, GLASS_PANE);
                        setItem(36 + i, GLASS_PANE);
                }
                setItem(9, GLASS_PANE);
                setItem(17, GLASS_PANE);
                setItem(18, GLASS_PANE);
                setItem(26, GLASS_PANE);
                setItem(27, GLASS_PANE);
                setItem(35, GLASS_PANE);

                setItem(11, BAN_ITEM, e -> openReasonInput(PunishmentType.BAN, -1));
                setItem(12, TEMP_BAN_ITEM, e -> openDurationGUIAsync(PunishmentType.TEMPBAN));
                setItem(13, KICK_ITEM, e -> openReasonInput(PunishmentType.KICK, -1));
                setItem(14, MUTE_ITEM, e -> openReasonInput(PunishmentType.MUTE, -1));
                setItem(15, TEMP_MUTE_ITEM, e -> openDurationGUIAsync(PunishmentType.TEMPMUTE));

                setItem(31, HISTORY_ITEM, e -> {
                        staff.closeInventory();
                        staff.sendMessage(MessageUtil.toComponent("&7Loading punishment history..."));
                        plugin.getPunishmentManager().getHistory(target.getUniqueId())
                                        .thenAccept(history -> {

                                                HistoryGUI gui = new HistoryGUI(plugin, staff, target, history);
                                                gui.build();
                                                plugin.getSchedulerManager().runForEntity(staff, gui::open);
                                        });
                });

                setItem(40, CLOSE_ITEM, CLOSE_HANDLER);

                ItemStack headItem = new ItemBuilder(Material.PLAYER_HEAD)
                                .name(MessageUtil.colorize("&e" + target.getName()))
                                .lore(
                                                "",
                                                MessageUtil.colorize("&7UUID: &f"
                                                                + target.getUniqueId().toString().substring(0, 8)
                                                                + "..."),
                                                MessageUtil.colorize(
                                                                "&7Online: " + (target.isOnline() ? "&aYes" : "&cNo")),
                                                "",
                                                MessageUtil.colorize("&7Select a punishment below"))
                                .build();
                setItem(4, headItem);
        }


        private void openDurationGUIAsync(PunishmentType type) {
                staff.closeInventory();
                plugin.getSchedulerManager().runAsync(() -> {
                        DurationGUI gui = new DurationGUI(plugin, staff, target, type);
                        gui.build();
                        plugin.getSchedulerManager().runForEntity(staff, gui::open);
                });
        }

        private void openReasonInput(PunishmentType type, long durationMs) {
                staff.closeInventory();
                new ReasonInputHandler(plugin, staff, target, type, durationMs).start();
        }

        public void open() {
                open(staff);
        }


        public static void openAsync(WeGuardian plugin, Player staff, OfflinePlayer target) {
                plugin.getSchedulerManager().runAsync(() -> {
                        PunishmentGUI gui = new PunishmentGUI(plugin, staff, target);
                        gui.build();
                        plugin.getSchedulerManager().runForEntity(staff, gui::open);
                });
        }
}
