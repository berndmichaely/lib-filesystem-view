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

import de.bernd_michaely.common.filesystem.view.base.common.SynchronizableSortedDistinctList;
import java.util.AbstractList;
import java.util.Comparator;
import java.util.RandomAccess;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.binarySearch;

/**
 * A read only list view accessing the embedded path.
 *
 * @see #findNodeByName(String)
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
final class SubNodesPathView extends AbstractList<String> implements RandomAccess
{
  private final SynchronizableSortedDistinctList<DirectoryEntry> list;
  private final Comparator<String> comparator;

  SubNodesPathView(SynchronizableSortedDistinctList<DirectoryEntry> list, Comparator<String> comparator)
  {
    this.list = list;
    this.comparator = comparator;
  }

  @Override
  public String get(int index)
  {
    final DirectoryEntry entry = list.get(index);
    return entry != null ? entry.getName() : "";
  }

  @Override
  public int size()
  {
    return list.size();
  }

  /**
   * Find a list item by path name.
   *
   * @param name the name to search for
   * @return the entry found or {@code null}
   */
  @Nullable
  DirectoryEntry findNodeByName(String name)
  {
    final int index = binarySearch(this, name, comparator);
    return index >= 0 ? list.get(index) : null;
  }
}
