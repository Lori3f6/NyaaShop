package cat.nyaa.nyaashop

import cat.nyaa.nyaashop.data.Shop
import cat.nyaa.nyaashop.data.ShopType
import cat.nyaa.nyaashop.magic.DyeMap.Companion.dyeColor
import cat.nyaa.nyaashop.magic.Permissions
import cat.nyaa.nyaashop.magic.Utils.Companion.addItemByDrop
import cat.nyaa.nyaashop.magic.Utils.Companion.getTextContent
import cat.nyaa.nyaashop.magic.Utils.Companion.isPlayerHoldingSignDecorationItem
import cat.nyaa.nyaashop.magic.Utils.Companion.isRelevantToShopSign
import cat.nyaa.nyaashop.magic.Utils.Companion.producekt
import com.destroystokyo.paper.MaterialTags
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.block.sign.Side
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ShopPlayerListener(private val pluginInstance: NyaaShop) : Listener {
    private val shopDataManager = pluginInstance.getShopDataManager()

    @EventHandler(ignoreCancelled = true)
    fun forSignCreation(event: SignChangeEvent) {
        if (!event.player.hasPermission(Permissions.SHOP_CREATION.node))
            return
        val firstLine = event.line(0) ?: return
        val sellShop =
            getTextContent(firstLine) in pluginInstance.config.shopCreationSellHeader
        val buyShop =
            getTextContent(firstLine) in pluginInstance.config.shopCreationBuyHeader

        val secondLine = event.line(1) ?: return
        val thirdLine = event.line(2)
        val price = getTextContent(secondLine).toDoubleOrNull() ?: return
        val tradeLimit =
            thirdLine?.let { getTextContent(it) }?.toIntOrNull()
                ?: 0
        val offhandItem = event.player.inventory.itemInOffHand
        if (!sellShop && !buyShop) return
        if (price < 0) return

        if (event.block.blockData !is WallSign) return
        val data = event.block.blockData as WallSign
        if (isRelevantToShopSign(event.block.getRelative(data.facing.oppositeFace))) return

        val blockState = event.block.state as Sign
        val blockAgainst = event.block.getRelative(data.facing.oppositeFace)

        if (pluginInstance.config.preventWallSignShopCreatingOnSign) {
            if (MaterialTags.SIGNS.isTagged(blockAgainst.type)) return
        }

        if (pluginInstance.config.preventWallSignShopCreatingOnAir) {
            if (blockAgainst.type.isAir) return
        }

        if (blockAgainst.type in pluginInstance.config.preventWallSignShopCreatingOn)
            return

        if (pluginInstance.config.forceWallSignShopCreatingWithGlass) {
            val blockAgainstThenUp =
                event.block.getRelative(data.facing.oppositeFace).getRelative(
                    BlockFace.UP
                )
            if (!MaterialTags.GLASS.isTagged(blockAgainstThenUp.type)) return
        }

        if (shopDataManager.countPlayerCreatedShops(event.player.uniqueId) >= pluginInstance.config.maximumShopsPerPlayer) {
            event.player.sendMessage(
                pluginInstance.language.tooManyShops.producekt(
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
                Material.MELON_SLICE,1
            ),
            0,
            tradeLimit.coerceAtLeast(0),
            price
        )
        shopDataManager.createNewShop(shop)
        event.player.sendMessage(
            pluginInstance.language.shopCreated.producekt(
                "shopTitle" to when (shop.type) {
                    ShopType.SELL -> pluginInstance.language.sellShopTitle.produce()
                    ShopType.BUY -> pluginInstance.language.buyShopTitle.produce()
                },
                "shopId" to shop.id
            )
        )
    }

    @EventHandler
    fun onPlayerLeaving(event: PlayerQuitEvent) {
        shopDataManager.clearPlayerSelectedShop(event.player.uniqueId)
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
                            event.player.addItemByDrop(
                                shop.itemStack,
                                shop.stock
                            )
                            shopDataManager
                                .deleteShopData(shop)
                            event.player.sendMessage(
                                pluginInstance.language.shopDeleted.producekt(
                                    "shopTitle" to shop.shopTitle(),
                                    "shopID" to shopID
                                )
                            )
                        } else {
                            if (event.player.hasPermission(Permissions.SHOP_ADMIN.node)) {
                                if (MaterialTags.AXES.isTagged(event.player.inventory.itemInMainHand.type)) {
                                    shop.clearItemDisplay()
                                    shopDataManager.unloadShop(shopID)
                                    event.player.sendMessage(
                                        pluginInstance.language.deletedOthersEntry.producekt(
                                            "owner" to Bukkit.getOfflinePlayer(
                                                shop.ownerUniqueID
                                            ).name,
                                            "shopTitle" to shop.shopTitle(),
                                            "shopId" to shopID
                                        )
                                    )
                                    return
                                }
                            }
                            event.isCancelled = true
                            if (pluginInstance.config.sendMessageOnStoppingPlayerBreaking) {
                                event.player.sendMessage(pluginInstance.language.unableToBreak.produce())
                            }
                        }
                    } ?: return
            }
        }
    }

    @EventHandler
    fun forShopRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action == Action.LEFT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.state !is Sign) return
            val sign = block.state as Sign
        if (!Shop.isShopSign(sign)) return
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

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val shopID =
            shopDataManager.getPlayerSelectedShopID(player.uniqueId) ?: return
        val shop = shopDataManager.getShopData(shopID)
        if (shop == null || shop.distanceFrom(event.to) > pluginInstance.config.shopInteractiveRange) {
            shopDataManager.clearPlayerSelectedShop(player.uniqueId)
            if (pluginInstance.config.sendMessageOnPlayerLeavingStore) {
                player.sendMessage(
                    pluginInstance.language.playerLeaveShop.producekt(
                        "shopID" to shopID
                    )
                )
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (isRelevantToShopSign(event.block)) {
            event.isCancelled = true
            if (pluginInstance.config.sendMessageOnStoppingPlayerBreaking)
                event.player.sendMessage(pluginInstance.language.unableToBreak.produce())
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
                val shop = shopDataManager.loadShop(shopID, true)
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