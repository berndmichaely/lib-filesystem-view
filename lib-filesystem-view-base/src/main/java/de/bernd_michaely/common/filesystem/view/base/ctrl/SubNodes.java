/*
 * Copyright 2024 Bernd Michaely (info@bernd-michaely.de).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.bernd_michaely.common.filesystem.view.base.ctrl;

import de.bernd_michaely.common.filesystem.view.base.NodeView;
import de.bernd_michaely.common.filesystem.view.base.UserNodeConfiguration;
import de.bernd_michaely.common.filesystem.view.base.common.SynchronizableSortedDistinctList;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.*;

import static de.bernd_michaely.common.filesystem.view.base.ctrl.SubNodes.ExpansionState.*;
import static java.lang.System.Logger.Level.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Class for synchronized access to directory entry sub nodes.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public final class SubNodes
{
	private static final Logger logger = System.getLogger(SubNodes.class.getName());
	private final SynchronizableSortedDistinctList<DirectoryEntry> subNodes;
	private final DirectoryEntry directoryEntry;
	private final NodeConfig nodeConfig;
	private final UserNodeConfiguration userNodeConfiguration;
	private final NodeView nodeView;
	private ExpansionState expansionState = COLLAPSED;
	private final SubNodesPathView subNodesPathView;

	enum ExpansionState
	{
		COLLAPSED(false), EXPANDING(true), EXPANDED(true);

		private final boolean targetState;

		ExpansionState(boolean targetState)
		{
			this.targetState = targetState;
		}

		boolean getTargetState()
		{
			return targetState;
		}
	}

	/**
	 * Internal, package local interface to notify unit tests about watch service
	 * actions.
	 */
	interface UnitTestCallback
	{
		void call(boolean added, DirectoryEntry subDirectoryEntry, int index);
	}

	SubNodes(DirectoryEntry directoryEntry, NodeConfig nodeConfig)
	{
		this.directoryEntry = directoryEntry;
		this.nodeConfig = nodeConfig;
		this.nodeView = this.nodeConfig.getNodeViewFactory().apply(this.directoryEntry);
		this.userNodeConfiguration = nodeConfig.getUserNodeConfiguration(directoryEntry.getPath());
		this.subNodes = new SynchronizableSortedDistinctList<>(
			this.nodeConfig.getDirectoryEntryComparatorSupplier().get());
		this.subNodesPathView = new SubNodesPathView(subNodes,
			this.nodeConfig.getFileNameComparatorSupplier().get());
		subNodes.setOnItemAdd((subDirectoryEntry, index) ->
		{
			final NodeCtrl subNodeCtrl = subDirectoryEntry.initNodeCtrl(getNodeConfig());
			final NodeView subNodeView = subNodeCtrl.getNodeView();
			subNodeView.setLeafNode(subNodeCtrl.isLeafNode());
			getNodeView().insertSubNodeAt(index, subNodeView);
			if (getNodeView() instanceof UnitTestCallback callback)
			{
				logger.log(TRACE, () -> "Calling callback for ADD »" + subDirectoryEntry + "« @ " + index);
				callback.call(true, subDirectoryEntry, index);
			}
		});
		subNodes.setOnItemsAddAll(entries ->
		{
			final List<NodeView> subNodeViews = new ArrayList<>();
			for (DirectoryEntry subDirectoryEntry : entries)
			{
				final NodeCtrl subNodeCtrl = subDirectoryEntry.initNodeCtrl(getNodeConfig());
				final NodeView subNodeView = subNodeCtrl.getNodeView();
				subNodeView.setLeafNode(subNodeCtrl.isLeafNode());
				subNodeViews.add(subNodeView);
			}
			getNodeView().addAllSubNodes(subNodeViews);
			logger.log(TRACE, () -> "ADD ALL " + entries.size() + " items");
		});
		subNodes.setOnItemRemove((subDirectoryEntry, index) ->
		{
			logger.log(TRACE, () -> "Removing »" + subDirectoryEntry + "«");
			final NodeCtrl subNodeCtrl = subDirectoryEntry.getNodeCtrl();
			if (subNodeCtrl != null)
			{
				getNodeView().removeSubNodeAt(index);
				subNodeCtrl.setExpanded(false);
			}
			else
			{
				logger.log(WARNING, () -> getClass().getName() + " : Invalid subNodeCtrl in OnItemRemove handler");
			}
			if (getNodeView() instanceof UnitTestCallback callback)
			{
				logger.log(TRACE, () -> "Calling callback for REMOVE »" + subDirectoryEntry + "« @ " + index);
				callback.call(false, subDirectoryEntry, index);
			}
		});
		subNodes.setOnItemsClear(entries ->
		{
			entries.forEach(subDirectoryEntry ->
			{
				final NodeCtrl subNodeCtrl = subDirectoryEntry.getNodeCtrl();
				if (subNodeCtrl != null)
				{
					subNodeCtrl.setExpanded(false);
				}
				else
				{
					logger.log(WARNING, () -> getClass().getName() + " : Invalid subNodeCtrl in OnItemsClear handler");
				}
			});
			getNodeView().clear();
		});
	}

	DirectoryEntry getDirectoryEntry()
	{
		return directoryEntry;
	}

	NodeConfig getNodeConfig()
	{
		return nodeConfig;
	}

	UserNodeConfiguration getUserNodeConfiguration()
	{
		return userNodeConfiguration;
	}

	NodeView getNodeView()
	{
		return nodeView;
	}

	synchronized @Nullable
	DirectoryEntry findNodeByName(Path name)
	{
		return subNodesPathView.findNodeByName(name.toString());
	}

	synchronized private boolean isLeafNode()
	{
		return getUserNodeConfiguration().isLeafNode(getDirectoryEntry().getPath());
	}

	synchronized boolean isExpanded()
	{
		return expansionState.getTargetState();
	}

	synchronized void setExpanded(boolean expanded)
	{
		if (isExpanded() != expanded)
		{
			if (expanded)
			{
				expansionState = EXPANDING;
				try
				{
					doExpand();
				}
				finally
				{
					expansionState = EXPANDED;
				}
			}
			else
			{
				try
				{
					doCollapse();
				}
				finally
				{
					expansionState = COLLAPSED;
				}
			}
		}
	}

	/**
	 * Applies path filters and returns a new DirectoryEntry or null. This method
	 * is to be used for node expansion and for watch service.
	 *
	 * @param path the Path to encapsulate
	 * @return a new DirectoryEntry or null
	 */
	synchronized private @Nullable
	DirectoryEntry pathToDirectoryEntry(Path path)
	{
		final var unc = getUserNodeConfiguration();
		final var linkOptions = unc.getLinkOptions();
		if (Files.isDirectory(path, linkOptions))
		{
			return unc.isCreatingNodeForDirectory(path) ?
				new DirectoryEntrySubDirectory(path) : null;
		}
		else if (Files.isRegularFile(path, linkOptions))
		{
			return unc.isCreatingNodeForFile(path) ?
				new DirectoryEntryRegularFile(path, unc) : null;
		}
		else
		{
			return null;
		}
	}

	synchronized private void doExpand()
	{
		getNodeView().setExpanded(true);
		updateDirectoryEntries();
	}

	synchronized private void readFileSystem(@Nullable FileSystem fileSystem, boolean skipSingleRoot)
	{
		if (fileSystem != null)
		{
			if (fileSystem.isOpen())
			{
				final SortedSet<DirectoryEntry> set = new TreeSet<>(
					getNodeConfig().getDirectoryEntryComparatorSupplier().get());
				fileSystem.getRootDirectories().forEach(path ->
					set.add(new DirectoryEntrySubDirectory(path)));
				if (skipSingleRoot && set.size() == 1 && set.first().getName().equals("/"))
				{
					readDirectory(fileSystem.getPath("/"));
				}
				else
				{
					subNodes.synchronizeTo(set);
				}
			}
			else
			{
				logger.log(WARNING, () -> getClass() +
					"#doUpdateDirectoryEntries(FileSystem) : FileSystem not open : " + fileSystem);
			}
		}
	}

	synchronized private void readDirectory(Path directory)
	{
		try (final Stream<Path> stream = Files.list(directory))
		{
			final SortedSet<DirectoryEntry> sortedSet = new TreeSet<>(
				getNodeConfig().getDirectoryEntryComparatorSupplier().get());
			// stream.map(this::pathToDirectoryEntry).filter(Objects::nonNull).forEach(sortedSet::add);
			// Note: filter(Objects::nonNull) currently won't work, see:
			// https://github.com/typetools/checker-framework/issues/1345
			stream.forEach(path ->
			{
				final var entry = pathToDirectoryEntry(path);
				if (entry != null)
				{
					sortedSet.add(entry);
				}
			});
			subNodes.synchronizeTo(sortedSet);
		}
		catch (AccessDeniedException ex)
		{
			logger.log(INFO, () -> "Access denied for path »" + ex.getFile() + "«");
		}
		catch (IOException ex)
		{
			logger.log(WARNING, () -> "reading directory", ex);
		}
		getNodeConfig().getWatchServiceCtrl().registerPath(
			getDirectoryEntry().getPath(), (eventKind, context) ->
		{
			if (ENTRY_CREATE.equals(eventKind))
			{
				if (context != null)
				{
					final Path subPath = getDirectoryEntry().getPath().resolve(context.toString());
					final var entry = pathToDirectoryEntry(subPath);
					if (entry != null)
					{
						add(entry);
					}
				}
			}
			else if (ENTRY_DELETE.equals(eventKind))
			{
				if (context != null)
				{
					final Path subPath = getDirectoryEntry().getPath().resolve(context.toString());
					removeItem(new DirectoryEntrySubDirectory(subPath));
				}
			}
			else if (OVERFLOW.equals(eventKind))
			{
				updateDirectoryEntries();
			}
		});
	}

	synchronized void updateDirectoryEntries()
	{
		logger.log(TRACE, () -> "Updating directory »" + getDirectoryEntry().getPath() + "«");
		if (isExpanded())
		{
			if (isLeafNode())
			{
				subNodes.clear();
			}
			else
			{
				if (directoryEntry instanceof DirectoryEntrySubDirectory entry)
				{
					readDirectory(entry.getPath());
				}
				else if (directoryEntry instanceof DirectoryEntryRegularFile entry)
				{
					readFileSystem(entry.getCustomFileSystem(), true);
				}
				else if (directoryEntry instanceof DirectoryEntryFileSystem entry)
				{
					readFileSystem(entry.getFileSystem(), false);
				}
			}
		}
	}

	synchronized void updateTree()
	{
		if (isExpanded())
		{
			updateDirectoryEntries();
			forEach(entry ->
			{
				if (entry instanceof DirectoryEntrySubDirectory)
				{
					final var nodeCtrl = entry.getNodeCtrl();
					if (nodeCtrl != null)
					{
						nodeCtrl.updateTree();
					}
				}
			});
		}
	}

	synchronized private void doCollapse()
	{
		if (directoryEntry instanceof DirectoryEntrySubDirectory)
		{
			getNodeView().setExpanded(false);
			getNodeConfig().getWatchServiceCtrl().unregisterPath(getDirectoryEntry().getPath());
			subNodes.clear();
		}
		else if (directoryEntry instanceof DirectoryEntryRegularFile entry)
		{
			getNodeView().setExpanded(false);
			subNodes.clear();
			entry.clearCustomFileSystem();
		}
		else if (directoryEntry instanceof DirectoryEntryFileSystem entry)
		{
			getNodeView().setExpanded(false);
			subNodes.clear();
		}
	}

	synchronized DirectoryEntry get(int index)
	{
		return subNodes.get(index);
	}

	synchronized boolean add(DirectoryEntry item)
	{
		return isExpanded() ? subNodes.add(item) : false;
	}

	synchronized boolean removeItem(DirectoryEntry item)
	{
		return subNodes.removeItem(item);
	}

	synchronized int size()
	{
		return subNodes.size();
	}

	synchronized boolean isEmpty()
	{
		return subNodes.isEmpty();
	}

	synchronized void forEach(Consumer<DirectoryEntry> c)
	{
		subNodes.forEach(c);
	}
}
