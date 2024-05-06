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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.*;

import static java.util.Objects.requireNonNull;

/**
 * DirectoryEntry implementation for the global filesystem.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public final class DirectoryEntryFileSystem extends DirectoryEntry
{
	private @MonotonicNonNull NodeCtrlFileSystemRootsGlobal nodeCtrlFileSystemRootsGlobal;
	private final FileSystem fileSystem;

	DirectoryEntryFileSystem(FileSystem fileSystem)
	{
		this.fileSystem = requireNonNull(fileSystem, getClass().getName() + " : fileSystem is null");
	}

	FileSystem getFileSystem()
	{
		return fileSystem;
	}

	@Override
	public Path getPath()
	{
		return getFileSystem().getPath("");
	}

	@Override
	NodeCtrlFileSystemRootsGlobal initNodeCtrl(NodeConfig nodeConfig)
	{
		nodeCtrlFileSystemRootsGlobal = new NodeCtrlFileSystemRootsGlobal(this, nodeConfig);
		return nodeCtrlFileSystemRootsGlobal;
	}

	@Override @Nullable
	NodeCtrlFileSystemRootsGlobal getNodeCtrl()
	{
		return nodeCtrlFileSystemRootsGlobal;
	}
}
