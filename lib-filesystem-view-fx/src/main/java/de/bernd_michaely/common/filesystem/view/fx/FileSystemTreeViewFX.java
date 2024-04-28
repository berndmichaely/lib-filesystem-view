/* Created on Sep 14, 2021 */
package de.bernd_michaely.common.filesystem.view.fx;

import de.bernd_michaely.common.filesystem.view.base.Configuration;
import de.bernd_michaely.common.filesystem.view.base.PathView;
import de.bernd_michaely.common.filesystem.view.base.RootNodeCtrl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedSet;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import org.checkerframework.checker.nullness.qual.*;

/**
 * FileSystemTreeView implementation for JavaFX.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
class FileSystemTreeViewFX implements FileSystemTreeView
{
	private final ReadOnlyObjectWrapper<@Nullable Path> selectedPathProperty;
	private final ReadOnlyBooleanWrapper pathSelectedProperty;
	private final TreeView<PathView> treeView;
	private final RootNodeCtrl rootNodeCtrl;

	FileSystemTreeViewFX(Configuration configuration)
	{
		this.selectedPathProperty = new ReadOnlyObjectWrapper<>();
		this.pathSelectedProperty = new ReadOnlyBooleanWrapper();
		this.pathSelectedProperty.bind(this.selectedPathProperty.isNotNull());
		this.treeView = new TreeView<>();
		this.rootNodeCtrl = RootNodeCtrl.create(configuration, NodeViewFX::new);
		if (this.rootNodeCtrl.getNodeView() instanceof NodeViewFX nodeViewFX)
		{
			this.treeView.setRoot(nodeViewFX.getTreeItem());
		}
		this.treeView.setShowRoot(false);
		this.treeView.getSelectionModel().getSelectedItems()
			.addListener((Change<? extends TreeItem<PathView>> change) ->
			{
				final ObservableList<? extends TreeItem<PathView>> list = change.getList();
				selectedPathProperty.setValue(list.isEmpty() ? null : list.get(0).getValue().getPath());
			});
		this.rootNodeCtrl.setExpanded(true);
	}

	@Override
	public @Nullable
	Path expandPath(@Nullable Path absolutePath, boolean expandLastElement, boolean select)
	{
		if (absolutePath != null)
		{
			if (rootNodeCtrl.expandPath(absolutePath, expandLastElement) instanceof NodeViewFX nodeViewFX)
			{
				final TreeItem<PathView> treeItem = nodeViewFX.getTreeItem();
				if (select)
				{
					Platform.runLater(() -> treeView.getSelectionModel().select(treeItem));
				}
				return treeItem.getValue().getPath();
			}
			else
			{
				return null;
			}
		}
		else
		{
			if (select)
			{
				Platform.runLater(() -> treeView.getSelectionModel().clearSelection());
			}
			return null;
		}
	}

	@Override
	public SortedSet<Path> getExpandedPaths()
	{
		return rootNodeCtrl.getExpandedPaths();
	}

	@Override
	public final ReadOnlyObjectProperty<@Nullable Path> selectedPathProperty()
	{
		return selectedPathProperty.getReadOnlyProperty();
	}

	@Override
	public final ReadOnlyBooleanProperty pathSelectedProperty()
	{
		return pathSelectedProperty.getReadOnlyProperty();
	}

	@Override
	public void updateTree()
	{
		rootNodeCtrl.updateTree();
	}

	@Override
	public Region getComponent()
	{
		return treeView;
	}

	@Override
	public TreeView<PathView> getTreeView()
	{
		return treeView;
	}

	@Override
	public void close() throws IOException
	{
		rootNodeCtrl.close();
	}
}
