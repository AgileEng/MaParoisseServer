/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 26.11.2009 16:50:02
 */
package eu.agileeng.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>Description:
 * 
 * An implementation of <code>Map</code> that wraps another <code>Map</code>
 * and flags itself 'dirty' when it is modified.
 */
@SuppressWarnings({ "serial", "unchecked", "rawtypes" })
public class AEDirtyFlagMap implements Map, Cloneable, java.io.Serializable {

    private boolean dirty = false;

    private Map map;

    /**
     * Create a DirtyFlagMap that 'wraps' the given <code>Map</code>.
     */
    public AEDirtyFlagMap(Map mapToWrap) {
        if (mapToWrap == null) {
            throw new IllegalArgumentException("mapToWrap cannot be null!");
        }
        map = mapToWrap;
    }

    /**
     * Create a DirtyFlagMap that 'wraps' a <code>HashMap</code>.
     * 
     * @see java.util.HashMap
     */
    public AEDirtyFlagMap() {
        map = new HashMap();
    }

    /**
     * Create a DirtyFlagMap that 'wraps' a <code>HashMap</code> that has the
     * given initial capacity.
     * 
     * @see java.util.HashMap
     */
    public AEDirtyFlagMap(int initialCapacity) {
        map = new HashMap(initialCapacity);
    }

    /**
     * Create a DirtyFlagMap that 'wraps' a <code>HashMap</code> that has the
     * given initial capacity and load factor.
     * 
     * @see java.util.HashMap
     */
    public AEDirtyFlagMap(int initialCapacity, float loadFactor) {
        map = new HashMap(initialCapacity, loadFactor);
    }

    /**
     * Clear the 'dirty' flag (set dirty flag to <code>false</code>).
     */
    public void clearDirtyFlag() {
        dirty = false;
    }

    /**
     * Determine whether the <code>Map</code> is flagged dirty.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Get a direct handle to the underlying Map.
     */
    public Map getWrappedMap() {
        return map;
    }

    public void clear() {
        dirty = true;
        map.clear();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object val) {
        return map.containsValue(val);
    }

    public Set entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AEDirtyFlagMap))
            return false;

        return map.equals(((AEDirtyFlagMap) obj).getWrappedMap());
    }

    public Object get(Object key) {
        return map.get(key);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set keySet() {
        return map.keySet();
    }

    public Object put(Object key, Object val) {
        dirty = true;

        return map.put(key, val);
    }

    public void putAll(Map t) {
        if (!t.isEmpty())
            dirty = true;

        map.putAll(t);
    }

    public Object remove(Object key) {
        Object obj = map.remove(key);

        if (obj != null)
            dirty = true;

        return obj;
    }

    public int size() {
        return map.size();
    }

	public Collection values() {
        return map.values();
    }

    public Object clone() {
        AEDirtyFlagMap copy;
        try {
            copy = (AEDirtyFlagMap) super.clone();
            if (map instanceof HashMap) {
                copy.map = (Map) ((HashMap) map).clone();
            }
        } catch (CloneNotSupportedException ex) {
            throw new IncompatibleClassChangeError("Not Cloneable.");
        }
        return copy;
    }
}