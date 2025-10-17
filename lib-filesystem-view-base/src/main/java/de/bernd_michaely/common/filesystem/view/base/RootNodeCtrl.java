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

import de.bernd_michaely.common.filesystem.view.base.ctrl.NodeCtrlFileSystemRootsGlobal;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.SortedSet;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Interface to describe the global root node control.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public interface RootNodeCtrl extends Closeable
{
	/**
	 * Returns the corresponding node view.
	 *
	 * @return the corresponding node view
	 */
	NodeView getNodeView();

	/**
	 * Expand or collapse this node.
	 *
	 * @param expanded true to expand, false to collapse this node
	 */
	void setExpanded(boolean expanded);

	/**
	 * Expands the given path in the tree view.
	 *
	 * @param absolutePath      the requested path, which must be an absolute path
	 * @param expandLastElement true to expand the last path element, false to
	 *                          keep it collapsed, if it is not expanded already
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
	NodeView expandPath(@Nullable Path absolutePath, boolean expandLastElement);

	/**
	 * Updates the content of the whole tree view.
	 */
	void updateTree();

	/**
	 * Returns the set of paths currently expanded.
	 *
	 * @return the set of paths currently expanded
	 */
	SortedSet<Path> getExpandedPaths();

	/**
	 * Expands all given paths.
	 *
	 * @param paths the paths to be expanded. All paths must be absolute.
	 * @throws IllegalArgumentException if any given path is not absolute
	 * @see #expandPath(Path, boolean)
	 */
	default void setExpandedPaths(Iterable<Path> paths)
	{
		paths.forEach(path -> expandPath(path, false));
	}

	/**
	 * Returns true, iff the file system roots are watched for changes.
	 *
	 * @return true, iff the file system roots are watched for changes
	 */
	boolean isWatchingFileSystemRoots();

	/**
	 * Factory method to construct the tree overall root node.
	 * <h4>Note:</h4>
	 * File system roots are watched under the following conditions only:
	 * <ul>
	 * <li>when running on a Windows OS, and not under a UNIX like OS</li>
	 * <li>FileSystem is the default FileSystem
	 * ({@link java.nio.file.FileSystems#getDefault()})</li>
	 * </ul>
	 *
	 * @param configuration   the global configuration
	 * @param nodeViewFactory factory for NodeView objects. Each method call must
	 *                        create a new instance
	 * @return a new node instance
	 */
	static RootNodeCtrl create(
		Configuration configuration, Function<PathView, NodeView> nodeViewFactory)
	{
		return NodeCtrlFileSystemRootsGlobal.create(configuration, nodeViewFactory);
	}
}
