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
import de.bernd_michaely.common.filesystem.view.base.PathView;
import de.bernd_michaely.common.filesystem.view.base.UserNodeConfiguration;
import de.bernd_michaely.common.filesystem.view.base.ctrl.SubNodes.UnitTestCallback;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * NodeView implementation for unit tests.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
class NodeViewImpl implements NodeView, UnitTestCallback
{
//	private final boolean beVerbose = true;
  private final boolean beVerbose = false;
  private final List<NodeView> nodeViews;
  private final List<NodeView> subNodes;
  private final PathView pathView;
  private boolean leafNode, expanded;
  private UnitTestCallback unitTestCallback;

  NodeViewImpl(PathView pathView)
  {
    this.nodeViews = new ArrayList<>();
    this.subNodes = Collections.unmodifiableList(nodeViews);
    this.pathView = pathView;
  }

  @Override
  public void insertSubNodeAt(int index, NodeView subNodeView)
  {
    if (beVerbose)
    {
      System.out.println("NodeView Test Impl : insertSubNode »" + subNodeView + "« @ " + index + " for »" +
        getPathView().getPath() + "«");
      System.out.println("                     -> subnodes: " + getSubNodes());
    }
    nodeViews.add(index, subNodeView);
    if (beVerbose)
    {
      System.out.println("                     => subnodes: " + getSubNodes());
    }
  }

  @Override
  public void addAllSubNodes(Collection<NodeView> subNodeViews)
  {
    nodeViews.addAll(subNodeViews);
  }

  @Override
  public void removeSubNodeAt(int index)
  {
    if (beVerbose)
    {
      System.out.println("NodeView Test Impl : removeSubNodeAt " + index + " for »" +
        getPathView().getPath() + "«");
      System.out.println("                     -> subnodes: " + getSubNodes());
    }
    nodeViews.remove(index);
    if (beVerbose)
    {
      System.out.println("                     => subnodes: " + getSubNodes());
    }
  }

  @Override
  public void clear()
  {
    nodeViews.clear();
  }

  boolean isLeafNode()
  {
    return leafNode;
  }

  @Override
  public void setLeafNode(boolean leafNode)
  {
    this.leafNode = leafNode;
  }

  @Override
  public void setExpanded(boolean expanded)
  {
    if (this.expanded != expanded)
    {
      this.expanded = expanded;
      pathView.handleNodeExpansion(expanded);
    }
  }

  //
  // Unit-Test specific methods:
  //
  void setUnitTestCallback(UnitTestCallback unitTestCallback)
  {
    this.unitTestCallback = unitTestCallback;
  }

  @Override
  public void call(boolean added, DirectoryEntry subDirectoryEntry, int index)
  {
    if (unitTestCallback != null)
    {
      unitTestCallback.call(added, subDirectoryEntry, index);
    }
  }

  List<NodeView> getSubNodes()
  {
    return subNodes;
  }

  DirectoryEntry getPathView()
  {
    return (DirectoryEntry) pathView;
  }

  UserNodeConfiguration getUserNodeConfiguration()
  {
    return getPathView().getNodeCtrl().getUserNodeConfiguration();
  }

  WatchServiceCtrl getWatchServiceCtrl()
  {
    return getPathView().getNodeCtrl().getNodeConfig().getWatchServiceCtrl();
  }

  @Override
  public String toString()
  {
    return pathView.toString();
  }
}
