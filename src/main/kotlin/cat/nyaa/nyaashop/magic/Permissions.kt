package cat.nyaa.nyaashop.magic

enum class Permissions(val node: String) {
    SHOP_CREATION("nyaashop.create"),
    SHOP_USE("nyaashop.use"),
    SHOP_ADMIN("nyaashop.admin"),
    SHOP_RELOAD("nyaashop.reload"),
}