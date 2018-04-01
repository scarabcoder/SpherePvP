package net.clownercraft.spherepvp.player

import net.clownercraft.spherepvp.SpherePvP
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

object PlayerChest: Listener {

    init {
        Bukkit.getPluginManager().registerEvents(this, SpherePvP.plugin)
    }

    private val storageChests = HashMap<UUID, Array<ItemStack?>>()
    private val open = ArrayList<UUID>()

    fun getPlayerChest(player: Player): Inventory {
        if(!storageChests.containsKey(player.uniqueId)) storageChests.put(player.uniqueId, Array(9) {null})
        val inventory = Bukkit.createInventory(null, 9, "${player.name}'s Storage")
        inventory.contents = storageChests[player.uniqueId]
        return inventory
    }

    fun open(player: Player) {
        player.openInventory(this[player])
        open.add(player.uniqueId)
    }

    @EventHandler
    fun playerCloseInventory(e: InventoryCloseEvent) {
        if(!open.contains(e.player.uniqueId)) return
        open.remove(e.player.uniqueId)
        storageChests.put(e.player.uniqueId, e.inventory.contents)
    }

    operator fun get(player: Player): Inventory {
        return getPlayerChest(player)
    }

    fun reset() = storageChests.clear()




}