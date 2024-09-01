package cat.nyaa.nyaashop

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot

class NyaaShopCommands(private val pluginInstance: NyaaShop) : TabExecutor,
    Listener {
    private val shopDataManager = pluginInstance.getShopDataManager()

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String>? {
        TODO("Not yet implemented")
    }

    private fun sendHelpAndReturn(sender: CommandSender): Boolean {
        sender.sendMessage(pluginInstance.language.help.produce())
        return true
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): Boolean {
        if (args.isNullOrEmpty()) {
            return sendHelpAndReturn(sender)
        }

        if (args[0].equals("reload", true)) {
            if (sender.hasPermission("nyaashop.admin")) {
                pluginInstance.reload()
                sender.sendMessage(pluginInstance.language.plugin_reloaded.produce())
            } else {
                sender.sendMessage(pluginInstance.language.permission_denied.produce())
            }
            return true
        }

        if (sender !is Player) {
            sender.sendMessage(pluginInstance.language.player_only_command.produce())
            return true //ignore entity call other than player
        }

        val senderPlayer = sender
        val selectedShopID =
            shopDataManager.getPlayerSelectedShop(senderPlayer.uniqueId)
        val shop = selectedShopID?.let { shopDataManager.getShopData(it) }
        if (shop == null) {
            sender.sendMessage(pluginInstance.language.shop_not_valid.produce())
            shopDataManager.clearPlayerSelectedShop(senderPlayer.uniqueId)
            return true
        }

        when (args[0]) {
            "set" -> {
                if (shop.ownerUniqueID != senderPlayer.uniqueId) {
                    sender.sendMessage(pluginInstance.language.cant_change_others_shop.produce())
                    return true
                }
                if (args.size < 2) {
                    return sendHelpAndReturn(sender)
                }
                when (args[1]) {
                    "item" -> {
                        val slot = if (args.size > 2) {
                            if (args[2].equals(
                                    "mainhand",
                                    true
                                )
                            ) EquipmentSlot.HAND else EquipmentSlot.OFF_HAND
                        } else {
                            EquipmentSlot.HAND
                        }
                        val item = senderPlayer.inventory.getItem(slot)
                        if (item.type.isAir) {
                            sender.sendMessage(pluginInstance.language.unable_to_change_item_to_air.produce())
                            return true
                        }
                        if (shop.getRemainingStock() != 0) {
                            sender.sendMessage(pluginInstance.language.unable_to_change_item_stock.produce())
                            return true
                        }
                        shopDataManager.updateItemStack(shop.id, item)
                        shop.refreshItemDisplay()
                        shop.updateSign()
                    }

                    "price" -> {
                        TODO()
                    }

                    "tradlimit" -> {
                        TODO()
                    }

                    else -> return sendHelpAndReturn(sender)
                }
            }

            "stock" -> {
                when (args[1]) {
                    "add" -> {
                        TODO()
                    }

                    "retrieve" -> {
                        TODO()
                    }
                }
            }

            "buy" -> {
                TODO()
            }
        }
        return true
    }
}