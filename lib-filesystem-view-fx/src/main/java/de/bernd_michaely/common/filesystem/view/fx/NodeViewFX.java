/* Created on Mar 29, 2022 */
package de.bernd_michaely.common.filesystem.view.fx;

import de.bernd_michaely.common.filesystem.view.base.NodeView;
import de.bernd_michaely.common.filesystem.view.base.PathView;
import java.lang.System.Logger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.checkerframework.checker.nullness.qual.Nullable;

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
	private @Nullable ScheduledExecutorService executorService;
	private @Nullable ScheduledFuture<?> scheduledFuture;
	private volatile boolean isRead;

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
		final Runnable action = () ->
		{
			isRead = true;
			if (scheduledFuture != null)
			{
				scheduledFuture.cancel(true);
			}
			final ObservableList<TreeItem<PathView>> children = treeItem.getChildren();
			children.clear();
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
		};
		if (Platform.isFxApplicationThread())
		{
			action.run();
		}
		else
		{
			Platform.runLater(action);
		}
	}

	@Override
	public void addAllSubNodes(Collection<NodeView> subNodeViews)
	{
		final Runnable action = () ->
		{
			isRead = true;
			if (scheduledFuture != null)
			{
				scheduledFuture.cancel(true);
			}
			treeItem.getChildren().setAll(
				subNodeViews.stream()
					.filter(subNodeView -> subNodeView instanceof NodeViewFX)
					.map(subNodeViewFX -> (NodeViewFX) subNodeViewFX)
					.map(NodeViewFX::getTreeItem)
					.toList());
		};
		if (Platform.isFxApplicationThread())
		{
			action.run();
		}
		else
		{
			Platform.runLater(action);
		}
	}

	@Override
	public void removeSubNodes(List<Integer> indices)
	{
		final Runnable action = () ->
		{
			isRead = false;
			final ObservableList<TreeItem<PathView>> children = treeItem.getChildren();
			indices.forEach(index ->
			{
				if (index >= 0 && index < children.size())
				{
					children.remove((int) index);
				}
			});
		};
		if (Platform.isFxApplicationThread())
		{
			action.run();
		}
		else
		{
			Platform.runLater(action);
		}
	}

	@Override
	public void clear()
	{
		final Runnable action = () ->
		{
			isRead = false;
			treeItem.getChildren().clear();
		};
		if (Platform.isFxApplicationThread())
		{
			action.run();
		}
		else
		{
			Platform.runLater(action);
		}
	}

	private static class ProgressPathView implements PathView
	{
		@Override
		public String getName()
		{
			return "";
		}

		@Override
		public Path getPath()
		{
			return Paths.get("");
		}

		@Override
		public void handleNodeExpansion(boolean expand)
		{
		}

		@Override
		public String toString()
		{
			return "";
		}
	}

	@Override
	public void setExpanded(boolean expanded)
	{
		final Runnable action = () -> treeItem.setExpanded(expanded);
		if (Platform.isFxApplicationThread())
		{
			action.run();
			if (executorService == null)
			{
				executorService = Executors.newSingleThreadScheduledExecutor();
				scheduledFuture = executorService.schedule(() ->
				{
					if (!isRead)
					{
//						@SuppressWarnings("type.arguments.not.inferred")
						final TreeItem<PathView> treeItem1 = new TreeItem<>(
							new ProgressPathView(), new ProgressIndicator());
						if (scheduledFuture != null && !scheduledFuture.isCancelled())
						{
							treeItem.getChildren().add(treeItem1);
						}
					}
				}, 500, TimeUnit.MILLISECONDS);
			}
		}
		else
		{
			Platform.runLater(action);
		}
	}
}
