package cat.nyaa.nyaashop

import cat.nyaa.ecore.ServiceFeePreference
import cat.nyaa.nyaashop.data.Shop
import cat.nyaa.nyaashop.data.ShopType
import cat.nyaa.nyaashop.magic.Utils.Companion.addItemByDrop
import cat.nyaa.nyaashop.magic.Utils.Companion.hasAtLeast
import cat.nyaa.nyaashop.magic.Utils.Companion.removeItem
import cat.nyaa.ukit.api.UKitAPI
import land.melon.lab.simplelanguageloader.utils.ItemUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
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
    private val playerPermissionNode = "nyaashop.use"

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
        if(!sender.hasPermission(playerPermissionNode))
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
        if(!sender.hasPermission(playerPermissionNode)){
            sender.sendMessage(pluginInstance.language.permissionDenied.produce())
            return true
        }

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
                            sender.sendMessage(
                                pluginInstance.language.notValidNumber.produce(
                                    "input" to args[2]
                                )
                            )
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
                        if (tradeLimit > shop.stockCapacity()) {
                            sender.sendMessage(
                                pluginInstance.language.tradeLimitTooHigh.produce(
                                    "inventoryCapacity" to shop.stockCapacity()
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
                        val availableStockSpace = shop.remainingStockCapacity()
                        if (itemAmount > availableStockSpace) {
                            sender.sendMessage(
                                pluginInstance.language.addStockFailedCapacityExceed.produce(
                                    "capacity" to availableStockSpace
                                )
                            )
                            return true
                        }
                        val item = shop.itemStack
                        if (!senderPlayer.hasAtLeast(item, itemAmount)
                        ) {
                            sender.sendMessage(
                                pluginInstance.language.requestFailedItemNotEnough.produceAsComponent(
                                    "item" to ItemUtils.itemTextWithHover(item),
                                    "amount" to itemAmount
                                )
                            )
                            return true
                        }
                        senderPlayer.removeItem(item, itemAmount)
                        shopDataManager.updateStock(
                            shop.id,
                            shop.stock + itemAmount
                        )
                        sender.sendMessage(
                            pluginInstance.language.stockAdded.produceAsComponent(
                                "item" to ItemUtils.itemTextWithHover(item),
                                "amount" to itemAmount,
                                "stock" to shop.stock,
                                "capacity" to shop.stockCapacity()
                            )
                        )
                    }
                    "retrieve" -> {
                        val item = shop.itemStack
                        if (itemAmount > shop.stock) {
                            sender.sendMessage(
                                pluginInstance.language.retrieveStockFailedStockNotEnough.produceAsComponent(
                                    "stock" to shop.stock,
                                    "item" to ItemUtils.itemTextWithHover(item)
                                )
                            )
                            return true
                        }
                        senderPlayer.addItemByDrop(item, itemAmount)

                        shopDataManager.updateStock(
                            shop.id,
                            shop.stock - itemAmount
                        )
                        sender.sendMessage(
                            pluginInstance.language.stockRetrieved.produceAsComponent(
                                "item" to ItemUtils.itemTextWithHover(item),
                                "amount" to itemAmount,
                                "stock" to shop.stock,
                                "capacity" to shop.stockCapacity()
                            )
                        )
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
                senderPlayer.addItemByDrop(item, itemAmount)

                val tradeResult = pluginInstance.economyProvider.playerTrade(
                    senderPlayer.uniqueId,
                    shop.ownerUniqueID,
                    shop.price * itemAmount,
                    pluginInstance.config.shopTradeFeeRateSellInDouble,
                    ServiceFeePreference.ADDITIONAL
                ) // this one should only success due to the balance has checked, otherwise it might be some exception happens

                if (!tradeResult.isSuccess) {
                    pluginInstance.logger.warning("Failed to trade between ${tradeResult.receipt.payer} and ${tradeResult.receipt.receiver}, reason: ${tradeResult.status()}")
                    senderPlayer.removeItem(item, itemAmount)
                    senderPlayer.sendMessage(
                        pluginInstance.language.transactionFailedUnknown.produce()
                    )
                    return true
                }

                shopDataManager.updateStock(shop.id, shop.stock - itemAmount)

                sender.sendMessage(
                    pluginInstance.language.buySuccessNotice.produceAsComponent(
                        "item" to ItemUtils.itemTextWithHover(item),
                        "amount" to itemAmount,
                        "owner" to Bukkit.getOfflinePlayer(shop.ownerUniqueID).name,
                        "cost" to tradeResult.receipt.amountTotally,
                        "tax" to tradeResult.receipt.feeTotally,
                        "taxPercentage" to pluginInstance.config.shopTradeFeeRateSellInDouble * 100,
                        "currencyName" to pluginInstance.economyProvider.currencyNamePlural()
                    )
                )

                val ownerMessage =
                    pluginInstance.language.sellShopTradeNoticeForOwner.produceAsComponent(
                        "shopTitle" to shop.shopTitle(),
                        "shopId" to shop.id,
                        "playerName" to sender.name,
                        "item" to ItemUtils.itemTextWithHover(item),
                        "amount" to itemAmount,
                        "owner" to Bukkit.getOfflinePlayer(shop.ownerUniqueID).name,
                        "income" to tradeResult.receipt.amountArriveTotally,
                        "tax" to tradeResult.receipt.feeTotally,
                        "taxPercentage" to pluginInstance.config.shopTradeFeeRateSellInDouble * 100,
                        "currencyName" to pluginInstance.economyProvider.currencyNamePlural()
                    )
                sendMessageOrOfflineMessageIfUkitExist(
                    shop.ownerUniqueID,
                    ownerMessage
                )

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
                val item = shop.itemStack
                if (!senderPlayer.hasAtLeast(item, itemAmount)) {
                    sender.sendMessage(
                        pluginInstance.language.requestFailedItemNotEnough.produceAsComponent(
                            "item" to ItemUtils.itemTextWithHover(item)
                        )
                    )
                    return true
                }
                if (itemAmount > shop.remainingTradeStock()) {
                    sender.sendMessage(pluginInstance.language.merchantStorageFull.produce())
                    return true
                }
                if ((itemAmount + shop.stock) > shop.stockCapacity()) {
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

                val itemRemoved = senderPlayer.removeItem(item, itemAmount)

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
                        "owner" to Bukkit.getOfflinePlayer(shop.ownerUniqueID).name,
                        "cost" to tradeResult.receipt.amountTotally,
                        "tax" to tradeResult.receipt.feeTotally,
                        "taxPercentage" to pluginInstance.config.shopTradeFeeRateBuyInDouble * 100,
                        "currencyName" to pluginInstance.economyProvider.currencyNamePlural()
                    )
                )

                val ownerMessage =
                    pluginInstance.language.buyShopTradeNoticeForOwner.produceAsComponent(
                        "shopTitle" to shop.shopTitle(),
                        "shopId" to shop.id,
                        "playerName" to sender.name,
                        "item" to ItemUtils.itemTextWithHover(item),
                        "amount" to itemAmount,
                        "owner" to Bukkit.getOfflinePlayer(shop.ownerUniqueID).name,
                        "cost" to tradeResult.receipt.amountTotally,
                        "tax" to tradeResult.receipt.feeTotally,
                        "taxPercentage" to pluginInstance.config.shopTradeFeeRateSellInDouble * 100,
                        "currencyName" to pluginInstance.economyProvider.currencyNamePlural()
                    )

                sendMessageOrOfflineMessageIfUkitExist(
                    shop.ownerUniqueID,
                    ownerMessage
                )
            }

            "list" -> {
            }
        }
        return true
    }

    private fun shopDetailComponent(shop: Shop): ComponentLike {
        val component = Component.text()

        return component
    }

    private fun sendMessageOrOfflineMessageIfUkitExist(
        receiverUniqueID: UUID,
        message: Component
    ) {
        if (pluginInstance.isUkitSetup) {
            UKitAPI.getAPIInstance().pushMessage(
                receiverUniqueID,
                message,
                pluginInstance.language.offlineMessageSenderName.produceAsComponent()
            )
        } else {
            Bukkit.getPlayer(receiverUniqueID)?.sendMessage(
                message
            )
        }
    }

    private fun checkBalance(accountUniqueID: UUID, amount: Double): Boolean {
        return pluginInstance.economyProvider.getPlayerBalance(accountUniqueID) >= amount
    }
}