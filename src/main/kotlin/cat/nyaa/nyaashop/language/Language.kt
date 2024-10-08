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
    val pluginReloaded = Text.of("&7NyaaShop reloaded")
    val permissionDenied = Text.of("&cPermission denied")
    val playerOnlyCommand =
        Text.of("This command can only be executed by players in game except /ns reload")
    val shopCreated = Text.of("Shop created! right click to manage your shop!")

    val shopSign = listOf(
        Text.of("{title}"),
        Text.of("{item}"),
        Text.of("{price} {currencyName}"),
        Text.of("{remaining} Remaining")
    )
    val shopInteractOwnerBuy = Text.of(
        "&7You are viewing your {shopTitle} &8(&3#{shopId}&8)&7:",
        "&7Item: &6{item} {changeItemButton}",
        "&7Price: &6{price} {currencyName} {changePriceButton}",
        "&7Stock: &6{stock} {addStockButton} {retrieveStockButton}",
        "&7Trade Limit (only valid for {buyShopTitle}): &6{tradeLimit} {changeTradeLimitButton}",
        "&7Click to manage your shop"
    )
    val shopInteractOwnerSell = Text.of(
        "&7You are viewing your {shopTitle} &8(&3#{shopId}&8)&7:",
        "&7Item: &6{item} {changeItemButton}",
        "&7Price: &6{price} {currencyName} {changePriceButton}",
        "&7Stock: &6{stock} {addStockButton} {retrieveStockButton}",
        "&7Trade Limit (only valid for {buyShopTitle}): &6{tradeLimit} {changeTradeLimitButton}",
        "&7Click to manage your shop"
    )
    val shopInteractGuest = Text.of(
        "&7You are viewing a {shopTitle} from {owner}:",
        "&7Item: &6{item}",
        "&7Price: &6{price} {currencyName}",
        "&7Stock: &6{stock} {tradeButton}",
    )
    val playerLeaveShop = Text.of("You left the shop {shopID}")
    val shopNotValid = Text.of("You have not selected a shop or the shop you selected is invalid now, right click one to select")
    val cantChangeOthersShop =
        Text.of("You can't change the setting of other player's shop")
    val changeItemButtonText = Text.of("&7[&6Change Item&7]")
    val changeItemButtonDescription =
        Text.of("Execute to change the shop item to your main hand")
    val unableToChangeItemStock =
        Text.of("Unable to change Item due to stock is not 0")
    val unableToChangeItemToAir =
        Text.of("Unable to change Item to air")
    val changePriceButtonText = Text.of("&7[&6Change Price&7]")
    val changePriceButtonDescription =
        Text.of("Execute to change the shop price")
    val notValidNumber = Text.of("{number} is not a valid number")
    val addStockButtonText = Text.of("&7[&6Add Stock&7]")
    val addStockButtonDescription = Text.of("add stock to the shop")
    val requestFailedItemNotEnough =
        Text.of("You don't have enough {item} to complete this request")
    val addStockFailedCapacityExceed =
        Text.of("Exceed capacity, you can only add at most {capacity} stock")
    val stockAdded =
        Text.of("Added {item} x {amount} to the shop, it's now {stock}/{capacity} loaded")
    val retrieveStockButtonText = Text.of("&7[&6Retrieve Stock&7]")
    val retrieveStockButtonDescription =
        Text.of("retrieve stock from the shop")
    val retrieveStockFailedStockNotEnough =
        Text.of("This shop only has {item} x {amount} to retrieve")
    val stockRetrieved =
        Text.of("Retrieved {item} x {amount} from the shop, it's now {stock}/{capacity} loaded")
    val changeTradeLimitButtonText = Text.of("&7[&6Change Trade Limit&7]")
    val changeTradeLimitButtonDescription =
        Text.of("Execute to change the shop trade limit (only for {sellShopTitle})")
    val tradeLimitTooHigh =
        Text.of("Trade limit unable to exceed {inventoryCapacity}")
    val buyTradeButtonText = Text.of("&7[&6Buy&7]")
    val buyTradeButtonDescription = Text.of("Buy {item} from this shop")
    val sellTradeButtonText = Text.of("&7[&6Sell&7]")
    val sellTradeButtonDescription = Text.of("Sell {item} to this shop")
    val unMatchedShopType =
        Text.of("Shop type not match, {shopTitle} can't perform this action")
    val buySuccessNotice = Text.of(
        "Successfully buy {item} x {amount} from {owner}'s store",
        "Total cost: {cost}{currencyName}, paid tax {tax}{currencyName}({taxPercentage}%)"
    )
    val sellSuccessNotice = Text.of(
        "Successfully sell {item} x {amount} to {owner}'s store",
        "Total cost: {cost}{currencyName}, paid tax {tax}{currencyName}({taxPercentage}%)"
    )

    val buyShopTitle = Text.of("&9[Buy Shop]")
    val sellShopTitle = Text.of("&6[Sell Shop]")
    val shopDeleted = Text.of("Shop deleted!")
    val unableToBreak =
        Text.of("You are not allowed to break this shop sign!")
    val playerNotEnoughMoney = Text.of("You don't have enough money ({money}{currencyName}) to complete this purchase")
    val merchantNotEnoughMoney = Text.of("The merchant doesn't have enough money to complete this purchase")
    val merchantOutOfStock = Text.of("The merchant doesn't have enough stock to complete this purchase")
    val merchantStorageFull = Text.of("The merchant's storage is full or the stock reach the reade limit")
    val transactionFailedUnknown = Text.of("Transaction failed due to unknown reason")
    val tooManyShops = Text.of("You have {limit} shops already and can't create more, use &b/ns list&7 to see and manage your shops")
    val buyShopTradeNoticeForOwner = Text.of(
        "{player} sold {item} x {amount} to your {shopTitle}#{shopId}",
        "Paid {cost}{currencyName}, included tax {tax}{currencyName}({taxPercentage}%)"
    )
    val sellShopTradeNoticeForOwner = Text.of(
        "{player} ordered {item} x {amount} from your {shopTitle}#{shopId}",
        "Got {income}{currencyName}, customer has paid tax {tax}{currencyName}({taxPercentage}%)"
    )
    val offlineMessageSenderName = Text.of("&7NyaaShop")
    val selfNoShopYet = Text.of("&8You have no shops yet")
    val othersNoShopYet = Text.of("&8{player} have no shops yet")
    val selfShopDetailsIntro = Text.of("These are your shops:")
    val othersShopDetailsIntro = Text.of("These are {player}'s shops:")
    val playerNotExist = Text.of("{name} has no record on this server, check name spelling?")
    val worldNamePlaceHolderIfInvalid = Text.of("Invalid World")
    val shopDetail = Text.of(
        "&3{index}&8# &8... {shopTitle}#{shopId}(x{worldX}, y{worldY}, z{worldZ}, {worldName})",
        "Contains {item} x {stock}/{shopCapacity}, trade remaining {tradeRemaining}, {status}"
    )
    val shopStatusForDetailActive = Text.of("Active")
    val shopStatusForDetailStandBy = Text.of("StandingBy")
    val shopStatusForDetailInaccessible = Text.of("Inaccessible")
    val descriptionForShopStatusActive =
        Text.of("Your shop is loaded and ready for treading")
    val descriptionForShopStatusStandBy =
        Text.of("The chunk your shop belongs to is not loaded")
    val descriptionForShopStatusInaccessible = Text.of(
        "This shop is broken and no longer working",
        "click to purge the shop",
        "note: this action will retrieve all the stocks at a time"
    )
    val shopPurged = Text.of("Purged inaccessible shop {shopTitle}#{shopId}")
    val shopNotPurged = Text.of("Unable to purge {shopTitle}#{shopId} for now, might because the location of shop isn't load anymore")
    val deletedOthersEntry = Text.of("You deleted the entry of {owner}'s {shopTitle}#{shopId}")
}