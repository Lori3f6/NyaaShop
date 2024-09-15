package cat.nyaa.nyaashop

import cat.nyaa.nyaashop.magic.Utils.Companion.isRelevantToShopSign
import cat.nyaa.nyaashop.magic.Utils.Companion.isShopSign
import com.destroystokyo.paper.event.block.BlockDestroyEvent
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.TNTPrimeEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.world.StructureGrowEvent


class ShopEnvironmentListener : Listener {
    private fun isProtected(block: Block): Boolean {
        return isShopSign(block) || isRelevantToShopSign(block)
    }

    private fun isProtected(blockState: BlockState): Boolean {
        return isShopSign(blockState) || isRelevantToShopSign(blockState)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf(::isProtected)
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeIf(::isProtected)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        event.isCancelled = event.blocks.any(::isProtected)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        event.isCancelled = event.blocks.any(::isProtected)
    }

    @EventHandler(ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        event.isCancelled = event.blocks.any(::isProtected)
    }

    @EventHandler(ignoreCancelled = true)
    fun onTNTPrime(event: TNTPrimeEvent) {
        event.isCancelled = isProtected(event.block)
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        event.isCancelled = isProtected(event.block)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockDestroy(event: BlockDestroyEvent) {
        event.isCancelled = isProtected(event.block)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        event.isCancelled = isProtected(event.block)
    }
}