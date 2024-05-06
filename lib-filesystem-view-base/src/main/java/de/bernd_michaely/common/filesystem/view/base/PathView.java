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

import de.bernd_michaely.common.filesystem.view.base.ctrl.DirectoryEntry;
import java.nio.file.Path;

/**
 * Encapsulation of a Path object for tree node UI user object. The
 * {@link Object#toString() toString()} implementation must provide a custom
 * display name.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public sealed interface PathView permits DirectoryEntry
{
  /**
   * Returns the encapsulated path.
   *
   * @return the encapsulated path
   */
  Path getPath();

  /**
   * Returns a name for this path. This is the name of the last element of the
   * path or the root name, if the path has no name elements, or the empty
   * String, if the path has also no root name.
   *
   * @return a name for this path
   */
  String getName();

  /**
   * Notifies the file system tree about the node being expanded or collapsed.
   *
   * @param expand true, if the node has been expanded, false, if collapsed
   */
  public abstract void handleNodeExpansion(boolean expand);
}
