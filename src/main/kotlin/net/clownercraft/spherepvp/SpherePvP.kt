package net.clownercraft.spherepvp

import net.clownercraft.spherepvp.player.PlayerChest
import net.clownercraft.spherepvp.setup.registerPlayerCommands
import net.clownercraft.spherepvp.setup.registerSetupCommands
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class SpherePvP: JavaPlugin() {

    override fun onEnable() {

        if(!dataFolder.exists()) dataFolder.mkdirs()
        val arenas = File(dataFolder, "arenas/")
        if(!arenas.exists()) arenas.mkdir()
        GameConfig.save()

        this.server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        registerSetupCommands()
        registerPlayerCommands()

        Arena.loadIn()

        if(GameConfig.enabled) {
            Game.initReset()
        }

    }

    override fun onDisable() {
        Arena.arenas.forEach { it.save() }
        GameConfig.save()
        PlayerChest.reset()
    }

    companion object {
        val plugin: Plugin
            get() = Bukkit.getPluginManager().getPlugin("SpherePvP")
    }

}