/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 26.11.2009 16:50:02
 */
package eu.agileeng.util;

import java.io.Serializable;
import java.util.*;

/**
 * Description:
 * <p>
 * Holds a Flexible Dynamic Properties.
 * <p>
 * Fixed properties begin to fail when you have a large amount of them, 
 * or you need to change them frequently, possibly at <b>run time</b>. 
 * These forces lead you to the varieties of Dynamic Property. All dynamic properties 
 * have the quality of a parameterized attribute where to query a property you need to
 * use a query method with a parameter. The simplest of these is the Flexible Dynamic Property 
 * where the parameter is just a string. 
 * This makes it easy to define and use properties, <b>but is difficult to control</b>.
 */
@SuppressWarnings({"serial", "unchecked", "rawtypes"})
public class AEDynamicProperties extends AEDirtyFlagMap implements Serializable {

    private boolean allowsTransientData = false;

    /**
     * <p>
     * Create an empty <code>AEDynamicProperties</code>.
     * </p>
     */
    public AEDynamicProperties() {
        super(15);
    }

    /**
     * <p>
     * Create a <code>AEDynamicProperties</code> with the given data.
     * </p>
     */
	public AEDynamicProperties(Map map) {
        this();
        putAll(map);
    }

    /**
     * <p>
     * Tell the <code>AEDynamicProperties</code> that it should allow non- <code>Serializable</code>
     * data.
     * </p>
     * 
     * <p>
     * If the <code>AEDynamicProperties</code> does contain non- <code>Serializable</code>
     * objects, and it belongs to a non-volatile <code>Job</code> that is
     * stored in a <code>JobStore</code> that supports persistence, then
     * those elements will be nulled-out during persistence.
     * </p>
     */
    public void setAllowsTransientData(boolean allowsTransientData) {

        if (containsTransientData() && !allowsTransientData)
            throw new IllegalStateException("Cannot set property 'allowsTransientData' to 'false' "
                    + "when data map contains non-serializable objects.");

        this.allowsTransientData = allowsTransientData;
    }

    public boolean getAllowsTransientData() {
        return allowsTransientData;
    }

    public boolean containsTransientData() {

        if (!getAllowsTransientData()) // short circuit...
            return false;

        String[] keys = getKeys();

        for (int i = 0; i < keys.length; i++) {
            Object o = super.get(keys[i]);
            if (!(o instanceof Serializable))
                return true;
        }

        return false;
    }

    /**
     * <p>
     * Nulls-out any data values that are non-Serializable.
     * </p>
     */
    public void removeTransientData() {

        if (!getAllowsTransientData()) // short circuit...
            return;

        String[] keys = getKeys();

        for (int i = 0; i < keys.length; i++) {
            Object o = super.get(keys[i]);
            if (!(o instanceof Serializable))
                remove(keys[i]);
        }

    }

    /**
     * <p>
     * Adds the name-value pairs in the given <code>Map</code> to the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * <p>
     * All keys must be <code>String</code>s, and all values must be <code>Serializable</code>.
     * </p>
     */
    public void putAll(Map map) {
        Iterator itr = map.keySet().iterator();
        while (itr.hasNext()) {
            Object key = itr.next();
            Object val = map.get(key);

            put(key, val);
            // will throw IllegalArgumentException if value not serilizable
        }
    }

    /**
     * <p>
     * Adds the given <code>int</code> value to the <code>Job</code>'s
     * data map.
     * </p>
     */
    public void put(String key, int value) {
        super.put(key, new Integer(value));
    }

    /**
     * <p>
     * Adds the given <code>long</code> value to the <code>Job</code>'s
     * data map.
     * </p>
     */
    public void put(String key, long value) {
        super.put(key, new Long(value));
    }

    /**
     * <p>
     * Adds the given <code>float</code> value to the <code>Job</code>'s
     * data map.
     * </p>
     */
    public void put(String key, float value) {
        super.put(key, new Float(value));
    }

    /**
     * <p>
     * Adds the given <code>double</code> value to the <code>Job</code>'s
     * data map.
     * </p>
     */
    public void put(String key, double value) {
        super.put(key, new Double(value));
    }

    /**
     * <p>
     * Adds the given <code>boolean</code> value to the <code>Job</code>'s
     * data map.
     * </p>
     */
    public void put(String key, boolean value) {
        super.put(key, new Boolean(value));
    }

    /**
     * <p>
     * Adds the given <code>char</code> value to the <code>Job</code>'s
     * data map.
     * </p>
     */
    public void put(String key, char value) {
        super.put(key, new Character(value));
    }

    /**
     * <p>
     * Adds the given <code>String</code> value to the <code>Job</code>'s
     * data map.
     * </p>
     */
    public void put(String key, String value) {
        super.put(key, value);
    }

    /**
     * <p>
     * Adds the given <code>boolean</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, boolean value) {
        String strValue = new Boolean(value).toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>Boolean</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, Boolean value) {
        String strValue = value.toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>char</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, char value) {
        String strValue = new Character(value).toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>Character</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, Character value) {
        String strValue = value.toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>double</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, double value) {
        String strValue = new Double(value).toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>Double</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, Double value) {
        String strValue = value.toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>float</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, float value) {
        String strValue = new Float(value).toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>Float</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, Float value) {
        String strValue = value.toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>int</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, int value) {
        String strValue = new Integer(value).toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>Integer</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, Integer value) {
        String strValue = value.toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>long</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, long value) {
        String strValue = new Long(value).toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>Long</code> value as a string version to the
     * <code>Job</code>'s data map.
     * </p>
     */
    public void putAsString(String key, Long value) {
        String strValue = value.toString();

        super.put(key, strValue);
    }

