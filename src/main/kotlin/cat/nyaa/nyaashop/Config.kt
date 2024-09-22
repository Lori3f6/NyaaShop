package cat.nyaa.nyaashop

import org.bukkit.DyeColor
import org.bukkit.Material

class Config {
    val maximumShopsPerPlayer = 32
    val shopInteractiveRange = 10
    val shopTradeFeeRateBuyInDouble = 0.02
    val shopTradeFeeRateSellInDouble = 0.02
    val shopInventoryCapacity = 6 * 9 // big chest
    val makeShopInventoryCapacityPresentInSlotsSoItCalculatedByMaximumStackSizeOfItem = true
    val shopCreationSellHeader = listOf("[sell]")
    val shopCreationBuyHeader = listOf("[buy]")
    val sellShopSignColor = DyeColor.LIME
    val buyShopSignColor = DyeColor.LIGHT_BLUE
    val enableShopSignGlowing = true
    val sendMessageOnPlayerLeavingStore = false
    val forceWallSignShopCreatingWithGlass = true
    val preventWallSignShopCreatingOnSign = true
    val preventWallSignShopCreatingOnAir = true
    val preventWallSignShopCreatingOn = listOf(Material.LIGHT, Material.BARRIER)
    val sendMessageOnStoppingPlayerBreaking = false
    val itemDisplayRotation = true
    val itemDisplayRotationSpeedInDegreesPerSecond = 45.0
}