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
package de.bernd_michaely.common.filesystem.view.base;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Clients may implement this interface and provide an instance to the
 * {@link de.bernd_michaely.common.filesystem.view.base.Configuration.Builder Configuration.Builder}
 * to create a custom configuration for the file system view component.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public interface UserNodeConfiguration
{
  /**
   * The default link options, an empty array, which means that symbolic links
   * are always followed.
   */
  LinkOption[] DEFAULT_LINK_OPTIONS = new LinkOption[0];

  /**
   * Returns the matching LinkOptions. By default returns the
   * {@link #DEFAULT_LINK_OPTIONS}.
   *
   * @return the matching LinkOptions
   * @see LinkOption#NOFOLLOW_LINKS
   */
  default LinkOption[] getLinkOptions()
  {
    return DEFAULT_LINK_OPTIONS;
  }

  /**
   * A predicate to indicate which subdirectories to create a node for. The
   * default implementation returns true for non hidden directories.
   *
   * @param directory a path to a directory (never {@code null})
   * @return true, if a node should be created for the given subdirectory
   */
  default boolean isCreatingNodeForDirectory(Path directory)
  {
    try
    {
      return !Files.isHidden(directory);
    }
    catch (IOException ex)
    {
      return false;
    }
  }

  /**
   * A predicate to indicate which files to create a node for. The default
   * implementation returns always false.
   *
   * @param file a path to a file (never {@code null})
   * @return true, if a node should be created for the given file
   * @see #createFileSystemFor(Path)
   */
  default boolean isCreatingNodeForFile(Path file)
  {
    return false;
  }

  /**
   * This method can optionally return a FileSystem for a file path. E.g. the
   * method could create a Zip file system for a given {@code *.zip} file. In
   * this case, the {@link #isCreatingNodeForFile(Path)} method must return
   * {@code true}.
   *
   * @param file a path to a file (never {@code null})
   * @return a new FileSystem for the file or {@code null}. The default
   *         implementation always returns {@code null}.
   */
  default @Nullable
  FileSystem createFileSystemFor(Path file)
  {
    return null;
  }

  /**
   * This method is called when a FileSystem is to be closed. Whenever a
   * {@link FileSystem}, which has been created by
   * {@link #createFileSystemFor(Path)}, is about to be closed, the client is
   * responsible for releasing all related resources. (An exception is the
   * {@link java.nio.file.FileSystems#getDefault() default file system} which
   * will never be closed.) This method does nothing by default.
   *
   * @param fileSystem the FileSystem to be closed
   */
  default void onClosingFileSystem(FileSystem fileSystem)
  {
  }

  /**
   * If this method returns true, the node is conceptually treated as a leaf
   * node. A client might want to hide the physical directory structure below
   * special directories. E.g. for directories named "{@code DCIM}",
   * "{@code .svn}" or "{@code .git}", the client might want to display a
   * logical view to a {@code DCF} file system, or a Subversion or Git client.
   *
   * @param path the path corresponding to this node
   * @return true to request the node acting as a leaf node. The default
   *         implementation always returns false.
   */
  default boolean isLeafNode(Path path)
  {
    return false;
  }

  /**
   * To receive an update notifier callback object for the corresponding tree
   * node, this method must return true. The callback will be provided
   * immediately after creation via {@link #setUpdateNotifier(Runnable)}.
   * <em>Note:</em> Only the instances returned by
   * {@link #getUserNodeConfigurationFor(Path)} will have called
   * {@link #setUpdateNotifier(Runnable) setUpdateNotifier(callback)}, not the
   * prototype instance initially given with the {@link Configuration}.
   *
   * @return true to request an update notifier (the default implementation
   *         returns false)
   */
  default boolean isRequestingUpdateNotifier()
  {
    return false;
  }

  /**
   * Set a notifier callback for tree node updates. If an application wants to
   * have a node updated as a result of a change of its configuration state
   * (e.g. to switch between showing/hiding hidden directory entries), it must
   * remember the provided Runnable and run it to update the node. The default
   * implementation does nothing.
   *
   * @param callback a callback to update the corresponding tree node
   * @see #isRequestingUpdateNotifier()
   */
  default void setUpdateNotifier(@Nullable Runnable callback)
  {
  }

  /**
   * Factory method to create configurations per node. Depending on the desired
   * configuration, this method may be implemented differently:
   * <ul>
   * <li>In case that no node specific configuration is desired, the same global
   * instance, e.g. a stateless singleton, can be returned in all cases.</li>
   * <li>In case a node specific configuration is desired, a different instance
   * can be returned for each path, holding its on state per path.</li>
   * </ul>
   *
   * @param path the path of the node to configure
   * @return an instance of this type
   */
  UserNodeConfiguration getUserNodeConfigurationFor(Path path);
}
