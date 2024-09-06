package cat.nyaa.nyaashop.data

import cat.nyaa.nyaashop.NyaaShop
import cat.nyaa.ukit.api.UKitAPI
import land.melon.lab.simplelanguageloader.utils.LocaleUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.block.sign.Side
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

data class Shop(
    var id: Int,
    val ownerUniqueID: UUID,
    val worldUniqueID: UUID,
    val worldX: Int,
    val worldY: Int,
    val worldZ: Int,
    var type: ShopType,
    var itemStack: ItemStack,
    var stock: Int,
    var tradeLimit: Int,
    var price: Double
) {
    companion object {
        val shopIDPDCKey = NamespacedKey(NyaaShop.instance, "shop_id")
        fun isShopSign(sign: Sign): Boolean {
            return sign.persistentDataContainer.has(
                shopIDPDCKey,
                PersistentDataType.INTEGER
            )
        }

        fun getShopIDFromSign(sign: Sign): Int? {
            return sign.persistentDataContainer.get(
                shopIDPDCKey,
                PersistentDataType.INTEGER
            )
        }

        fun clearShopIDPDC(sign: Sign) {
            sign.persistentDataContainer.remove(shopIDPDCKey)
            sign.persistentDataContainer.remove(UKitAPI.signEditLockTagKey)
            sign.update()
        }
    }

    private fun getSignBlock(): Block {
        return Bukkit.getWorld(worldUniqueID)
            ?.getBlockAt(worldX, worldY, worldZ) ?: throw IllegalStateException(
            "Block not exist"
        )
    }

    private fun getWallSign(): WallSign {
        val blockState = getSignBlock().state.blockData
        if (blockState !is WallSign) {
            throw IllegalStateException("Shop block is not a wall sign!")
        }
        return blockState
    }

    private fun getSignFacing(): BlockFace {
        return getWallSign().facing
    }

    private fun itemDisplayLocation(): Location {
        val baseBlock = getSignBlock().getRelative(getSignFacing().oppositeFace)
        return baseBlock.location.apply { add(0.5, 1.5, 0.5) }
    }

    fun refreshItemDisplay(): ItemDisplay {
        clearItemDisplay()
        return createItemDisplay()
    }

    fun getRemainingStock(): Int {
        return when (type) {
            ShopType.BUY -> tradeLimit - stock
            ShopType.SELL -> stock
        }
    }

    fun distanceFrom(location: Location): Double {
        if (location.world.uid != worldUniqueID) {
            return Double.MAX_VALUE
        }
        return distanceFrom(
            Triple(
                location.x,
                location.y,
                location.z
            )
        )
    }

    private fun distanceFrom(location: Triple<Double, Double, Double>): Double {
        val (x, y, z) = location
        return sqrt(
            (x - worldX).pow(2) + (y - worldY).pow(2) + (z - worldZ).pow(2)
        )
    }

    fun clearItemDisplay() {
        val itemDisplayLoc = itemDisplayLocation()
        itemDisplayLoc.getNearbyEntitiesByType(ItemDisplay::class.java, 1.0)
            .forEach {
                if (it.persistentDataContainer.get(
                        shopIDPDCKey,
                        PersistentDataType.INTEGER
                    ) == id
                ) {
                    it.remove()
                }
            }
    }

    private fun createItemDisplay(): ItemDisplay {
        val location = itemDisplayLocation()
        val itemDisplay =
            location.world?.spawn(location, ItemDisplay::class.java)
        itemDisplay!!.setItemStack(itemStack)
        itemDisplay.billboard = Display.Billboard.FIXED
        itemDisplay.displayWidth = 0.9F
        itemDisplay.displayHeight = 0.9F
        itemDisplay.setRotation(blockFaceIntoYaw(getSignFacing()), 0F)
        itemDisplay.persistentDataContainer.set(
            shopIDPDCKey,
            PersistentDataType.INTEGER, id
        )
        return itemDisplay
    }

    private fun blockFaceIntoYaw(face: BlockFace): Float {
        return when (face) {
            BlockFace.NORTH -> 0F
            BlockFace.EAST -> 90F
            BlockFace.SOUTH -> 180F
            BlockFace.WEST -> 270F
            else -> 0F
        }
    }

    fun writeShopIDPDC() {
        val block = getSignBlock()
        if (block.state !is Sign) {
            throw IllegalStateException("Shop block is not a sign!")
        }
        val sign = block.state as Sign
        sign.persistentDataContainer.set(
            shopIDPDCKey,
            PersistentDataType.INTEGER,
            id
        )
        sign.persistentDataContainer.set(
            UKitAPI.signEditLockTagKey,
            PersistentDataType.BOOLEAN,
            true
        )
        sign.update()
    }

    fun updateSign() {
        val block = getSignBlock()
        if (block.state !is Sign) {
            throw IllegalStateException("Shop block is not a sign!")
        }
        val sign = block.state as Sign
        val signSide = sign.getSide(Side.FRONT)
        for (i in 0..3) {
            signSide.line(
                i,
                NyaaShop.instance.language.shopSign[i].produceAsComponent(
                    "title" to when (type) {
                        ShopType.BUY -> NyaaShop.instance.language.buyShopTitle.produce()
                        ShopType.SELL -> NyaaShop.instance.language.sellShopTitle.produce()
                    },
                    "item" to
                            LocaleUtils.getTranslatableItemComponent(itemStack),
                    "price" to price,
                    "currencyName" to
                            NyaaShop.instance.economyProvider.currencyNamePlural(),
                    "remaining" to getRemainingStock()
                )
            )
        }
        sign.isWaxed = true
        sign.update()
    }
}