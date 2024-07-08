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
import java.util.function.Consumer;
import java.util.stream.IntStream;
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
	private @Nullable ItemsAddHandler onItemsAdd;
	private @Nullable ItemsRemoveHandler onItemsRemove;
	private @Nullable ItemsAddAllHandler<T> onItemsAddAll;
	private @Nullable ItemsClearHandler<T> onItemsClear;

	/**
	 * Event handler for adding elements. The items have to be processed in sort
	 * order.
	 *
	 */
	public interface ItemsAddHandler extends Consumer<List<Integer>>
	{
	}

	/**
	 * Event handler for removing elements. The items have to be processed in the
	 * given order.
	 */
	public interface ItemsRemoveHandler extends Consumer<List<Integer>>
	{
	}

	/**
	 * Event handler for adding all elements of a collection to an empty list.
	 *
	 * @param <T> the type of elements
	 */
	public interface ItemsAddAllHandler<T> extends Consumer<Collection<T>>
	{
	}

	/**
	 * Event handler for clearing the whole list.
	 *
	 * @param <T> the type of elements
	 */
	public interface ItemsClearHandler<T> extends Consumer<Collection<T>>
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
	 * @see #setOnItemsAdd(ItemAddHandler)
	 * @return the previously set event handler
	 */
	public @Nullable
	ItemsAddHandler getOnItemsAdd()
	{
		return onItemsAdd;
	}

	/**
	 * Set an event handler to receive notifications about added single elements.
	 *
	 * @param onItemsAdd an event handler reporting the insertion index and the
	 *                   added element. {@code null} can be used to clear a
	 *                   previously set handler.
	 */
	public void setOnItemsAdd(@Nullable ItemsAddHandler onItemsAdd)
	{
		this.onItemsAdd = onItemsAdd;
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
	 * {@link #setOnItemsAdd(ItemAddHandler) single add event} during
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
	 * @see #setOnItemsRemove(ItemRemoveHandler)
	 * @return the previously set event handler
	 */
	public @Nullable
	ItemsRemoveHandler getOnItemsRemove()
	{
		return onItemsRemove;
	}

	/**
	 * Set an event handler to receive notifications about removed single
	 * elements.
	 *
	 * @param onItemsRemove an event handler reporting the previous index of the
	 *                      removed element and the removed element itself.
	 *                      {@code null} can be used to clear a previously set
	 *                      handler.
	 */
	public void setOnItemsRemove(@Nullable ItemsRemoveHandler onItemsRemove)
	{
		this.onItemsRemove = onItemsRemove;
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
	 * {@link #setOnItemsRemove(ItemRemoveHandler) single remove event} during
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
			final var addHandler = this.onItemsAdd;
			if (addHandler != null)
			{
				addHandler.accept(List.of(insertionPoint));
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
			return synchronizeTo(collection);
		}
		else
		{
			return addNewItems(collection);
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
		if (onItemsRemove != null)
		{
			onItemsRemove.accept(List.of(index));
		}
		list.remove(index);
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
	 * @return true, if the list has changed
	 */
	public boolean synchronizeTo(@UnknownInitialization(SynchronizableSortedDistinctList.class)
		SynchronizableSortedDistinctList<T> this,
		Collection<? extends T> currentItems)
	{
		if (currentItems instanceof TreeSet treeSet &&
			Objects.equals(treeSet.comparator(), this.comparator))
		{
			@SuppressWarnings("unchecked")
			final SortedSet<T> sortedItems = unmodifiableSortedSet(treeSet);
			return _synchronizeTo(sortedItems);
		}
		else
		{
			final TreeSet<T> sortedSet = new TreeSet<>(this.comparator);
			sortedSet.addAll(currentItems);
			return _synchronizeTo(sortedSet);
		}
	}

	/**
	 * Add new items in ascending order.
	 *
	 * @param currentItems new target state
	 * @return true, iff new items have been added
	 */
	private boolean addNewItems(@UnknownInitialization(SynchronizableSortedDistinctList.class)
		SynchronizableSortedDistinctList<T> this,
		Collection<? extends T> collection)
	{
		final List<Integer> itemsToAdd = new ArrayList<>();
		collection.forEach(item ->
		{
			final int index = indexOfItem(item);
			final boolean wasNew = index < 0;
			if (wasNew)
			{
				final int insertionPoint = -(index + 1);
				list.add(insertionPoint, item);
				itemsToAdd.add(insertionPoint);
			}
		});
		final boolean hasAdditions = !itemsToAdd.isEmpty();
		if (hasAdditions && onItemsAdd != null)
		{
			onItemsAdd.accept(unmodifiableList(itemsToAdd));
		}
		return hasAdditions;
	}

	/**
	 * Synchronizes the content of this list with the content of the given data.
	 *
	 * @param currentItems the content to synchronize this list with
	 * @return true, if the list has changed
	 */
	private boolean _synchronizeTo(@UnknownInitialization(SynchronizableSortedDistinctList.class)
		SynchronizableSortedDistinctList<T> this,
		SortedSet<T> currentItems)
	{
		final boolean isEmptyBefore = list.isEmpty();
		final boolean isEmptyAfter = currentItems.isEmpty();
		if (isEmptyBefore)
		{
			if (isEmptyAfter)
			{
				return false;
			}
			else
			{
				list.addAll(currentItems);
				if (onItemsAddAll != null)
				{
					onItemsAddAll.accept(currentItems);
				}
				return true;
			}
		}
		else
		{
			if (isEmptyAfter)
			{
				if (onItemsClear != null)
				{
					onItemsClear.accept(listView);
				}
				list.clear();
				return true;
			}
			else
			{
				// remove items, which are not present any more, in descending order:
				final List<Integer> listRemove = unmodifiableList(IntStream
					.iterate(list.size() - 1, i -> i >= 0, i -> i - 1)
					.filter(i ->
					{
						try
						{
							@SuppressWarnings("argument")
							final boolean matches = !currentItems.contains(list.get(i));
							return matches;
						}
						catch (NullPointerException ex)
						{
							throw new IllegalStateException(getClass().getName() +
								" : Comparator must support null value", ex);
						}
					})
					.mapToObj(Integer::valueOf).toList());
				final boolean hasDeletions = !listRemove.isEmpty();
				if (hasDeletions && onItemsRemove != null)
				{
					onItemsRemove.accept(listRemove);
				}
				listRemove.forEach(i -> list.remove((int) i));
				// add new items in ascending order:
				final boolean hasNewItems = addNewItems(currentItems);
				return hasDeletions || hasNewItems;
			}
		}
	}
}
