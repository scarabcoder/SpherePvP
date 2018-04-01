package net.clownercraft.spherepvp

import com.scarabcoder.commons.*
import net.clownercraft.spherepvp.Game.Period.*
import net.clownercraft.spherepvp.player.FreezeHandler
import net.clownercraft.spherepvp.player.Messages
import org.apache.commons.io.FileUtils
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.io.File
import java.util.*
import kotlin.collections.HashMap

object Game: Listener {

    var round = 1
    /**
     * Includes spectators!
     */
    val players: List<Player>
        get() = Bukkit.getOnlinePlayers().toList()
    val ingamePlayers: List<Player>
        get() = players.filter{ !it.isSpectatingGame }
    var status: Status = Status.WAITING
    val spectators = ArrayList<UUID>()
    lateinit var arena: Arena
    val world
        get() = Bukkit.getWorld("arena_${arena.name}")
    var timerStart: Long = 0
    var lastSecond: Long = 0
    var bossbar: BossBar = Bukkit.getServer().createBossBar(Messages.WAITING.parse(GameConfig.minPlayers), BarColor.BLUE, BarStyle.SOLID)
    var gamePeriod = GAME_WAITING
        set(value) {
            timerStart = System.currentTimeMillis()
            lastSecond = System.currentTimeMillis()
            field = value
        }
    val hasBeenSecond: Boolean
        get() {
            if(System.currentTimeMillis() - lastSecond >= 1000){
                lastSecond = System.currentTimeMillis()
                return true
            }
            return false
        }
    val timeLeft: Long
        get() {
            var until: Number = when(gamePeriod){

                Game.Period.GAME_WAITING -> System.currentTimeMillis()
                Game.Period.GAME_STARTING -> GameConfig.lobbyTime
                Game.Period.PRE_ROUND -> GameConfig.prepTime
                Game.Period.IN_ROUND -> GameConfig.roundTime
                Game.Period.ROUND_COUNTDOWN -> GameConfig.preRoundStartTime
                Game.Period.GAME_OVER -> GameConfig.gameOverTime
            }
            return until.toLong() * 1000 - (System.currentTimeMillis() - timerStart)
        }
    val timeLeftSeconds: Double
        get() = timeLeft / 1000.toDouble()
    val timeSince: Long
        get() = System.currentTimeMillis() - timerStart
    val timeSinceSeconds: Double
        get() = timeSince / 1000.toDouble()
    val playerKills = HashMap<UUID, Int>()

    init {
        Bukkit.getPluginManager().registerEvents(Game, SpherePvP.plugin)
        doTimer(0) {
            loop()
        }
    }

