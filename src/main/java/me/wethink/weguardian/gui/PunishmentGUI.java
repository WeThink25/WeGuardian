package me.wethink.weguardian.gui;

import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.PunishmentType;
import me.wethink.weguardian.util.MessageUtil;
import me.wethink.weguardian.util.MessagesManager;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
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
                MessagesManager msg = WeGuardian.getInstance().getMessagesManager();

                GLASS_PANE = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                                .name(" ")
                                .build();

                List<String> banLore = msg.getMessageList("gui.punish.items.ban.lore");
                BAN_ITEM = new ItemBuilder(Material.BARRIER)
                                .name(MessageUtil.colorize(msg.getMessage("gui.punish.items.ban.name")))
                                .lore(banLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                                .build();

                List<String> tempbanLore = msg.getMessageList("gui.punish.items.tempban.lore");
                TEMP_BAN_ITEM = new ItemBuilder(Material.CLOCK)
                                .name(MessageUtil.colorize(msg.getMessage("gui.punish.items.tempban.name")))
                                .lore(tempbanLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                                .build();

                List<String> kickLore = msg.getMessageList("gui.punish.items.kick.lore");
                KICK_ITEM = new ItemBuilder(Material.LEATHER_BOOTS)
                                .name(MessageUtil.colorize(msg.getMessage("gui.punish.items.kick.name")))
                                .lore(kickLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                                .build();

                List<String> muteLore = msg.getMessageList("gui.punish.items.mute.lore");
                MUTE_ITEM = new ItemBuilder(Material.PAPER)
                                .name(MessageUtil.colorize(msg.getMessage("gui.punish.items.mute.name")))
                                .lore(muteLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                                .build();

                List<String> tempmuteLore = msg.getMessageList("gui.punish.items.tempmute.lore");
                TEMP_MUTE_ITEM = new ItemBuilder(Material.WRITABLE_BOOK)
                                .name(MessageUtil.colorize(msg.getMessage("gui.punish.items.tempmute.name")))
                                .lore(tempmuteLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                                .build();

                List<String> historyLore = msg.getMessageList("gui.punish.items.history.lore");
                HISTORY_ITEM = new ItemBuilder(Material.BOOK)
                                .name(MessageUtil.colorize(msg.getMessage("gui.punish.items.history.name")))
                                .lore(historyLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                                .build();

                List<String> closeLore = msg.getMessageList("gui.punish.items.close.lore");
                CLOSE_ITEM = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                                .name(MessageUtil.colorize(msg.getMessage("gui.punish.items.close.name")))
                                .lore(closeLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                                .build();
        }

        private static final Consumer<org.bukkit.event.inventory.InventoryClickEvent> CLOSE_HANDLER = e -> e
                        .getWhoClicked().closeInventory();

        private final WeGuardian plugin;
        private final Player staff;
        private final OfflinePlayer target;

        public PunishmentGUI(WeGuardian plugin, Player staff, OfflinePlayer target) {
                super(45, MessageUtil.colorize(plugin.getMessagesManager().getMessage("gui.punish.title", "{player}",
                                target.getName())));

                this.plugin = plugin;
                this.staff = staff;
                this.target = target;
        }

        public void build() {
                MessagesManager msg = plugin.getMessagesManager();

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

                if (staff.hasPermission(PunishmentType.BAN.getPermission())) {
                        setItem(11, BAN_ITEM, e -> openReasonInput(PunishmentType.BAN, -1));
                }
                if (staff.hasPermission(PunishmentType.TEMPBAN.getPermission())) {
                        setItem(12, TEMP_BAN_ITEM, e -> openDurationGUIAsync(PunishmentType.TEMPBAN));
                }
                if (staff.hasPermission(PunishmentType.KICK.getPermission())) {
                        setItem(13, KICK_ITEM, e -> openReasonInput(PunishmentType.KICK, -1));
                }
                if (staff.hasPermission(PunishmentType.MUTE.getPermission())) {
                        setItem(14, MUTE_ITEM, e -> openReasonInput(PunishmentType.MUTE, -1));
                }
                if (staff.hasPermission(PunishmentType.TEMPMUTE.getPermission())) {
                        setItem(15, TEMP_MUTE_ITEM, e -> openDurationGUIAsync(PunishmentType.TEMPMUTE));
                }

                setItem(31, HISTORY_ITEM, e -> {
                        staff.closeInventory();
                        staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.reason.loading-history")));
                        plugin.getPunishmentManager().getHistory(target.getUniqueId())
                                        .thenAccept(history -> {

                                                HistoryGUI gui = new HistoryGUI(plugin, staff, target, history);
                                                gui.build();
                                                plugin.getSchedulerManager().runForEntity(staff, gui::open);
                                        });
                });

                setItem(40, CLOSE_ITEM, CLOSE_HANDLER);

                String onlineStatus = target.isOnline()
                                ? msg.getMessage("online-yes")
                                : msg.getMessage("online-no");

                List<String> headLore = msg.getMessageList("gui.punish.items.player-head.lore",
                                "{uuid}", target.getUniqueId().toString().substring(0, 8) + "...",
                                "{online}", MessageUtil.colorize(onlineStatus));

                ItemStack headItem = new ItemBuilder(Material.PLAYER_HEAD)
                                .name(MessageUtil.colorize(msg.getMessage("gui.punish.items.player-head.name",
                                                "{player}", target.getName())))
                                .lore(headLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                                .build();
                setItem(4, headItem);
        }

        private void openDurationGUIAsync(PunishmentType type) {
                if (!staff.hasPermission(type.getPermission())) {
                        staff.closeInventory();
                        staff.sendMessage(
                                        MessageUtil.toComponent(plugin.getMessagesManager()
                                                        .getMessage("input.reason.no-permission")));
                        return;
                }
                staff.closeInventory();
                plugin.getSchedulerManager().runAsync(() -> {
                        DurationGUI gui = new DurationGUI(plugin, staff, target, type);
                        gui.build();
                        plugin.getSchedulerManager().runForEntity(staff, gui::open);
                });
        }

        private void openReasonInput(PunishmentType type, long durationMs) {
                if (!staff.hasPermission(type.getPermission())) {
                        staff.closeInventory();
                        staff.sendMessage(
                                        MessageUtil.toComponent(plugin.getMessagesManager()
                                                        .getMessage("input.reason.no-permission")));
                        return;
                }
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
