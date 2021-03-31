package de.jeff_media.AngelChest.commands;

import de.jeff_media.AngelChest.Main;
import de.jeff_media.AngelChest.config.ConfigDumper;
import de.jeff_media.AngelChest.config.ConfigUtils;
import de.jeff_media.AngelChest.config.Permissions;
import de.jeff_media.AngelChest.data.AngelChest;
import de.jeff_media.AngelChest.data.BlacklistEntry;
import de.jeff_media.AngelChest.enums.BlacklistResult;
import de.jeff_media.AngelChest.enums.Features;
import de.jeff_media.AngelChest.utils.BlacklistUtils;
import de.jeff_media.AngelChest.utils.HologramFixer;
import de.jeff_media.AngelChest.utils.Utils;
import de.jeff_media.daddy.Daddy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public final class CommandDebug implements CommandExecutor, TabCompleter {

    private final Main main;

    public CommandDebug() {
        this.main = Main.getInstance();
    }

    private static String[] shift(String[] args) {
        return Arrays.stream(args).skip(1).toArray(String[]::new);
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!commandSender.hasPermission(Permissions.DEBUG)) {
            commandSender.sendMessage(main.messages.MSG_NO_PERMISSION);
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "on":
                    debug(commandSender, true);
                    break;
                case "off":
                    debug(commandSender, false);
                    break;
                case "blacklist":
                    blacklist(commandSender, shift(args));
                    break;
                case "config":
                    config(commandSender, shift(args));
                    break;
                case "info":
                    info(commandSender);
                    break;
                case "group":
                    group(commandSender, shift(args));
                    break;
                case "checkconfig":
                    checkconfig(commandSender);
                    break;
                case "dump":
                    dump(commandSender);
                    break;
                case "fixholograms":
                    fixholograms(commandSender);
                    break;
            }
            return true;
        }

        commandSender.sendMessage(new String[]{
                "§eAvailable debug commands:",
                "/acd on §6Enables debug mode",
                "/acd off §6Disables debug mode",
                "/acd blacklist §6Shows blacklist information",
                "/acd checkconfig §6Checks config files for errors",
                "/acd info §6Shows general debug information",
                "/acd group §6Shows group information",
                "/acd dump §6Dump debug information",
                "/acd fixholograms §6Removes dead holograms"
                //"- config"
        });

        return true;
    }

    /*private void createhologram(CommandSender commandSender) {
        Location loc = ((Player)commandSender).getLocation();
        int rand = new Random().nextInt(Integer.MAX_VALUE);
        ArmorStand entity = (ArmorStand) loc.getWorld().spawnEntity(loc,EntityType.ARMOR_STAND);
        entity.setVisible(false);
        entity.setCustomName(""+rand);
        entity.setCustomNameVisible(true);
        entity.setInvulnerable(true);
        NBTAPI.addNBT(entity, NBTTags.IS_HOLOGRAM, NBTValues.TRUE);
    }*/

    private void fixholograms(CommandSender commandSender) {
        int deadHolograms = 0;
        for(World world : Bukkit.getWorlds()) {
            deadHolograms += HologramFixer.removeDeadHolograms(world);
        }

        if(deadHolograms==0) {
            commandSender.sendMessage(new String[] {
                    ChatColor.GRAY+"There are no dead AngelChest holograms.",
                    ChatColor.GRAY+"Please note that this command can only remove holograms in loaded chunks created in AngelChest 3.3.0 or later. Join my discord to get a command that can remove all dead holograms (including those created by other plugins): "+Main.DISCORD_LINK
            });
        } else {
            commandSender.sendMessage(ChatColor.GREEN+"Removed "+deadHolograms+" dead AngelChest holograms.");
        }
    }

    private void dump(CommandSender commandSender) {
        ConfigDumper.dump(commandSender);
    }

    private void checkconfig(CommandSender commandSender) {
        commandSender.sendMessage("§6");
        commandSender.sendMessage("§6===[§bAngelChest ConfigCheck§6]===");
        commandSender.sendMessage("§6Please not that you have to run /acreload after making changes to your config.");
        List<String> errors = main.invalidConfigFiles == null ? new ArrayList<>() : Arrays.asList(main.invalidConfigFiles);
        if (main.invalidConfigFiles == null) {
            commandSender.sendMessage("§aAll your config files are valid.");
        } else {
            commandSender.sendMessage("§cSome of your config files are invalid.");
        }

        if(errors.contains("config.yml")) {
            commandSender.sendMessage("§e- config.yml: §cinvalid");
        } else {
            commandSender.sendMessage("§e- config.yml: §avalid");
        }

        if (new File(main.getDataFolder(), "groups.yml").exists()) {
            if(errors.contains("groups.yml")) {
                commandSender.sendMessage("§e- groups.yml: §cinvalid");
            } else {
                commandSender.sendMessage("§e- groups.yml: §avalid");
            }
        } else {
            commandSender.sendMessage("§e- groups.yml: §6does not exist");
        }
        if (new File(main.getDataFolder(), "blacklist.yml").exists()) {
            if(errors.contains("blacklist.yml")) {
                commandSender.sendMessage("§e- blacklist.yml: §cinvalid");
            } else {
                commandSender.sendMessage("§e- blacklist.yml: §avalid");
            }
        } else {
            commandSender.sendMessage("§e- blacklist.yml: §6does not exist");
        }
    }

    private void blacklist(CommandSender commandSender, String[] args) {

        if(!Daddy.allows(Features.GENERIC)) {
            commandSender.sendMessage(main.messages.MSG_PREMIUMONLY);
            return;
        }

        if(args.length > 0 && args[0].equalsIgnoreCase("add")) {
            if(!(commandSender instanceof Player)) {
                commandSender.sendMessage(main.messages.MSG_PLAYERSONLY);
                return;
            }
            Player player = (Player) commandSender;
            ItemStack item = player.getInventory().getItemInMainHand();

            if(Utils.isEmpty(item)) {
                player.sendMessage(ChatColor.RED+"You must hold an item in your hand.");
                return;
            }

            if(args.length<2) {
                player.sendMessage(ChatColor.RED+"You must specify a name for this blacklist entry.");
                return;
            }

            String[] lines = BlacklistUtils.addToBlacklist(item,args[1]);
            if(lines != null) {
                player.sendMessage(ChatColor.GREEN+"Added following blacklist entry:");
                player.sendMessage(lines);
                ConfigUtils.reloadCompleteConfig(true);
            } else {
                player.sendMessage(ChatColor.RED+"Blacklist already contains an entry called \""+args[1]+"\"");
            }

        } else if (args.length > 0 && args[0].equalsIgnoreCase("info")) {

            args = shift(args);

            commandSender.sendMessage(" ");
            commandSender.sendMessage("§6===[§bAngelChest Blacklist Info§6]===");

            if (!(commandSender instanceof Player) && args.length == 0) {
                commandSender.sendMessage("Use this command as player or specify a player name.");
                return;
            }
            Player player;
            boolean isAnotherPlayer = false;
            if (args.length > 0) {
                if (Bukkit.getPlayer(args[0]) == null) {
                    commandSender.sendMessage("Player " + args[0] + " not found.");
                    return;
                } else {
                    player = Bukkit.getPlayer(args[0]);
                    isAnotherPlayer = true;
                }
            } else {
                player = (Player) commandSender;
            }
            assert player != null;
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null) {
                commandSender.sendMessage((isAnotherPlayer ? player.getName() : "You") + " must hold an item in the main hand.");
                return;
            }
            ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());

            commandSender.sendMessage("§e===[§6Material§e]===");
            commandSender.sendMessage(item.getType().name().toUpperCase());
            commandSender.sendMessage("§e===[§6Item Name§e]===");
            commandSender.sendMessage(meta.hasDisplayName() ? "\"" + meta.getDisplayName().replaceAll("§", "&") + "\"" : " ");
            commandSender.sendMessage("§e===[§6Lore§e]===");
            if (meta.hasLore()) {
                for (String line : meta.getLore()) {
                    commandSender.sendMessage("- \"" + line.replaceAll("§", "&") + "\"");
                }
            } else {
                commandSender.sendMessage(" ");
            }
            commandSender.sendMessage("§e===[§6Blacklist Status§e]===");
            String blacklisted = main.isItemBlacklisted(item);
            commandSender.sendMessage(blacklisted == null ? "Not blacklisted" : "Blacklisted as \"" + blacklisted + "\"");
        } else if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
            args = shift(args);
            commandSender.sendMessage(new String[]{" ", "§6===[§bAngelChest Blacklist Test§6]==="});


            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage("You must be a player to severe this command.");
                return;
            }
            Player player = (Player) commandSender;

            if (args.length == 0) {
                commandSender.sendMessage("You must specify a blacklist definition (e.g. \"exampleAllHelmets\" from the example blacklist).");
                return;
            }

            BlacklistEntry blacklistEntry = main.itemBlacklist.get(args[0].toLowerCase());
            if (blacklistEntry == null) {
                commandSender.sendMessage("Blacklist definition \"" + args[0] + "\" not found.");
                return;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null) {
                commandSender.sendMessage("You must hold an item in the main hand.");
                return;
            }

            BlacklistResult result = blacklistEntry.matches(item);

            if (result == BlacklistResult.MATCH) {
                commandSender.sendMessage("§aThis item matches the blacklist definition \"" + result.getName() + "\"");
            } else {
                commandSender.sendMessage("§cThis item does not match the blacklist definition \"" + main.itemBlacklist.get(args[0].toLowerCase()).getName() + "\"");
                commandSender.sendMessage("§eReason: " + result.name());
            }


        } else {

            commandSender.sendMessage(new String[]{
                    "§eAvailable blacklist commands:",
                    "/acd blacklist add <name> §6Adds current item to to the blacklist as <name>",
                    "/acd blacklist info §6Shows material, name and lore of the current item including the blacklist definition it matches",
                    "/acd blacklist test <item> §6Shows whether the current item matches a given blacklist definition including the reason when it does not match"
            });
        }

    }

    private void debug(CommandSender commandSender, boolean enabled) {
        ConfigUtils.reloadCompleteConfig(true);
        main.debug = enabled;
        main.getConfig().set("debug", enabled);
        commandSender.sendMessage(ChatColor.GRAY + "AngelChest debug mode has been " + (enabled ? "enabled" : "disabled"));
    }

    private void group(CommandSender commandSender, String[] args) {
        Player player;
        if (args.length == 0) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage("Use this command as player or specify a player name.");
                return;
            } else {
                player = (Player) commandSender;
            }
        } else {
            if (Bukkit.getPlayer(args[0]) == null) {
                commandSender.sendMessage("Player " + args[0] + " not found.");
                return;
            } else {
                player = Bukkit.getPlayer(args[0]);
            }
        }

        int maxChests = main.groupUtils.getChestsPerPlayer(player);
        int duration = main.groupUtils.getDurationPerPlayer(player);
        double priceSpawn = main.groupUtils.getSpawnPricePerPlayer(player);
        double priceOpen = main.groupUtils.getOpenPricePerPlayer(player);
        double priceTeleport = main.groupUtils.getTeleportPricePerPlayer(player);
        double priceFetch = main.groupUtils.getFetchPricePerPlayer(player);
        double xpPercentage = main.groupUtils.getXPPercentagePerPlayer(player);
        int unlockDuration = main.groupUtils.getUnlockDurationPerPlayer(player);
        double spawnChance = main.groupUtils.getSpawnChancePerPlayer(player);
        int itemLoss = main.groupUtils.getItemLossPerPlayer(player);

        commandSender.sendMessage("§6Max Chests:§b " + maxChests);
        commandSender.sendMessage("§6Duration:§b " + duration);
        commandSender.sendMessage("§6Price Spawn:§b " + priceSpawn + " §8(depending on current balance)");
        commandSender.sendMessage("§6Price Open:§b " + priceOpen + " §8(depending on current balance)");
        commandSender.sendMessage("§6Price Teleport:§b " + priceTeleport + " §8(depending on current balance)");
        commandSender.sendMessage("§6Price Fetch:§b " + priceFetch + " §8(depending on current balance)");
        commandSender.sendMessage("§6XP Percentage:§b " + xpPercentage);
        commandSender.sendMessage("§6Unlock Duration:§b " + unlockDuration);
        commandSender.sendMessage("§6Spawn Chance:§b " + spawnChance);
        commandSender.sendMessage("§6Item Loss:§b " + itemLoss+" §8(depending on current inv)");

    }

    private void info(CommandSender commandSender) {
        int expectedAngelChests = main.angelChests.size();
        int realAngelChests = 0;
        int expectedHolograms = main.getAllArmorStandUUIDs().size();
        int realHolograms = 0;

        for (AngelChest angelChest : main.angelChests.values()) {
            if (angelChest != null) {
                realAngelChests++;
            }
        }

        for (UUID uuid : main.getAllArmorStandUUIDs()) {
            if (Bukkit.getEntity(uuid) != null) {
                realHolograms++;
            }
        }

        String text1 = "AngelChests: %d (%d), Holograms: %d (%d)";
        String text2 = "Watchdog: %d Holograms";

        commandSender.sendMessage(String.format(text1, realAngelChests, expectedAngelChests, realHolograms, expectedHolograms));
        commandSender.sendMessage(String.format(text2, main.watchdog.getCurrentUnsavedArmorStands()));
    }

    private void config(CommandSender commandSender, String[] args) {
        if (args.length == 0) {
            commandSender.sendMessage(new String[]{
                    "Available config commands:" +
                            "- get <option>",
                    "- set <type> <option> <value>"
            });
        }

        switch (args[0].toLowerCase()) {
            case "set":
                setConfig(commandSender, shift(args));
                break;
            case "get":
                getConfig(commandSender, shift(args));
                break;
        }
    }

    @SuppressWarnings("EmptyMethod")
    private void setConfig(CommandSender commandSender, String[] args) {
        if (args.length >= 2) {
            String node = args[0].toLowerCase();
            args = shift(args);
            String value = String.join(" ", args);
            main.getConfig().set(node, value);
            commandSender.sendMessage(String.format("Set \"%s\" to \"%s\"", node, value));
        } else {
            commandSender.sendMessage("Usage: /acd config set <option> <value>");
        }
    }

    @SuppressWarnings("EmptyMethod")
    private void getConfig(CommandSender commandSender, String[] args) {
        if (args.length == 1) {
            String node = args[0].toLowerCase();
            commandSender.sendMessage(String.format("%s = %s", node, main.getConfig().get(node).toString()));
        } else {
            commandSender.sendMessage("Usage: /acd config get <option>");
        }
    }

    private @Nullable List<String> getMatching(String[] commands, String entered) {
        List<String> list = new ArrayList<>(Arrays.asList(commands));
        list.removeIf(current -> !current.startsWith(entered));
        return list;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        String[] mainCommands = {"on", "off", "blacklist", "info", "group", "checkconfig","dump","fixholograms"};
        String[] blacklistCommands = {"info", "test","add"};

        // Debug
        /*main.verbose("args.lengh = "+args.length);
        for(int i = 0; i < args.length; i++) {
            main.verbose("args["+i+"] = "+args[i]);
        }*/

        if (args.length == 0) {
            return Arrays.asList(mainCommands);
        }
        if (args.length == 1) {
            return getMatching(mainCommands, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("blacklist")) {
            return getMatching(blacklistCommands, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("blacklist") && args[1].equalsIgnoreCase("test")) {
            String[] definedItems = new String[main.itemBlacklist.size()];
            int i = 0;
            for (BlacklistEntry blacklistEntry : main.itemBlacklist.values()) {
                definedItems[i] = blacklistEntry.getName();
                i++;
            }
            return getMatching(definedItems, args[2]);
        }

        return null;
    }
}
