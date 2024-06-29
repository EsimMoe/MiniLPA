package moe.sekiu.minilpa.ui.component

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeSelectionModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import moe.sekiu.minilpa.action
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.json
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.setClipboard


class MiniJsonTree(title : String, data : JsonElement) : JTree(data.toTreeModel(title))
{
    companion object
    {
        private fun JsonElement.toTreeModel(title : String) : TreeModel
        {
            val root = DefaultMutableTreeNode(TreeData("$title:", this))
            visit(this, root)
            return DefaultTreeModel(root)
        }

        private fun visit(element : JsonElement, node : DefaultMutableTreeNode)
        {
            when (element)
            {
                is JsonObject ->
                {
                    element.forEach { key, value ->
                        when (value)
                        {
                            is JsonPrimitive -> node.add(DefaultMutableTreeNode(TreeData("$key: $value", key to value)))
                            else ->
                            {
                                val child = DefaultMutableTreeNode(TreeData("$key:", key to value))
                                node.add(child)
                                visit(value, child)
                            }
                        }
                    }
                }

                is JsonArray -> element.forEach { child ->
                    if (child is JsonPrimitive) node.add(DefaultMutableTreeNode(TreeData("$child", child)))
                    else visit(child, node)
                }

                is JsonPrimitive -> { }
            }
        }

        data class TreeData(val display : String, val data : Pair<String?, JsonElement>)
        {
            constructor(display : String, data : JsonElement) : this(display, null to data)

            fun hasKey() = data.first != null

            override fun toString() = display
        }
    }

    init
    {
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        isFocusable = false
        addMouseListener(object : MouseAdapter()
        {
            override fun mousePressed(event : MouseEvent)
            {
                if (SwingUtilities.isRightMouseButton(event))
                {
                    val path = getPathForLocation(event.x, event.y) ?: return
                    val pathBounds = getUI().getPathBounds(this@MiniJsonTree, path)
                    if (pathBounds.contains(event.x, event.y))
                    {
                        val treeData = path.lastPathComponent.cast<DefaultMutableTreeNode>().userObject.cast<TreeData>()
                        selectionModel.selectionPath = path
                        val menu = JPopupMenu()
                        if (treeData.hasKey()) menu.add(JMenuItem(language.`copy-key`).action { setClipboard(treeData.data.first!!) })
                        menu.add(JMenuItem(language.`copy-value`).action { setClipboard(json.encodeToString(treeData.data.second)) })
                        if (treeData.hasKey()) menu.add(JMenuItem(language.`copy-key-and-value`).action { setClipboard(
                            json.encodeToString(JsonObject(mapOf(treeData.data.first!! to treeData.data.second)))) })
                        menu.show(this@MiniJsonTree, event.x, event.y)
                    }
                }
            }
        })
    }
}