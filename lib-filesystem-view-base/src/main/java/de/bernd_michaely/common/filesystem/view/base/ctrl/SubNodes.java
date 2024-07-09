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
import java.lang.System.Logger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
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
	private volatile ExpansionState expansionState = COLLAPSED;
	private final SubNodesPathView subNodesPathView;

	enum ExpansionState
	{
		COLLAPSED, EXPANDING, WAITING, EXPANDED;

		private static final EnumSet<ExpansionState> setExpand =
			EnumSet.of(EXPANDING, WAITING, EXPANDED);

		boolean isIn(ExpansionState... expansionStates)
		{
			for (ExpansionState expansionState : expansionStates)
			{
				if (equals(expansionState))
				{
					return true;
				}
			}
			return false;
		}

		boolean asBoolean()
		{
			return setExpand.contains(this);
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
		subNodes.setOnItemsAdd(indices ->
		{
			final SortedMap<Integer, NodeView> mapSubNodeViews = new TreeMap<>();
			indices.forEach(index ->
			{
				final DirectoryEntry subDirectoryEntry = subNodes.get(index);
				final NodeCtrl subNodeCtrl = subDirectoryEntry.initNodeCtrl(getNodeConfig());
				final NodeView subNodeView = subNodeCtrl.getNodeView();
				subNodeView.setLeafNode(subNodeCtrl.isLeafNode());
				mapSubNodeViews.put(index, subNodeView);
				if (getNodeView() instanceof UnitTestCallback callback)
				{
					logger.log(TRACE, () -> "Calling callback for ADD »" + subDirectoryEntry + "« @ " + index);
					callback.call(true, subDirectoryEntry, index);
				}
			});
			getNodeView().insertSubNodes(mapSubNodeViews);
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
		subNodes.setOnItemsRemove((List<Integer> indices) ->
		{
			getNodeView().removeSubNodes(indices);
			indices.forEach(index ->
			{
				final DirectoryEntry subDirectoryEntry = subNodes.get(index);
				logger.log(TRACE, () -> "Removing »" + subDirectoryEntry + "«");
				final NodeCtrl subNodeCtrl = subDirectoryEntry.getNodeCtrl();
				if (subNodeCtrl != null)
				{
					subNodeCtrl.setExpanded(false);
				}
				else
				{
					logger.log(WARNING, () -> getClass().getName() +
						" : Invalid subNodeCtrl in OnItemRemove handler");
				}
				if (getNodeView() instanceof UnitTestCallback callback)
				{
					logger.log(TRACE, () -> "Calling callback for REMOVE »" +
						subDirectoryEntry + "« @ " + index);
					callback.call(false, subDirectoryEntry, index);
				}
			});
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
					logger.log(WARNING, () -> getClass().getName() +
						" : Invalid subNodeCtrl in OnItemsClear handler");
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
		return expansionState.asBoolean();
	}

	synchronized void setExpanded(boolean expanded)
	{
		if (isExpanded() != expanded)
		{
			if (expanded)
			{
				setExpansionState(EXPANDING);
			}
			else
			{
				setExpansionState(COLLAPSED);
			}
		}
	}

	synchronized private void setExpansionState(ExpansionState newState)
	{
		switch (newState)
		{
			case COLLAPSED ->
			{
				if (!this.expansionState.isIn(COLLAPSED))
				{
					try
					{
						doCollapse();
					}
					finally
					{
						this.expansionState = COLLAPSED;
					}
				}
			}
			case EXPANDING ->
			{
				this.expansionState = EXPANDING;
				getNodeView().setExpanded(true);
				updateDirectoryEntries();
			}
			case WAITING ->
			{
			}
			case EXPANDED ->
			{
				this.expansionState = newState;
			}
			default -> throw new AssertionError("Invalid ExpansionState");
		}
	}

	private void startWatchService()
	{
		getNodeConfig().getWatchServiceCtrl().registerPath(
			getDirectoryEntry().getPath(), (eventKind, context) ->
		{
			if (ENTRY_CREATE.equals(eventKind))
			{
				if (context != null)
				{
					final Path subPath = getDirectoryEntry().getPath().resolve(context.toString());
					final var entry = UserNodeConfigurationUtil.pathToDirectoryEntry(
						getUserNodeConfiguration(), subPath);
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
				readDirectoryEntries(directoryEntry);
			}
		}
	}

	private @Nullable
	DirectoryReaderTask createDirectoryReaderTask(DirectoryEntry directoryEntry)
	{
		final var comparator = getNodeConfig().getDirectoryEntryComparatorSupplier().get();
		final DirectoryReaderTask task;
		if (directoryEntry instanceof DirectoryEntrySubDirectory entry)
		{
			task = new DirectoryReaderTask(this::handleDirectoryReaderTask,
				entry.getPath(), comparator, getUserNodeConfiguration());
		}
		else if (directoryEntry instanceof DirectoryEntryRegularFile entry)
		{
			final var customFileSystem = entry.getCustomFileSystem();
			task = customFileSystem != null ? new DirectoryReaderTask(this::handleDirectoryReaderTask,
				customFileSystem, true, comparator, getUserNodeConfiguration()) : null;
		}
		else if (directoryEntry instanceof DirectoryEntryFileSystem entry)
		{
			task = new DirectoryReaderTask(this::handleDirectoryReaderTask,
				entry.getFileSystem(), false, comparator, getUserNodeConfiguration());
		}
		else
		{
			task = null;
		}
		return task;
	}

	private synchronized void readDirectoryEntries(DirectoryEntry directoryEntry)
	{
		final var task = createDirectoryReaderTask(directoryEntry);
		if (task != null)
		{
//			java.util.concurrent.ForkJoinPool.commonPool().submit(task);
			task.run();
		}
		else
		{
			logger.log(WARNING, "readDirectoryEntries : task is null");
		}
	}

	private synchronized void handleDirectoryReaderTask(DirectoryReaderTask.TaskResult result)
	{
		if (this.expansionState.asBoolean())
		{
			subNodes.synchronizeTo(result.sortedSet());
			setExpansionState(EXPANDED);
			if (result.startingWatchService())
			{
				startWatchService();
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
		else if (directoryEntry instanceof DirectoryEntryFileSystem)
		{
			getNodeView().setExpanded(false);
			subNodes.clear();
		}
	}

	synchronized boolean add(DirectoryEntry item)
	{
		return isExpanded() ? subNodes.add(item) : false;
	}

	synchronized boolean removeItem(DirectoryEntry item)
	{
		return subNodes.removeItem(item);
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
