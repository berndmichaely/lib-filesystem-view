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

import java.io.Closeable;
import java.nio.file.Path;
import java.util.SortedSet;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Interface describing common API for file system tree views.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public interface IFileSystemTreeView extends Closeable
{
  /**
   * Expands and optionally selects the given path in the tree view.
   *
   * @param absolutePath      the requested path, which must be an absolute path
   * @param expandLastElement true to expand the last path element, false to
   *                          keep it collapsed, if it is not expanded already
   * @param select            if true,
   * <ul>
   * <li>… and the full requested path (and not only a prefix of it) could be
   * expanded (that is the path exists in the file system), the path will also
   * be selected</li>
   * <li>… and the requested path is {@code null}, a current selection will be
   * cleared</li>
   * </ul>
   * @return the path which exists and could be expanded. This may be only a
   *         prefix of the requested path. {@code null} will be returned if the
   *         requested path is {@code null}
   * @throws IllegalArgumentException if:
   * <ul>
   * <li>the requested path is not absolute</li>
   * <li>the file system of the path is different from the one used by this
   * component</li>
   * </ul>
   */
  @Nullable
  Path expandPath(@Nullable Path absolutePath, boolean expandLastElement, boolean select);

  /**
   * Clears the current path selection. Same as
   * {@link #expandPath(java.nio.file.Path, boolean, boolean) expandPath(null, *, true)}.
   */
  default void clearSelection()
  {
    expandPath(null, false, true);
  }

  /**
   * Returns the currently selected path.
   *
   * @return the selected path, null, if none is selected
   */
  @Nullable
  Path getSelectedPath();

  /**
   * Returns true, iff a path is currently selected.
   *
   * @return true, iff a path is currently selected
   */
  default boolean isPathSelected()
  {
    return getSelectedPath() != null;
  }

  /**
   * Returns the set of paths currently expanded.
   *
   * @return the set of paths currently expanded
   */
  SortedSet<Path> getExpandedPaths();

  /**
   * Update the whole tree view.
   */
  void updateTree();
}
