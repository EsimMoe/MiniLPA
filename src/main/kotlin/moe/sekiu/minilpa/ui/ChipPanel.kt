package moe.sekiu.minilpa.ui

import java.awt.BorderLayout
import kotlinx.serialization.json.decodeFromJsonElement
import moe.sekiu.minilpa.backend
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.json
import moe.sekiu.minilpa.model.ChipInfo.EuiccInfo2Lite
import moe.sekiu.minilpa.ui.component.ChipInfo
import moe.sekiu.minilpa.ui.component.ChipToolBar
import moe.sekiu.minilpa.ui.component.BottomInfo

class ChipPanel : MiniPanel()
{
    companion object
    {
        lateinit var instance : ChipPanel
    }

    init
    {
        instance = this
        layout = BorderLayout()
        add(ChipToolBar(), BorderLayout.NORTH)
        add(BottomInfo(), BorderLayout.SOUTH)
    }

    suspend fun refreshChipInfo()
    {
        val chipInfo = backend.getChipInfo()
        layout.cast<BorderLayout>().getLayoutComponent(BorderLayout.CENTER)?.apply { remove(this) }
        add(BorderLayout.CENTER, ChipInfo(chipInfo))
        BottomInfo.updateFreeSpace(json.decodeFromJsonElement<EuiccInfo2Lite>(chipInfo.eUICCInfo2).extCardResource.freeNonVolatileMemory)
    }
}