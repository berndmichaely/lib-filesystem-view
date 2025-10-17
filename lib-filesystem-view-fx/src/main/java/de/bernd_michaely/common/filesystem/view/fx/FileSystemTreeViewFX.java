/* Created on Sep 14, 2021 */
package de.bernd_michaely.common.filesystem.view.fx;

import de.bernd_michaely.common.filesystem.view.base.Configuration;
import de.bernd_michaely.common.filesystem.view.base.PathView;
import de.bernd_michaely.common.filesystem.view.base.RootNodeCtrl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedSet;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
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

	private static class PathViewTreeCell extends TreeCell<PathView>
	{
		private @MonotonicNonNull Font fontDefault;
		private @MonotonicNonNull Font fontItalic;

		private PathViewTreeCell(TreeView<PathView> treeView)
		{
		}

		@SuppressWarnings("argument")
		private void clear()
		{
			setText(null);
			setGraphic(null);
		}

		@Override
		protected void updateItem(PathView item, boolean empty)
		{
			super.updateItem(item, empty);
			if (empty || item == null)
			{
				clear();
			}
			else
			{
				if (fontDefault == null || fontItalic == null)
				{
					fontDefault = getFont();
					fontItalic = Font.font(
						fontDefault.getFamily(), FontPosture.ITALIC, fontDefault.getSize());
				}
				setFont(Files.isSymbolicLink(item.getPath()) ? fontItalic : fontDefault);
				setText(item.toString());
			}
		}
	}

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
				while (change.next())
				{
					if (change.wasAdded())
					{
						for (TreeItem<PathView> treeItem : change.getAddedSubList())
						{
							selectedPathProperty.setValue(treeItem.getValue().getPath());
						}
					}
					else if (change.wasRemoved())
					{
						selectedPathProperty.setValue(null);
					}
				}
			});
		this.treeView.setCellFactory(PathViewTreeCell::new);
		// TODO:
		//this.treeView.setCellFactory(TreeCellPathView::createInstance);
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
					Platform.runLater(() ->
					{
						final var selectionModel = treeView.getSelectionModel();
						selectionModel.select(treeItem);
						treeView.scrollTo(selectionModel.getSelectedIndex());
					});
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
