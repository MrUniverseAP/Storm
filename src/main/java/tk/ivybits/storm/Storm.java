/*
 * Storm Copyright (C) 2012 Icyene, Xiaomao, Thidox
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package tk.ivybits.storm;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import tk.ivybits.storm.nms.NMS;
import tk.ivybits.storm.utility.ReflectCommand;
import tk.ivybits.storm.utility.ReflectConfiguration;
import tk.ivybits.storm.utility.data.Statistics;
import tk.ivybits.storm.utility.data.Updater;
import tk.ivybits.storm.utility.data.WorldConfigLoader;
import tk.ivybits.storm.weather.WeatherManager;
import tk.ivybits.storm.weather.acidrain.AcidRain;
import tk.ivybits.storm.weather.blizzard.Blizzard;
import tk.ivybits.storm.weather.meteor.Meteor;
import tk.ivybits.storm.weather.quake.Earthquake;
import tk.ivybits.storm.weather.thunderstorm.ThunderStorm;
import tk.ivybits.storm.weather.volcano.Volcano;
import tk.ivybits.storm.weather.wildfire.Wildfire;

import java.util.HashMap;
import java.util.logging.Level;

/**
 * The main Storm class.
 */
public class Storm extends JavaPlugin implements Listener {

    /**
     * A HashMap containing world name and configuration object.
     */
    public static final HashMap<String, WorldVariables> wConfigs = new HashMap<String, WorldVariables>();
    /**
     * A global Random object, to avoid needless construction.
     */
    //public static final java.util.Random random = new java.util.Random();
    public static final tk.ivybits.storm.utility.math.Random random = new tk.ivybits.storm.utility.math.Random();
    /**
     * The server's plugin manager, to avoid fetching each use.
     */
    public static PluginManager pm;
    /**
     * The storm WeatherManager.
     */
    public static WeatherManager manager;
    public static boolean verbose = false;
    public static ReflectCommand commandRegistrator = null;
    public static Storm instance;

    @Override
    public void onEnable() {
        try {
            instance = this;

            GlobalVariables glob = new GlobalVariables(this, "global_variables");
            glob.load();
            verbose = glob.Verbose__Logging;

            commandRegistrator = new ReflectCommand(this);

            if (glob.Check_For_Updates)
                checkForUpdate();

            commandRegistrator.register(getClass());

            (pm = getServer().getPluginManager()).registerEvents((manager = new WeatherManager()), this); //Register texture/world events

            initConfiguration();

            Statistics.graph(this);

            MiscCommands.load();
            AcidRain.load();
            Wildfire.load();
            Blizzard.load();
            ThunderStorm.load();
            if (NMS.isSupported()) {
                Meteor.load();
                Volcano.load();
                Earthquake.load();
            } else {
                getLogger().log(Level.WARNING, "Storm is not compatible with the current MC version: meteors, volcanoes and earthquakes disabled.");
            }

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Storm failed to start.");
            e.printStackTrace();
            setEnabled(false);
        }
    }

    private void checkForUpdate() {
        Updater updater = new Updater(this, 42483, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, false);
        //Updater updater = new Updater(this, "storm", getFile(), Updater.UpdateType.NO_DOWNLOAD, false);
        Updater.UpdateResult result = updater.getResult();
        if (result == Updater.UpdateResult.UPDATE_AVAILABLE) {
            String link = updater.getLatestFileLink();
            getLogger().log(Level.INFO, String.format("Found a Storm update! You can download it from '%s'.", link));
        }
    }

    private void initConfiguration() {
        // Make per-world configuration files           
        for (World world : Bukkit.getWorlds()) {
            String name = world.getName();
            WorldVariables config = new WorldVariables(this, name, ".worlds");
            config.load();
            wConfigs.put(name, config);
        }

        pm.registerEvents(new WorldConfigLoader(this), this); //For late loading worlds loaded by world plugins al a MultiVerse
    }

    public class GlobalVariables extends ReflectConfiguration {
        public boolean Verbose__Logging = false;
        public boolean Check_For_Updates = true;

        public GlobalVariables(Plugin plugin, String name) {
            super(plugin, name);
        }
    }
}