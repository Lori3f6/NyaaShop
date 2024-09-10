package cat.nyaa.nyaashop

class Config {
    val maximumShopsPerPlayer = 32
    val shopInteractiveRange = 10
    val shopTradeFeeRateBuyInDouble = 0.02
    val shopTradeFeeRateSellInDouble = 0.02
    val shopInventoryCapacity = 6 * 9 * 64 // big chest
    val shopCreationSellHeader = listOf("[sell]")
    val shopCreationBuyHeader = listOf("[buy]")
}