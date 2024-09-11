package cat.nyaa.nyaashop

import cat.nyaa.nyaashop.data.Shop
import cat.nyaa.nyaashop.data.ShopType
import cat.nyaa.nyaashop.magic.DyeMap.Companion.dyeColor
import cat.nyaa.nyaashop.magic.Utils.Companion.getTextContent
import cat.nyaa.nyaashop.magic.Utils.Companion.isPlayerHoldingSignDecorationItem
import cat.nyaa.nyaashop.magic.Utils.Companion.isRelevantToShopSign
import cat.nyaa.nyaashop.magic.Utils.Companion.isShopSign
import com.destroystokyo.paper.MaterialTags
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.block.sign.Side
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.entity.EntityExplodeEvent
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
        if (price < 0) return

        val data = event.block.blockData as WallSign
        if (isRelevantToShopSign(event.block.getRelative(data.facing.oppositeFace))) return

        if (shopDataManager.countPlayerCreatedShops(event.player.uniqueId) >= pluginInstance.config.maximumShopsPerPlayer) {
            event.player.sendMessage(
                pluginInstance.language.tooManyShops.produce(
                    "limit" to pluginInstance.config.maximumShopsPerPlayer
                )
            )
            return
        }

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
                            event.player.sendMessage(
                                pluginInstance.language.shopDeleted.produce(
                                    "shopTitle" to when (shop.type) {
                                        ShopType.BUY -> pluginInstance.language.buyShopTitle
                                        ShopType.SELL -> pluginInstance.language.buyShopTitle
                                    },
                                    "shopID" to shopID
                                )
                            )
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
                        shopDataManager.makePlayerSelectShop(
                            event.player.uniqueId,
                            shop
                        )
                        if (shop.ownerUniqueID == event.player.uniqueId) {
                            if (isPlayerHoldingSignDecorationItem(event.player)) {
                                val mainHandItem =
                                    event.player.inventory.itemInMainHand
                                val signSide = sign.getSide(Side.FRONT)
                                when (mainHandItem.type) {
                                    Material.INK_SAC -> {
                                        signSide.isGlowingText = false
                                    }

                                    Material.GLOW_INK_SAC -> {
                                        signSide.isGlowingText = true
                                    }

                                    else -> {
                                        signSide.color =
                                            mainHandItem.type.dyeColor()
                                    }
                                }
                                sign.update()
                            }
                            shopDataManager
                                .sendShopDetailsMessageForOwner(
                                    event.player,
                                    shop
                                )
                        } else {
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

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        event.isCancelled = isRelevantToShopSign(event.block)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { block ->
            isShopSign(block) || isRelevantToShopSign(block)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeIf { block ->
            isShopSign(block) || isRelevantToShopSign(block)
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
}