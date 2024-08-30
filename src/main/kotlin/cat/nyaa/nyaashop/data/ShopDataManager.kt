package cat.nyaa.nyaashop.data

import cat.nyaa.nyaashop.NyaaShop
import cat.nyaa.nyaashop.data.db.ShopDBService
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.concurrent.ConcurrentHashMap

// auto async preload + save changes instantly
class ShopDataManager(
    private val sqliteFile: File,
    private val pluginInstance: NyaaShop
) : Listener {

    private val shopDBService: ShopDBService = ShopDBService(sqliteFile)
    private val loadedShopMap = ConcurrentHashMap<Int, Shop>()
    fun getShopData(shopID: Int): Shop? {
        return loadedShopMap[shopID]
    }

    fun deleteShopData(shopID: Int) {
        if (shopDBService.checkShopExistence(shopID)) {
            shopDBService.deleteShop(shopID)
            loadedShopMap.remove(shopID)
        }
    }

    private fun updateShopMeta(shop: Shop) {
        loadedShopMap[shop.id] = shop
        shopDBService.updateShopMeta(shop)
    }

    fun updateTradeLimit(shopID: Int, tradeLimit: Int) {
        val shopData = loadedShopMap[shopID]
        if (shopData != null) {
            shopData.tradeLimit = tradeLimit
            updateShopMeta(shopData)
        }
    }

    fun updateType(shopID: Int, type: ShopType) {
        val shopData = loadedShopMap[shopID]
        if (shopData != null) {
            shopData.type = type
            updateShopMeta(shopData)
        }
    }

    fun updateItemStack(shopID: Int, itemStack: ItemStack) {
        val shopData = loadedShopMap[shopID]
        if (shopData != null) {
            shopData.itemStack = itemStack
            updateShopMeta(shopData)
        }
    }

    fun updatePrice(shopID: Int, price: Double) {
        val shopData = loadedShopMap[shopID]
        if (shopData != null) {
            shopData.price = price
            updateShopMeta(shopData)
        }
    }

    fun updateStock(shopID: Int, stock: Int) {
        val shopData = loadedShopMap[shopID]
        if (shopData != null) {
            shopData.stock = stock
            updateShopMeta(shopData)
        }
    }

    fun createNewShop(shop: Shop) {
        val id = shopDBService.insertShop(shop)
        loadedShopMap[id] = shop
    }

    @EventHandler
    public fun onChunkLoad(event: ChunkLoadEvent) {
        // load all the shops into memory on chunk loading
        // by filtering all the tile entity and check if it is a shop sign
        // and load them asynchronously
        pluginInstance.server.asyncScheduler.runNow(pluginInstance) {
            event.chunk.tileEntities.filterIsInstance<Sign>().forEach {
                val sign = it
                if (Shop.isShopSign(sign)) {
                    val shopID = Shop.getShopIDFromSign(sign) ?: return@forEach
                    val shop = shopDBService.getShopDataFromShopID(shopID)
                        ?: return@forEach
                    loadedShopMap[shopID] = shop
                }
            }
        }
    }

    @EventHandler
    public fun onChunkUnload(event: ChunkUnloadEvent) {
        // remove all the shops from memory on chunk unloading
        pluginInstance.server.asyncScheduler.runNow(pluginInstance) {
            event.chunk.tileEntities.filterIsInstance<Sign>().forEach {
                val sign = it
                if (Shop.isShopSign(sign)) {
                    val shopID = Shop.getShopIDFromSign(sign) ?: return@forEach
                    loadedShopMap.remove(shopID)
                }
            }
        }
    }

    fun shutdown() {
        shopDBService.shutdown()
    }
}