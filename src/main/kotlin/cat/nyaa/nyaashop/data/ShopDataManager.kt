package cat.nyaa.nyaashop.data

import cat.nyaa.nyaashop.NyaaShop
import cat.nyaa.nyaashop.data.db.ShopDBService
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*

// auto async preload + save changes instantly
class ShopDataManager(
    private val sqliteFile: File,
    private val pluginInstance: NyaaShop
) : Listener {
    private val shopDBService: ShopDBService = ShopDBService(sqliteFile)
    private val loadedShopMap = mutableMapOf<Int, Shop>()
    private val shopSelectionMap = mutableMapOf<UUID, Int>()

    init {
        //load all sign shop from loaded chunks
        pluginInstance.server.worlds.forEach { world ->
            world.loadedChunks.forEach { chunk ->
                chunk.tileEntities.filterIsInstance<Sign>()
                    .forEach innerLoop@{ sign ->
                        if (Shop.isShopSign(sign)) {
                            val shopID =
                                Shop.getShopIDFromSign(sign) ?: return@innerLoop
                            val shop =
                                shopDBService.getShopDataFromShopID(shopID)
                                    ?: return@innerLoop
                            loadedShopMap[shopID] = shop
                            shop.refreshItemDisplay()
                            pluginInstance.logger.info("Loaded shop #$shopID")
                        }
                    }
            }
        }
    }

    fun getPlayerSelectedShop(player: UUID): Int? {
        return shopSelectionMap[player]
    }

    fun makePlayerSelectShop(player: UUID, shop: Shop) {
        shopSelectionMap[player] = shop.id
    }

    fun clearPlayerSelectedShop(player: UUID) {
        shopSelectionMap.remove(player)
    }

    fun getShopData(shopID: Int): Shop? {
        return loadedShopMap[shopID]
    }

    fun deleteShopData(shop: Shop) {
        shopDBService.deleteShop(shop.id)
        loadedShopMap.remove(shop.id)
        shop.clearItemDisplay()
    }

    private fun updateShopMetaToDB(shop: Shop) {
        loadedShopMap[shop.id] = shop
        shopDBService.updateShopMeta(shop)
        shop.refreshItemDisplay()
    }

    fun updateTradeLimit(shopID: Int, tradeLimit: Int) {
        val shopData = loadedShopMap[shopID]
        if (shopData != null) {
            shopData.tradeLimit = tradeLimit
            updateShopMetaToDB(shopData)
            shopData.updateSign()
        }
    }

    fun updateType(shopID: Int, type: ShopType) {
        val shopData = loadedShopMap[shopID]
        if (shopData != null) {
            shopData.type = type
            updateShopMetaToDB(shopData)
            shopData.updateSign()
        }
    }

    fun updateItemStack(shopID: Int, itemStack: ItemStack) {
        val shopData = loadedShopMap[shopID]
        if (shopData != null) {
            shopData.itemStack = itemStack
            updateShopMetaToDB(shopData)
            shopData.refreshItemDisplay()
            shopData.updateSign()
        }
    }

    fun updatePrice(shopID: Int, price: Double) {
        val shopData = loadedShopMap[shopID]
        if (shopData != null) {
            shopData.price = price
            updateShopMetaToDB(shopData)
            shopData.updateSign()
        }
    }

    fun updateStock(shopID: Int, stock: Int) {
        val shopData = loadedShopMap[shopID]
        if (shopData != null) {
            shopData.stock = stock
            updateShopMetaToDB(shopData)
            shopData.updateSign()
        }
    }

    fun getStock(shopID: Int): Int {
        val shopData = loadedShopMap[shopID]
        return shopData?.stock ?: -1
    }

    fun createNewShop(shop: Shop) {
        val id = shopDBService.insertShop(shop)
        loadedShopMap[id] = shop
        shop.id = id
        shop.writeShopIDPDC()
        shop.refreshItemDisplay()
        pluginInstance.server.scheduler.runTaskLater(
            pluginInstance,
            { _ -> shop.updateSign() },
            1
        )
    }

    private fun loadShop(
        shopID: Int,
        refreshItemDisplay: Boolean = false
    ): Boolean {
        val shop = shopDBService.getShopDataFromShopID(shopID) ?: return false
        loadedShopMap[shopID] = shop
        if (refreshItemDisplay) {
            shop.refreshItemDisplay()
        }
        pluginInstance.logger.info("Loaded shop #$shopID")
        return true
    }

    private fun unloadShop(shopID: Int) {
        loadedShopMap.remove(shopID)
        pluginInstance.logger.info("Unloaded shop #$shopID")
    }

    private fun cleanUpSign(sign:Sign){
        sign.persistentDataContainer.remove(Shop.shopIDPDCKey)
    }

    @EventHandler
    public fun onChunkLoad(event: ChunkLoadEvent) {
        // load all the shops into memory on chunk loading
        // by filtering all the tile entity and check if it is a shop sign
        // and load them into memory
        event.chunk.tileEntities.filterIsInstance<Sign>().forEach { sign ->
            if (Shop.isShopSign(sign)) {
                val shopID = Shop.getShopIDFromSign(sign) ?: return@forEach
                val shopExist = loadShop(shopID)
                if(!shopExist){

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
                unloadShop(shopID)
            }
        }
    }

    fun shutdown() {
        shopDBService.shutdown()
    }
}