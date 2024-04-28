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
import java.lang.System.Logger;
import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.*;

import static java.lang.System.Logger.Level.*;
import static java.util.Objects.requireNonNull;

/**
 * Base class for controller classes for tree nodes.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public abstract sealed class NodeCtrl permits NodeCtrlFileSystemRoots, NodeCtrlDirectory
{
  private static final Logger logger = System.getLogger(NodeCtrl.class.getName());
  private final SubNodes subNodes;

  /**
   * Creates a new instance.
   *
   * @param directoryEntry
   * @param nodeConfig
   * @param parentState
   * @see #postInit()
   */
  NodeCtrl(DirectoryEntry directoryEntry, NodeConfig nodeConfig)
  {
    this.subNodes = new SubNodes(
      requireNonNull(directoryEntry, getClass().getName() + ": DirectoryEntry must not be null"),
      requireNonNull(nodeConfig, getClass().getName() + ": NodeCtrl must not be null"));
  }

  SubNodes getSubNodes()
  {
    return subNodes;
  }

  DirectoryEntry getDirectoryEntry()
  {
    return getSubNodes().getDirectoryEntry();
  }

  NodeConfig getNodeConfig()
  {
    return getSubNodes().getNodeConfig();
  }

  UserNodeConfiguration getUserNodeConfiguration()
  {
    return getSubNodes().getUserNodeConfiguration();
  }

  /**
   * Returns the corresponding node view.
   *
   * @return the corresponding node view
   */
  public NodeView getNodeView()
  {
    return getSubNodes().getNodeView();
  }

  NodeCtrl postInit()
  {
    getSubNodes().setExpansionHandler(expanded ->
    {
      if (expanded)
      {
        logger.log(TRACE, "Expanding node »" + getDirectoryEntry() + "«");
        getNodeView().setExpanded(true);
        updateDirectoryEntries();
        onExpand();
      }
      else
      {
        logger.log(TRACE, "Collapsing node »" + getDirectoryEntry() + "«");
        onCollapse();
        clearNode();
        getNodeView().setExpanded(false);
      }
    });
    if (getUserNodeConfiguration().isRequestingUpdateNotifier())
    {
      getUserNodeConfiguration().setUpdateNotifier(this::updateDirectory);
    }
    return this;
  }

  @Nullable
  DirectoryEntry findNodeByName(Path name)
  {
    return getSubNodes().findNodeByName(name);
  }

  /**
   * Returns true, if this node is expanded, false, if it is collapsed.
   *
   * @return true, if this node is expanded, false, if it is collapsed
   */
  boolean isExpanded()
  {
    return getSubNodes().isExpanded();
  }

  boolean isLeafNode()
  {
    return getUserNodeConfiguration().isLeafNode(getDirectoryEntry().getPath());
  }

  /**
   * Expand or collapse this node.
   *
   * @param expanded true to expand, false to collapse this node
   */
  public void setExpanded(boolean expanded)
  {
    getSubNodes().setExpanded(expanded && !isLeafNode());
  }

  final void synchronizeSubNodes(SortedSet<DirectoryEntry> currentItems)
  {
    getSubNodes().synchronizeTo(currentItems);
  }

  @Override
  public String toString()
  {
    return getDirectoryEntry().toString();
  }

  abstract void updateDirectoryEntries();

  void clearNode()
  {
    getSubNodes().clear();
  }

  private void updateDirectory()
  {
    logger.log(TRACE, () -> "Updating directory »" + getDirectoryEntry().getPath() + "«");
    getSubNodes().runIfExpanded(() ->
    {
      if (isLeafNode())
      {
        clearNode();
      }
      else
      {
        updateDirectoryEntries();
      }
    });
  }

  @Nullable
  NodeView expandPath(Path absolutePath, int index, boolean expandLastElement)
  {
    final int nameCount = absolutePath.getNameCount();
    if (index < nameCount)
    {
      final int nextIndex = index + 1;
      final boolean isLastElement = nextIndex == nameCount;
      setExpanded(true);
      final Path name = absolutePath.getName(index);
      final DirectoryEntry entry = findNodeByName(name);
      if (entry != null)
      {
        final var nodeCtrl = entry.getNodeCtrl();
        if (nodeCtrl != null)
        {
          final NodeView subNodeView = nodeCtrl.expandPath(absolutePath, nextIndex, expandLastElement);
          final NodeView result = subNodeView != null ? subNodeView : nodeCtrl.getNodeView();
          if (isLastElement && expandLastElement)
          {
            nodeCtrl.setExpanded(true);
          }
          return result;
        }
      }
    }
    return null;
  }

  void updateTree()
  {
    if (isExpanded())
    {
      updateDirectory();
      getSubNodes().forEach(entry ->
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

  /**
   * Returns the currently expanded paths of the sub tree of this node.
   *
   * @return the currently expanded paths
   */
  public SortedSet<Path> getExpandedPaths()
  {
    final var set = new TreeSet<Path>();
    if (getSubNodes().isEmpty())
    {
      set.add(getDirectoryEntry().getPath());
    }
    else
    {
      getSubNodes().forEach(entry ->
      {
        if (entry instanceof DirectoryEntrySubDirectory entrySubDir)
        {
          final NodeCtrlDirectory nodeCtrl = entrySubDir.getNodeCtrl();
          if (nodeCtrl != null)
          {
            set.addAll(nodeCtrl.getExpandedPaths());
          }
        }
      });
    }
    return set;
  }

  /**
   * Method to perform actions after a node has been expanded.
   */
  abstract void onExpand();

  /**
   * Method to perform actions after a node has been collapsed.
   */
  abstract void onCollapse();
}
