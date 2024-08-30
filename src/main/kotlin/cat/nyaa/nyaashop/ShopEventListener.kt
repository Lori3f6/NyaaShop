package cat.nyaa.nyaashop

import cat.nyaa.nyaashop.data.Shop
import cat.nyaa.nyaashop.data.ShopType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent

class ShopEventListener(private val pluginInstance: NyaaShop) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun forSignCreation(event: SignChangeEvent) {
        val firstLine = event.line(0) ?: return
        val sellShop =
            getTextContent(firstLine) in pluginInstance.config.shopCreationSellHeader
        val buyShop =
            getTextContent(firstLine) in pluginInstance.config.shopCreationBuyHeader

        val secondLine = event.line(1) ?: return
        val price = getTextContent(secondLine).toDoubleOrNull() ?: return

        if (!sellShop && !buyShop) return
        //shop creation
        val shop = Shop(
            -1,
            event.player.uniqueId,
            event.block.world.uid,
            event.block.x,
            event.block.y,
            event.block.z,
            if (sellShop) ShopType.SELL else ShopType.BUY,
            event.player.inventory.itemInOffHand,
            0,
            0,
            price
        )
        pluginInstance.getShopDataManager().createNewShop(shop)
        event.player.sendMessage(pluginInstance.language.shop_created.produce())
    }

    private fun getTextContent(component: Component): String {
        if (component is TextComponent) {
            return component.content()
        } else
            throw IllegalArgumentException("Component is not a TextComponent")
    }
}