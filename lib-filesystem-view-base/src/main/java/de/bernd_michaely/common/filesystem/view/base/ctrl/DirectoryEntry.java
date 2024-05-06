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

import de.bernd_michaely.common.filesystem.view.base.PathView;
import java.nio.file.Path;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Class to represent a directory entry. It encapsulates a filesystem and
 * optionally a path.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public abstract sealed class DirectoryEntry implements PathView
  permits DirectoryEntryFileSystem, DirectoryEntrySubDirectory, DirectoryEntryRegularFile
{
  /**
   * Display String of empty path.
   */
  static final String EPSILON = "ε";

  DirectoryEntry()
  {
  }

  abstract NodeCtrl initNodeCtrl(NodeConfig nodeConfig);

  abstract @Nullable
  NodeCtrl getNodeCtrl();

  @Override
  public void handleNodeExpansion(boolean expand)
  {
    final NodeCtrl nodeCtrl = getNodeCtrl();
    if (nodeCtrl != null)
    {
      nodeCtrl.setExpanded(expand);
    }
  }

  /**
   * Returns a name for this path. This is the name of the last element of the
   * path or the root name, if the path has no name elements, or the empty
   * String, if the path has also no root name.
   *
   * @return a name for this path
   */
  @Override
  final public String getName()
  {
    final Path path = getPath();
    final Path p = path.getNameCount() > 0 ? path.getFileName() : path.getRoot();
    final String name = p != null ? p.toString() : null;
    return name != null ? name : "";
  }

  @Override
  public int hashCode()
  {
    return Objects.hashCode(getPath());
  }

  @Override
  public boolean equals(@Nullable Object object)
  {
    if (object instanceof DirectoryEntry other)
    {
      return this.getPath().equals(other.getPath());
    }
    else
    {
      return false;
    }
  }

  /**
   * Provides a path name suitable for display in a UI.
   *
   * @return a path name suitable for display
   */
  @Override
  public String toString()
  {
    final String name = getName();
    return "".equals(name) ? EPSILON : name;
  }
}
