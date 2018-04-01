package net.clownercraft.spherepvp.player

import com.scarabcoder.commons.*
import com.scarabcoder.commons.config.configFriendlyName
import net.clownercraft.spherepvp.SpherePvP
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

enum class Messages(val message: String, vararg val placeholders: String) {

    JOIN_GAME("$RED{player} joined the game ({min}/{max})", "player", "min", "max"),
    SPECTATOR_JOIN("$GRAY${ITALIC}{player} joined as a spectator", "player"),
    WAITING("${GRAY}Waiting for players... $ITALIC({min} more required)", "min"),
    STARTING_IN("${GREEN}Game starting in {time}", "time"),
    ROUND_INPLAY("${GOLD}Collect Resources and Kill Other Players! $GRAY$ITALIC{time}s", "time"),
    ROUND_PREP("${DARK_AQUA}Choose items to carry over for next round. $GRAY$ITALIC{time}s", "time"),
    ROUND_START_COUNTDOWN("${DARK_AQUA}Start collecting resources in {time}", "time"),
    ENDING_SOON("$DARK_AQUA${BOLD}Round ends in {time} seconds!", "time"),
    PLAYER_KILL("$GREEN${BOLD}Killed {player}, +1 kills", "player"),
    MUST_BE_IN_ROUND("${RED}Command only usable while in a round."),
    GAME_IN_SESSION("${RED}Game is currently in session, please wait for it to end."),
    GAME_END("${AQUA}${BOLD}Game Over"),
    PLAYER_FIRST("${GOLD}First place winner: {player} with {kills} kills", "player", "kills");



    fun parse(vararg replacements: Any): String {
        var msg = if(!cfg.contains(message)) message else cfg.getString(name.configFriendlyName)
        cfg.set(name.configFriendlyName, msg)
        for(placeholder in placeholders.withIndex()){
            msg = msg.replace("{${placeholder.value}}", replacements[placeholder.index].toString())
        }
        return msg.colored()
    }

    companion object {
        val cfg: FileConfiguration
        val file = File(SpherePvP.plugin.dataFolder, "messages.yml")

        init {
            if(!file.exists()) {
                file.createNewFile()
                cfg = YamlConfiguration.loadConfiguration(file)
            }else
                cfg = YamlConfiguration.loadConfiguration(file)
        }

        fun save() {
            cfg.save(file)
        }

    }

}