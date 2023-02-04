package com.iamsoccer.plugins.lessannoyingitemframes;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LessAnnoyingItemFrames extends JavaPlugin implements Listener {

    private final Map<Player, Long> lastClick = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onItemFrameRotate(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (event.isCancelled() || player.isSneaking() || !player.hasPermission("cancelitemframerotate.enabled") && !player.hasPermission("openitemframechest.enabled") && !player.hasPermission("cancelallitemframerotate.enabled")) {
            return;
        }
        Entity entity = event.getRightClicked();
        if (entity instanceof ItemFrame frame) {
        if (frame.getItem().getType().equals(Material.AIR)) {
            return;
        }
            BlockFace attachedFace = frame.getAttachedFace();
            Block block = frame.getLocation().add(attachedFace.getDirection()).getBlock();
        if (player.hasPermission("cancelallitemframerotate.enabled")) {
            spamChecker(player, "togglecancelallitemframerotate");
            event.setCancelled(true);
            if (block.getType().equals(Material.CHEST) && player.hasPermission("openitemframechest.enabled") || block.getType().equals(Material.BARREL) && player.hasPermission("openitemframechest.enabled")) {
                BlockState state = block.getState();
                blockTypeChecker(player, state);
                }
            return;
            }
        if (block.getType().equals(Material.CHEST) || block.getType().equals(Material.BARREL)) {
        if (player.hasPermission("openitemframechest.enabled")) {
            BlockState state = block.getState();
            blockTypeChecker(player, state);
            event.setCancelled(true);
        }
        else if (player.hasPermission("cancelitemframerotate.enabled")) {
            spamChecker(player, "togglecancelitemframerotate");
            event.setCancelled(true);
            }
        }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        Player player = (Player) sender;
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        if (command.getName().equalsIgnoreCase("togglecancelitemframerotate")) {
            togglePermission(sender, user, "cancelitemframerotate", "Item frame rotation on chest/barrels canceling has been Enabled", "Item frame rotation on chest/barrels canceling has been Disabled");
            return true;
        } else if (command.getName().equalsIgnoreCase("toggleopenitemframechest")) {
            togglePermission(sender, user, "openitemframechest", "Click through item frames on chest/barrels Enabled", "Click through item frames on chest/barrels Disabled");
            return true;
        } else if (command.getName().equalsIgnoreCase("togglecancelallitemframerotate")) {
            togglePermission(sender, user, "cancelallitemframerotate", "All item frame rotation has been Enabled", "All item frame rotation has been Disabled");
            return true;
        }

        return false;
    }

    // For those players who spam the item frame trying to get it to rotate not realizing they have the frame rotate toggled
    public void spamChecker(Player player, String type) {
        long now = System.currentTimeMillis();
        if (lastClick.containsKey(player) && now - lastClick.get(player) < 2000) {
            int clickCount = 0;
            List<MetadataValue> clickCountData = player.getMetadata("clickCount");
            if (!clickCountData.isEmpty()) {
                clickCount = clickCountData.get(0).asInt();
            }
            clickCount++;
            if (clickCount >= 5) {
                player.sendMessage("Toggle the item frame rotation feature, by running /" + type);
                clickCount = 0;
            }
            player.setMetadata("clickCount", new FixedMetadataValue(this, clickCount));
        }
        lastClick.put(player, now);
    }

    // Checks what inventory block it is (We just want to check Chests and Barrels) then handle each block properly
    public void blockTypeChecker(Player player, BlockState state) {
        if (state instanceof Chest) {
            Inventory inventory = ((Chest) state).getBlockInventory();
            player.openInventory(inventory);
        } else if (state instanceof Barrel) {
            Barrel barrel = (Barrel) state;
            Inventory inventory = barrel.getInventory();
            player.openInventory(inventory);
        }
    }

    // Just so there's less spam in all the commands, adds or removes the permission after the player executes a command and sends an enabled or disabled message
    public void togglePermission(CommandSender sender, User user, String permission, String messageEnabled, String messageDisabled) {
        if (sender.hasPermission(permission + ".enabled")) {
            user.data().remove(Node.builder(permission + ".enabled").build());
            LuckPermsProvider.get().getUserManager().saveUser(user);
            sender.sendMessage(ChatColor.RED + messageDisabled);
        } else {
            user.data().add(Node.builder(permission + ".enabled").build());
            LuckPermsProvider.get().getUserManager().saveUser(user);
            sender.sendMessage(ChatColor.GREEN + messageEnabled);
            }
        }
}