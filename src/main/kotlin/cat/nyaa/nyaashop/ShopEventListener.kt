package cat.nyaa.nyaashop

import cat.nyaa.nyaashop.data.Shop
import cat.nyaa.nyaashop.data.ShopType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack

class ShopEventListener(private val pluginInstance: NyaaShop) : Listener {
    private val shopDataManager = pluginInstance.getShopDataManager()
    private val changeItemButton =
        Component.text(pluginInstance.language.change_item_button_text.produce())
            .hoverEvent(
                Component.text(pluginInstance.language.change_item_button_text.produce())
            ).clickEvent(
                ClickEvent.suggestCommand("/ns set item ")
            )
    private val changePriceButton =
        Component.text(pluginInstance.language.change_price_button_text.produce())
            .hoverEvent(
                Component.text(pluginInstance.language.change_price_button_description.produce())
            ).clickEvent(
                ClickEvent.suggestCommand("/shop set price ")
            )
    private val addStockButton =
        Component.text(pluginInstance.language.add_stock_button_text.produce())
            .hoverEvent(
                Component.text(pluginInstance.language.add_stock_button_description.produce())
            ).clickEvent(
                ClickEvent.suggestCommand("/ns stock add ")
            )
    private val retrieveStockButton =
        Component.text(pluginInstance.language.retrieve_stock_button_text.produce())
            .hoverEvent(
                Component.text(pluginInstance.language.retrieve_stock_button_description.produce())
            ).clickEvent(
                ClickEvent.suggestCommand("/ns stock retrieve ")
            )
    private val changeTradeLimitButton =
        Component.text(pluginInstance.language.change_trade_limit_button_text.produce())
            .hoverEvent(
                Component.text(pluginInstance.language.change_trade_limit_button_description.produce())
            ).clickEvent(
                ClickEvent.suggestCommand("/ns set tradelimit ")
            )

    private val buyTradeButton =
        Component.text(pluginInstance.language.buy_trade_button_text.produce())
            .hoverEvent(
                Component.text(pluginInstance.language.buy_trade_button_description.produce())
            ).clickEvent(
                ClickEvent.suggestCommand("/ns buy ")
            )
    private val sellTradeButton =
        Component.text(pluginInstance.language.sell_trade_button_text.produce())
            .hoverEvent(
                Component.text(pluginInstance.language.sell_trade_button_description.produce())
            ).clickEvent(
                ClickEvent.suggestCommand("/ns sell ")
            )

    @EventHandler(ignoreCancelled = true)
    fun forSignCreation(event: SignChangeEvent) {
        val firstLine = event.line(0) ?: return
        val sellShop =
            getTextContent(firstLine) in pluginInstance.config.shopCreationSellHeader
        val buyShop =
            getTextContent(firstLine) in pluginInstance.config.shopCreationBuyHeader

        val secondLine = event.line(1) ?: return
        val price = getTextContent(secondLine).toDoubleOrNull() ?: return
        val offhandItem = event.player.inventory.itemInOffHand
        if (!sellShop && !buyShop) return
        //shop creation
        val shop = Shop(
            -1,
            event.player.uniqueId,
            event.block.world.uid,
            event.block.x,
            event.block.y,
            event.block.z,
            if (sellShop) ShopType.SELL else ShopType.BUY,
            if (!offhandItem.type.isAir) offhandItem.clone() else ItemStack(
                Material.APPLE
            ),
            0,
            0,
            price
        )
        pluginInstance.getShopDataManager().createNewShop(shop)
        event.player.sendMessage(pluginInstance.language.shop_created.produce())
    }

    @EventHandler(ignoreCancelled = true)
    fun forSignBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.state is Sign) {
            val sign = block.state as Sign
            if (Shop.isShopSign(sign)) {
                val shopID = Shop.getShopIDFromSign(sign) ?: return
                pluginInstance.getShopDataManager().getShopData(shopID)
                    ?.let { shop ->
                        if (shop.ownerUniqueID == event.player.uniqueId) {
                            pluginInstance.getShopDataManager()
                                .deleteShopData(shop)
                            event.player.sendMessage(pluginInstance.language.shop_deleted.produce())
                        } else {
                            event.isCancelled = true
                            event.player.sendMessage(pluginInstance.language.unable_to_break.produce())
                        }
                    } ?: return
            }
        }
    }

    @EventHandler
    fun forShopRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.state is Sign) {
            val sign = block.state as Sign
            if (Shop.isShopSign(sign)) {
                val shopID = Shop.getShopIDFromSign(sign) ?: return
                pluginInstance.getShopDataManager().getShopData(shopID)
                    ?.let { shop ->
                        if (shop.ownerUniqueID == event.player.uniqueId) {
                            shopDataManager.makePlayerSelectShop(
                                event.player.uniqueId,
                                shop
                            )
                            event.player.sendMessage(
                                pluginInstance.language.shop_interact_owner.produceAsComponent(
                                    "shop_title" to when (shop.type) {
                                        ShopType.BUY -> pluginInstance.language.buyShopTitle.produce()
                                        ShopType.SELL -> pluginInstance.language.sellShopTitle.produce()
                                    },
                                    "id" to shopID,
                                    "item" to shop.itemStack,
                                    "change_item_button" to changeItemButton,
                                    "price" to shop.price,
                                    "change_price_button" to changePriceButton,
                                    "currencyName" to pluginInstance.economyProvider.currencyNamePlural(),
                                    "stock" to shop.stock,
                                    "add_stock_button" to addStockButton,
                                    "retrieve_stock_button" to retrieveStockButton,
                                    "tradeLimit" to shop.tradeLimit,
                                    "change_trade_limit_button" to changeTradeLimitButton
                                )
                            )
                        } else {
                            shopDataManager.makePlayerSelectShop(
                                event.player.uniqueId,
                                shop
                            )
                            event.player.sendMessage(
                                pluginInstance.language.shop_interact_guest.produceAsComponent(
                                    "shop_title" to when (shop.type) {
                                        ShopType.BUY -> pluginInstance.language.buyShopTitle.produce()
                                        ShopType.SELL -> pluginInstance.language.sellShopTitle.produce()
                                    },
                                    "owner" to shop.ownerUniqueID,
                                    "item" to shop.itemStack,
                                    "price" to shop.price,
                                    "currencyName" to pluginInstance.economyProvider.currencyNamePlural(),
                                    "stock" to shop.stock,
                                    "trade_button" to when (shop.type) {
                                        ShopType.BUY -> buyTradeButton
                                        ShopType.SELL -> sellTradeButton
                                    }
                                )
                            )
                        }
                    } ?: return
            }
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val shopID =
            shopDataManager.getPlayerSelectedShop(player.uniqueId) ?: return
        val shop = pluginInstance.getShopDataManager().getShopData(shopID)
        if (shop == null || shop.distanceFrom(event.to) > pluginInstance.config.shopInteractiveRange) {
            shopDataManager.clearPlayerSelectedShop(player.uniqueId)
            player.sendMessage(
                pluginInstance.language.player_leave_shop.produce(
                    "shopID" to shopID
                )
            )
        }
    }


    private fun getTextContent(component: Component): String {
        if (component is TextComponent) {
            return component.content()
        } else
            throw IllegalArgumentException("Component is not a TextComponent")
    }
}