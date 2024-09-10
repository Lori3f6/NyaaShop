package cat.nyaa.nyaashop.magic

import org.bukkit.DyeColor
import org.bukkit.Material
import java.util.EnumMap

class DyeMap {
    companion object {
        /**
         * Map of material to its dye color
         * ref: https://www.spigotmc.org/threads/get-dyecolor-from-material.434803/
         */
        private val materialColorMap =
            EnumMap<Material, DyeColor>(Material::class.java).apply {
                Material.entries.forEach {
                    val name = it.name
                    when {
                        name.startsWith("WHITE") -> put(it, DyeColor.WHITE)
                        name.startsWith("ORANGE") -> put(it, DyeColor.ORANGE)
                        name.startsWith("MAGENTA") -> put(it, DyeColor.MAGENTA)
                        name.startsWith("LIGHT_BLUE") -> put(
                            it,
                            DyeColor.LIGHT_BLUE
                        )

                        name.startsWith("YELLOW") -> put(it, DyeColor.YELLOW)
                        name.startsWith("LIME") -> put(it, DyeColor.LIME)
                        name.startsWith("PINK") -> put(it, DyeColor.PINK)
                        name.startsWith("GRAY") -> put(it, DyeColor.GRAY)
                        name.startsWith("LIGHT_GRAY") -> put(
                            it,
                            DyeColor.LIGHT_GRAY
                        )

                        name.startsWith("CYAN") -> put(it, DyeColor.CYAN)
                        name.startsWith("PURPLE") -> put(it, DyeColor.PURPLE)
                        name.startsWith("BLUE") -> put(it, DyeColor.BLUE)
                        name.startsWith("BROWN") -> put(it, DyeColor.BROWN)
                        name.startsWith("GREEN") -> put(it, DyeColor.GREEN)
                        name.startsWith("RED") -> put(it, DyeColor.RED)
                        name.startsWith("BLACK") -> put(it, DyeColor.BLACK)
                    }
                }
            }

        /**
         * Gets the dye color of this material, or null if not found.
         *
         * The dye color is simply determined by checking the prefix of the material.
         * This method uses an extremely fast EnumMap lookup.
         */
        fun Material.dyeColor(): DyeColor? = materialColorMap[this]
    }
}