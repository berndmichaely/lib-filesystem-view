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

import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.*;

import static java.lang.System.Logger.Level.*;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Controller class for directory nodes.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public final class NodeCtrlDirectory extends NodeCtrl
{
  private static final Logger logger = System.getLogger(NodeCtrlDirectory.class.getName());

  private NodeCtrlDirectory(DirectoryEntry directoryEntry, NodeConfig nodeConfig)
  {
    super(directoryEntry, nodeConfig);
  }

  static NodeCtrlDirectory create(DirectoryEntry directoryEntry, NodeConfig nodeConfig)
  {
    final var nodeCtrl = new NodeCtrlDirectory(directoryEntry, nodeConfig);
    nodeCtrl.postInit().getNodeView();
    return nodeCtrl;
  }

  /**
   * Applies path filters and returns a new DirectoryEntry or null. This method
   * is to be used for node expansion and for watch service.
   *
   * @param path the Path to encapsulate
   * @return a new DirectoryEntry or null
   */
  private @Nullable
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
        new DirectoryEntryRegularFile(path) : null;
    }
    else
    {
      return null;
    }
  }

  @Override
  void updateDirectoryEntries()
  {
    try (final Stream<Path> stream = Files.list(getDirectoryEntry().getPath()))
    {
      // Note: filter(Objects::nonNull) currently won't work, see:
      // https://github.com/typetools/checker-framework/issues/5237
      final SortedSet<DirectoryEntry> sortedSet = new TreeSet<>(
        getNodeConfig().getDirectoryEntryComparatorSupplier().get());
      stream.forEach(path ->
      {
        final var entry = pathToDirectoryEntry(path);
        if (entry != null)
        {
          sortedSet.add(entry);
        }
      });
      synchronizeSubNodes(sortedSet);
    }
    catch (AccessDeniedException ex)
    {
      logger.log(INFO, "Access denied for path »" + ex.getFile() + "«");
    }
    catch (IOException ex)
    {
      logger.log(WARNING, ex.toString());
    }
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
    getNodeConfig().getWatchServiceCtrl().registerPath(
      getDirectoryEntry().getPath(), (eventKind, context) ->
    {
      if (ENTRY_CREATE.equals(eventKind))
      {
        if (context != null)
        {
          final Path subPath = getDirectoryEntry().getPath().resolve(context.toString());
          final var directoryEntry = pathToDirectoryEntry(subPath);
          if (directoryEntry != null)
          {
            getSubNodes().add(directoryEntry);
          }
        }
      }
      else if (ENTRY_DELETE.equals(eventKind))
      {
        if (context != null)
        {
          final Path subPath = getDirectoryEntry().getPath().resolve(context.toString());
          getSubNodes().removeItem(new DirectoryEntrySubDirectory(subPath));
        }
      }
      else if (OVERFLOW.equals(eventKind))
      {
        updateDirectoryEntries();
      }
    });
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
    getNodeConfig().getWatchServiceCtrl().unregisterPath(getDirectoryEntry().getPath());
  }
}
