package cat.nyaa.nyaashop

import cat.nyaa.ecore.EconomyCore
import cat.nyaa.nyaashop.data.ShopDataManager
import cat.nyaa.nyaashop.language.Language
import land.melon.lab.simplelanguageloader.SimpleLanguageLoader
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

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

    override fun onEnable() {
        dataFolder.mkdirs()
        reload()
    }

    private fun reload() {
        onDisable()

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

        if (!setupEconomy())
            throw IllegalStateException("ECore not found")

        dataManager =
            ShopDataManager(dataFolder.resolve("shops.sqlite3"), this)

        Bukkit.getPluginManager().registerEvents(dataManager, this)

        instance = this
    }

    override fun onDisable() {
        Bukkit.getAsyncScheduler().cancelTasks(this)
        HandlerList.unregisterAll(this)
        dataManager.shutdown()
    }

    fun getShopDataManager(): ShopDataManager {
        return dataManager
    }

    private fun setupEconomy(): Boolean {
        try {
            Class.forName("cat.nyaa.ecore.EconomyCore")
        } catch (e: ClassNotFoundException) {
            return false
        }

        val rsp = Bukkit.getServicesManager().getRegistration(
            EconomyCore::class.java
        )
        economyProvider = rsp?.provider ?: return false
        return true
    }

}