    /**
     * <p>
     * Adds the given <code>Serializable</code> object value to the <code>AEDynamicProperties</code>.
     * </p>
     */
    public Object put(Object key, Object value) {
        if (!(key instanceof String))
            throw new IllegalArgumentException("Keys in map must be Strings.");

        return super.put(key, value);
    }

    /**
     * <p>
     * Retrieve the identified <code>int</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not an Integer.
     */
    public int getInt(String key) {
        Object obj = get(key);

        try {
            return ((Integer) obj).intValue();
        } catch (Exception e) {
            throw new ClassCastException("Identified object is not an Integer.");
        }
    }

    /**
     * <p>
     * Retrieve the identified <code>long</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a Long.
     */
    public long getLong(String key) {
        Object obj = get(key);

        try {
            return ((Long) obj).longValue();
        } catch (Exception e) {
            throw new ClassCastException("Identified object is not a Long.");
        }
    }

    /**
     * <p>
     * Retrieve the identified <code>float</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a Float.
     */
    public float getFloat(String key) {
        Object obj = get(key);

        try {
            return ((Float) obj).floatValue();
        } catch (Exception e) {
            throw new ClassCastException("Identified object is not a Float.");
        }
    }

    /**
     * <p>
     * Retrieve the identified <code>double</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a Double.
     */
    public double getDouble(String key) {
        Object obj = get(key);

        try {
            return ((Double) obj).doubleValue();
        } catch (Exception e) {
            throw new ClassCastException("Identified object is not a Double.");
        }
    }

    /**
     * <p>
     * Retrieve the identified <code>boolean</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a Boolean.
     */
    public boolean getBoolean(String key) {
        Object obj = get(key);

        try {
            return ((Boolean) obj).booleanValue();
        } catch (Exception e) {
            throw new ClassCastException("Identified object is not a Boolean.");
        }
    }

    /**
     * <p>
     * Retrieve the identified <code>char</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a Character.
     */
    public char getChar(String key) {
        Object obj = get(key);

        try {
            return ((Character) obj).charValue();
        } catch (Exception e) {
            throw new ClassCastException("Identified object is not a Character.");
        }
    }

    /**
     * <p>
     * Retrieve the identified <code>String</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public String getString(String key) {
        Object obj = get(key);

        try {
            return (String) obj;
        } catch (Exception e) {
            throw new ClassCastException("Identified object is not a String.");
        }
    }

    /**
     * <p>
     * Retrieve the identified <code>int</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public int getIntFromString(String key) {
        Object obj = get(key);

        return new Integer((String) obj).intValue();
    }

    /**
     * <p>
     * Retrieve the identified <code>int</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public Integer getIntegerFromString(String key) {
        Object obj = get(key);

        return new Integer((String) obj);
    }

    /**
     * <p>
     * Retrieve the identified <code>boolean</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public boolean getBooleanValueFromString(String key) {
        Object obj = get(key);

        return new Boolean((String) obj).booleanValue();
    }

    /**
     * <p>
     * Retrieve the identified <code>Boolean</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public Boolean getBooleanFromString(String key) {
        Object obj = get(key);

        return new Boolean((String) obj);
    }

    /**
     * <p>
     * Retrieve the identified <code>char</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public char getCharFromString(String key) {
        Object obj = get(key);

        return ((String) obj).charAt(0);
    }

    /**
     * <p>
     * Retrieve the identified <code>Character</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public Character getCharacterFromString(String key) {
        Object obj = get(key);

        return new Character(((String) obj).charAt(0));
    }

    /**
     * <p>
     * Retrieve the identified <code>double</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public double getDoubleValueFromString(String key) {
        Object obj = get(key);

        return new Double((String) obj).doubleValue();
    }

    /**
     * <p>
     * Retrieve the identified <code>Double</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public Double getDoubleFromString(String key) {
        Object obj = get(key);

        return new Double((String) obj);
    }

    /**
     * <p>
     * Retrieve the identified <code>float</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public float getFloatValueFromString(String key) {
        Object obj = get(key);

        return new Float((String) obj).floatValue();
    }

    /**
     * <p>
     * Retrieve the identified <code>Float</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public Float getFloatFromString(String key) {
        Object obj = get(key);

        return new Float((String) obj);
    }

    /**
     * <p>
     * Retrieve the identified <code>long</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public long getLongValueFromString(String key) {
        Object obj = get(key);

        return new Long((String) obj).longValue();
    }

    /**
     * <p>
     * Retrieve the identified <code>Long</code> value from the <code>AEDynamicProperties</code>.
     * </p>
     * 
     * @throws ClassCastException
     *           if the identified object is not a String.
     */
    public Long getLongFromString(String key) {
        Object obj = get(key);

        return new Long((String) obj);
    }

    public String[] getKeys() {
        return (String[]) keySet().toArray(new String[size()]);
    }

}
