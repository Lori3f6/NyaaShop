package cat.nyaa.nyaashop

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class NyaaShopCommands(pluginInstance: NyaaShop) : TabExecutor {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String>? {
        TODO("Not yet implemented")
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): Boolean {
        TODO("Not yet implemented")
    }
}