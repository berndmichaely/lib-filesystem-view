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

import java.lang.System.Logger;
import java.nio.file.FileSystem;
import java.util.SortedSet;
import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.*;

import static java.lang.System.Logger.Level.*;
import static java.util.Objects.requireNonNullElse;

/**
 * Base class for controller classes for tree root nodes.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public abstract sealed class NodeCtrlFileSystemRoots extends NodeCtrl
  permits NodeCtrlFileSystemRootsGlobal, NodeCtrlFileSystemRootsCustom
{
  private static final Logger logger = System.getLogger(NodeCtrlFileSystemRoots.class.getName());

  NodeCtrlFileSystemRoots(DirectoryEntry directoryEntry, NodeConfig nodeConfig)
  {
    super(directoryEntry, nodeConfig);
  }

  /**
   * Updates the FileSystem root directory entries.
   */
  void doUpdateDirectoryEntries(@Nullable FileSystem fileSystem)
  {
//		iterableCache.doUpdateDirectoryEntries(fileSystem);
    if (fileSystem != null)
    {
      if (fileSystem.isOpen())
      {
        final SortedSet<DirectoryEntry> set = new TreeSet<>(
          getNodeConfig().getDirectoryEntryComparatorSupplier().get());
        fileSystem.getRootDirectories().forEach(path -> set.add(new DirectoryEntrySubDirectory(path)));
        synchronizeSubNodes(set);
      }
      else
      {
        logger.log(WARNING, getClass() +
          "#doUpdateDirectoryEntries(FileSystem) : FileSystem not open : " + fileSystem);
      }
    }
  }

  @Override
  void clearNode()
  {
    super.clearNode();
//		iterableCache.clear();
  }

  @Override
  public String toString()
  {
    return requireNonNullElse(
      getDirectoryEntry().getPath().getFileSystem().toString(),
      getClass().getSimpleName());
  }
}

// The following is a potential optimization, but is difficult to test,
// so don't use it for now …
//	/**
//	 * This class provides a performance optimization for the
//	 * {@link #doUpdateDirectoryEntries(FileSystem)} method.
//	 */
//	private class IterableCache
//	{
//		private final ArrayList<Path> iterableCacheNew = new ArrayList<>();
//		private final SortedSet<Path> iterableCacheOld = new TreeSet<>();
//
//		/**
//		 * Updates the FileSystem root directory entries. This method might run
//		 * frequently in a scheduled thread and rarely detect any changes. So
//		 * (although {@link #synchronizeSubNodes(SortedSet)} would logically already
//		 * do the same) it tries not to produce avoidable heap garbage by reusing a
//		 * cache of items read in the previous iteration.
//		 */
//		void doUpdateDirectoryEntries(@Nullable FileSystem fileSystem)
//		{
//			if (fileSystem != null)
//			{
//				if (fileSystem.isOpen())
//				{
//					boolean hasChanged = false;
//					final int n = iterableCacheNew.size();
//					int i = 0;
//					for (Path rootDirectory : fileSystem.getRootDirectories())
//					{
//						if (i < n)
//						{
//							iterableCacheNew.set(i, rootDirectory);
//						}
//						else
//						{
//							iterableCacheNew.add(rootDirectory);
//							hasChanged = true;
//						}
//						i++;
//						if (!hasChanged)
//						{
//							hasChanged = !iterableCacheOld.contains(rootDirectory);
//						}
//					}
//					hasChanged |= i != n;
//					// remove unused list indices in case the list has shrunk:
//					for (int k = n - 1; k >= i; k--)
//					{
//						iterableCacheNew.remove(k);
//					}
//					if (hasChanged)
//					{
//						iterableCacheOld.clear();
//						iterableCacheOld.addAll(iterableCacheNew);
//						final SortedSet<DirectoryEntry> newSet = new TreeSet<>(
//							getNodeConfig().getDirectoryEntryComparatorSupplier().get());
//						iterableCacheNew.stream()
//							.map(DirectoryEntrySubDirectory::new)
//							.forEach(newSet::add);
//						synchronizeSubNodes(newSet);
//					}
//				}
//				else
//				{
//					logger.log(WARNING, getClass() +
//						"#doUpdateDirectoryEntries(FileSystem) : FileSystem not open : " + fileSystem);
//				}
//			}
//		}
//
//		void clear()
//		{
//			iterableCacheNew.clear();
//			iterableCacheOld.clear();
//		}
//	}
//	private final IterableCache iterableCache = new IterableCache();
