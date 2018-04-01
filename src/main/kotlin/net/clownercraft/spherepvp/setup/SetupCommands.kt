package net.clownercraft.spherepvp.setup

import com.scarabcoder.commandapi2.ArgumentParsers
import com.scarabcoder.commandapi2.Command
import com.scarabcoder.commandapi2.CommandRegistry
import com.scarabcoder.commandapi2.exception.ArgumentParseException
import com.scarabcoder.commons.DARK_AQUA
import com.scarabcoder.commons.GRAY
import com.scarabcoder.commons.GREEN
import com.scarabcoder.commons.RED
import net.clownercraft.spherepvp.Arena
import net.clownercraft.spherepvp.GameConfig
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

object SetupCommands {

    @Command(permission = "spherepvp.setup")
    fun newarena(sender: Player, name: String) {
        if(Bukkit.getWorld(name) == null){
            sender.sendMessage("${RED}A world must exist with the same name as the arena!")
            return
        }
        Arena.new(name)
        sender.sendMessage("${GREEN}Created new arena $name.")
    }

    @Command(permission = "spherepvp.setup")
    fun removearena(sender: Player, arena: Arena) {
        Arena.remove(arena.name)
        sender.sendMessage("${GREEN}Removed arena ${arena.name}")
    }

    @Command(permission = "spherepvp.setup")
    fun setlobby(sender: Player) {
        GameConfig.lobby = sender.location
        sender.sendMessage("${GREEN}Set lobby location to current location.")
    }

    @Command(permission = "spherepvp.setup")
    fun addspawn(sender: Player, arena: Arena, round: Int) {
        if(round !in 1..5){
            sender.sendMessage("${RED}Round must be 1-5!")
            return
        }

        arena.spawnLocations[round]!!.add(sender.location)
        sender.sendMessage("${GREEN}Added spawn #${arena.spawnLocations[round]!!.lastIndex} to ${arena.name} round #$round")
    }

    @Command(permission = "spherepvp.setup")
    fun removespawn(sender: Player, arena: Arena, round: Int, spawn: Int? = null) {
        if(round !in 1..5) {
            sender.sendMessage("${RED}Round must be 1-5!")
            return
        }
        val spawn: Int = spawn ?: arena.spawnLocations[round]!!.lastIndex
        if(arena.spawnLocations[round]!!.lastIndex < spawn || spawn < 0) {
            sender.sendMessage("${RED}Invalid spawn number!")
            return
        }

        arena.spawnLocations[round]!!.removeAt(spawn)
        sender.sendMessage("${GREEN}Removed spawn $spawn from arena ${arena.name} round #$round")
    }

    @Command(permission = "spherepvp.setup")
    fun listspawns(sender: Player, arena: Arena, round: Int) {
        if(round !in 1..5) {
            sender.sendMessage("${RED}Round must be 1-5!")
            return
        }
        sender.sendMessage("$GRAY----- ${DARK_AQUA}Spawns for ${arena.name} round $round $GRAY-----")
        for((index, spawn) in arena.spawnLocations[round]!!.withIndex()) {
            val msg = TextComponent("x${spawn.blockX} y${spawn.blockY} z${spawn.blockZ} #$index")
            msg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpspawn ${arena.name} $round $index")
            msg.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Arrays.asList(TextComponent("Click to teleport")).toTypedArray())
            sender.spigot().sendMessage(msg)
        }
    }

    @Command(permission = "spherepvp.setup")
    fun tpspawn(sender: Player, arena: Arena, round: Int, spawn: Int) {
        if(round !in 1..5) {
            sender.sendMessage("${RED}Round must be 1-5!")
            return
        }
        if(arena.spawnLocations[round]!!.lastIndex < spawn){
            sender.sendMessage("${RED}Spawn #$spawn does not exist!")
            return
        }
        sender.teleport(arena.spawnLocations[round]!![spawn])
    }

    @Command(permission = "spherepvp.setup")
    fun enable(sender: Player, arena: Arena) {
        arena.enabled = true
        sender.sendMessage("${GREEN}Enabled arena ${arena.name}")
    }

    @Command(permission = "spherepvp.setup")
    fun disable(sender: Player, arena: Arena) {
        arena.enabled = false
        sender.sendMessage("${GREEN}Disabled arena ${arena.name}")
    }

    @Command(permission = "spherepvp.setup")
    fun enablegame(sender: Player) {
        GameConfig.enabled = true
        sender.sendMessage("${GREEN}Game enabled, restart server to take effect!")
    }

    @Command(permission = "spherepvp.setup")
    fun disablegame(sender: Player) {
        GameConfig.enabled = false
        sender.sendMessage("${GREEN}Game disabled, restart server to take effect!")
    }

}


internal fun registerSetupCommands() {

    ArgumentParsers.registerArgument(Arena::class) { arg ->
        Arena[arg] ?: throw ArgumentParseException("Arena $arg does not exist!")
    }

    CommandRegistry.registerMultiCommands(SetupCommands)


}