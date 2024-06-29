package moe.sekiu.minilpa.ui.component

import java.awt.FlowLayout
import javax.swing.JPanel
import moe.sekiu.minilpa.backend
import moe.sekiu.minilpa.filter
import moe.sekiu.minilpa.freeze
import moe.sekiu.minilpa.model.Notification
import moe.sekiu.minilpa.ui.WrapLayout

class NotificationList : JPanel()
{
    companion object
    {
        lateinit var instance : NotificationList
            private set
    }

    val cards
        get() = components.filterIsInstance<NotificationCard>()

    var selectionMode = false
        set(value)
        {
            field = value
            cards.forEach { it.selectionCheckbox.isVisible = value }
            NotificationToolBar.instance.selectionTools.forEach { it.isVisible = value }
        }

    var lastSelected : NotificationCard? = null

    val selectedNotifications
        get() = cards.filter { it.selectionCheckbox.isSelected }

    init
    {
        instance = this
        val wrapLayout = WrapLayout(FlowLayout.LEFT)
        wrapLayout.vgap = 10
        wrapLayout.hgap = 10
        wrapLayout.alignOnBaseline = true
        layout = wrapLayout
    }

    fun switchNotificationIccidMask(show : Boolean) { cards.forEach { it.switchIccidMask(show) } }

    fun filterNotification(notificationCards : List<NotificationCard> = cards) =
        NotificationToolBar.instance.searchBox.filter(notificationCards, NotificationCard::notification, listOf(Notification::seq, Notification::operation, Notification::address, Notification::iccid))

    suspend fun refreshNotificationData()
    {
        val notificationCards = backend.getNotificationList().sortedByDescending { it.seq }.map { NotificationCard(it) }
        filterNotification(notificationCards)
        lastSelected = null
        selectionMode = false
        removeAll()
        for (card in notificationCards) add(card)
        freeze(this, false)
        updateUI()
    }
}