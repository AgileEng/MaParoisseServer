/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.06.2010 14:26:13
 */
package eu.agileeng.domain.measurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 * A unit of measurement is a definite magnitude of a physical quantity, 
 * defined and adopted by convention and/or by law, that is used as a 
 * standard for measurement of the same physical quantity.
 *  Any other value of the physical quantity can be expressed as a 
 *  simple multiple of the unit of measurement.
 *  
 *  For example, length is a physical quantity. 
 *  The metre is a unit of length that represents a definite predetermined length. 
 *  When we say 10 metres (or 10 m), 
 *  we actually mean 10 times the definite predetermined length called "metre".
 */
@SuppressWarnings("serial")
public class UnitOfMeasurement extends AEDomainObject {

	/**
	 * property TIME
	 */
	public static final UnitOfMeasurement hour = new UnitOfMeasurement("Hour", "Hour", 100);
	public static final UnitOfMeasurement day = new UnitOfMeasurement("Day", "Day", 110);
	public static final UnitOfMeasurement week = new UnitOfMeasurement("Week", "Week", 3);
	public static final UnitOfMeasurement month = new UnitOfMeasurement("Month", "Month", 130);
	private static final List<UnitOfMeasurement> timeUnits = new ArrayList<UnitOfMeasurement>();
	
	/**
	 * property CURRENCY
	 */
	public static final UnitOfMeasurement euro = new UnitOfMeasurement("Euro", "Euro", 5);
	private static final List<UnitOfMeasurement> currencyUnits = new ArrayList<UnitOfMeasurement>();
	
	/**
	 * property FACTURATION
	 */
//	public static final UnitOfMeasurement piece = new UnitOfMeasurement("Piece", "Piece", 200);
	public static final UnitOfMeasurement heure = new UnitOfMeasurement("heure", "heure", 205);
	public static final UnitOfMeasurement kg = new UnitOfMeasurement("kg", "kg", 210);
	public static final UnitOfMeasurement tonne = new UnitOfMeasurement("tonne", "tonne", 211);
	public static final UnitOfMeasurement litre = new UnitOfMeasurement("litre", "litre", 215);
	public static final UnitOfMeasurement metre2 = new UnitOfMeasurement("sq mètre", "sq mètre", 225);
	public static final UnitOfMeasurement metre = new UnitOfMeasurement("mètre", "mètre", 220);
	public static final UnitOfMeasurement metre3 = new UnitOfMeasurement("cu mètre", "cu mètre", 226);
	public static final UnitOfMeasurement piece2 = new UnitOfMeasurement("pièce", "pièce", 230);
	private static final List<UnitOfMeasurement> facturationUnits = new ArrayList<UnitOfMeasurement>();
	
	private static final Map<Property, List<UnitOfMeasurement>> unitsMap = new HashMap<Property, List<UnitOfMeasurement>>(); 
	static {
		// TIME
		timeUnits.add(hour);
		timeUnits.add(day);
		timeUnits.add(week);
		timeUnits.add(month);
		
		// CURRENCY
		currencyUnits.add(euro);
		
		// FACTURATION
		facturationUnits.add(piece2);
		facturationUnits.add(heure);
		facturationUnits.add(metre);
		facturationUnits.add(metre2);
		facturationUnits.add(kg);
		facturationUnits.add(tonne);
		facturationUnits.add(litre);
		facturationUnits.add(metre3);
		
		// ALL
		unitsMap.put(Property.TIME, timeUnits);
		unitsMap.put(Property.CURRENCY, currencyUnits);
		unitsMap.put(Property.FACTURATION, facturationUnits);
	}
	
	static public enum Property {
		NA(0L),
		USER_DEFINED(5),
		TIME(10L),
		CURRENCY(20l),
		FACTURATION(30l);
		
		private long propertyID;
		
		private Property(long propertyID) {
			this.propertyID = propertyID;
		}
		
		public final long getPropertyID() {
			return this.propertyID;
		}
		
		public static Property valueOf(long propertyID) {
			Property ret = null;
			for (Property inst : Property.values()) {
				if(inst.getPropertyID() == propertyID) {
					ret = inst;
					break;
				}
			}
			if(ret == null) {
				ret = NA;
			}
			return ret;
		}
	}
	
	/**
	 * @param clazz
	 */
	public UnitOfMeasurement() {
		super(DomainClass.UnitOfMeasurement);
	}
	
	public UnitOfMeasurement(String name, String code, long id) {
		this();
		setName(name);
		setCode(code);
		setID(id);
	}
	
	public UnitOfMeasurement(long id) {
		this();
		setID(id);
	}
	
	public static List<UnitOfMeasurement> getByProperty(UnitOfMeasurement.Property property) {
		return unitsMap.get(property);
	}
	
	public static List<UnitOfMeasurement> getMonthAndDay(UnitOfMeasurement.Property property) {
		List<UnitOfMeasurement> list = new ArrayList<UnitOfMeasurement>();
		list.add(day);
		list.add(month);
		return Collections.unmodifiableList(list);
	}
	
	public static List<UnitOfMeasurement> getMonthAndHour(UnitOfMeasurement.Property property) {
		List<UnitOfMeasurement> list = new ArrayList<UnitOfMeasurement>();
		list.add(hour);
		list.add(month);
		return Collections.unmodifiableList(list);
	}
	
	public static List<UnitOfMeasurement> getFacturationUnits() {
		return Collections.unmodifiableList(facturationUnits);
	}
	
	public static UnitOfMeasurement getByID(long id) {
		UnitOfMeasurement found = null;
		Property[] properties = Property.values();
		end:
		for (int i = 0; i < properties.length; i++) {
			Property prop = properties[i];
			List<UnitOfMeasurement> list = unitsMap.get(prop);
			if(list != null && !list.isEmpty()) {
				for (UnitOfMeasurement unitOfMeasurement : list) {
					if(unitOfMeasurement.getID() == id) {
						found = unitOfMeasurement;
						break end;
					}
				}
			}
		}
		return found;
	}
	
	public static JSONArray toJSONArray(List<UnitOfMeasurement> uoms) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		if(uoms != null) {
			for (UnitOfMeasurement uom : uoms) {
				jsonArray.put(uom.toJSONObject());
			}
		}
		return jsonArray;
	}
}
