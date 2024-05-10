package mcmgnetwork.mcmg_networkhandler;

import com.velocitypowered.api.plugin.PluginContainer;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class ConfigManager
{
    private static YamlDocument config;

    public static void initializeConfig(Path dataDirectory)
    {
        try
        {
            config = YamlDocument.create(new File(dataDirectory.toFile(), "config.yml"),
                    Objects.requireNonNull(ConfigManager.class.getResourceAsStream("/config.yml")),  // Get config from resources folder & populate defaults into config file
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
}
