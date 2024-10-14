package cat.nyaa.nyaashop.data

import cat.nyaa.nyaashop.NyaaShop
import cat.nyaa.nyaashop.data.db.ShopDBService
import cat.nyaa.nyaashop.magic.Utils
import cat.nyaa.nyaashop.magic.Utils.Companion.produceAsComponentkt
import cat.nyaa.nyaashop.magic.Utils.Companion.producekt
import land.melon.lab.simplelanguageloader.utils.ItemUtils
import org.bukkit.Bukkit
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*

// auto preload + save changes instantly
class ShopDataManager(
    private val sqliteFile: File,
    private val pluginInstance: NyaaShop
) {
    val shopDBService: ShopDBService = ShopDBService(sqliteFile)
    private val loadedShopMap = mutableMapOf<Int, Shop>()
    private val shopSelectionMap = mutableMapOf<UUID, Int>()
    private val changeItemButton = Utils.suggestCommandButtonOf(
        pluginInstance.language.changeItemButtonText.producekt(),
        pluginInstance.language.changeItemButtonDescription.producekt(),
        "/ns set item mainhand"
    )
    private val changePriceButton = Utils.suggestCommandButtonOf(
        pluginInstance.language.changePriceButtonText.producekt(),
        pluginInstance.language.changePriceButtonDescription.producekt(),
        "/ns set price "
    )
    private val addStockButton = Utils.suggestCommandButtonOf(
        pluginInstance.language.addStockButtonText.producekt(),
        pluginInstance.language.addStockButtonDescription.producekt(),
        "/ns stock add "
    )
    private val retrieveStockButton = Utils.suggestCommandButtonOf(
        pluginInstance.language.retrieveStockButtonText.producekt(),
        pluginInstance.language.retrieveStockButtonDescription.producekt(),
        "/ns stock retrieve "
    )
    private val changeTradeLimitButton = Utils.suggestCommandButtonOf(
        pluginInstance.language.changeTradeLimitButtonText.producekt(),
        pluginInstance.language.changeTradeLimitButtonDescription.producekt(
            "sellShopTitle" to pluginInstance.language.sellShopTitle.producekt()
        ),
        "/ns set tradelimit "
    )
    private val buyTradeButton = Utils.suggestCommandButtonOf(
        pluginInstance.language.buyTradeButtonText.producekt(),
        pluginInstance.language.buyTradeButtonDescription.producekt(),
        "/ns buy "
    )
    private val sellTradeButton = Utils.suggestCommandButtonOf(
        pluginInstance.language.sellTradeButtonText.producekt(),
        pluginInstance.language.sellTradeButtonDescription.producekt(),
        "/ns sell "
    )

    init {
        //load all sign shop from loaded chunks
        pluginInstance.server.worlds.forEach { world ->
            world.loadedChunks.forEach { chunk ->
                chunk.tileEntities.filterIsInstance<Sign>()
                    .forEach innerLoop@{ sign ->
                        if (Shop.isShopSign(sign)) {
                            val shopID =
                                Shop.getShopIDFromSign(sign) ?: return@innerLoop
                            loadShop(shopID)
                        }
                    }
            }
        }
    }

    fun getPlayerSelectedShopID(player: UUID): Int? {
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

    fun deleteShop(shop: Shop) {
        shopDBService.deleteShop(shop.id)
        loadedShopMap.remove(shop.id)
        shop.clearItemDisplay()
    }

    private fun updateShopMetaToDB(shop: Shop) {
        loadedShopMap[shop.id] = shop
        shopDBService.updateShopMeta(shop)
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
            shopData.updateSign()
            shopData.updateItemDisplay()
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
            shopDBService.updateStock(shopData, stock)
            shopData.updateSign()
        }
    }

    fun getStock(shopID: Int): Int {
        val shopData = loadedShopMap[shopID]
        return shopData?.stock ?: -1
    }

    fun countPlayerCreatedShops(uniqueID: UUID): Int {
        return shopDBService.countPlayerCreatedShops(uniqueID)
    }

    fun getPlayerCreatedShops(uniqueID: UUID): List<Shop> {
        return shopDBService.getPlayerCreatedShops(uniqueID)
    }

    fun createNewShop(shop: Shop) {
        val id = shopDBService.insertShop(shop)
        shop.id = id
        shop.initializeShopSign()
        pluginInstance.server.scheduler.runTaskLater(
            pluginInstance,
            { _ -> shop.updateSign() },
            1
        )
        loadShop(id)
    }

    fun sendShopDetailsMessageForOwner(player: Player, shop: Shop) {
        player.sendMessage(
            when (shop.type) {
                ShopType.BUY -> pluginInstance.language.shopInteractOwnerBuy
                ShopType.SELL -> pluginInstance.language.shopInteractOwnerSell
            }.produceAsComponentkt(
                "shopTitle" to shop.shopTitle(),
                "shopId" to shop.id,
                "item" to ItemUtils.itemTextWithHover(shop.itemStack),
                "changeItemButton" to changeItemButton,
                "price" to shop.price,
                "changePriceButton" to changePriceButton,
                "currencyName" to pluginInstance.economyProvider.currencyNamePlural(),
                "stock" to shop.stock,
                "tax" to shop.price * shopFeeRate(shop.type),
                "capacity" to shop.stockCapacity(),
                "addStockButton" to addStockButton,
                "retrieveStockButton" to retrieveStockButton,
                "tradeLimit" to shop.tradeLimit,
                "buyShopTitle" to pluginInstance.language.buyShopTitle.producekt(),
                "changeTradeLimitButton" to changeTradeLimitButton
            )
        )
    }

    fun sendShopDetailsMessageForGuest(player: Player, shop: Shop) {
        player.sendMessage(
            pluginInstance.language.shopInteractGuest.produceAsComponentkt(
                "shopTitle" to shop.shopTitle(),
                "owner" to Bukkit.getOfflinePlayer(shop.ownerUniqueID).name,
                "shopId" to shop.id,
                "item" to ItemUtils.itemTextWithHover(shop.itemStack),
                "price" to shop.price,
                "tax" to shop.price * shopFeeRate(shop.type),
                "currencyName" to pluginInstance.economyProvider.currencyNamePlural(),
                "stock" to shop.remainingTradeStock(),
                "capacityAbleToTrade" to shop.capacityAbleToTrade(),
                "capacity" to shop.stockCapacity(),
                "tradeButton" to when (shop.type) {
                    ShopType.SELL -> buyTradeButton
                    ShopType.BUY -> sellTradeButton
                }
            )
        )
    }

    private fun shopFeeRate(type: ShopType): Double {
        return when (type) {
            ShopType.BUY -> pluginInstance.config.shopTradeFeeRateBuyInDouble
            ShopType.SELL -> pluginInstance.config.shopTradeFeeRateSellInDouble
        }
    }

    fun loadShop(
        shopID: Int,
    ): Shop? {
        val shop = shopDBService.getShopDataFromShopID(shopID) ?: return null
        loadedShopMap[shopID] = shop
        shop.updateItemDisplay()

        pluginInstance.logger.fine("Loaded shop #$shopID")
        return shop
    }

    fun unloadShop(shopID: Int) {
        loadedShopMap.remove(shopID)
        pluginInstance.logger.fine("Unloaded shop #$shopID")
    }

    fun getAllLoadedShops(): Collection<Shop> {
        return Collections.unmodifiableCollection<Shop>(loadedShopMap.values)
    }

    private fun cleanUpSign(sign:Sign){
        sign.persistentDataContainer.remove(Shop.shopIDPDCKey)
    }

    fun shutdown() {
        shopDBService.shutdown()
    }
}