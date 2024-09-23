package cat.nyaa.nyaashop

import cat.nyaa.ecore.EconomyCore
import cat.nyaa.nyaashop.data.Shop
import cat.nyaa.nyaashop.data.ShopDataManager
import cat.nyaa.nyaashop.language.Language
import land.melon.lab.simplelanguageloader.SimpleLanguageLoader
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin

class NyaaShop : JavaPlugin() {
    companion object {
        lateinit var instance: NyaaShop
            private set
    }

    private val simpleLanguageLoader = SimpleLanguageLoader()
    private lateinit var dataManager: ShopDataManager

    lateinit var economyProvider: EconomyCore
        private set
    lateinit var config: Config
        private set
    lateinit var language: Language
        private set
    var isUkitSetup: Boolean = false
        private set
    

    override fun onEnable() {
        dataFolder.mkdirs()
        reload()
    }

    fun reload() {
        onDisable()
        instance = this

        config = simpleLanguageLoader.loadOrInitialize(
            dataFolder.resolve("config.json"),
            Config::class.java,
            ::Config
        )
        language = simpleLanguageLoader.loadOrInitialize(
            dataFolder.resolve("language.json"),
            Language::class.java,
            ::Language
        )
        
        isUkitSetup = Bukkit.getPluginManager().getPlugin("Ukit") != null

        if (!setupEconomy())
            throw IllegalStateException("ECore not found")

        dataManager =
            ShopDataManager(dataFolder.resolve("shops.sqlite3"), this)

        getCommand("nyaashop")?.setExecutor(NyaaShopCommands(this))

        Bukkit.getPluginManager().registerEvents(ShopPlayerListener(this), this)
        Bukkit.getPluginManager()
            .registerEvents(ShopEnvironmentListener(), this)

        if (config.itemDisplayRotation)
            server.scheduler.runTaskTimer(this, { _ ->
                rotateAllShopDisplay()
            }, 1, 20)
    }

    private fun rotateAllShopDisplay() {
        getShopDataManager().getAllLoadedShops()
            .filter { it.itemDisplay != null }.forEach { shop ->
                val itemDisplay = shop.itemDisplay!!
                val yaw = itemDisplay.yaw
                itemDisplay.teleport(
                    itemDisplay.location.clone()
                        .apply { setYaw(yaw + config.itemDisplayRotationSpeedInDegreesPerSecond.toFloat()) })
            }
    }

    override fun onDisable() {
        Bukkit.getAsyncScheduler().cancelTasks(this)
        Bukkit.getScheduler().cancelTasks(this)
        HandlerList.unregisterAll(this)
        if (::dataManager.isInitialized) {
            dataManager.shutdown()
        }
    }

    fun getShopDataManager(): ShopDataManager {
        return dataManager
    }

    private fun setupEconomy(): Boolean {
        try {
            Class.forName("cat.nyaa.ecore.EconomyCore")
        } catch (_: ClassNotFoundException) {
            return false
        }

        val rsp = Bukkit.getServicesManager().getRegistration(
            EconomyCore::class.java
        )
        economyProvider = rsp?.provider ?: return false
        return true
    }

}