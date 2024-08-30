package cat.nyaa.nyaashop.data

import cat.nyaa.nyaashop.NyaaShop
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

data class Shop(
    val id: Int,
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
        private val shopIDPDCKey = NamespacedKey(NyaaShop.instance, "shop_id")
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
    }

    fun getSignBlock(): Block {
        return Bukkit.getWorld(worldUniqueID)
            ?.getBlockAt(worldX, worldY, worldZ) ?: throw IllegalStateException(
            "Block not exist"
        )
    }

    fun getWallSign(): WallSign {
        val block = getSignBlock()
        if (block.state !is WallSign) {
            throw IllegalStateException("Shop block is not a wall sign!")
        }
        return block.state as WallSign
    }

    fun getSignFacing(): BlockFace {
        return getWallSign().facing
    }

    fun itemDisplayLocation(): Location {
        val baseBlock = getSignBlock().getRelative(getSignFacing().oppositeFace)
        return baseBlock.location.apply { add(0.5, 1.5, 0.5) }
    }

    fun refreshItemDisplay(): ItemDisplay {
        clearItemDisplay()
        return createItemDisplay()
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

    fun createItemDisplay(): ItemDisplay {
        val location = itemDisplayLocation()
        val itemDisplay =
            location.world?.spawn(location, ItemDisplay::class.java)
        itemDisplay!!.setItemStack(itemStack)
        itemDisplay.billboard = Display.Billboard.FIXED
        itemDisplay.displayWidth = 0.9F
        itemDisplay.displayHeight = 0.9F
        itemDisplay.setRotation(blockFaceIntoYaw(getSignFacing()), 0F)
        return itemDisplay
    }

    fun blockFaceIntoYaw(face: BlockFace): Float {
        return when (face) {
            BlockFace.NORTH -> 0F
            BlockFace.EAST -> 90F
            BlockFace.SOUTH -> 180F
            BlockFace.WEST -> 270F
            else -> 0F
        }
    }

    fun updateSign() {
        val block = getSignBlock()
        if (block.state !is Sign) {
            throw IllegalStateException("Shop block is not a sign!")
        }
        val sign = block.state as Sign
        sign.getSide(Side.FRONT).line(0,NyaaShop.instance.language.shop_sign_line1.produceAsComponent(
            Pair.of("type", type.name)
        ))
    }
}