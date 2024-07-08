/* Created on Sep 20, 2021 */
package de.bernd_michaely.common.filesystem.view.swing;

import de.bernd_michaely.common.filesystem.view.base.Configuration;
import de.bernd_michaely.common.filesystem.view.base.RootNodeCtrl;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.checkerframework.checker.guieffect.qual.*;
import org.checkerframework.checker.nullness.qual.*;

import static java.lang.Integer.min;
import static java.lang.System.Logger.Level.*;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * FileSystemTreeView implementation for Java Swing.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
class JFileSystemTreeViewImpl implements JFileSystemTreeView
{
	private static final Logger logger = System.getLogger(JFileSystemTreeViewImpl.class.getName());
	private final RootNodeCtrl rootNodeCtrl;
	private final JTree jTree;
	private final JScrollPane jScrollPane;
	private @Nullable Path selectedPath;
	private final List<Consumer<@Nullable Path>> pathSelectionListeners = new ArrayList<>();

	@UIType
	private class TreeSelectionListenerImpl implements TreeSelectionListener
	{
		@Override
		public void valueChanged(TreeSelectionEvent event)
		{
			invokeLater(() ->
			{
				if (event.getSource() instanceof JTree source)
				{
					final var lastSelectedPathComponent = source.getLastSelectedPathComponent();
					if (lastSelectedPathComponent instanceof JNodeView.NodeViewTreeNode treeNode)
					{
						final Path path = treeNode.getUserObject().getPath();
						selectedPath = path;
						pathSelectionListeners.forEach(consumer -> consumer.accept(path));
					}
					else if (lastSelectedPathComponent == null)
					{
						selectedPath = null;
						pathSelectionListeners.forEach(consumer -> consumer.accept(null));
					}
					else
					{
						logger.log(WARNING, getClass().getName() +
							": unknown lastPathComponent type: " + lastSelectedPathComponent);
					}
				}
			});
		}
	}

	@UIType
	private static class TreeExpansionListenerImpl implements TreeExpansionListener
	{
		private void handleEvent(TreeExpansionEvent event, boolean expanded)
		{
			invokeLater(() ->
			{
				final var lastPathComponent = event.getPath().getLastPathComponent();
				if (lastPathComponent instanceof JNodeView.NodeViewTreeNode treeNode)
				{
					treeNode.getUserObject().handleNodeExpansion(expanded);
				}
				else
				{
					logger.log(WARNING, getClass().getName() +
						": invalid lastPathComponent type: " + lastPathComponent);
				}
			});
		}

		@Override
		public void treeExpanded(TreeExpansionEvent event)
		{
			handleEvent(event, true);
		}

		@Override
		public void treeCollapsed(TreeExpansionEvent event)
		{
			handleEvent(event, false);
		}
	}

	@UIEffect
	JFileSystemTreeViewImpl(Configuration configuration)
	{
		jTree = new JTree();
		rootNodeCtrl = RootNodeCtrl.create(configuration, pathView ->
		{
			final var jNodeView = new JNodeView(pathView);
			jNodeView.setTree(jTree);
			return jNodeView;
		});
		if (rootNodeCtrl.getNodeView() instanceof JNodeView jNodeView)
		{
			jTree.setModel(new DefaultTreeModel(jNodeView.getTreeNode(), true));
		}
		jTree.setRootVisible(false);
		jTree.setShowsRootHandles(true);
		jTree.setScrollsOnExpand(true);
		jTree.setExpandsSelectedPaths(true);
		jTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		jTree.addTreeSelectionListener(new TreeSelectionListenerImpl());
		jTree.addTreeExpansionListener(new TreeExpansionListenerImpl());
		this.jScrollPane = new JScrollPane(jTree);
	}

	@Override
	public void addPathSelectionListener(Consumer<@Nullable Path> consumer)
	{
		pathSelectionListeners.add(consumer);
	}

	@Override
	public boolean removePathSelectionListener(Consumer<@Nullable Path> consumer)
	{
		return pathSelectionListeners.remove(consumer);
	}

	@Override
	public @Nullable
	Path expandPath(@Nullable Path absolutePath, boolean expandLastElement, boolean select)
	{
		if (absolutePath != null)
		{
			if (rootNodeCtrl.expandPath(absolutePath, expandLastElement) instanceof JNodeView jNodeView)
			{
				if (select && jNodeView.getTreeNode() instanceof DefaultMutableTreeNode treeNode)
				{
					invokeLater(() ->
					{
						final TreeSelectionModel selectionModel = jTree.getSelectionModel();
						selectionModel.setSelectionPath(new TreePath(treeNode.getPath()));
						final int index = selectionModel.getMaxSelectionRow();
						if (index >= 0)
						{
							final int size = jTree.getRowCount();
							jTree.scrollRowToVisible(min(index + 1, size - 1));
						}
					});
				}
				return jNodeView.getPathView().getPath();
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
				invokeLater(() -> jTree.getSelectionModel().clearSelection());
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
	public @Nullable
	Path getSelectedPath()
	{
		return selectedPath;
	}

	@Override
	public JComponent getComponent()
	{
		return jScrollPane;
	}

	@Override
	public JTree getTree()
	{
		return jTree;
	}

	@Override
	public void updateTree()
	{
		rootNodeCtrl.updateTree();
	}

	@Override
	public void close() throws IOException
	{
		rootNodeCtrl.close();
	}
}
