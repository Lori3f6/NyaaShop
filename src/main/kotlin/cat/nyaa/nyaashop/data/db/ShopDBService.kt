package cat.nyaa.nyaashop.data.db

import cat.nyaa.nyaashop.data.Shop
import cat.nyaa.nyaashop.data.ShopType
import org.bukkit.inventory.ItemStack
import java.io.File
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.util.*


class ShopDBService(private val sqliteFile: File) {
    private val initializeTableSQL = """
        CREATE TABLE IF NOT EXISTS "shops" (
        "id"	INTEGER NOT NULL UNIQUE,
        "ownerUniqueID"	TEXT NOT NULL,
        "worldUniqueID"	TEXT NOT NULL,
        "worldX"	INTEGER NOT NULL,
        "worldY"	INTEGER NOT NULL,
        "worldZ"	INTEGER NOT NULL,
        "type"	TEXT NOT NULL DEFAULT 'sell',
        "itemSerializedAsBytesToBase64"	TEXT NOT NULL,
        "stock"	INTEGER NOT NULL DEFAULT 0,
        "tradeLimit"	INTEGER NOT NULL DEFAULT -1,
        "price"	INTEGER NOT NULL DEFAULT 0
        PRIMARY KEY("id" AUTOINCREMENT));
    """.trimIndent()
    private val initializeIndexSQL = """
        CREATE INDEX IF NOT EXISTS "OwnerUniqueIDIndex" ON "shops" (
        	"ownerUniqueID"
        );
    """.trimIndent()
    private var connection: Connection

    init {
        if (!sqliteFile.createNewFile() && !sqliteFile.isFile) throw IOException(
            sqliteFile.absolutePath + " should be a file, but found a directory."
        )
        connection =
            DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.absolutePath)
        connection.autoCommit = true

