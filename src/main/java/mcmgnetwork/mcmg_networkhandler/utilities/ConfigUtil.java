package mcmgnetwork.mcmg_networkhandler.utilities;

import com.velocitypowered.api.plugin.PluginContainer;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import mcmgnetwork.mcmg_networkhandler.MCMG_NetworkHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Description: <p>
 *  Manages the configuration of the MCMG_NetworkHandler plugin, including the maximum number of server instances
 *  allowed on the network and the port numbers of each server instance.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/10/24
 */
public class ConfigUtil
{
    private static YamlDocument config;

    /**
     * Initializes the Boosted YAML config for this plugin, providing access to the config's contents/data.
     * @param dataDirectory The directory to this plugin's data folder
     */
    public static void initializeConfig(Path dataDirectory)
    {
        try
        {
            config = YamlDocument.create(new File(dataDirectory.toFile(), "config.yml"),
                    Objects.requireNonNull(ConfigUtil.class.getResourceAsStream("/config.yml")),  // Get config from resources folder & populate defaults into config file
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(), // Config file will update automatically without user interaction
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))    // Set route to config.yml for automatic versioning
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
            );

            config.update();
            config.save();
        } catch (IOException ex)
        {
            MCMG_NetworkHandler.getLogger().error("Could not create/load plugin config! Plugin shutting down...");
            shutdownPlugin();
        }
        catch (NullPointerException ex)
        {
            MCMG_NetworkHandler.getLogger().error("Plugin .jar was built without a config file! Plugin shutting down...");
            shutdownPlugin();
        }

        MCMG_NetworkHandler.getLogger().info("MCMG_NetworkHandler was successfully configured!");
    }

    /**
     * Shuts down the plugin, preventing it from operating on the proxy server.
     */
    private static void shutdownPlugin() {
        // Shutdown plugin executor
        Optional<PluginContainer> container = MCMG_NetworkHandler.getProxy().getPluginManager().getPlugin("mcmg-network-handler");
        container.ifPresent(pluginContainer -> pluginContainer.getExecutorService().shutdown());
    }

    /**
     * @param serverType A server type recognized by the Velocity proxy server's MCMG_NetworkHandler plugin
     * @return The maximum number of servers of the specified type allowed on the network
     */
    public static int getMaxServerTypeCount(String serverType)
    { return Integer.parseInt(config.getString(Route.fromString("max-server-type-counts." + serverType))); }

    /**
     * @param serverName The name of the server type instance to retrieve the port of
     * @return The port number of the specified server type instance as a string
     */
    public static String getServerPort(String serverName)
    { return config.getString(Route.fromString("server-port." + serverName)); }
}
