package cat.nyaa.nyaashop

import cat.nyaa.ecore.ServiceFeePreference
import cat.nyaa.nyaashop.Utils.Companion.hasAtLeast
import cat.nyaa.nyaashop.Utils.Companion.removeItem
import cat.nyaa.nyaashop.Utils.Companion.tryToAddItem
import cat.nyaa.nyaashop.data.ShopType
import land.melon.lab.simplelanguageloader.utils.ItemUtils
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID

class NyaaShopCommands(private val pluginInstance: NyaaShop) : TabExecutor,
    Listener {
    private val shopDataManager = pluginInstance.getShopDataManager()
    private val adminPermissionNode = "nyaashop.admin"

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String>? {
        if (sender is ConsoleCommandSender)
            return mutableListOf("reload")
        if (sender !is Player)
            return null

        val selectedShopID =
            shopDataManager.getPlayerSelectedShopID(sender.uniqueId)
        val selectedShop =
            selectedShopID?.let { shopDataManager.getShopData(it) }
                ?: return mutableListOf("list")

        if (selectedShop.ownerUniqueID == sender.uniqueId) {
            // ns set item <mainhand|offhand>
            // ns set price <price>
            // ns set tradelimit <tradelimit>
            // ns stock <add|retrieve> <number>
            if (args.isNullOrEmpty() || args.size == 1) {
                return mutableListOf("set", "stock")
            }
            if (args.size == 2) {
                return when (args[0]) {
                    "set" -> mutableListOf("item", "price", "tradelimit")
                    "stock" -> mutableListOf("add", "retrieve")
                    else -> mutableListOf()
                }.filter { it.startsWith(args[1]) }.toMutableList()
            }
            if (args.size == 3) {
                return when (args[0]) {
                    "set" -> when (args[1]) {
                        "item" -> mutableListOf("mainhand", "offhand")
                        else -> mutableListOf("<amount...>")
                    }

                    "stock" -> when (args[1]) {
                        "add" -> mutableListOf("<amount...>")
                        "retrieve" -> mutableListOf("<amount...>")
                        else -> mutableListOf()
                    }

                    else -> mutableListOf()
                }.filter { it.startsWith(args[2]) }.toMutableList()
            }

        } else {
            //ns buy <number> if type = sell
            //ns sell <number> if type = buy
            if (args.isNullOrEmpty() || args.size == 1) {
                return when (selectedShop.type) {
                    ShopType.SELL -> mutableListOf("buy")
                    ShopType.BUY -> mutableListOf("sell")
                }.filter { it.startsWith(args?.get(0) ?: "") }.toMutableList()
            }
            if (args.size == 2) {
                return when (args[0]) {
                    "buy" -> mutableListOf("<amount...>").filter {
                        it.startsWith(
                            args[1]
                        )
                    }.toMutableList()

                    "sell" ->
                        mutableListOf("<amount...>").filter {
                            it.startsWith(
                                args[1]
                            )
                        }.toMutableList()

                    else -> mutableListOf()

                }
            }
        }
        return mutableListOf()
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
            if (sender.hasPermission(adminPermissionNode)) {
                pluginInstance.reload()
                sender.sendMessage(pluginInstance.language.pluginReloaded.produce())
            } else {
                sender.sendMessage(pluginInstance.language.permissionDenied.produce())
            }
            return true
        }

        if (sender !is Player) {
            sender.sendMessage(pluginInstance.language.playerOnlyCommand.produce())
            return true //ignore entity call other than player
        }

        val senderPlayer = sender
        val selectedShopID =
            shopDataManager.getPlayerSelectedShopID(senderPlayer.uniqueId)
        val shop = selectedShopID?.let { shopDataManager.getShopData(it) }
        if (shop == null) {
            sender.sendMessage(pluginInstance.language.shopNotValid.produce())
            shopDataManager.clearPlayerSelectedShop(senderPlayer.uniqueId)
            return true
        }

        when (args[0]) {
            "set" -> {
                if (shop.ownerUniqueID != senderPlayer.uniqueId) {
                    sender.sendMessage(pluginInstance.language.cantChangeOthersShop.produce())
                    return true
                }
                if (args.size < 3) {
                    return sendHelpAndReturn(sender)
                }
                when (args[1]) {
                    "item" -> {
                        val slot = if (args[2].equals(
                                "offhand",
                                true
                            )
                        ) EquipmentSlot.OFF_HAND else EquipmentSlot.HAND
                        val item = senderPlayer.inventory.getItem(slot).asOne()
                        if (item.type.isAir) {
                            sender.sendMessage(pluginInstance.language.unableToChangeItemToAir.produce())
                            return true
                        }
                        if (shop.stock != 0) {
                            sender.sendMessage(pluginInstance.language.unableToChangeItemStock.produce())
                            return true
                        }
                        shopDataManager.updateItemStack(shop.id, item)
                        shopDataManager.sendShopDetailsMessageForOwner(
                            senderPlayer,
                            shop
                        )
                    }
                    "price" -> {
                        val price = args[2].toDoubleOrNull()
                        if (price == null) {
                            sender.sendMessage(pluginInstance.language.notValidNumber.produce())
                            return true
                        }
                        shopDataManager.updatePrice(shop.id, price)
                        shopDataManager.sendShopDetailsMessageForOwner(
                            senderPlayer,
                            shop
                        )
                    }

                    "tradelimit" -> {
                        val tradeLimit = args[2].toIntOrNull()
                        if (tradeLimit == null) {
                            sender.sendMessage(pluginInstance.language.notValidNumber.produce())
                            return true
                        }
                        if (tradeLimit > pluginInstance.config.shopInventoryCapacity) {
                            sender.sendMessage(
                                pluginInstance.language.tradeLimitTooHigh.produce(
                                    "inventoryCapacity" to pluginInstance.config.shopInventoryCapacity
                                )
                            )
                            return true
                        }
                        shopDataManager.updateTradeLimit(shop.id, tradeLimit)
                        shopDataManager.sendShopDetailsMessageForOwner(
                            senderPlayer,
                            shop
                        )
                    }
                    else -> return sendHelpAndReturn(sender)
                }
            }

            "stock" -> { //ns stock add 1
                if (args.size < 3) {
                    return sendHelpAndReturn(sender)
                }
                val itemAmount = args[2].toIntOrNull()
                if (itemAmount == null) {
                    sender.sendMessage(
                        pluginInstance.language.notValidNumber.produce(
                            "number" to args[2]
                        )
                    )
                    return true
                }
                when (args[1]) {
                    "add" -> {
                        val availableStockSpace =
                            pluginInstance.config.shopInventoryCapacity - shop.stock
                        if (itemAmount > availableStockSpace) {
                            sender.sendMessage(
                                pluginInstance.language.addStockFailedCapacityExceed.produce(
                                    "capacity" to availableStockSpace
                                )
                            )
                            return true
                        }
                        val item = shop.itemStack
                        if (!senderPlayer.hasAtLeast(item)
                        ) {
                            sender.sendMessage(
                                pluginInstance.language.requestFailedItemNotEnough.produceAsComponent(
                                    "item" to ItemUtils.itemTextWithHover(item),
                                    "amount" to itemAmount
                                )
                            )
                            return true
                        }
                        val itemToRemove =
                            item.clone().apply { amount = itemAmount }
                        senderPlayer.removeItem(itemToRemove)
                        shopDataManager.updateStock(
                            shop.id,
                            shop.stock + itemAmount
                        )
                        sender.sendMessage(
                            pluginInstance.language.stockAdded.produceAsComponent(
                                "item" to ItemUtils.itemTextWithHover(item),
                                "amount" to itemAmount,
                                "stock" to shop.stock,
                                "capacity" to pluginInstance.config.shopInventoryCapacity
                            )
                        )
                    }
                    "retrieve" -> {
                        if (itemAmount > shop.stock) {
                            sender.sendMessage(
                                pluginInstance.language.retrieveStockFailedStockNotEnough.produce(
                                    "stock" to shop.stock
                                )
                            )
                            return true
                        }
                        val item = shop.itemStack
                        val itemToAdd =
                            item.clone().apply { amount = itemAmount }
                        val itemsAdded =
                            senderPlayer.tryToAddItem(itemToAdd)
                        shopDataManager.updateStock(
                            shop.id,
                            shop.stock - itemsAdded
                        )
                        sender.sendMessage(
                            pluginInstance.language.stockRetrieved.produceAsComponent(
                                "item" to ItemUtils.itemTextWithHover(item),
                                "amount" to itemsAdded,
                                "stock" to shop.stock,
                                "capacity" to pluginInstance.config.shopInventoryCapacity
                            )
                        )
                        if (itemToAdd.amount != itemsAdded) {
                            sender.sendMessage(pluginInstance.language.requestCantFullyComply.produce())
                        }
                    }

                    else -> return sendHelpAndReturn(sender)
                }
            }

            "buy" -> {
                if (shop.type != ShopType.SELL) {
                    sender.sendMessage(
                        pluginInstance.language.unMatchedShopType.produceAsComponent(
                            "shopTitle" to pluginInstance.language.sellShopTitle.produce()
                        )
                    )
                    return true
                }
                if (args.size < 2) {
                    return sendHelpAndReturn(sender)
                }
                val itemAmount = args[1].toIntOrNull()
                if (itemAmount == null) {
                    sender.sendMessage(
                        pluginInstance.language.notValidNumber.produce(
                            "number" to args[1]
                        )
                    )
                    return true
                }
                if (itemAmount <= 0) {
                    sender.sendMessage(pluginInstance.language.notValidNumber.produce())
                    return true
                }
                if (shop.stock < itemAmount) {
                    sender.sendMessage(pluginInstance.language.merchantOutOfStock.produce())
                    return true
                }
                val moneyNeed =
                    shop.price * (pluginInstance.config.shopTradeFeeRateSellInDouble + 1) * itemAmount
                if (pluginInstance.economyProvider.getPlayerBalance(senderPlayer.uniqueId) < moneyNeed) {
                    sender.sendMessage(
                        pluginInstance.language.playerNotEnoughMoney.produce(
                            "money" to moneyNeed,
                            "currencyName" to pluginInstance.economyProvider.currencyNamePlural()
                        )
                    )
                    return true
                }
                val item = shop.itemStack
                val itemToAdd = item.clone().apply { setAmount(itemAmount) }
                val itemAdded = senderPlayer.tryToAddItem(itemToAdd)

                val tradeResult = pluginInstance.economyProvider.playerTrade(
                    senderPlayer.uniqueId,
                    shop.ownerUniqueID,
                    shop.price * itemAdded,
                    pluginInstance.config.shopTradeFeeRateSellInDouble,
                    ServiceFeePreference.ADDITIONAL
                ) // this one should only success due to the balance has checked, otherwise it might be some exception happens

                if (!tradeResult.isSuccess) {
                    pluginInstance.logger.warning("Failed to trade between ${tradeResult.receipt.payer} and ${tradeResult.receipt.receiver}, reason: ${tradeResult.status()}")
                    senderPlayer.removeItem(
                        item.clone().apply { amount = itemAdded })
                    senderPlayer.sendMessage(
                        pluginInstance.language.transactionFailedUnknown.produce()
                    )
                    return true
                }

                shopDataManager.updateStock(shop.id, shop.stock - itemAdded)

                sender.sendMessage(
                    pluginInstance.language.buySuccessNotice.produceAsComponent(
                        "item" to ItemUtils.itemTextWithHover(item),
                        "amount" to itemAdded,
                        "owner" to Bukkit.getPlayer(shop.ownerUniqueID)?.name,
                        "cost" to tradeResult.receipt.amountTotally,
                        "tax" to tradeResult.receipt.feeTotally,
                        "taxPercentage" to pluginInstance.config.shopTradeFeeRateSellInDouble * 100,
                        "currencyName" to pluginInstance.economyProvider.currencyNamePlural()
                    )
                )
                if (itemToAdd.amount != itemAdded) {
                    sender.sendMessage(pluginInstance.language.requestCantFullyComply.produce())
                }
            }

            "sell" -> {
                if (shop.type != ShopType.BUY) {
                    sender.sendMessage(
                        pluginInstance.language.unMatchedShopType.produce(
                            "shopTitle" to pluginInstance.language.buyShopTitle.produce()
                        )
                    )
                    return true
                }
                if (args.size < 2) {
                    return sendHelpAndReturn(sender)
                }
                val itemAmount = args[1].toIntOrNull()
                if (itemAmount == null) {
                    sender.sendMessage(
                        pluginInstance.language.notValidNumber.produce(
                            "number" to args[1]
                        )
                    )
                    return true
                }
                if (itemAmount <= 0) {
                    sender.sendMessage(pluginInstance.language.notValidNumber.produce())
                    return true
                }
                if (itemAmount > shop.getRemainingStock()) {
                    sender.sendMessage(pluginInstance.language.merchantStorageFull.produce())
                    return true
                }
                val item = shop.itemStack
                val itemToRemove = item.clone().apply { amount = itemAmount }
                if (!senderPlayer.hasAtLeast(itemToRemove)) {
                    sender.sendMessage(
                        pluginInstance.language.requestFailedItemNotEnough.produceAsComponent(
                            "item" to ItemUtils.itemTextWithHover(item)
                        )
                    )
                    return true
                }
                if ((itemAmount + shop.stock) > pluginInstance.config.shopInventoryCapacity) {
                    sender.sendMessage(
                        pluginInstance.language.merchantStorageFull.produce()
                    )
                    return true
                }
                if (!checkBalance(
                        shop.ownerUniqueID,
                        shop.price * itemAmount * (1 + pluginInstance.config.shopTradeFeeRateBuyInDouble)
                    )
                ) {
                    sender.sendMessage(
                        pluginInstance.language.merchantNotEnoughMoney.produce()
                    )
                    return true
                }

                val itemRemoved = senderPlayer.removeItem(itemToRemove)

                val tradeResult = pluginInstance.economyProvider.playerTrade(
                    shop.ownerUniqueID,
                    senderPlayer.uniqueId,
                    shop.price * itemAmount,
                    pluginInstance.config.shopTradeFeeRateBuyInDouble,
                    ServiceFeePreference.ADDITIONAL
                ) // this one should only success due to the balance has checked, otherwise it might be some exception happens

                if (!tradeResult.isSuccess) {
                    pluginInstance.logger.warning("Failed to trade between ${tradeResult.receipt.payer} and ${tradeResult.receipt.receiver}, reason: ${tradeResult.status()}")
                    senderPlayer.inventory.addItem(
                        item.clone().apply { amount = itemAmount })
                    sender.sendMessage(
                        pluginInstance.language.transactionFailedUnknown.produce()
                    )
                    return true
                }
                shopDataManager.updateStock(shop.id, shop.stock + itemAmount)

                sender.sendMessage(
                    pluginInstance.language.sellSuccessNotice.produceAsComponent(
                        "item" to ItemUtils.itemTextWithHover(item),
                        "amount" to itemAmount,
                        "owner" to Bukkit.getPlayer(shop.ownerUniqueID)?.name,
                        "cost" to tradeResult.receipt.amountTotally,
                        "tax" to tradeResult.receipt.feeTotally,
                        "taxPercentage" to pluginInstance.config.shopTradeFeeRateBuyInDouble * 100,
                        "currencyName" to pluginInstance.economyProvider.currencyNamePlural()
                    )
                )
            }
        }
        return true
    }

    private fun checkBalance(accountUniqueID: UUID, amount: Double): Boolean {
        return pluginInstance.economyProvider.getPlayerBalance(accountUniqueID) >= amount
    }
}