package moe.sekiu.minilpa.ui.component

import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.KeyboardFocusManager
import javax.swing.JPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import moe.sekiu.minilpa.backend
import moe.sekiu.minilpa.filter
import moe.sekiu.minilpa.freeze
import moe.sekiu.minilpa.model.Profile
import moe.sekiu.minilpa.ui.WrapLayout

class ProfileList : JPanel()
{
    companion object
    {
        lateinit var instance : ProfileList
            private set
    }

    val cards
        get() = components.filterIsInstance<ProfileCard>()

    var enabledProfileCard : ProfileCard? = null
    var focusProfileIccid : String = ""

    init
    {
        instance = this
        val wrapLayout = WrapLayout(FlowLayout.LEFT)
        wrapLayout.vgap = 10
        wrapLayout.hgap = 10
        wrapLayout.alignOnBaseline = true
        layout = wrapLayout
        size = Dimension(500, 500)
    }

    fun switchProfileIccidMask(show : Boolean) { cards.forEach { it.switchIccidMask(show) } }

    fun filterProfile(profileCards : List<ProfileCard> = cards) =
        ProfileToolBar.instance.searchBox.filter(profileCards, ProfileCard::profile, listOf(Profile::iccid, Profile::nickname, Profile::serviceProviderName))

    suspend fun refreshProfileData()
    {
        val focusOwner = withContext(Dispatchers.Swing) { KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner }
        if (focusOwner is ProfileCard) focusProfileIccid = focusOwner.profile.iccid
        val profileList = backend.getProfileList()
        val profileCards = profileList.map { ProfileCard(it) }
        filterProfile(profileCards)
        removeAll()
        for (card in profileCards) add(card)
        freeze(this, false)
        updateUI()
    }
}