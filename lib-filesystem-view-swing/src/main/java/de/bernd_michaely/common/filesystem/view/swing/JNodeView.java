/* Created on Mar 13, 2023 */
package de.bernd_michaely.common.filesystem.view.swing;

import de.bernd_michaely.common.filesystem.view.base.NodeView;
import de.bernd_michaely.common.filesystem.view.base.PathView;
import java.lang.System.Logger;
import java.lang.ref.WeakReference;
import java.util.Collection;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.checkerframework.checker.guieffect.qual.*;
import org.checkerframework.checker.nullness.qual.*;

import static java.lang.System.Logger.Level.*;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * Specialization of UINode for Swing.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
class JNodeView implements NodeView
{
	private static final Logger logger = System.getLogger(JNodeView.class.getName());
	private @MonotonicNonNull WeakReference<JTree> wrTree;
	private final NodeViewTreeNode treeNode;

	static class NodeViewTreeNode extends DefaultMutableTreeNode
	{
		private NodeViewTreeNode(PathView pathView)
		{
			super(pathView);
		}

		@Override
		public PathView getUserObject()
		{
			return (PathView) super.getUserObject();
		}
	}

	JNodeView(PathView pathView)
	{
		this.treeNode = new NodeViewTreeNode(pathView);
	}

	PathView getPathView()
	{
		return treeNode.getUserObject();
	}

	TreeNode getTreeNode()
	{
		return treeNode;
	}

	@UIEffect
	private @Nullable
	DefaultTreeModel getTreeModel()
	{
		final JTree tree = getTree();
		return tree != null && tree.getModel() instanceof DefaultTreeModel model ? model : null;
	}

	@Nullable
	JTree getTree()
	{
		return this.wrTree != null ? this.wrTree.get() : null;
	}

	void setTree(JTree tree)
	{
		if (tree != null)
		{
			this.wrTree = new WeakReference<>(tree);
		}
	}

	@Override
	public void insertSubNodeAt(int index, NodeView subNodeView)
	{
		invokeLater(() ->
		{
			if (subNodeView instanceof JNodeView jSubNodeView)
			{
				final JTree tree = getTree();
				if (tree != null)
				{
					final var model = getTreeModel();
					if (model != null)
					{
						model.insertNodeInto(jSubNodeView.treeNode, treeNode, index);
					}
				}
			}
			else
			{
				logger.log(WARNING, getClass().getName() +
					"::insertSubNodeAt : Invalid NodeView : " + subNodeView);
			}
		});
	}

	@Override
	public void addAllSubNodes(Collection<NodeView> subNodeViews)
	{
		invokeLater(() ->
		{
			final JTree tree = getTree();
			if (tree != null)
			{
				final var model = getTreeModel();
				if (model != null)
				{
					int index = 0;
					for (NodeView subNodeView : subNodeViews)
					{
						if (subNodeView instanceof JNodeView jSubNodeView)
						{
							model.insertNodeInto(jSubNodeView.treeNode, treeNode, index++);
						}
						else
						{
							logger.log(WARNING, getClass().getName() +
								"::addAllSubNodes : Invalid NodeView : " + subNodeView);
						}
					}
				}
			}
		});
	}

	@Override
	public void removeSubNodeAt(int index)
	{
		invokeLater(() ->
		{
			if (index >= 0 && index < treeNode.getChildCount())
			{
				final var treeModel = getTreeModel();
				if (treeModel != null &&
					treeModel.getChild(treeNode, index) instanceof MutableTreeNode mutableTreeNode)
				{
					treeModel.removeNodeFromParent(mutableTreeNode);
				}
			}
		});
	}

	@Override
	public void clear()
	{
		invokeLater(() ->
		{
			final var treeModel = getTreeModel();
			if (treeModel != null)
			{
				treeNode.removeAllChildren();
				treeModel.reload(treeNode);
			}
		});
	}

	@Override
	public void setExpanded(boolean expanded)
	{
		invokeLater(() ->
		{
			final JTree tree = getTree();
			if (tree != null)
			{
				final var treePath = new TreePath(treeNode.getPath());
				final boolean isExpanded = tree.isExpanded(treePath);
				if (expanded && !isExpanded)
				{
					tree.expandPath(treePath);
				}
				else if (!expanded && isExpanded)
				{
					tree.collapsePath(treePath);
				}
				logger.log(INFO, (expanded ? "Expand" : "Collapse") + " node »" + getPathView() + "«");
			}
			else
			{
				logger.log(WARNING, getClass().getName() + "::setExpanded : Invalid JTree reference");
			}
		});
	}

	@Override
	public void setLeafNode(boolean leafNode)
	{
		invokeLater(() -> treeNode.setAllowsChildren(!leafNode));
	}
}
