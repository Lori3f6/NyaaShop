package cat.nyaa.nyaashop.magic

import cat.nyaa.nyaashop.data.Shop
import com.destroystokyo.paper.MaterialTags
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


class Utils {
    companion object {
        fun isPlayerHoldingSignDecorationItem(player: Player): Boolean {
            val item = player.inventory.itemInMainHand
            return MaterialTags.DYES.isTagged(item.type) || item.type == Material.INK_SAC || item.type == Material.GLOW_INK_SAC
        }

        fun isShopSign(block: Block): Boolean {
            return block.state is Sign && Shop.isShopSign(block.state as Sign)
        }

        fun isRelevantToShopSign(block: Block): Boolean {
            val blockToSearch = listOf(
                block.getRelative(BlockFace.NORTH),
                block.getRelative(BlockFace.SOUTH),
                block.getRelative(BlockFace.EAST),
                block.getRelative(BlockFace.WEST)
            )
            return blockToSearch.any {
                if (!isShopSign(it)) return false
                val sign = it as Sign
                val wallSign = sign.blockData as WallSign
                it.getRelative(wallSign.facing.oppositeFace).location == block.location
            }
        }

        fun getTextContent(component: Component): String {
            if (component is TextComponent) {
                return component.content()
            } else
                throw IllegalArgumentException("Component is not a TextComponent")
        }

        fun blockFaceIntoYaw(face: BlockFace): Float {
            return when (face) {
                BlockFace.NORTH -> 0F
                BlockFace.EAST -> 90F
                BlockFace.SOUTH -> 180F
                BlockFace.WEST -> 270F
                else -> 0F
            }
        }

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

        fun Player.addItemByDrop(itemStack: ItemStack, amount: Int) {
            var amountRemaining = amount
            while (itemStack.maxStackSize < amountRemaining) {
                val itemStack = itemStack.asOne()
                itemStack.amount = itemStack.maxStackSize
                dropItemForPickup(itemStack)
                amountRemaining -= itemStack.maxStackSize
            }
            if (amountRemaining > 0) {
                val itemStack = itemStack.asOne()
                itemStack.amount = amountRemaining
                dropItemForPickup(itemStack)
            }
        }

        fun Player.dropItemForPickup(itemStack: ItemStack): Item {
            val itemDropped = world.dropItem(location, itemStack)
            itemDropped.setCanMobPickup(false)
            itemDropped.setCanPlayerPickup(true)
            itemDropped.owner = uniqueId
            itemDropped.pickupDelay = 0
            itemDropped.customName(Component.text("hihi"))
            return itemDropped
        }

        fun Player.hasAtLeast(itemStack: ItemStack, amount: Int): Boolean {
            return inventory.containsAtLeast(itemStack, amount)
        }

        fun Player.removeItem(itemStack: ItemStack, amount: Int): Boolean {
            val itemStack = itemStack.asOne()
            itemStack.amount = amount
            return inventory.removeItem(itemStack).isNullOrEmpty()
        }
    }
}