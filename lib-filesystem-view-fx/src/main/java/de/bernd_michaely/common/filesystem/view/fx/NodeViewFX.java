/* Created on Mar 29, 2022 */
package de.bernd_michaely.common.filesystem.view.fx;

import de.bernd_michaely.common.filesystem.view.base.NodeView;
import de.bernd_michaely.common.filesystem.view.base.PathView;
import java.lang.System.Logger;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import static java.lang.System.Logger.Level.*;

/**
 * Specialization of UINode for JavaFX.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
class NodeViewFX implements NodeView
{
	private static final Logger logger = System.getLogger(NodeViewFX.class.getName());
	private final TreeNode treeItem;

	private static class TreeNode extends TreeItem<PathView>
	{
		private boolean leaf;

		private TreeNode(PathView pathView)
		{
			super(pathView);
		}

		@Override
		public boolean isLeaf()
		{
			return leaf;
		}

		private void setLeaf(boolean leaf)
		{
			this.leaf = leaf;
		}
	}

	NodeViewFX(PathView pathView)
	{
		this.treeItem = new TreeNode(pathView);
		this.treeItem.expandedProperty().addListener((observable, oldValue, newValue) ->
			pathView.handleNodeExpansion(newValue));
	}

	@Override
	public void setLeafNode(boolean leafNode)
	{
		treeItem.setLeaf(leafNode);
		logger.log(TRACE, "SET FOR NODE »" + treeItem.getValue() + "« LEAF TO »" + leafNode + "«");
	}

	TreeItem<PathView> getTreeItem()
	{
		return treeItem;
	}

	@Override
	public void insertSubNodes(SortedMap<Integer, NodeView> mapSubNodeViews)
	{
		Platform.runLater(() ->
		{
			final ObservableList<TreeItem<PathView>> children = treeItem.getChildren();
			mapSubNodeViews.forEach((index, subNodeView) ->
			{
				if (subNodeView instanceof NodeViewFX subNodeViewFX)
				{
					children.add(index, subNodeViewFX.treeItem);
				}
				else
				{
					logger.log(WARNING, getClass().getName() +
						"::insertSubNodeAt : Invalid NodeView : " + subNodeView);
				}
			});
		});
	}

	@Override
	public void addAllSubNodes(Collection<NodeView> subNodeViews)
	{
		Platform.runLater(() ->
			treeItem.getChildren().addAll(
				subNodeViews.stream()
					.filter(subNodeView -> subNodeView instanceof NodeViewFX)
					.map(subNodeViewFX -> (NodeViewFX) subNodeViewFX)
					.map(NodeViewFX::getTreeItem)
					.toList()));
	}

	@Override
	public void removeSubNodes(List<Integer> indices)
	{
		Platform.runLater(() ->
		{
			final ObservableList<TreeItem<PathView>> children = treeItem.getChildren();
			indices.forEach(index ->
			{
				if (index >= 0 && index < children.size())
				{
					children.remove((int) index);
				}
			});
		});
	}

	@Override
	public void clear()
	{
		Platform.runLater(() -> treeItem.getChildren().clear());
	}

	@Override
	public void setExpanded(boolean expanded)
	{
		Platform.runLater(() -> treeItem.setExpanded(expanded));
	}
}
