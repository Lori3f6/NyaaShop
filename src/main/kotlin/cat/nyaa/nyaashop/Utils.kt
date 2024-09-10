package cat.nyaa.nyaashop

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class Utils {
    companion object {
        fun suggestCommandButtonOf(
            buttonText: Component,
            hoverText: Component,
            command: String
        ): Component {
            return buttonText.hoverEvent(hoverText)
                .clickEvent(ClickEvent.suggestCommand(command))
        }

        fun suggestCommandButtonOf(
            buttonText: String,
            hoverText: String,
            command: String
        ): Component {
            return suggestCommandButtonOf(
                LegacyComponentSerializer.legacySection()
                    .deserialize(buttonText),
                LegacyComponentSerializer.legacySection()
                    .deserialize(hoverText),
                command
            )
        }

        //return: number of items added
        fun Player.tryToAddItem(itemStack: ItemStack): Int {
            val itemSize = itemStack.amount
            val itemNotAdded = inventory.addItem(itemStack)[0]?.amount ?: 0
            return itemSize - itemNotAdded
        }

        fun Player.hasAtLeast(itemStack: ItemStack): Boolean {
            return inventory.containsAtLeast(itemStack, itemStack.amount)
        }

        fun Player.removeItem(itemStack: ItemStack): Boolean {
            return inventory.removeItem(itemStack).isEmpty
        }
    }
}