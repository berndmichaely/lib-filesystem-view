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

import de.bernd_michaely.common.filesystem.view.base.common.SynchronizableSortedDistinctList.ItemAddHandler;
import de.bernd_michaely.common.filesystem.view.base.common.SynchronizableSortedDistinctList.ItemRemoveHandler;
import de.bernd_michaely.common.filesystem.view.base.common.SynchronizableSortedDistinctList.ItemsAddAllHandler;
import de.bernd_michaely.common.filesystem.view.base.common.SynchronizableSortedDistinctList.ItemsClearHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.IntStream;
import org.junit.jupiter.api.*;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SynchronizableSortedDistinctList.
 *
 * @author Bernd Michaely (info@bernd-michaely.de)
 */
public class SynchronizableSortedDistinctListTest
{
  /**
   * Test default constructor using {@code null} comparator, that is natural
   * ordering of elements.
   */
  @Test
  public void testConstructorDefault()
  {
    final var list = new SynchronizableSortedDistinctList<Integer>();
    assertTrue(list.isEmpty());
    assertEquals(0, list.size());
    assertTrue(list.add(3));
    assertFalse(list.isEmpty());
    assertEquals(1, list.size());
    assertTrue(list.add(2));
    assertEquals(2, list.size());
    assertTrue(list.add(1));
    assertEquals(3, list.size());
    assertFalse(list.add(2));
    assertEquals(3, list.size());
    assertTrue(list.addAll(List.of(9, 8, 7, 2, 4, 5, 6)));
    assertIterableEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), list);
    assertFalse(list.addAll(List.of(9, 8, 7, 6, 5, 4, 3, 2, 1)));
    assertIterableEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), list);
    list.clear();
    assertTrue(list.isEmpty());
    assertEquals(0, list.size());
  }

  @Test
  public void testConstructorComparator()
  {
    final var comparator = ((Comparator<Integer>) Integer::compareTo).reversed();
    final var list = new SynchronizableSortedDistinctList<>(comparator);
    assertEquals(comparator, list.getComparator());
    assertTrue(list.isEmpty());
    assertEquals(0, list.size());
    assertTrue(list.add(2));
    assertEquals(1, list.size());
    assertFalse(list.isEmpty());
    assertTrue(list.add(1));
    assertEquals(2, list.size());
    assertTrue(list.add(3));
    assertEquals(3, list.size());
    assertFalse(list.add(2));
    assertEquals(3, list.size());
    list.addAll(List.of(9, 8, 7, 4, 5, 6));
    assertIterableEquals(List.of(9, 8, 7, 6, 5, 4, 3, 2, 1), list);
    final var listNaturalOrdering = new SynchronizableSortedDistinctList<String>(
      (Comparator<String>) null);
    assertNull(listNaturalOrdering.getComparator());
    listNaturalOrdering.add("b");
    listNaturalOrdering.add("a");
    listNaturalOrdering.add("b");
    assertIterableEquals(List.of("a", "b"), listNaturalOrdering);
  }

  @Test
  public void testConstructorCollection()
  {
    assertThrows(NullPointerException.class,
      () -> new SynchronizableSortedDistinctList<>((Collection<?>) null));
    final var list = new SynchronizableSortedDistinctList<Integer>(
      List.of(3, 2, 3, 1, 9, 8, 7, 4, 5, 6));
    assertFalse(list.isEmpty());
    assertEquals(9, list.size());
    assertIterableEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), list);
  }

  @Test
  public void testCopyConstructor()
  {
    assertThrows(NullPointerException.class,
      () -> new SynchronizableSortedDistinctList<>((SynchronizableSortedDistinctList<?>) null));
    final var comparator = ((Comparator<Integer>) Integer::compareTo).reversed();
    final var listSrc = new SynchronizableSortedDistinctList<>(comparator);
    listSrc.addAll(List.of(2, 1, 3, 2));
    final var listDst = new SynchronizableSortedDistinctList<>(listSrc);
    assertFalse(listDst.isEmpty());
    assertEquals(3, listDst.size());
    listDst.addAll(List.of(9, 8, 7, 4, 5, 6));
    assertEquals(9, listDst.size());
    assertIterableEquals(List.of(9, 8, 7, 6, 5, 4, 3, 2, 1), listDst);
  }

  @Test
  public void testContains()
  {
    final var list = new SynchronizableSortedDistinctList<Integer>(List.of(1, 2, 3));
    assertTrue(list.containsItem(2));
    assertTrue(list.contains(3));
    assertFalse(list.contains("An argument of non matching type"));
  }

  @Test
  public void testListFunctionality()
  {
    final var list = new SynchronizableSortedDistinctList<Integer>();
    assertTrue(list.isEmpty());
    assertEquals(0, list.size());
    assertTrue(list.add(17));
    assertFalse(list.isEmpty());
    assertEquals(1, list.size());
    assertFalse(list.add(17));
    assertFalse(list.isEmpty());
    assertEquals(1, list.size());
    assertTrue(list.add(18));
    assertFalse(list.isEmpty());
    assertEquals(2, list.size());
    assertEquals(17, list.get(0));
    assertEquals(18, list.get(1));
    assertEquals(1, list.indexOfItem(18));
    assertTrue(list.add(16));
    assertFalse(list.isEmpty());
    assertEquals(3, list.size());
    assertIterableEquals(List.of(16, 17, 18), list);
    assertEquals(1, list.indexOfItem(17));
    assertTrue(list.indexOfItem(3) < 0);
    assertTrue(list.containsItem(16));
    assertFalse(list.containsItem(4));
    assertTrue(list.removeItem(17));
    assertFalse(list.isEmpty());
    assertEquals(2, list.size());
    assertIterableEquals(List.of(16, 18), list);
    assertFalse(list.removeItem(17));
    assertFalse(list.isEmpty());
    assertEquals(2, list.size());
    assertIterableEquals(List.of(16, 18), list);
    assertTrue(list.removeItem(18));
    assertTrue(list.removeItem(16));
    assertTrue(list.isEmpty());
    assertEquals(0, list.size());
    list.add(7);
    list.add(8);
    list.add(9);
    assertIterableEquals(List.of(7, 8, 9), list);
    list.remove(1);
    assertIterableEquals(List.of(7, 9), list);
    list.clear();
    assertTrue(list.isEmpty());
    assertEquals(0, list.size());
  }

  static <C extends Comparable<C>> boolean isStrictlyMonotonicIncreasing(Iterable<C> iterable)
  {
    return isStrictlyMonotonicIncreasing(iterable.iterator());
  }

  static <C extends Comparable<C>> boolean isStrictlyMonotonicIncreasing(Iterator<C> iterator)
  {
    if (iterator.hasNext())
    {
      C item1 = iterator.next();
      while (iterator.hasNext())
      {
        C item2 = iterator.next();
        if (item1.compareTo(item2) >= 0)
        {
          return false;
        }
        item1 = item2;
      }
    }
    return true;
  }

  private static List<Integer> initList()
  {
    final List<Integer> list = new ArrayList<>();
    IntStream.rangeClosed(-999, 999).forEach(list::add);
    return unmodifiableList(list);
  }

  @Test
  public void testSorting()
  {
    final var listFill = initList();
    final var listRandom = new ArrayList<Integer>(listFill);
    do
    {
      Collections.shuffle(listRandom);
    }
    while (isStrictlyMonotonicIncreasing(listRandom));
    final var list1 = new SynchronizableSortedDistinctList<>(listRandom);
    assertTrue(isStrictlyMonotonicIncreasing(list1));
    assertEquals(listFill, list1);
    final var list2 = new SynchronizableSortedDistinctList<Integer>(
      List.of(1111, -2222, 3333));
    assertEquals(List.of(-2222, 1111, 3333), list2);
    list2.synchronizeTo(new TreeSet<>(listFill));
    assertTrue(isStrictlyMonotonicIncreasing(list2));
    assertEquals(list1, list2);
  }

  @Test
  public void testUnsupportedOperations()
  {
    final var listFill = List.of("One", "Two", "Three", "Four", "Five");
    final var list = new SynchronizableSortedDistinctList<>(listFill);
    final var ex = UnsupportedOperationException.class;
    assertThrows(ex, () -> list.add(1, "Seven"));
    assertThrows(ex, () -> list.addAll(1, List.of("Seven", "Eight")));
    assertThrows(ex, () ->
    {
      final var iterator = list.iterator();
      while (iterator.hasNext())
      {
        iterator.next();
        iterator.remove();
      }
    });
    assertThrows(ex, () -> list.set(1, "Seven"));
    assertThrows(ex, () -> list.sort(null));
    assertThrows(ex, () -> list.sort(String::compareTo));
  }

  @Test
  public void testEquals_HashCode()
  {
    final List<Integer> listFill = List.of(9, 5, 1, 7, 5, 3, 8, 5, 2, 6, 5, 4);
    final var ssdl1 = new SynchronizableSortedDistinctList<>(listFill);
    final var comparator = ((Comparator<Integer>) Integer::compareTo).reversed();
    final var ssdl1_rev = new SynchronizableSortedDistinctList<>(comparator);
    ssdl1_rev.addAll(listFill);
    final var ssdl2 = new SynchronizableSortedDistinctList<>(listFill);
    final var ssdl2_rev = new SynchronizableSortedDistinctList<>(comparator);
    ssdl2_rev.addAll(listFill);
    final List<Integer> list1 = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
    final List<Integer> list1_rev = List.of(9, 8, 7, 6, 5, 4, 3, 2, 1);
    final List<Integer> list2 = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    // test list equality
    assertTrue(ssdl1.equals(list1));
    assertEquals(ssdl1.hashCode(), list1.hashCode());
    assertTrue(ssdl1_rev.equals(list1_rev));
    assertEquals(ssdl1_rev.hashCode(), list1_rev.hashCode());
    assertTrue(ssdl1.equals(ssdl2));
    assertEquals(ssdl1.hashCode(), ssdl2.hashCode());
    assertTrue(ssdl1_rev.equals(ssdl2_rev));
    assertEquals(ssdl1_rev.hashCode(), ssdl2_rev.hashCode());
    assertFalse(list2.equals(new SynchronizableSortedDistinctList<>(ssdl2)));
    assertFalse(ssdl1.equals(new TreeSet<>(listFill)));
    assertTrue(list1.equals(ssdl1));
    assertTrue(list1_rev.equals(ssdl1_rev));
  }

  @Test
  public void testStream()
  {
    assertIterableEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
      new SynchronizableSortedDistinctList<>(
        List.of(9, 5, 1, 7, 5, 3, 8, 5, 2, 6, 5, 4)).stream().toList());
  }

  @Test
  void testNullItem()
  {
    final List<Integer> listExpNullFirst = unmodifiableList(asList(null, 1));
    final List<Integer> listExpNullLast = unmodifiableList(asList(1, null));
    final List<Integer> listExpNullLast2 = unmodifiableList(asList(1, 3, null));
    final Comparator<Integer> compNullsFirst = Comparator.nullsFirst(Integer::compareTo);
    final Comparator<Integer> compNullsLast = Comparator.nullsLast(Integer::compareTo);
    final var listNullFirst = new SynchronizableSortedDistinctList<Integer>(compNullsFirst);
    final var listNullLast = new SynchronizableSortedDistinctList<Integer>(compNullsLast);
    // list functionality
    assertTrue(listNullLast.isEmpty());
    listNullLast.add(3);
    listNullLast.add(null);
    listNullLast.add(1);
    assertEquals(1, listNullLast.get(0));
    assertEquals(3, listNullLast.get(1));
    assertNull(listNullLast.get(2));
    assertTrue(listNullLast.contains(null));
    assertTrue(listNullLast.containsItem(null));
    assertEquals(listNullLast.indexOfItem(null), 2);
    listNullLast.removeItem(null);
    assertIterableEquals(List.of(1, 3), listNullLast);
    listNullLast.add(null);
    assertIterableEquals(listExpNullLast2, listNullLast);
    listNullLast.clear();
    assertTrue(listNullLast.isEmpty());
    // sorting (1)
    listNullFirst.add(null);
    listNullFirst.add(1);
    assertIterableEquals(listExpNullFirst, listNullFirst);
    // sorting (2)
    listNullFirst.clear();
    listNullFirst.add(1);
    listNullFirst.add(null);
    assertIterableEquals(listExpNullFirst, listNullFirst);
    // sorting (3)
    listNullLast.clear();
    listNullLast.add(null);
    listNullLast.add(1);
    assertIterableEquals(listExpNullLast, listNullLast);
    // sorting (4)
    listNullLast.clear();
    listNullLast.add(1);
    listNullLast.add(null);
    assertIterableEquals(listExpNullLast, listNullLast);
    // addAll
    listNullLast.clear();
    assertTrue(listNullLast.isEmpty());
    listNullLast.addAll(asList(3, null, 1));
    assertIterableEquals(listExpNullLast2, listNullLast);
    // synchronizeTo
    listNullLast.clear();
    assertTrue(listNullLast.isEmpty());
    listNullLast.synchronizeTo(asList(3, null, 1));
    assertIterableEquals(listExpNullLast2, listNullLast);
  }

  private static class Counter
  {
    private int saved, counter;

    int counter()
    {
      return counter;
    }

    int delta()
    {
      return counter - saved;
    }

    void inc()
    {
      counter++;
    }

    void save()
    {
      saved = counter;
    }
  }

  private static class EventHandlerAdd<T> extends Counter implements ItemAddHandler<T, Integer>
  {
    int lastIndex;
    T lastItem;

    @Override
    public void accept(T item, Integer index)
    {
      lastItem = item;
      lastIndex = index;
      inc();
    }
  }

  private static class EventHandlerRemove<T> extends Counter implements ItemRemoveHandler<T, Integer>
  {
    T lastItem;
    int lastIndex;

    @Override
    public void accept(T item, Integer index)
    {
      lastItem = item;
      lastIndex = index;
      inc();
    }
  }

  private static class EventHandlerAddAll<T> extends Counter implements ItemsAddAllHandler<T>
  {
    List<? extends T> lastItems;

    @Override
    public void accept(Collection<? extends T> collection)
    {
      inc();
      lastItems = List.copyOf(collection);
    }
  }

  private static class EventHandlerClear<T> extends Counter implements ItemsClearHandler<T>
  {
    List<? extends T> lastItems;

    @Override
    public void accept(Collection<? extends T> collection)
    {
      inc();
      lastItems = List.copyOf(collection);
    }
  }

  private static class ListEvents<T>
  {
    private final EventHandlerAdd<T> onItemAdd = new EventHandlerAdd<>();
    private final EventHandlerRemove<T> onItemRemove = new EventHandlerRemove<>();
    private final EventHandlerAddAll<T> onItemsAddAll = new EventHandlerAddAll<>();
    private final EventHandlerClear<T> onItemsClear = new EventHandlerClear<>();

    private ListEvents(SynchronizableSortedDistinctList<T> list)
    {
      list.setOnItemAdd(onItemAdd);
      list.setOnItemRemove(onItemRemove);
      list.setOnItemsAddAll(onItemsAddAll);
      list.setOnItemsClear(onItemsClear);
    }

    private void saveAll()
    {
      onItemAdd.save();
      onItemRemove.save();
      onItemsAddAll.save();
      onItemsClear.save();
    }

    private boolean checkCounter(int counterAdd, int counterRemove, int counterAddAll, int counterClear)
    {
      final boolean result =
        counterAdd == onItemAdd.counter() &&
        counterRemove == onItemRemove.counter() &&
        counterAddAll == onItemsAddAll.counter() &&
        counterClear == onItemsClear.counter();
      saveAll();
      return result;
    }

    private boolean checkInc(boolean incAdd, boolean incRemove, boolean incAddAll, boolean incClear)
    {
      final boolean result =
        onItemAdd.delta() == (incAdd ? 1 : 0) &&
        onItemRemove.delta() == (incRemove ? 1 : 0) &&
        onItemsAddAll.delta() == (incAddAll ? 1 : 0) &&
        onItemsClear.delta() == (incClear ? 1 : 0);
      saveAll();
      return result;
    }
  }

  @Test
  public void testListEventsSingleItems()
  {
    final var list = new SynchronizableSortedDistinctList<String>();
    final var listEvents = new ListEvents<>(list);
    final var onItemAdd = listEvents.onItemAdd;
    final var onItemRemove = listEvents.onItemRemove;
    // init
    assertIterableEquals(List.of(), list);
    listEvents.checkCounter(0, 0, 0, 0);
    // add
    assertTrue(list.add("1"));
    assertIterableEquals(List.of("1"), list);
    listEvents.checkInc(true, false, false, false);
    assertEquals(0, onItemAdd.lastIndex);
    assertEquals("1", onItemAdd.lastItem);
    // add
    assertFalse(list.add("1"));
    assertIterableEquals(List.of("1"), list);
    listEvents.checkInc(false, false, false, false);
    // add
    assertTrue(list.add("3"));
    assertIterableEquals(List.of("1", "3"), list);
    listEvents.checkInc(true, false, false, false);
    assertEquals(1, onItemAdd.lastIndex);
    assertEquals("3", onItemAdd.lastItem);
    // add
    assertTrue(list.add("2"));
    assertIterableEquals(List.of("1", "2", "3"), list);
    listEvents.checkInc(true, false, false, false);
    assertEquals(1, onItemAdd.lastIndex);
    assertEquals("2", onItemAdd.lastItem);
    // remove
    listEvents.checkCounter(3, 0, 0, 0);
    assertTrue(list.removeItem("2"));
    assertIterableEquals(List.of("1", "3"), list);
    listEvents.checkInc(false, true, false, false);
    assertEquals(1, onItemRemove.lastIndex);
    assertEquals("2", onItemRemove.lastItem);
    // remove
    assertFalse(list.removeItem("2"));
    assertIterableEquals(List.of("1", "3"), list);
    listEvents.checkInc(false, false, false, false);
    // remove
    assertTrue(list.removeItem("3"));
    assertIterableEquals(List.of("1"), list);
    listEvents.checkInc(false, true, false, false);
    assertEquals(1, onItemRemove.lastIndex);
    assertEquals("3", onItemRemove.lastItem);
    // remove
    assertTrue(list.removeItem("1"));
    assertIterableEquals(List.of(), list);
    listEvents.checkInc(false, true, false, false);
    assertEquals(0, onItemRemove.lastIndex);
    assertEquals("1", onItemRemove.lastItem);
    listEvents.checkCounter(3, 3, 0, 0);
    // remove handler
    assertEquals(onItemAdd, list.getOnItemAdd());
    assertEquals(onItemRemove, list.getOnItemRemove());
    list.setOnItemAdd(null);
    list.setOnItemRemove(null);
    assertNull(list.getOnItemAdd());
    assertNull(list.getOnItemRemove());
    assertTrue(list.isEmpty());
    assertTrue(list.add("One"));
    assertFalse(list.isEmpty());
    listEvents.checkCounter(3, 3, 0, 0);
    assertTrue(list.removeItem("One"));
    assertTrue(list.isEmpty());
    listEvents.checkCounter(3, 3, 0, 0);
  }

  @Test
  public void testListEventsBulk()
  {
    final var list = new SynchronizableSortedDistinctList<String>(
      ((Comparator<String>) String::compareTo).reversed());
    final var listEvents = new ListEvents<>(list);
    // init
    assertIterableEquals(List.of(), list);
    listEvents.checkCounter(0, 0, 0, 0);
    // add all
    assertFalse(list.addAll(List.of()));
    listEvents.checkInc(false, false, false, false);
    assertTrue(list.addAll(List.of("3", "1", "2")));
    listEvents.checkInc(false, false, true, false);
    assertIterableEquals(List.of("3", "2", "1"), listEvents.onItemsAddAll.lastItems);
    assertFalse(list.addAll(List.of("3", "1")));
    listEvents.checkInc(false, false, false, false);
    assertTrue(list.addAll(List.of("3", "1", "2", "4")));
    listEvents.checkInc(true, false, false, false);
    // clear
    list.clear();
    listEvents.checkInc(false, false, false, true);
    assertIterableEquals(List.of("4", "3", "2", "1"), listEvents.onItemsClear.lastItems);
    list.clear();
    listEvents.checkInc(false, false, false, false);
  }

  @Test
  public void testListSynchronization()
  {
    final var list = new SynchronizableSortedDistinctList<String>();
    final var listEvents = new ListEvents<>(list);
    final var onItemAdd = listEvents.onItemAdd;
    final var onItemRemove = listEvents.onItemRemove;
    final var onItemAddAll = listEvents.onItemsAddAll;
    // init
    list.setOnItemAdd(onItemAdd);
    list.setOnItemRemove(onItemRemove);
    assertIterableEquals(List.of(), list);
    listEvents.checkCounter(0, 0, 0, 0);
    final SortedSet<String> sortedSet = new TreeSet<>();
    // sync
    list.synchronizeTo(sortedSet);
    assertIterableEquals(List.of(), list);
    listEvents.checkCounter(0, 0, 0, 0);
    // sync
    sortedSet.addAll(List.of("3", "1", "2", "1"));
    list.synchronizeTo(sortedSet);
    assertIterableEquals(List.of("1", "2", "3"), list);
    listEvents.checkCounter(0, 0, 1, 0);
    assertIterableEquals(List.of("1", "2", "3"), onItemAddAll.lastItems);
    // sync
    sortedSet.add("4");
    sortedSet.remove("2");
    list.synchronizeTo(sortedSet);
    assertIterableEquals(List.of("1", "3", "4"), list);
    assertEquals(2, onItemAdd.lastIndex);
    assertEquals("4", onItemAdd.lastItem);
    assertEquals(1, onItemRemove.lastIndex);
    assertEquals("2", onItemRemove.lastItem);
    listEvents.checkCounter(1, 1, 1, 0);
    // sync
    sortedSet.clear();
    list.synchronizeTo(sortedSet);
    assertTrue(list.isEmpty());
    listEvents.checkCounter(1, 1, 1, 1);
  }

  @Test
  void testSubList()
  {
    final var list = new SynchronizableSortedDistinctList<>(List.of("c", "i", "a", "g", "e"));
    assertEquals(List.of("a", "c", "e", "g", "i"), list);
    final var subList = list.subList(1, 4);
    assertEquals(List.of("c", "e", "g"), subList);
    final var ex = UnsupportedOperationException.class;
    assertThrows(ex, () -> subList.add(1, "f"));
    assertThrows(ex, () -> subList.add("f"));
    assertThrows(ex, () -> subList.set(1, "f"));
    subList.remove(1);
    assertEquals(List.of("c", "g"), subList);
    assertEquals(List.of("a", "c", "g", "i"), list);
    subList.clear();
    assertEquals(List.of(), subList);
    assertEquals(List.of("a", "i"), list);
  }
}
