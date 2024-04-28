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
import java.util.List;
import java.util.SortedSet;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.System.Logger.Level.*;

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
  private boolean expanded;
  private @Nullable Consumer<Boolean> expansionHandler;
  private final SubNodesPathView subNodesPathView;

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
        logger.log(TRACE, "Calling callback for ADD »" + subDirectoryEntry + "« @ " + index);
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
      logger.log(TRACE, "ADD ALL " + entries.size() + " items");
    });
    subNodes.setOnItemRemove((subDirectoryEntry, index) ->
    {
      System.out.format("Removing »%s«%n", subDirectoryEntry);
      final NodeCtrl subNodeCtrl = subDirectoryEntry.getNodeCtrl();
      if (subNodeCtrl != null)
      {
        getNodeView().removeSubNodeAt(index);
        subNodeCtrl.setExpanded(false);
      }
      else
      {
        logger.log(WARNING, getClass().getName() + " : Invalid subNodeCtrl in OnItemRemove handler");
      }
      if (getNodeView() instanceof UnitTestCallback callback)
      {
        logger.log(TRACE, "Calling callback for REMOVE »" + subDirectoryEntry + "« @ " + index);
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
          logger.log(WARNING, getClass().getName() + " : Invalid subNodeCtrl in OnItemsClear handler");
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

  void setExpansionHandler(@Nullable Consumer<Boolean> expansionHandler)
  {
    this.expansionHandler = expansionHandler;
  }

  @Nullable
  DirectoryEntry findNodeByName(Path name)
  {
    return subNodesPathView.findNodeByName(name.toString());
  }

  synchronized boolean isExpanded()
  {
    return expanded;
  }

  synchronized void setExpanded(boolean expanded)
  {
    if (this.expanded != expanded)
    {
      this.expanded = expanded;
      if (expansionHandler != null)
      {
        expansionHandler.accept(expanded);
      }
    }
  }

  synchronized DirectoryEntry get(int index)
  {
    return subNodes.get(index);
  }

  synchronized boolean add(DirectoryEntry item)
  {
    return expanded ? subNodes.add(item) : false;
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

  synchronized void clear()
  {
    subNodes.clear();
  }

  synchronized void synchronizeTo(SortedSet<DirectoryEntry> currentItems)
  {
    if (expanded)
    {
      subNodes.synchronizeTo(currentItems);
    }
    else
    {
      clear();
    }
  }

  synchronized void forEach(Consumer<DirectoryEntry> c)
  {
    subNodes.forEach(c);
  }

  synchronized void runIfExpanded(Runnable runnable)
  {
    if (expanded)
    {
      runnable.run();
    }
  }
}
