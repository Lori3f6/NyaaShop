package cat.nyaa.nyaashop.language

import land.melon.lab.simplelanguageloader.components.Text

class Language {
    val help = Text.of(
        "&7[&6NyaaShop&7]",
        "&7Put a Sign on to a block and put [buy] or [sell] on the first line",
        "&7Price on the second line to create a shop",
        "&7/ns set &6<type|item|price|tradlimit> &7- Edit shop properties",
        "&7/ns stock &6<add|retrieve> &7- add/retrieve stock",
        "&7/ns buy <number>&7- Buy item from a shop",
        "&7/ns sell <number>&7- Sell item to a shop"
    )
    val plugin_reloaded = Text.of("&7NyaaShop reloaded")
    val permission_denied = Text.of("&cPermission denied")
    val player_only_command = Text.of("This command can only be executed by players in game except /ns reload")
    val shop_created = Text.of("Shop created! right click to manage your shop!")

    val shop_sign = listOf(
        Text.of("[{type}]"),
        Text.of("{item}"),
        Text.of("{price} {currencyName}"),
        Text.of("{remaining} Remaining")
    )
    val shop_interact_owner = Text.of(
        "&7You are viewing your {shop_title} &8(#&4{id}&8)&7:",
        "&7Item: &6{item} {change_item_button}",
        "&7Price: &6{price} {currencyName} {change_price_button}",
        "&7Stock: &6{stock} {add_stock_button} {retrieve_stock_button}",
        "&7Trade Limit: &6{tradeLimit} {change_trade_limit_button}",
        "&7Click to manage your shop"
    )
    val shop_interact_guest = Text.of(
        "&7You are viewing a {shop_title} from {owner}:",
        "&7Item: &6{item}",
        "&7Price: &6{price} {currencyName}",
        "&7Stock: &6{stock}",
        "&7{trade_button}"
    )
    val player_leave_shop = Text.of("You left the shop {shopID}")
    val shop_not_selected = Text.of("You have not selected a shop, right click one to select")
    val shop_not_valid = Text.of("The shop you selected is not valid now")
    val cant_change_others_shop = Text.of("You can't change the setting of other player's shop")
    val change_item_button_text = Text.of("&7[&6Change Item&7]")
    val change_item_button_description =
        Text.of("Execute to change the shop item to your main hand")
    val unable_to_change_item_stock =
        Text.of("Unable to change Item due to stock is not 0")
    val unable_to_change_item_to_air =
        Text.of("Unable to change Item to air")
    val change_price_button_text = Text.of("&7[&6Change Price&7]")
    val change_price_button_description =
        Text.of("Execute to change the shop price")
    val not_a_valid_number = Text.of("Not a valid number")
    val add_stock_button_text = Text.of("&7[&6Add Stock&7]")
    val add_stock_button_description = Text.of("add stock to the shop")
    val retrieve_stock_button_text = Text.of("&7[&6Retrieve Stock&7]")
    val retrieve_stock_button_description =
        Text.of("retrieve stock from the shop")
    val change_trade_limit_button_text = Text.of("&7[&6Change Trade Limit&7]")
    val change_trade_limit_button_description =
        Text.of("Execute to change the shop trade limit (only for {sellShopTitle})")
    val buy_trade_button_text = Text.of("&7[&6Buy&7]")
    val buy_trade_button_description = Text.of("Buy {item} from this shop")
    val sell_trade_button_text = Text.of("&7[&6Sell&7]")
    val sell_trade_button_description = Text.of("Sell {item} to this shop")
    val buy_success_notice = Text.of(
        "Successfully buy {item} x {amount} from {owner}'s store",
        "Total cost: {cost}{currencyName}, paid tax {tax}{currencyName}({taxPercentage}%)"
    )
    val sell_success_notice = Text.of(
        "Successfully sell {item} x {amount} to {owner}'s store",
        "Total cost: {cost}{currencyName}, paid tax {tax}{currencyName}({taxPercentage}%)"
    )

    val buyShopTitle = Text.of("&9[Buy Shop]")
    val sellShopTitle = Text.of("&6[Sell Shop]")
    val shop_deleted = Text.of("Shop deleted!")
    val unable_to_break =
        Text.of("You are not allowed to break this shop sign!")
}