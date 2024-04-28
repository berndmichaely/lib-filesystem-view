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
package de.bernd_michaely.common.filesystem.view.base.common;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

import static java.util.Collections.binarySearch;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSortedSet;

/**
 * A list implementation to keep a sorted list of distinct items which can be
 * continuously adapted to a target state, tracking adding and removing of list
 * items by callbacks.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 * @param <T> the type of list items
 */
public class SynchronizableSortedDistinctList<T> extends AbstractList<T>
  implements RandomAccess
{
  private final @Nullable Comparator<? super T> comparator;
  private final SortedSet<T> emptySortedSet;
  private final List<T> list, listView;
  private @Nullable ItemAddHandler<T, Integer> onItemAdd;
  private @Nullable ItemRemoveHandler<T, Integer> onItemRemove;
  private @Nullable ItemsAddAllHandler<T> onItemsAddAll;
  private @Nullable ItemsClearHandler<T> onItemsClear;

  /**
   * Event handler for adding single elements.
   *
   * @param <T>       the added item
   * @param <Integer> the insert position
   */
  public interface ItemAddHandler<T, Integer> extends BiConsumer<T, Integer>
  {
  }

  /**
   * Event handler for adding single elements.
   *
   * @param <T>       the removed item
   * @param <Integer> the former position of the removed item
   */
  public interface ItemRemoveHandler<T, Integer> extends BiConsumer<T, Integer>
  {
  }

  /**
   * Event handler for adding all elements of a collection to an empty list.
   *
   * @param <T> the elements to add
   */
  public interface ItemsAddAllHandler<T> extends Consumer<Collection<? extends T>>
  {
  }

  /**
   * Event handler for clearing the whole list.
   *
   * @param <T> the elements which are to be removed (an unmodifiable view to
   *            the current list)
   */
  public interface ItemsClearHandler<T> extends Consumer<Collection<? extends T>>
  {
  }

  /**
   * Creates a new instance using a {@code null} comparator.
   */
  public SynchronizableSortedDistinctList()
  {
    this((Comparator<? super T>) null);
  }

  /**
   * Creates a new instance using a given comparator.
   *
   * @param comparator a comparator
   */
  public SynchronizableSortedDistinctList(@Nullable Comparator<? super T> comparator)
  {
    this.list = new ArrayList<>();
    this.listView = unmodifiableList(this.list);
    this.comparator = comparator;
    this.emptySortedSet = unmodifiableSortedSet(new TreeSet<>(comparator));
  }

  /**
   * Creates a new instance initialized with the elements of the given
   * collection.
   *
   * @param collection a collection to be added initially
   * @throws NullPointerException if collection is {@code null}
   */
  public SynchronizableSortedDistinctList(Collection<? extends T> collection)
  {
    this();
    addAll(collection);
  }

  /**
   * Creates a new instance using a given instance.
   *
   * @param collection a collection to be added initially
   * @throws NullPointerException if collection is {@code null}
   */
  public SynchronizableSortedDistinctList(SynchronizableSortedDistinctList<T> collection)
  {
    this(collection.comparator);
    addAll(collection);
  }

  /**
   * Returns the configured comparator.
   *
   * @return the configured comparator (may be null for the natural ordering)
   */
  public @Nullable
  Comparator<? super T> getComparator()
  {
    return comparator;
  }

  /**
   * Returns a previously set event handler.
   *
   * @see #setOnItemAdd(ItemAddHandler)
   * @return the previously set event handler
   */
  public @Nullable
  ItemAddHandler<T, Integer> getOnItemAdd()
  {
    return onItemAdd;
  }

  /**
   * Set an event handler to receive notifications about added single elements.
   *
   * @param onItemAdd an event handler reporting the insertion index and the
   *                  added element. {@code null} can be used to clear a
   *                  previously set handler.
   */
  public void setOnItemAdd(@Nullable ItemAddHandler<T, Integer> onItemAdd)
  {
    this.onItemAdd = onItemAdd;
  }

  /**
   * Returns a previously set event handler.
   *
   * @see #setOnItemsAddAll(ItemsAddAllHandler)
   * @return the previously set event handler
   */
  public @Nullable
  ItemsAddAllHandler<T> getOnItemsAddAll()
  {
    return onItemsAddAll;
  }

  /**
   * Set an event handler to receive notifications about a bulk add operation.
   * This event will be preferred over the
   * {@link #setOnItemAdd(ItemAddHandler) single add event} during
   * {@link #synchronizeTo(Collection) synchronization}, if the list was empty
   * before the synchronization.
   *
   * @param onItemsAddAll an event handler reporting the added elements.
   *                      {@code null} can be used to clear a previously set
   *                      handler.
   */
  public void setOnItemsAddAll(@Nullable ItemsAddAllHandler<T> onItemsAddAll)
  {
    this.onItemsAddAll = onItemsAddAll;
  }

  /**
   * Returns a previously set event handler.
   *
   * @see #setOnItemRemove(ItemRemoveHandler)
   * @return the previously set event handler
   */
  public @Nullable
  ItemRemoveHandler<T, Integer> getOnItemRemove()
  {
    return onItemRemove;
  }

  /**
   * Set an event handler to receive notifications about removed single
   * elements.
   *
   * @param onItemRemove an event handler reporting the previous index of the
   *                     removed element and the removed element itself.
   *                     {@code null} can be used to clear a previously set
   *                     handler.
   */
  public void setOnItemRemove(@Nullable ItemRemoveHandler<T, Integer> onItemRemove)
  {
    this.onItemRemove = onItemRemove;
  }

  /**
   * Returns a previously set event handler.
   *
   * @see #setOnItemsClear(ItemsClearHandler)
   * @return the previously set event handler
   */
  public @Nullable
  ItemsClearHandler<T> getOnItemsClear()
  {
    return onItemsClear;
  }

  /**
   * Set an event handler to receive notifications about a bulk remove
   * operation. This event will be preferred over the
   * {@link #setOnItemRemove(ItemRemoveHandler) single remove event} during
   * {@link #synchronizeTo(Collection) synchronization}, if the list will be
   * empty after the synchronization. The event handler will be called
   * <em>before</em> the list is cleared.
   *
   * @param onItemsClear an event handler reporting the clear list operation.
   *                     {@code null} can be used to clear a previously set
   *                     handler.
   */
  public void setOnItemsClear(@Nullable ItemsClearHandler<T> onItemsClear)
  {
    this.onItemsClear = onItemsClear;
  }

  /**
   * Searches the collection for the given object. The search time is
   * {@code O(log n)}.
   *
   * @param object the object to search for
   * @return true, iff the object is contained in this collection
   * @see #containsItem(T)
   */
  @Override
  public boolean contains(Object object)
  {
    try
    {
      @SuppressWarnings("unchecked")
      final T item = (T) object;
      return containsItem(item);
    }
    catch (ClassCastException ex)
    {
      return false;
    }
  }

  /**
   * Searches the collection for the given item. The search time is
   * {@code O(log n)}.
   *
   * @param item the item to search for
   * @return true, iff the item is contained in this collection
   */
  public boolean containsItem(T item)
  {
    return indexOfItem(item) >= 0;
  }

  /**
   * Returns the list item at the given index.
   *
   * @param index the given index
   * @return the list item at the given index
   * @throws IndexOutOfBoundsException if index is out of range
   */
  @Override
  public T get(int index)
  {
    return listView.get(index);
  }

  /**
   * Returns the index of the given item in the list, if the list contains the
   * item, or a negative value otherwise. The search time is {@code O(log n)}.
   *
   * @param item the item to search for
   * @return the index of the given item, if it is contained in the list, or a
   *         negative value otherwise
   */
  public int indexOfItem(@UnknownInitialization(SynchronizableSortedDistinctList.class)
		SynchronizableSortedDistinctList<T> this,
		T item)
  {
    return binarySearch(list, item, comparator);
  }

  @Override
  public Iterator<T> iterator()
  {
    return listView.iterator();
  }

  @Override
  public int size()
  {
    return listView.size();
  }

  /**
   * Adds the item to the list in sort order, if it is not already contained in
   * the list.
   *
   * @param item the item to add
   * @return true, if the item was added (and was not already contained in the
   *         list)
   */
  @Override
  public boolean add(@UnknownInitialization(SynchronizableSortedDistinctList.class)
		SynchronizableSortedDistinctList<T> this,
		T item)
  {
    final int index = indexOfItem(item);
    final boolean wasNew = index < 0;
    if (wasNew)
    {
      final int insertionPoint = -(index + 1);
      list.add(insertionPoint, item);
      if (this.onItemAdd != null)
      {
        this.onItemAdd.accept(item, insertionPoint);
      }
    }
    return wasNew;
  }

  /**
   * Adds all items of the collection to the list in sort order, if not already
   * contained in the list.
   *
   * @param collection the items to add
   * @return true, if the list has changed
   */
  @Override
  public boolean addAll(@UnknownInitialization(SynchronizableSortedDistinctList.class)
		SynchronizableSortedDistinctList<T> this,
		Collection<? extends T> collection)
  {
    if (list.isEmpty())
    {
      synchronizeTo(collection);
      return !collection.isEmpty();
    }
    else
    {
      boolean result = false;
      for (T item : collection)
      {
        result |= add(item);
      }
      return result;
    }
  }

  /**
   * Removes the element at the given position.
   *
   * @param index {@inheritDoc}
   * @return {@inheritDoc}
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  @Override
  public T remove(int index)
  {
    return list.remove(index);
  }

  /**
   * Removes the given item from this list, if present.
   *
   * @param item the item to remove
   * @return true, if the list was changed
   */
  public boolean removeItem(T item)
  {
    final int index = indexOfItem(item);
    final boolean wasPresent = index >= 0;
    if (wasPresent)
    {
      removeAt(index);
    }
    return wasPresent;
  }

  private void removeAt(@UnknownInitialization(SynchronizableSortedDistinctList.class)
		SynchronizableSortedDistinctList<T> this,
		int index)
  {
    final T item = list.remove(index);
    if (item != null && onItemRemove != null)
    {
      onItemRemove.accept(item, index);
    }
  }

  @Override
  public void clear()
  {
    synchronizeTo(this.emptySortedSet);
  }

  /**
   * Synchronizes the content of this list with the content of the given data.
   *
   * @param currentItems the content to synchronize this list with
   */
  public void synchronizeTo(@UnknownInitialization(SynchronizableSortedDistinctList.class)
		SynchronizableSortedDistinctList<T> this,
		Collection<? extends T> currentItems)
  {
    if (currentItems instanceof SortedSet sortedSet &&
      Objects.equals(sortedSet.comparator(), this.comparator))
    {
      @SuppressWarnings("unchecked")
      final SortedSet<T> sortedItems = sortedSet;
      _synchronizeTo(sortedItems);
    }
    else
    {
      final SortedSet<T> sortedSet = new TreeSet<>(this.comparator);
      sortedSet.addAll(currentItems);
      _synchronizeTo(sortedSet);
    }
  }

  private void _synchronizeTo(@UnknownInitialization(SynchronizableSortedDistinctList.class)
		SynchronizableSortedDistinctList<T> this,
		SortedSet<? extends T> currentItems)
  {
    final boolean emptyBefore = list.isEmpty();
    final boolean emptyAfter = currentItems.isEmpty();
    if (emptyBefore)
    {
      if (!emptyAfter)
      {
        list.addAll(currentItems);
        if (onItemsAddAll != null)
        {
          onItemsAddAll.accept(currentItems);
        }
      }
    }
    else
    {
      if (emptyAfter)
      {
        if (onItemsClear != null)
        {
          onItemsClear.accept(listView);
        }
        list.clear();
      }
      else
      {
        // remove items, which are not present any more, in backward order:
        for (int i = list.size() - 1; i >= 0; i--)
        {
          final T item = list.get(i);
          if (item != null && !currentItems.contains(item))
          {
            removeAt(i);
          }
        }
        // add new items:
        currentItems.forEach(this::add);
      }
    }
  }
}
