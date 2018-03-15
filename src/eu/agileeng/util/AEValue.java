package eu.agileeng.util;

import java.util.Date;

import eu.agileeng.domain.AEException;

/*
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: NABLA</p>
 * @author Vesko Vatov
 * @version 1.0
 * Created on 13.04.2011
 */

public class AEValue {

	public static enum XType {
		NULL,
		BOOLEAN,
		INTEGER,
		LONG,
		DOUBLE,
		STRING,
		DATE
	}

	/**
	 * The object where the value kept.
	 */
	private Object value = null;


	/**
	 * Constructs a <code>null value</code> AEValue.
	 */
	public AEValue() {
	}
	
	public AEValue(double value) {
		this.value = value;
	}

	public AEValue(long value) {
		this.value = value;
	}
	
	public AEValue(Date value) {
		this.value = value;
	}
	
	public AEValue(Boolean value) {
		this.value = value;
	}
	
	public AEValue(String value) {
		this.value = value;
	}
	
	/**
	 * Constructs {@link AEValue} instance 
	 * from specified <code>String</code> and specified <code>XType</code>.  
	 * 
	 * @param value
	 * @param xType
	 * @throws AEException if specified <code>value</code> 
	 *         is <code>null</code> 
	 *         or is empty string 
	 *         or cannot be parsed as specified <code>XType</code>.  
	 */
	public AEValue(String value, XType xType) throws AEException {
		create(value, xType);
	}

	/**
	 * Constructs {@link AEValue} instance 
	 * from specified <code>String</code> and specified <code>xType</code>.  
	 * 
	 * @param value
	 * @param xType
	 * @throws AEException if specified <code>value</code> 
	 *         is <code>null</code> 
	 *         or is empty string 
	 *         or cannot be parsed as specified <code>XType</code>.  
	 */
	public AEValue(String value, String xType) throws AEException {
		if(!AEStringUtil.isEmpty(xType)) {
			create(value, XType.valueOf(xType.trim().toUpperCase()));
		} else {
			this.value = AEStringUtil.EMPTY_STRING;
		}
	}

	private void create(String value, XType xType) throws AEException {
		if(xType != null) {
			try {
				switch (xType) {
				case STRING :
					if(value != null) {
						this.value = value.trim();
					} else {
						this.value = AEStringUtil.EMPTY_STRING;
					}
					break;
				case INTEGER:
					if(value != null) {
						this.value = new Integer(Integer.parseInt(value));
					} else {
						this.value = 0L;
					}
					break;
				case LONG:
					if(value != null) {
						this.value = new Long(Long.parseLong(value));
					} else {
						this.value = 0L;
					}
					break;
				case DOUBLE:
					try {
						if(value != null) {
							this.value = new Double(AEMath.parseDouble(value, true));
						} else {
							this.value = new Double(0.0);
						} 
					} catch(Exception e) {
						this.value = new Double(0.0);
					}
					break;
				case DATE:
					if(value != null) {
						this.value = AEDateUtil.parseDateStrict(value);
					}
					break;
				default:
					// includes XType.NULL
					this.value = AEStringUtil.EMPTY_STRING;
					break;
				};
			} catch (Exception e) {
				this.value = AEStringUtil.EMPTY_STRING;
			}
		} else {
			this.value = AEStringUtil.EMPTY_STRING;
		}
	}

	/**
	 * Get the boolean value.
	 *
	 * @return      The truth.
	 * @throws      AEException if the value is not a Boolean or the String "true" or "false".
	 */
	public boolean getBoolean() throws AEException {
		if (this.value.equals(Boolean.FALSE) 
				|| (this.value instanceof String && ((String)this.value).equalsIgnoreCase("false"))) {

			return false;
		} else if (this.value.equals(Boolean.TRUE) 
				|| (this.value instanceof String && ((String)this.value).equalsIgnoreCase("true"))) {

			return true;
		}
		throw new AEException("AEValue is not a Boolean.");
	}


	/**
	 * Get the double value.
	 * 
	 * @return	The double value.
	 * @throws	AEException if the value is not a Number object and cannot be converted to a double.
	 */
	public double getDouble() throws AEException {
		try {
			return this.value instanceof Number ?
					((Number) this.value).doubleValue() :
						AEMath.parseDouble((String) this.value, true);
		} catch (Exception e) {
			throw new AEException("AEValue is not a number.");
		}
	}

	/**
	 * Get the double value.
	 * 
	 * @return	The double value.
	 * @throws	AEException if the value is not a Number object and cannot be converted to a double.
	 */
	public double optDouble() {
		double ret = 0.0;
		try {
			ret = this.value instanceof Number ?
					((Number) this.value).doubleValue() :
					AEMath.parseDouble((String) this.value, false);
			if(Double.isNaN(ret) || Double.isInfinite(ret)) {
				ret = 0.0;
			}
		} catch (Throwable t) {
		}
		return ret;
	}
	
