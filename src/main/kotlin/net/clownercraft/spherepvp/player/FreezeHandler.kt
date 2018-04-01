package net.clownercraft.spherepvp.player

import net.minecraft.server.v1_12_R1.EntityZombie
import net.minecraft.server.v1_12_R1.PacketPlayOutCamera
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityDestroy
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntityLiving
import org.bukkit.GameMode
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import java.util.*

object FreezeHandler {

    private val frozen = ArrayList<UUID>()
    private val entities = HashMap<UUID, EntityZombie>()

    fun freezePlayer(player: Player) {

        player.allowFlight = true

        frozen.add(player.uniqueId)

        val entity = EntityZombie((player.world as CraftWorld).handle)
        val l = player.location
        entity.setLocation(l.x, l.y, l.z, l.yaw, l.pitch)
        entity.isInvisible = true
        entity.isSilent = true

        val pc = (player as CraftPlayer).handle.playerConnection
        pc.sendPacket(PacketPlayOutSpawnEntityLiving(entity))
        player.gameMode = GameMode.SPECTATOR
        pc.sendPacket(PacketPlayOutCamera(entity))

        entities.put(player.uniqueId, entity)
    }




    fun unfreeze(player: Player) {
        if(!frozen.contains(player.uniqueId)) return

        val pc = (player as CraftPlayer).handle.playerConnection
        pc.sendPacket(PacketPlayOutCamera(player.handle))
        player.gameMode = GameMode.SURVIVAL
        pc.sendPacket(PacketPlayOutEntityDestroy(entities[player.uniqueId]!!.id))
        entities.remove(player.uniqueId)
        frozen.remove(player.uniqueId)
    }


}