    private fun loop() {
        when(status) {
            Status.WAITING ->  {

                if(gamePeriod == GAME_WAITING && GameConfig.minPlayers - players.size <= 0){
                    gamePeriod = GAME_STARTING
                    lastSecond = System.currentTimeMillis()
                    timerStart = System.currentTimeMillis()

                    players.sendMessage(Messages.STARTING_IN.parse(GameConfig.lobbyTime))
                }else if(gamePeriod == GAME_STARTING && timeLeft <= 0){
                    status = Status.INGAME
                    gamePeriod = ROUND_COUNTDOWN
                    teleportAll(1)
                    ingamePlayers.forEach { FreezeHandler.freezePlayer(it) }
                }
            }
            Game.Status.INGAME -> {
                when(gamePeriod) {
                    ROUND_COUNTDOWN -> {
                        if(timeLeft <= 0) {
                            gamePeriod = Period.IN_ROUND
                            ingamePlayers.forEach { FreezeHandler.unfreeze(it) }
                            ingamePlayers.forEach { it.playSound(it.location, Sound.ENTITY_ENDERDRAGON_GROWL, 1f, 1.2f) }
                        }

                    }
                    IN_ROUND -> {
                        if(Math.ceil(timeLeftSeconds).toInt() == GameConfig.nearRoundEnd || timeLeftSeconds <= GameConfig.roundEndCountdown) {
                            if(hasBeenSecond) {
                                ingamePlayers.sendMessage(Messages.ENDING_SOON.parse(Math.ceil(timeLeftSeconds).toInt()))
                                players.forEach { it.playSound(it.location, Sound.BLOCK_NOTE_PLING, 1f, 1f) }
                            }
                        }
                        if(timeLeft <= 0) {
                            if(round != 5){
                                round++
                                gamePeriod = Period.PRE_ROUND
                                ingamePlayers.forEach {
                                    it.teleport(GameConfig.lobby)
                                    ingamePlayers.filter { pl -> pl != it }.forEach { pl -> it.hidePlayer(SpherePvP.plugin, pl) }
                                    it.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 130))
                                    it.walkSpeed = 0f
                                }
                            }else {
                                gamePeriod = Period.GAME_OVER
                                gameEnd()
                            }
                        }
                    }
                    PRE_ROUND -> {
                        if(timeLeft <= 0){
                            gamePeriod = Period.ROUND_COUNTDOWN
                            teleportAll(round)
                            ingamePlayers.forEach {
                                it.playSound(it.location, Sound.ENTITY_ENDERDRAGON_GROWL, 1f, 1.2f)
                                it.removePotionEffect(PotionEffectType.JUMP)
                                it.walkSpeed = 0.2f
                                FreezeHandler.freezePlayer(it)
                                ingamePlayers.filter { pl -> pl != it }.forEach { pl -> it.showPlayer(SpherePvP.plugin, pl) }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        updateBossBar()
    }


    /**
     * Kick all players and setup/reset the arena.
     * Not blocking!
     */
    fun initReset() {

        players.sendToServerOrKick(GameConfig.hubServer, "${RED}Game restarting", 500) {
            playerKills.clear()
            if(!Game::arena.isInitialized){
                arena = Arena.arenas.first()
            }else {
                Bukkit.unloadWorld(arena.name, false)
                arena = Arena.arenas.nextElement(arena).second
            }
            val worldFile = File("arena_${arena.name}")
            if(worldFile.exists()) FileUtils.forceDelete(worldFile)

            val defArena = Bukkit.getWorld(arena.name)
            defArena.worldFolder.copyRecursively(worldFile)
            FileUtils.forceDelete(File(worldFile, "uid.dat"))
            generateEmptyWorld("arena_${arena.name}")
        }
    }

    fun teleportAll(round: Int) {
        val validSpawns = arena.getIngameSpawns(round).toMutableList()
        validSpawns.shuffle()
        for(player in ingamePlayers){
            val loc = validSpawns[0].clone()
            validSpawns.removeAt(0)
            loc.world = world
            player.teleport(loc)
        }

    }

    fun updateBossBar() {
        when(status) {

            Game.Status.WAITING -> {
                if(gamePeriod == GAME_STARTING) {
                    if(hasBeenSecond){
                        players.forEach { it.playSound(it.location, Sound.BLOCK_WOOD_PRESSUREPLATE_CLICK_ON, 0.5f, 2f) }
                    }
                    bossbar.title = Messages.STARTING_IN.parse(timeLeftSeconds.format("#0.00"))
                    bossbar.progress = timeSinceSeconds / GameConfig.lobbyTime.toDouble()
                }else {
                    bossbar.title = Messages.WAITING.parse(GameConfig.minPlayers - players.size)
                    bossbar.progress = 1.0
                }
            }
            Game.Status.INGAME -> {
                when(gamePeriod) {

                    PRE_ROUND -> {
                        bossbar.title = Messages.ROUND_PREP.parse(timeLeftSeconds.format("#0.00"))
                        bossbar.progress = timeSinceSeconds / GameConfig.prepTime.toDouble()
                    }
                    IN_ROUND -> {
                        if(hasBeenSecond) {
                            players.forEach { it.playSound(it.location, Sound.BLOCK_WOOD_PRESSUREPLATE_CLICK_ON, 2f * (1 - (timeLeftSeconds / GameConfig.roundTime).toFloat()), 2f) }
                        }
                        bossbar.title = Messages.ROUND_INPLAY.parse(timeLeftSeconds.format("#0.00"))
                        bossbar.progress = timeSinceSeconds / GameConfig.roundTime.toDouble()
                    }
                    ROUND_COUNTDOWN -> {
                        bossbar.title = Messages.ROUND_START_COUNTDOWN.parse(timeLeftSeconds.format("#0.00"))
                        bossbar.progress = timeSinceSeconds / GameConfig.preRoundStartTime
                    }
                    GAME_OVER -> {
                        bossbar.title = Messages.GAME_END.parse()
                        bossbar.progress = timeSinceSeconds / GameConfig.gameOverTime
                    }
                    else -> {}
                }
            }
        }

    }

    fun gameEnd() {

        players.sendMessage(Messages.GAME_END.parse())

        var winner = Bukkit.getPlayer(playerKills.keys.sortedBy { playerKills[it]!! }.last())
        players.sendMessage(Messages.PLAYER_FIRST.parse(winner.name, playerKills[winner.uniqueId]!!))

        doLater(80) {
            gamePeriod = Period.GAME_WAITING
            status = Status.WAITING
            initReset()
        }

    }

    @EventHandler
    private fun playerMove(e: PlayerMoveEvent) {

        if(!e.player.isSpectatingGame && gamePeriod == Period.PRE_ROUND){
            val f = e.from
            val t = e.to
            if(f.x != t.x || f.y != t.y || f.z != t.z) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler
    private fun playerDamaged(e: EntityDamageEvent) {
        if(e.entity !is Player) return
        if(gamePeriod != Game.Period.IN_ROUND) e.isCancelled = true
    }

    @EventHandler
    private fun playerKillPlayer(e: EntityDeathEvent) {
        if(e.entity !is Player) return
        doLater(2) {
            (e.entity as Player).setBedSpawnLocation(arena.getIngameSpawns(round).random(), true)
            (e.entity as Player).spigot().respawn()
        }
        if(e.entity.killer == null) return

        playerKills.put(e.entity.killer.uniqueId, playerKills[e.entity.killer.uniqueId]!! + 1)
        e.entity.killer.sendMessage(Messages.PLAYER_KILL.parse(e.entity.name))
        e.entity.killer.playSound(e.entity.killer.location, Sound.BLOCK_ANVIL_PLACE, 0.75f, 2f)
    }

    @EventHandler
    private fun onJoin(e: PlayerJoinEvent) {
        if(gamePeriod != Period.GAME_WAITING) {
            e.player.kickPlayer(Messages.GAME_IN_SESSION.parse())
            return
        }
        updateBossBar()
        bossbar.addPlayer(e.player)
        doLater(1) {
            e.player.removePotionEffect(PotionEffectType.JUMP)
            e.player.removePotionEffect(PotionEffectType.INVISIBILITY)
        }
        e.player.walkSpeed = 0.2f
        playerKills.put(e.player.uniqueId, 0)
        when(status) {
            Status.INGAME -> {
                e.player.gameMode = GameMode.SPECTATOR
                e.player.teleport(arena.spectatorSpawn)
                spectators.add(e.player.uniqueId)
                e.joinMessage = Messages.SPECTATOR_JOIN.parse(e.player.name)
            }
            Status.WAITING -> {
                e.player.gameMode = GameMode.SURVIVAL
                e.player.teleport(GameConfig.lobby)
                e.joinMessage = Messages.JOIN_GAME.parse(e.player.name, ingamePlayers.size.toString(), GameConfig.maxPlayers.toString())
            }
        }
    }

    enum class Status(val readableName: String) {
        INGAME("Ingame"), WAITING("Waiting");

        override fun toString() = this.readableName
    }

    enum class Period(val readableName: String) {
        GAME_WAITING("Waiting for players..."),
        GAME_STARTING("Game Starting"),
        PRE_ROUND("Prep time"),
        IN_ROUND("In play"),
        ROUND_COUNTDOWN("Get Ready"),
        GAME_OVER("Game Over");
    }

}

val Player.isSpectatingGame
    get() = Game.spectators.contains(this.uniqueId)