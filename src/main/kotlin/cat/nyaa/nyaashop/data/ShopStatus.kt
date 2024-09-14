package cat.nyaa.nyaashop.data

import cat.nyaa.nyaashop.NyaaShop
import net.kyori.adventure.text.ComponentLike

enum class ShopStatus(private val messageSupplier: () -> ComponentLike) {
    ACTIVE({ NyaaShop.instance.language.shopStatusForDetailActive.produceAsComponent() }),
    STANDBY({ NyaaShop.instance.language.shopStatusForDetailStandBy.produceAsComponent() }),
    INACCESSIBLE({ NyaaShop.instance.language.shopStatusForDetailInaccessible.produceAsComponent() });

    fun getStatusMessage(): ComponentLike {
        return messageSupplier.invoke()
    }
}