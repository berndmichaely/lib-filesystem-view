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

import de.bernd_michaely.common.filesystem.view.base.Configuration;
import de.bernd_michaely.common.filesystem.view.base.NodeView;
import de.bernd_michaely.common.filesystem.view.base.PathView;
import de.bernd_michaely.common.filesystem.view.base.RootNodeCtrl;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.*;

import static java.lang.System.Logger.Level.*;

/**
 * Controller class for the tree global root node.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public final class NodeCtrlFileSystemRootsGlobal extends NodeCtrlFileSystemRoots
  implements RootNodeCtrl
{
  /**
   * The delay in seconds for the file system roots watch service scheduler.
   */
  static final int WATCHSERVICE_SCHEDULE_DELAY_SEC = 2;

  private static final Logger logger = System.getLogger(NodeCtrlFileSystemRootsGlobal.class.getName());
  private final @Nullable ScheduledExecutorService scheduledExecutorService;

  NodeCtrlFileSystemRootsGlobal(DirectoryEntry directoryEntry, NodeConfig nodeConfig)
  {
    super(directoryEntry, nodeConfig);
    final var watchServiceCtrl = nodeConfig.getWatchServiceCtrl();
    this.scheduledExecutorService = watchServiceCtrl.isInUse() && isWatchingFileSystemRoots() ?
      Executors.newScheduledThreadPool(1, watchServiceCtrl.getThreadFactory()) : null;
  }

  private boolean isWatchingFileSystemRoots()
  {
    final boolean isDefaultFileSystem =
      getDirectoryEntry().getPath().getFileSystem().equals(FileSystems.getDefault());
    final boolean isRunningOnWindows = isRunningOnOS(System.getProperty("os.name"), "win");
    return isDefaultFileSystem && isRunningOnWindows;
  }

  /**
   * Checks whether running on a given OS.
   *
   * @param osName the OS name (e.g. taken from system properties)
   * @param infix  an identifying infix in lower case
   * @return true, iff running on the given OS
   */
  static boolean isRunningOnOS(@Nullable String osName, String infix)
  {
    return osName != null ? osName.toLowerCase().contains(infix) : false;
  }

  /**
   * Factory method to construct the tree overall root node.
   * <h4>Note:</h4>
   * File system roots are watched under the following conditions only:
   * <ul>
   * <li>when running on a Windows OS, and not under a UNIX like OS</li>
   * <li>FileSystem is the default FileSystem
   * ({@link FileSystems#getDefault()})</li>
   * </ul>
   *
   * @param configuration   the global configuration
   * @param nodeViewFactory factory for NodeView objects. Each method call must
   *                        create a new instance
   * @return a new node instance
   */
  public static NodeCtrlFileSystemRootsGlobal create(
    Configuration configuration, Function<PathView, NodeView> nodeViewFactory)
  {
    final var watchServiceCtrl = new WatchServiceCtrl(
      configuration.requestWatchService(), configuration.fileSystem());
    final var nodeConfig = new NodeConfig(nodeViewFactory, watchServiceCtrl,
      configuration.fileNameComparator(), configuration.userNodeConfiguration());
    final var directoryEntry = new DirectoryEntryFileSystem(configuration.fileSystem());
    final var nodeCtrl = directoryEntry.initNodeCtrl(nodeConfig);
    nodeCtrl.postInit().setExpanded(true);
    watchServiceCtrl.startWatchServiceThread();
    return nodeCtrl;
  }

  private FileSystem getFileSystem()
  {
    return getDirectoryEntry().getPath().getFileSystem();
  }

  @Override
  void updateDirectoryEntries()
  {
    doUpdateDirectoryEntries(getFileSystem());
  }

  @Override
  public void updateTree()
  {
    super.updateTree();
  }

  @Override
  public @Nullable
  NodeView expandPath(@Nullable Path absolutePath, boolean expandLastElement)
  {
    if (absolutePath != null)
    {
      if (absolutePath.isAbsolute())
      {
        if (absolutePath.getFileSystem().equals(getFileSystem()))
        {
          // assume this global root node is expanded
          final Path root = absolutePath.getRoot();
          if (root != null)
          {
            final DirectoryEntry entry = findNodeByName(root);
            if (entry != null)
            {
              final NodeCtrl nodeCtrl = entry.getNodeCtrl();
              if (nodeCtrl != null)
              {
                final NodeView subNodeView = nodeCtrl.expandPath(absolutePath, 0, expandLastElement);
                return subNodeView != null ? subNodeView : nodeCtrl.getNodeView();
              }
            }
          }
          return null;
        }
        else
        {
          throw new IllegalArgumentException(getClass().getName() +
            "#expandPath(Path) : path »" + absolutePath + "« has different file system");
        }
      }
      else
      {
        throw new IllegalArgumentException(getClass().getName() +
          "#expandPath(Path) : path »" + absolutePath + "« is not absolute");
      }
    }
    else
    {
      return null;
    }
  }

  @Override
  public void close() throws IOException
  {
    logger.log(TRACE, "Closing FileSystemTreeView root node");
    setExpanded(false);
    getNodeConfig().getWatchServiceCtrl().close();
  }

  /**
   * {@inheritDoc}
   * <p>
   * This implementation starts a WatchService.
   * </p>
   */
  @Override
  void onExpand()
  {
    if (scheduledExecutorService != null)
    {
      logger.log(TRACE, "Starting ExecutorService for watching file system roots");
      scheduledExecutorService.scheduleWithFixedDelay(this::updateDirectoryEntries,
        WATCHSERVICE_SCHEDULE_DELAY_SEC, WATCHSERVICE_SCHEDULE_DELAY_SEC, TimeUnit.SECONDS);
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * This implementation stops a WatchService.
   * </p>
   */
  @Override
  void onCollapse()
  {
    if (scheduledExecutorService != null)
    {
      logger.log(TRACE, "Shutting down ExecutorService for watching file system roots");
      scheduledExecutorService.shutdownNow();
    }
  }
}
