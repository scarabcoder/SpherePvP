package net.clownercraft.spherepvp.setup

import com.scarabcoder.commandapi2.Command
import com.scarabcoder.commandapi2.CommandRegistry
import com.scarabcoder.commandapi2.CommandValidator
import net.clownercraft.spherepvp.Game
import net.clownercraft.spherepvp.player.Messages
import net.clownercraft.spherepvp.player.PlayerChest
import org.bukkit.entity.Player

object PlayerCommands {

    @Command(validators = [InRoundValidator::class])
    fun chest(sender: Player) = PlayerChest.open(sender)

}

object InRoundValidator: CommandValidator<Player> {

    override fun validate(sender: Player): Boolean {
        if(Game.gamePeriod != Game.Period.IN_ROUND) {
            sender.sendMessage(Messages.MUST_BE_IN_ROUND.parse())
            return false
        }
        return true
    }
}

internal fun registerPlayerCommands() {

    CommandValidator.registerValidator(InRoundValidator)
    CommandRegistry.registerMultiCommands(PlayerCommands)

}