package net.clownercraft.spherepvp

import com.scarabcoder.commons.config.Config
import com.scarabcoder.commons.config.ConfigValue
import org.bukkit.Location

object GameConfig: Config("gameconfig") {

    var rounds: Int by ConfigValue(5)
    var maxPlayers: Int by ConfigValue(16)
    var lobby: Location? by ConfigValue(null)
    var hubServer: String by ConfigValue("hub")
    var arenas: ArrayList<String> by ConfigValue(ArrayList())
    var enabled: Boolean by ConfigValue(false)
    var minPlayers: Int by ConfigValue(6)
    var lobbyTime: Int by ConfigValue(15)
    var roundTime: Int by ConfigValue(360)
    var prepTime: Int by ConfigValue(60)
    var preRoundStartTime: Int by ConfigValue(10)
    var nearRoundEnd: Int by ConfigValue(10)
    var roundEndCountdown: Int by ConfigValue(5)
    var gameOverTime: Int by ConfigValue(6)

}