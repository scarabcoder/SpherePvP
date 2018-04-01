package net.clownercraft.spherepvp

import com.scarabcoder.commons.config.getTypedList
import com.scarabcoder.commons.config.setLocation
import com.scarabcoder.commons.doTimer
import org.bukkit.Bukkit
import org.bukkit.Effect
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

typealias Spawns =  HashMap<Int, MutableList<Location>>
class Arena private constructor(val name: String, val spawnLocations: Spawns, var enabled: Boolean = false) {

    val schedulerID: Int
    val file = File(SpherePvP.plugin.dataFolder, "arenas/$name.yml")


    var spectatorSpawn: Location? = null
        set(value) {
            if(value != null)
                cfg.setLocation("spectator-spawn", value)
            field = value
        }

    val cfg: FileConfiguration by lazy {
        val cfg = YamlConfiguration.loadConfiguration(file)
        if(!file.exists()){
            file.createNewFile()
            cfg.set("name", name)
            for(i in 1..5) {
                cfg.set("${i}_spawns", ArrayList<Location>())
            }
            cfg.set("enabled", enabled)
        }
        cfg
    }

    init {
        schedulerID = doTimer(3) {
            if(!GameConfig.enabled) {
                for ((round, spawns) in spawnLocations) {
                    for (spawn in spawns) {
                        spawn.world.playEffect(spawn.clone().add(0.5, 0.5, 0.5), Effect.MOBSPAWNER_FLAMES, 1)
                    }
                }
            }
        }.taskId
    }

    fun getIngameSpawns(round: Int): List<Location> {
        if(round !in 1..5) throw IllegalArgumentException("Round must be between 1 and 5!")

        return spawnLocations[round]!!.map { it.clone() }.map { it.world = Bukkit.getWorld("arena_$name"); it}
    }

    fun save() {
        val sec = cfg.createSection("spawn-locations")
        for((key, list) in spawnLocations){
            sec.set("${key}_spawns", list)
        }
        cfg.set("enabled", enabled)
        cfg.save(File(SpherePvP.plugin.dataFolder, "arenas/$name.yml"))
    }

    companion object {

        private val _arenas = ArrayList<Arena>()

        val arenas: List<Arena>
            get() = _arenas.toList()

        fun new(name: String): Arena {
            if(this[name] != null) throw IllegalArgumentException("Name is already in use!")
            val arenas = GameConfig.arenas
            arenas.add(name)
            GameConfig.arenas = arenas
            val spawns = Spawns()
            for(i in 1..5){
                spawns.put(i, ArrayList())
            }
            val arena = Arena(name, spawns)
            this._arenas.add(arena)
            return arena
        }

        fun remove(name: String) {
            if(this[name] == null) throw IllegalArgumentException("Arena with name $name does not exist.")
            Bukkit.getScheduler().cancelTask(this[name]!!.schedulerID)
            this[name]!!.file.delete()
            _arenas.remove(this[name])
        }

        fun loadIn() {
            for(arena in GameConfig.arenas){
                val spawns = Spawns()
                val cfg = YamlConfiguration.loadConfiguration(File(SpherePvP.plugin.dataFolder, "arenas/$arena.yml"))
                val spawnLocs = cfg.getConfigurationSection("spawn-locations")
                for(i in 1..5){
                    spawns.put(i, spawnLocs.getTypedList<Location>("${i}_spawns").toMutableList())
                }
                _arenas.add(Arena(arena, spawns, cfg.getBoolean("enabled")))
            }
        }

        operator fun get(index: String): Arena? {
            return _arenas.firstOrNull { it.name == index }
        }
    }

}