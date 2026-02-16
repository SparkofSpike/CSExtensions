package org.Spike.CSExtensions.api;

import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

class SpikeElementsProvider {
    private static SpikeElementsAPI instance;

    static SpikeElementsAPI getAPI() {
        if (instance == null) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("CSExtensions");
            if (plugin instanceof CSExtensions && plugin.isEnabled()) {
                CSExtensions csExt = (CSExtensions) plugin;
                if (csExt.getSpikeElementsManager() != null) {
                    instance = new SpikeElementsAPIImpl(csExt.getSpikeElementsManager());
                }
            }
        }
        return instance;
    }

    static void reset() {
        instance = null;
    }
}