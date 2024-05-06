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

import de.bernd_michaely.common.filesystem.view.base.UserNodeConfiguration;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.*;

import static java.lang.System.Logger.Level.*;
import static java.util.Objects.requireNonNull;

/**
 * DirectoryEntry implementation for files.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public final class DirectoryEntryRegularFile extends DirectoryEntry
{
  private static final Logger logger = System.getLogger(DirectoryEntryRegularFile.class.getName());
  private final Path path;
  private final UserNodeConfiguration userNodeConfiguration;
  private @MonotonicNonNull NodeCtrlFileSystemRootsCustom nodeCtrlFileSystemRootsCustom;
  private @Nullable FileSystem customFileSystem;

  DirectoryEntryRegularFile(Path path, UserNodeConfiguration userNodeConfiguration)
  {
    this.path = requireNonNull(path, getClass().getName() + " : path is null");
    this.userNodeConfiguration = userNodeConfiguration;
  }

  @Override
  NodeCtrlFileSystemRootsCustom initNodeCtrl(NodeConfig nodeConfig)
  {
    nodeCtrlFileSystemRootsCustom = NodeCtrlFileSystemRootsCustom.create(this, nodeConfig);
    return nodeCtrlFileSystemRootsCustom;
  }

  @Override @Nullable
  NodeCtrlFileSystemRootsCustom getNodeCtrl()
  {
    return nodeCtrlFileSystemRootsCustom;
  }

  @Override
  public Path getPath()
  {
    return path;
  }

  @Nullable
  FileSystem getCustomFileSystem()
  {
    if (customFileSystem == null)
    {
      customFileSystem = userNodeConfiguration.createFileSystemFor(getPath());
    }
    return customFileSystem;
  }

  void clearCustomFileSystem()
  {
    final FileSystem fs = customFileSystem;
    if (fs != null && !fs.equals(FileSystems.getDefault()) && fs.isOpen())
    {
      userNodeConfiguration.onClosingFileSystem(fs);
      try
      {
        fs.close();
      }
      catch (IOException ex)
      {
        logger.log(WARNING, ex.toString());
      }
    }
    customFileSystem = null;
  }
}
