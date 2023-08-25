package me.karltroid.letitburnbutnotmyhouse;

import com.google.common.collect.ImmutableSet;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class LetItBurnButNotMyHouse extends JavaPlugin implements Listener
{
    CoreProtectAPI coreProtect;

    private static final Set<Material> ALWAYS_NATURAL = ImmutableSet.of( // all item types that shouldn't be protected unless special
            Material.PODZOL, Material.COARSE_DIRT, Material.DIRT_PATH, Material.FARMLAND, Material.GRASS_BLOCK,
            Material.GRASS, Material.DIRT, Material.GRAVEL, Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.SUGAR_CANE, Material.BEETROOTS, Material.TALL_GRASS, Material.NETHERRACK, Material.SWEET_BERRY_BUSH,
            Material.SAND, Material.RED_SAND, Material.SNOW, Material.TNT
    );

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);

        coreProtect = getCoreProtect();
        if (coreProtect != null) // Ensure we have access to the API
            this.getLogger().info("CoreProtect API found!");
        else
        {
            this.getLogger().severe("CoreProtect not installed on server");
            Bukkit.getPluginManager().disablePlugin(this);
        }

    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event)
    {
        Block block = event.getBlock();
        if (naturalBlock(block)) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockExplode(EntityExplodeEvent event)
    {
        if (event.blockList().isEmpty()) return;

        event.blockList().removeIf(block -> !naturalBlock(block));
    }

    public boolean naturalBlock(Block block)
    {
        if (ALWAYS_NATURAL.contains(block.getType())) return true;

        List<String[]> lookupResult = coreProtect.blockLookup(block, 0).stream()
                .filter(result -> coreProtect.parseResult(result).getBlockData().getMaterial().equals(block.getType()))
                .collect(Collectors.toList());
        if (lookupResult.isEmpty()) return true;

        int blockState = 0;
        for (String[] strings : lookupResult)
        {
            CoreProtectAPI.ParseResult result = coreProtect.parseResult(strings);

            if (result.getActionId() == 1) blockState++;
            else if (result.getActionId() == 0) blockState--;
        }

        return blockState < 1;
    }

    private CoreProtectAPI getCoreProtect()
    {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (!(plugin instanceof CoreProtect)) return null;

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
        if (!CoreProtect.isEnabled()) return null;

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 9) return null;

        return CoreProtect;
    }
}