        initializeTable()
    }

    private fun initializeTable() {
        getConnection().createStatement().use { statement ->
            statement.execute(initializeTableSQL)
            statement.execute(initializeIndexSQL)
        }
    }

    private fun getConnection(): Connection {
        if (connection.isClosed) {
            connection =
                DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.absolutePath)
            connection.autoCommit = true
        }
        return connection
    }


    fun getShopDataFromShopID(shopID: Int): Shop? {
        getConnection().prepareStatement("SELECT * FROM shops WHERE id = ?")
            .use { statement ->
                statement.setInt(1, shopID)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    return Shop(
                        resultSet.getInt("id"),
                        UUID.fromString(resultSet.getString("ownerUniqueID")),
                        UUID.fromString(resultSet.getString("worldUniqueID")),
                        resultSet.getInt("worldX"),
                        resultSet.getInt("worldY"),
                        resultSet.getInt("worldZ"),
                        ShopType.valueOf(resultSet.getString("type")),
                        ItemStack.deserializeBytes(
                            Base64.getDecoder()
                                .decode(resultSet.getString("itemSerializedAsBytesToBase64"))
                        ),
                        resultSet.getInt("stock"),
                        resultSet.getInt("tradeLimit"),
                        resultSet.getDouble("price")
                    )
                }
            }
        return null
    }

    // shop meta = type, item, tradeLimit, price, displayItemID
    fun updateShopMeta(shop: Shop) {
        getConnection().prepareStatement(
            "UPDATE shops SET type = ?, itemSerializedAsBytesToBase64 = ?, tradeLimit = ?, price = ? WHERE id = ?"
        ).use { statement ->
            statement.setString(1, shop.type.name)
            statement.setString(
                2,
                Base64.getEncoder()
                    .encodeToString(shop.itemStack.serializeAsBytes())
            )
            statement.setInt(3, shop.tradeLimit)
            statement.setDouble(4, shop.price)
            statement.setInt(5, shop.id)
            statement.execute()
        }
    }

    fun updateStock(shop: Shop) {
        getConnection().prepareStatement(
            "UPDATE shops SET stock = ? WHERE id = ?"
        ).use { statement ->
            statement.setInt(1, shop.stock)
            statement.setInt(2, shop.id)
            statement.execute()
        }
    }


    fun deleteShop(shopID: Int) {
        getConnection().prepareStatement(
            "DELETE FROM shops WHERE id = ?"
        ).use { statement ->
            statement.setInt(1, shopID)
            statement.execute()
        }
    }

    fun insertShop(shop: Shop): Int {
        getConnection().prepareStatement(
            "INSERT INTO shops (ownerUniqueID, worldUniqueID, worldX, worldY, worldZ, type, itemSerializedAsBytesToBase64, stock, tradeLimit, price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        ).use { statement ->
            statement.setString(1, shop.ownerUniqueID.toString())
            statement.setString(2, shop.worldUniqueID.toString())
            statement.setInt(3, shop.worldX)
            statement.setInt(4, shop.worldY)
            statement.setInt(5, shop.worldZ)
            statement.setString(6, shop.type.name)
            statement.setString(
                7,
                Base64.getEncoder()
                    .encodeToString(shop.itemStack.serializeAsBytes())
            )
            statement.setInt(8, shop.stock)
            statement.setInt(9, shop.tradeLimit)
            statement.setDouble(10, shop.price)
            statement.execute()
            return statement.generatedKeys.getInt(1)
        }
    }

    fun updateShop(shop: Shop) {
        getConnection().prepareStatement(
            "UPDATE shops SET ownerUniqueID = ?, worldUniqueID = ?, worldX = ?, worldY = ?, worldZ = ?, type = ?, itemSerializedAsBytesToBase64 = ?, stock = ?, tradeLimit = ?, price = ? WHERE id = ?"
        ).use { statement ->
            statement.setString(1, shop.ownerUniqueID.toString())
            statement.setString(2, shop.worldUniqueID.toString())
            statement.setInt(3, shop.worldX)
            statement.setInt(4, shop.worldY)
            statement.setInt(5, shop.worldZ)
            statement.setString(6, shop.type.name)
            statement.setString(
                7,
                Base64.getEncoder()
                    .encodeToString(shop.itemStack.serializeAsBytes())
            )
            statement.setInt(8, shop.stock)
            statement.setInt(9, shop.tradeLimit)
            statement.setDouble(10, shop.price)
            statement.setInt(11, shop.id)
            statement.execute()
        }
    }

    fun getShopsByPlayerUniqueID(playerUniqueID: UUID): List<Shop> {
        val shopList = mutableListOf<Shop>()
        getConnection().prepareStatement("SELECT * FROM shops WHERE ownerUniqueID = ?")
            .use { statement ->
                statement.setString(1, playerUniqueID.toString())
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    shopList.add(
                        Shop(
                            resultSet.getInt("id"),
                            UUID.fromString(resultSet.getString("ownerUniqueID")),
                            UUID.fromString(resultSet.getString("worldUniqueID")),
                            resultSet.getInt("worldX"),
                            resultSet.getInt("worldY"),
                            resultSet.getInt("worldZ"),
                            ShopType.valueOf(resultSet.getString("type")),
                            ItemStack.deserializeBytes(
                                Base64.getDecoder()
                                    .decode(resultSet.getString("itemSerializedAsBytesToBase64"))
                            ),
                            resultSet.getInt("stock"),
                            resultSet.getInt("tradeLimit"),
                            resultSet.getDouble("price")
                        )
                    )
                }
            }
        return shopList
    }

    fun getPlayerShopNumbers(playerUniqueID: UUID): Int {
        getConnection().prepareStatement("SELECT COUNT(*) FROM shops WHERE ownerUniqueID = ?")
            .use { statement ->
                statement.setString(1, playerUniqueID.toString())
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    return resultSet.getInt(1)
                }
            }
        return 0
    }

    fun checkShopExistence(shopID: Int): Boolean {
        getConnection().prepareStatement("SELECT * FROM shops WHERE id = ?")
            .use { statement ->
                statement.setInt(1, shopID)
                val resultSet = statement.executeQuery()
                return resultSet.next()
            }
    }

    fun shutdown(){
        connection.close()
    }
}