	/**
	 * Get the int value. 
	 *
	 * @return   The integer value.
	 * @throws   AEException if the value cannot be converted to an integer.
	 */
	public int getInt() throws AEException {
		try {
			return this.value instanceof Number ?
					((Number) this.value).intValue() :
					Integer.parseInt((String) this.value);
		} catch (Exception e) {
			throw new AEException("AEValue is not an integer.");
		}
	}

	/**
	 * Get the long value. 
	 *
	 * @return  The long value.
	 * @throws  AEException if the value cannot be converted to a long.
	 */
	public long getLong() throws AEException {
		try {
			return this.value instanceof Number ?
					((Number) this.value).longValue() :
					Long.parseLong((String) this.value);
		} catch (Exception e) {
			throw new AEException("AEValue is not a long.");
		}
	}
	
	/**
	 * Get the long value. 
	 *
	 * @return  The long value.
	 */
	public long optLong() {
		try {
			return this.value instanceof Number ?
					((Number) this.value).longValue() :
					Long.parseLong((String) this.value);
		} catch (Throwable t) {
			return 0L;
		}
	}

	/**
	 * Returns a string representation of this value. 
	 * In general, the getString method returns a string that "textually represents" this object.  
	 *
	 * @return A string representation of this value. 
	 */
	public String getString() {
		return toString();
	}

	/**
	 * Determine if the value is null.
	 * @return true if the valueObject is null or valueObject contains null value.
	 */
	public static boolean isNull(AEValue valueObject) {
		if(valueObject != null) {
			return valueObject.value == null;
		} else {
			return true;
		}
	}

	/**
	 * Set a boolean value.
	 *
	 * @param value A boolean which is the value.
	 */
	public void set(boolean value) {
		this.value = new Boolean(value);
	}

	/**
	 * Set a double value.
	 *
	 * @param value A double which is the value.
	 */
	public void set(double value) {
		if(Double.isNaN(value)) {
			this.value = new Double(0.0);
		} else {
			this.value = new Double(value);
		}
	}

	/**
	 * Set an integer value.
	 *
	 * @param value An integer which is the value.
	 */
	public void set(int value) {
		this.value = new Integer(value);
	}

	/**
	 * Set a long value.
	 *
	 * @param value A long which is the value.
	 */
	public void set(long value) {
		this.value = new Long(value);
	}

	/**
	 * Set a atring value.
	 *
	 * @param value A string which is the value.
	 */
	public void set(String value) {
		this.value = value;
	}

	/**
	 * Throw an exception if the object is a NaN or infinite number.
	 * @param value The object to test.
	 * @throws AEException If o is a non-finite number.
	 */
	public static void testValidity(Object value) throws AEException {
		if (value != null) {
			if (value instanceof Double) {
				if (((Double)value).isInfinite() || ((Double)value).isNaN()) {
					throw new AEException(
							"The AEValue does not allow non-finite numbers.");
				}
			} else if (value instanceof Float) {
				if (((Float)value).isInfinite() || ((Float)value).isNaN()) {
					throw new AEException(
							"AEValue does not allow non-finite numbers.");
				}
			}
		}
	}

	public String toString() {
		String strReps = null;
		if(this.value != null) {
			if(this.value instanceof Date) {
				strReps = AEDateUtil.convertToString((Date) this.value, AEDateUtil.SYSTEM_DATE_FORMAT);
			} else if(this.value instanceof Double) {
				try {
					double dVal = getDouble();
					if(Double.isNaN(dVal) || Double.isInfinite(dVal)) {
						strReps = "0.0";
					} else {
						strReps = AEMath.toAmountString(getDouble());
					}
				} catch (AEException e) {}
			} else {
				strReps = this.value.toString();
			}
		}
		return strReps;
	}

	public static void main(String[] args) {
		AEValue value = new AEValue();

		value.set(25);
		try {
			assert(value.getInt() == 25);
		} catch (AEException e) {
			e.printStackTrace();
		}
		assert(value.toString().equals("25"));

		value.set(25.6);
		try {
			assert(value.getDouble() == 25.6);
		} catch (AEException e) {
			e.printStackTrace();
		}
		assert(value.toString().equals("25.6"));

		double d = 200.655342;
		String strD = Double.toString(d);
		try {
			AEValue d1 = new AEValue(strD, "double");
			assert(d == d1.getDouble());
		} catch (AEException e) {
			e.printStackTrace();
		}
	}

	public AEValue.XType getXType() {
		AEValue.XType xType = AEValue.XType.NULL;
		if(value instanceof Boolean) {
			xType = AEValue.XType.BOOLEAN;
		} else if(value instanceof Integer) {
			xType = AEValue.XType.INTEGER;
		} else if(value instanceof Long) {
			xType = AEValue.XType.LONG;
		} else if(value instanceof Double) {
			xType = AEValue.XType.DOUBLE;
		}  else if(value instanceof String) {
			xType = AEValue.XType.STRING;
		}  else if(value instanceof Date) {
			xType = AEValue.XType.DATE;
		} 
		return xType;
	}
}