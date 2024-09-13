package cat.nyaa.nyaashop

import org.bukkit.DyeColor

class Config {
    val maximumShopsPerPlayer = 32
    val shopInteractiveRange = 10
    val shopTradeFeeRateBuyInDouble = 0.02
    val shopTradeFeeRateSellInDouble = 0.02
    val shopInventoryCapacity = 6 * 9 * 64 // big chest
    val shopCreationSellHeader = listOf("[sell]")
    val shopCreationBuyHeader = listOf("[buy]")
    val sellShopSignColor = DyeColor.LIME
    val buyShopSignColor = DyeColor.LIGHT_BLUE
    val enableShopSignGlowing = true
    val sendMessageOnPlayerLeavingStore = false
    val forceWallSignShopCreateWithGlass = true
    val preventWallSignShopCreateOnSign = true
    val sendMessageOnStoppingPlayerBreaking = false
}