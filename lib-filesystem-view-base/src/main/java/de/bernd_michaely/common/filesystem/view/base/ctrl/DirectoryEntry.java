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
import org.checkerframework.checker.nullness.qual.*;

/**
 * Class to represent a directory entry. It encapsulates a path comparator,
 * turning an externally given path ordering into a natural sort order of this
 * wrapper. Also keeps information about the type of entry (file, dir) and
 * allows to associate a NodeCtrl object.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public abstract sealed class DirectoryEntry extends PathView
  permits DirectoryEntrySubDirectory, DirectoryEntryRegularFile
{
  DirectoryEntry(Path path)
  {
    super(path);
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
}
