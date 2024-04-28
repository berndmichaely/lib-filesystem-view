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
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.SortedSet;
import org.checkerframework.checker.nullness.qual.*;

import static java.lang.System.Logger.Level.*;

/**
 * Controller class for tree sub root nodes.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public final class NodeCtrlFileSystemRootsCustom extends NodeCtrlFileSystemRoots
{
  private static final Logger logger = System.getLogger(NodeCtrlFileSystemRootsCustom.class.getName());
  private @Nullable FileSystem fileSystem;
  private @Nullable NodeCtrl skippingNodeCtrl;

  private NodeCtrlFileSystemRootsCustom(DirectoryEntry directoryEntry, NodeConfig nodeConfig)
  {
    super(directoryEntry, nodeConfig);
  }

  /**
   * Factory method to construct sub tree FileSystem root nodes.
   *
   * @param directoryEntry
   * @param nodeConfig
   * @return a new node instance
   */
  static NodeCtrlFileSystemRootsCustom create(DirectoryEntry directoryEntry, NodeConfig nodeConfig)
  {
    final var result = new NodeCtrlFileSystemRootsCustom(directoryEntry, nodeConfig);
    result.postInit();
    return result;
  }

  /**
   * Returns true, iff there is a single root node which is to be skipped. A
   * single root, denoted as {@code "/"}, could not be included in a path. E.g.
   * consider a {@link Path} returned by
   * <pre>
   * {@code fileSystem.getPath("/", "tmp", "test.zip", "/", "content.txt")}
   * </pre>, which would be the same as
   * <pre>
   * {@code fileSystem.getPath("/", "tmp", "test.zip", "content.txt")}
   * </pre>
   *
   * @return true, iff there is a single root node which is to be skipped
   */
  @EnsuresNonNullIf(expression = "skippingNodeCtrl", result = true)
  private boolean isSkippingSingleRoot()
  {
    return skippingNodeCtrl != null;
  }

  @Override
  void updateDirectoryEntries()
  {
    if (fileSystem == null)
    {
      fileSystem = getUserNodeConfiguration()
        .createFileSystemFor(getDirectoryEntry().getPath());
    }
    doUpdateDirectoryEntries(fileSystem);
  }

  @Override
  void doUpdateDirectoryEntries(@Nullable FileSystem fileSystem)
  {
    if (skippingNodeCtrl instanceof NodeCtrlFileSystemRoots skipping)
    {
      skipping.doUpdateDirectoryEntries(fileSystem);
    }
    else
    {
      super.doUpdateDirectoryEntries(fileSystem);
    }
  }

  @Override
  SubNodes getSubNodes()
  {
    return isSkippingSingleRoot() ?
      skippingNodeCtrl.getSubNodes() :
      super.getSubNodes();
  }

  @Override
  public SortedSet<Path> getExpandedPaths()
  {
    return isSkippingSingleRoot() ?
      skippingNodeCtrl.getExpandedPaths() :
      super.getExpandedPaths();
  }

  @Override @Nullable
  NodeView expandPath(Path absolutePath, int index, boolean expandLastElement)
  {
    return isSkippingSingleRoot() ?
      skippingNodeCtrl.expandPath(absolutePath, index, expandLastElement) :
      super.expandPath(absolutePath, index, expandLastElement);
  }

  @Override
  public NodeView getNodeView()
  {
    return isSkippingSingleRoot() ?
      skippingNodeCtrl.getNodeView() :
      super.getNodeView();
  }

  @Override @Nullable
  DirectoryEntry findNodeByName(Path name)
  {
    return isSkippingSingleRoot() ?
      skippingNodeCtrl.findNodeByName(name) :
      super.findNodeByName(name);
  }

  @Override
  void updateTree()
  {
    if (isSkippingSingleRoot())
    {
      skippingNodeCtrl.updateTree();
    }
    else
    {
      super.updateTree();
    }
  }

  @Override
  void clearNode()
  {
    super.clearNode();
    final FileSystem fs = this.fileSystem;
    if (fs != null && !fs.equals(FileSystems.getDefault()) && fs.isOpen())
    {
      getUserNodeConfiguration().onClosingFileSystem(fs);
      try
      {
        fs.close();
      }
      catch (IOException ex)
      {
        logger.log(WARNING, ex.toString());
      }
    }
    this.fileSystem = null;
  }

  @Override
  public boolean isExpanded()
  {
    return super.getSubNodes().isExpanded();
  }

  @Override
  public void setExpanded(boolean expanded)
  {
    super.getSubNodes().setExpanded(expanded);
  }

  /**
   * {@inheritDoc}
   * <ul>
   * <li>This implementation checks whether the embedded file system has a
   * single root.</li>
   * <li>WatchServices are currently not supported for sub roots.</li>
   * </ul>
   */
  @Override
  void onExpand()
  {
    skippingNodeCtrl = null;
    final var subNodes = super.getSubNodes();
    if (subNodes.size() == 1)
    {
      final DirectoryEntry singleItem = subNodes.get(0);
      final String name = singleItem.getName();
      if ("/".equals(name))
      {
        final var subNodeCtrl = singleItem.getNodeCtrl();
        if (subNodeCtrl != null)
        {
          subNodeCtrl.setExpanded(true);
          skippingNodeCtrl = subNodeCtrl;
        }
        else
        {
          throw new IllegalStateException(getClass().getName() +
            "::onExpand : Invalid subNodeCtrl");
        }
      }
    }
  }

  @Override
  void onCollapse()
  {
    // WatchServices are currently not supported for sub roots.
    if (isSkippingSingleRoot())
    {
      skippingNodeCtrl.setExpanded(false);
      logger.log(TRACE, "RESET skipped node");
      skippingNodeCtrl = null;
    }
  }
}
