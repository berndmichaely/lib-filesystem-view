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

import static java.util.Objects.requireNonNull;

/**
 * DirectoryEntry implementation for subdirectories.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public final class DirectoryEntrySubDirectory extends DirectoryEntry
{
	private @MonotonicNonNull NodeCtrl nodeCtrlDirectory;
	private final Path path;

	DirectoryEntrySubDirectory(Path path)
	{
		this.path = requireNonNull(path, getClass().getName() + " : path is null");
	}

	@Override
	public Path getPath()
	{
		return path;
	}

	@Override
	NodeCtrl initNodeCtrl(NodeConfig nodeConfig)
	{
		nodeCtrlDirectory = new NodeCtrl(this, nodeConfig);
		return nodeCtrlDirectory;
	}

	@Override @Nullable
	NodeCtrl getNodeCtrl()
	{
		return nodeCtrlDirectory;
	}
}
