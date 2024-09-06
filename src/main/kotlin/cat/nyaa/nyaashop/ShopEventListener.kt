package cat.nyaa.nyaashop

import cat.nyaa.nyaashop.data.Shop
import cat.nyaa.nyaashop.data.ShopType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ShopEventListener(private val pluginInstance: NyaaShop) : Listener {
    private val shopDataManager = pluginInstance.getShopDataManager()

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
            if (!offhandItem.type.isAir) offhandItem.clone().asOne() else ItemStack(
                Material.APPLE,1
            ),
            0,
            0,
            price
        )
        shopDataManager.createNewShop(shop)
        event.player.sendMessage(pluginInstance.language.shopCreated.produce())
    }

    @EventHandler(ignoreCancelled = true)
    fun forSignBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.state is Sign) {
            val sign = block.state as Sign
            if (Shop.isShopSign(sign)) {
                val shopID = Shop.getShopIDFromSign(sign) ?: return
                shopDataManager.getShopData(shopID)
                    ?.let { shop ->
                        if (shop.ownerUniqueID == event.player.uniqueId) {
                            shopDataManager
                                .deleteShopData(shop)
                            event.player.sendMessage(pluginInstance.language.shopDeleted.produce())
                        } else {
                            event.isCancelled = true
                            event.player.sendMessage(pluginInstance.language.unableToBreak.produce())
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
                shopDataManager.getShopData(shopID)
                    ?.let { shop ->
                        if (shop.ownerUniqueID == event.player.uniqueId) {
                            shopDataManager.makePlayerSelectShop(
                                event.player.uniqueId,
                                shop
                            )
                            shopDataManager
                                .sendShopDetailsMessageForOwner(
                                    event.player,
                                    shop
                                )
                        } else {
                            shopDataManager.makePlayerSelectShop(
                                event.player.uniqueId,
                                shop
                            )
                            shopDataManager
                                .sendShopDetailsMessageForGuest(
                                    event.player,
                                    shop
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
            shopDataManager.getPlayerSelectedShopID(player.uniqueId) ?: return
        val shop = shopDataManager.getShopData(shopID)
        if (shop == null || shop.distanceFrom(event.to) > pluginInstance.config.shopInteractiveRange) {
            shopDataManager.clearPlayerSelectedShop(player.uniqueId)
            player.sendMessage(
                pluginInstance.language.playerLeaveShop.produce(
                    "shopID" to shopID
                )
            )
        }
    }

    @EventHandler
    public fun onChunkLoad(event: ChunkLoadEvent) {
        // load all the shops into memory on chunk loading
        // by filtering all the tile entity and check if it is a shop sign
        // and load them into memory
        event.chunk.tileEntities.filterIsInstance<Sign>().forEach { sign ->
            if (Shop.isShopSign(sign)) {
                val shopID = Shop.getShopIDFromSign(sign) ?: return@forEach
                val shop = shopDataManager.loadShop(shopID)
                if (shop == null) {
                    Shop.clearShopIDPDC(sign)
                    sign.world.getNearbyEntitiesByType(
                        ItemDisplay::class.java,
                        sign.location.toCenterLocation(),
                        5.0
                    ).filter {
                        it.persistentDataContainer.get(
                            Shop.shopIDPDCKey,
                            PersistentDataType.INTEGER
                        ) == shopID
                    }.forEach { it.remove() }
                    pluginInstance.logger.warning("Shop #$shopID not exist in database but the sign still exist in the world")
                    pluginInstance.logger.warning("Location: ${sign.location.x} ${sign.location.y} ${sign.location.z}, world: ${sign.location.world?.name}")
                    pluginInstance.logger.warning("cleaned up automatically")
                }
            }
        }
    }

    @EventHandler
    public fun onChunkUnload(event: ChunkUnloadEvent) {
        // remove all the shops from memory on chunk unloading
        event.chunk.tileEntities.filterIsInstance<Sign>().forEach { sign ->
            if (Shop.isShopSign(sign)) {
                val shopID = Shop.getShopIDFromSign(sign) ?: return@forEach
                shopDataManager.unloadShop(shopID)
            }
        }
    }


    private fun getTextContent(component: Component): String {
        if (component is TextComponent) {
            return component.content()
        } else
            throw IllegalArgumentException("Component is not a TextComponent")
    }
}