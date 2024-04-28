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

import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.*;

/**
 * DirectoryEntry implementation for subdirectories.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public final class DirectoryEntrySubDirectory extends DirectoryEntry
{
  private @MonotonicNonNull NodeCtrlDirectory nodeCtrlDirectory;

  DirectoryEntrySubDirectory(Path path)
  {
    super(path);
  }

  @Override
  NodeCtrlDirectory initNodeCtrl(NodeConfig nodeConfig)
  {
    return nodeCtrlDirectory = NodeCtrlDirectory.create(this, nodeConfig);
  }

  @Override @Nullable
  NodeCtrlDirectory getNodeCtrl()
  {
    return nodeCtrlDirectory;
  }
}
