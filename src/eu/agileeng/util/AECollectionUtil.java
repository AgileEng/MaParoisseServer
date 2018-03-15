/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 25.11.2009 12:01:59
 */
package eu.agileeng.util;

import java.util.*;

public class AECollectionUtil {

	private AECollectionUtil() {
	}

	public static <E> Set<E> asSet(E... elements) {
		if (elements == null || elements.length == 0) {
			return Collections.emptySet();
		}
		LinkedHashSet<E> set = new LinkedHashSet<E>(elements.length * 4 / 3 + 1);
		Collections.addAll(set, elements);
		return set;
	}

	/**
	 * Returns {@code true} if the specified {@code Collection} is {@code null} or {@link Collection#isEmpty empty},
	 * {@code false} otherwise.
	 *
	 * @param c the collection to check
	 * @return {@code true} if the specified {@code Collection} is {@code null} or {@link Collection#isEmpty empty},
	 *         {@code false} otherwise.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isEmpty(Collection c) {
		return c == null || c.isEmpty();
	}

	/**
	 * Returns {@code true} if the specified {@code Map} is {@code null} or {@link Map#isEmpty empty},
	 * {@code false} otherwise.
	 *
	 * @param m the {@code Map} to check
	 * @return {@code true} if the specified {@code Map} is {@code null} or {@link Map#isEmpty empty},
	 *         {@code false} otherwise.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isEmpty(Map m) {
		return m == null || m.isEmpty();
	}

	/**
	 * Returns the size of the specified collection or {@code 0} if the collection is {@code null}.
	 *
	 * @param c the collection to check
	 * @return the size of the specified collection or {@code 0} if the collection is {@code null}.
	 */
	@SuppressWarnings("rawtypes")
	public static int size(Collection c) {
		return c != null ? c.size() : 0;
	}

	/**
	 * Returns the size of the specified map or {@code 0} if the map is {@code null}.
	 *
	 * @param m the map to check
	 * @return the size of the specified map or {@code 0} if the map is {@code null}.
	 */
	@SuppressWarnings("rawtypes")
	public static int size(Map m) {
		return m != null ? m.size() : 0;
	}

	public static <E> List<E> asList(E... elements) {
		if (elements == null || elements.length == 0) {
			return Collections.emptyList();
		}
		// Avoid integer overflow when a large array is passed in
		int capacity = computeListCapacity(elements.length);
		ArrayList<E> list = new ArrayList<E>(capacity);
		Collections.addAll(list, elements);
		return list;
	}

	static int computeListCapacity(int arraySize) {
		return (int) Math.min(5L + arraySize + (arraySize / 10), Integer.MAX_VALUE);
	}
	
	 public static <K, V> Map<K, List<V>> index(Iterator<V> values, Function<? super V, K> keyFunction) {
	      Map<K, List<V>> map = new HashMap<K, List<V>>();
	      while (values.hasNext()) {
	          V value = values.next();
	          K key = keyFunction.apply(value);
	          List<V> list = map.get(key);
	          if(list == null) {
	              list = new ArrayList<V>();
	              map.put(key, list);
	          }
	          list.add(value);
	      }
	      return map;
	  }
}
