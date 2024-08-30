package cat.nyaa.nyaashop.language

import land.melon.lab.simplelanguageloader.components.Text

class Language {
    val shop_created = Text.of("Shop created! right click to manage your shop!")
    val shop_sign_line1 = Text.of("[{type}]")
    val shop_sign_line2 = Text.of("{item}")
    val shop_sign_line3 = Text.of("{price} {currencyName}")
    val shop_sign_line4 = Text.of("{remaining} Remaining")
}