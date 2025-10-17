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

import de.bernd_michaely.common.filesystem.view.base.Configuration;
import de.bernd_michaely.common.filesystem.view.base.IFileSystemTreeView;
import de.bernd_michaely.common.filesystem.view.base.NodeView;
import de.bernd_michaely.common.filesystem.view.base.PathView;
import de.bernd_michaely.common.filesystem.view.base.RootNodeCtrl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;

/**
 * FileSystemTreeView implementation for JUnit tests.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
class FileSystemTreeViewImpl implements IFileSystemTreeView
{
	private final RootNodeCtrl rootNodeCtrl;
	private Path selectedPath;
	private final Configuration configuration;

	FileSystemTreeViewImpl(Configuration configuration)
	{
		this.rootNodeCtrl = RootNodeCtrl.create(configuration, NodeViewImpl::new);
		this.configuration = configuration;
	}

	@Override
	public Path expandPath(Path absolutePath, boolean expandLastElement, boolean select)
	{
		if (absolutePath != null)
		{
			if (rootNodeCtrl.expandPath(absolutePath, expandLastElement) instanceof NodeViewImpl nodeView)
			{
				final Path path = nodeView.getPathView().getPath();
				if (select)
				{
					selectedPath = path;
				}
				return path;
			}
			else
			{
				return null;
			}
		}
		else
		{
			if (select)
			{
				clearSelection();
			}
			return null;
		}
	}

	@Override
	public void clearSelection()
	{
		selectedPath = null;
	}

	@Override
	public SortedSet<Path> getExpandedPaths()
	{
		return rootNodeCtrl.getExpandedPaths();
	}

	@Override
	public Path getSelectedPath()
	{
		return selectedPath;
	}

	@Override
	public void updateTree()
	{
		rootNodeCtrl.updateTree();
	}

	@Override
	public void close() throws IOException
	{
		rootNodeCtrl.close();
	}
	//
	// Unit-Test specific, package local methods:
	//

	List<NodeViewImpl> getSubNodes()
	{
		return getRootNodeView().getSubNodes().stream().map(nv -> ((NodeViewImpl) nv)).toList();
	}

	RootNodeCtrl getRootNodeCtrl()
	{
		return rootNodeCtrl;
	}

	NodeViewImpl getRootNodeView()
	{
		return (NodeViewImpl) rootNodeCtrl.getNodeView();
	}

	List<String> getEntryNames()
	{
		return getSubNodes().stream()
			.map(NodeViewImpl::getPathView).map(PathView::getName).toList();
	}

	/**
	 * Calls
	 * {@link RootNodeCtrl#expandPath(Path, boolean) RootNodeCtrl#expandPath(path, false)}.
	 *
	 * @param path the path to expand
	 * @return the unit test specific implementation of the {@link NodeView}
	 */
	NodeViewImpl _expandPath(Path path)
	{
		return (NodeViewImpl) rootNodeCtrl.expandPath(path, false);
	}

	Configuration getConfiguration()
	{
		return configuration;
	}
}
