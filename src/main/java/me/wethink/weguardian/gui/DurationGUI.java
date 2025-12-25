package me.wethink.weguardian.gui;

import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.PunishmentType;
import me.wethink.weguardian.util.MessageUtil;
import me.wethink.weguardian.util.MessagesManager;
import me.wethink.weguardian.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DurationGUI extends FastInv {

    private static ItemStack GLASS_PANE;
    private static ItemStack BACK_BUTTON;
    private static ItemStack PERM_ITEM;
    private static ItemStack CUSTOM_ITEM;

    private static ItemStack HOUR_1;
    private static ItemStack HOURS_6;
    private static ItemStack DAY_1;
    private static ItemStack DAYS_3;
    private static ItemStack WEEK_1;
    private static ItemStack DAYS_30;
    private static ItemStack DAYS_90;

    private static long DURATION_1H;
    private static long DURATION_6H;
    private static long DURATION_1D;
    private static long DURATION_3D;
    private static long DURATION_7D;
    private static long DURATION_30D;
    private static long DURATION_90D;

    public static void initializeIcons() {
        MessagesManager msg = WeGuardian.getInstance().getMessagesManager();

        DURATION_1H = TimeUtil.parseDuration("1h");
        DURATION_6H = TimeUtil.parseDuration("6h");
        DURATION_1D = TimeUtil.parseDuration("1d");
        DURATION_3D = TimeUtil.parseDuration("3d");
        DURATION_7D = TimeUtil.parseDuration("7d");
        DURATION_30D = TimeUtil.parseDuration("30d");
        DURATION_90D = TimeUtil.parseDuration("90d");

        GLASS_PANE = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();

        List<String> backLore = msg.getMessageList("gui.duration.items.back.lore");
        BACK_BUTTON = new ItemBuilder(Material.ARROW)
                .name(MessageUtil.colorize(msg.getMessage("gui.duration.items.back.name")))
                .lore(backLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                .build();

        List<String> permLore = msg.getMessageList("gui.duration.items.permanent.lore");
        PERM_ITEM = new ItemBuilder(Material.BEDROCK)
                .name(MessageUtil.colorize(msg.getMessage("gui.duration.items.permanent.name")))
                .lore(permLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                .build();

        List<String> customLore = msg.getMessageList("gui.duration.items.custom.lore");
        CUSTOM_ITEM = new ItemBuilder(Material.NAME_TAG)
                .name(MessageUtil.colorize(msg.getMessage("gui.duration.items.custom.name")))
                .lore(customLore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                .build();

        HOUR_1 = createDurationItem(msg, "1h", Material.LIME_DYE, DURATION_1H);
        HOURS_6 = createDurationItem(msg, "6h", Material.YELLOW_DYE, DURATION_6H);
        DAY_1 = createDurationItem(msg, "1d", Material.ORANGE_DYE, DURATION_1D);
        DAYS_3 = createDurationItem(msg, "3d", Material.RED_DYE, DURATION_3D);
        WEEK_1 = createDurationItem(msg, "7d", Material.PURPLE_DYE, DURATION_7D);
        DAYS_30 = createDurationItem(msg, "30d", Material.MAGENTA_DYE, DURATION_30D);
        DAYS_90 = createDurationItem(msg, "90d", Material.BLUE_DYE, DURATION_90D);
    }

    private static ItemStack createDurationItem(MessagesManager msg, String key, Material material, long durationMs) {
        String name = msg.getMessage("gui.duration.items." + key + ".name");
        List<String> lore = msg.getMessageList("gui.duration.items." + key + ".lore",
                "{duration}", TimeUtil.formatDuration(durationMs));
        return new ItemBuilder(material)
                .name(MessageUtil.colorize(name))
                .lore(lore.stream().map(MessageUtil::colorize).toArray(String[]::new))
                .build();
    }

    private final WeGuardian plugin;
    private final Player staff;
    private final OfflinePlayer target;
    private final PunishmentType type;

    public DurationGUI(WeGuardian plugin, Player staff, OfflinePlayer target, PunishmentType type) {
        super(36, MessageUtil
                .colorize(plugin.getMessagesManager().getMessage("gui.duration.title", "{player}", target.getName())));

        this.plugin = plugin;
        this.staff = staff;
        this.target = target;
        this.type = type;
    }

    public void build() {
        MessagesManager msg = plugin.getMessagesManager();

        for (int i = 0; i < 9; i++) {
            setItem(i, GLASS_PANE);
            setItem(27 + i, GLASS_PANE);
        }

        setItem(10, HOUR_1, e -> selectDuration(DURATION_1H));
        setItem(11, HOURS_6, e -> selectDuration(DURATION_6H));
        setItem(12, DAY_1, e -> selectDuration(DURATION_1D));
        setItem(13, DAYS_3, e -> selectDuration(DURATION_3D));
        setItem(14, WEEK_1, e -> selectDuration(DURATION_7D));
        setItem(15, DAYS_30, e -> selectDuration(DURATION_30D));
        setItem(16, DAYS_90, e -> selectDuration(DURATION_90D));

        setItem(22, PERM_ITEM, e -> {
            staff.closeInventory();
            PunishmentType permType = type == PunishmentType.TEMPBAN ? PunishmentType.BAN : PunishmentType.MUTE;
            if (!staff.hasPermission(permType.getPermission())) {
                staff.sendMessage(
                        MessageUtil.toComponent(msg.getMessage("input.custom-duration.no-permission-permanent")));
                return;
            }
            new ReasonInputHandler(plugin, staff, target, permType, -1).start();
        });

        setItem(31, CUSTOM_ITEM, e -> {
            staff.closeInventory();
            new CustomDurationHandler(plugin, staff, target, type).start();
        });

        setItem(27, BACK_BUTTON, e -> PunishmentGUI.openAsync(plugin, staff, target));
    }

    private void selectDuration(long durationMs) {
        staff.closeInventory();
        new ReasonInputHandler(plugin, staff, target, type, durationMs).start();
    }

    public void open() {
        open(staff);
    }

    public static void openAsync(WeGuardian plugin, Player staff, OfflinePlayer target, PunishmentType type) {
        plugin.getSchedulerManager().runAsync(() -> {
            DurationGUI gui = new DurationGUI(plugin, staff, target, type);
            gui.build();
            plugin.getSchedulerManager().runForEntity(staff, gui::open);
        });
    }
}
