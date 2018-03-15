/**
 * 
 */
package eu.agileeng.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import com.ibm.icu.text.RuleBasedNumberFormat;

import eu.agileeng.domain.AEException;

/**
 * @author vvatov
 *
 */
public class AEMath {

	private static double ZERO_AMOUNT_LIMIT = 0.005;
	
	private static DecimalFormat amountFormat = new DecimalFormat();
	private static DecimalFormat amountFormatComma = new DecimalFormat();
	private static DecimalFormat priceFormat2 = new DecimalFormat();
	private static DecimalFormat priceFormat3 = new DecimalFormat();
	
	static {
		DecimalFormatSymbols ds = new DecimalFormatSymbols();
		ds.setDecimalSeparator('.');
		amountFormat.setDecimalFormatSymbols(ds);
		amountFormat.applyPattern("#0.00");
		
		DecimalFormatSymbols dsComma = new DecimalFormatSymbols();
		dsComma.setDecimalSeparator(',');
		amountFormatComma.setDecimalFormatSymbols(dsComma);
		amountFormatComma.applyPattern("#0.00");
		
		DecimalFormatSymbols sPriceFormat2 = new DecimalFormatSymbols();
		sPriceFormat2.setDecimalSeparator('.');
		priceFormat2.setDecimalFormatSymbols(sPriceFormat2);
		priceFormat2.applyPattern("#0.00");
		
		DecimalFormatSymbols sPriceFormat3 = new DecimalFormatSymbols();
		sPriceFormat3.setDecimalSeparator('.');
		priceFormat3.setDecimalFormatSymbols(sPriceFormat3);
		priceFormat3.applyPattern("#0.000");
	}

    /**
     * Spellout rules for French.  French adds some interesting quirks of its
     * own: 1) The word "et" is interposed between the tens and ones digits,
     * but only if the ones digit if 1: 20 is "vingt," and 2 is "vingt-deux,"
     * but 21 is "vingt-et-un."  2)  There are no words for 70, 80, or 90.
     * "quatre-vingts" ("four twenties") is used for 80, and values proceed
     * by score from 60 to 99 (e.g., 73 is "soixante-treize" ["sixty-thirteen"]).
     * Numbers from 1,100 to 1,199 are rendered as hundreds rather than
     * thousands: 1,100 is "onze cents" ("eleven hundred"), rather than
     * "mille cent" ("one thousand one hundred")
     */
    public static final String french =
        // the main rule set
        "%main:\n"
               // negative-number and fraction rules
        + "    -x: moins >>;\n"
        + "    x.x: << virgule >>;\n"
               // words for numbers from 0 to 10
        + "    z\u00e9ro; un; deux; trois; quatre; cinq; six; sept; huit; neuf;\n"
        + "    dix; onze; douze; treize; quatorze; quinze; seize;\n"
        + "        dix-sept; dix-huit; dix-neuf;\n"
               // ords for the multiples of 10: %%alt-ones inserts "et"
               // when needed
        + "    20: vingt[->%%alt-ones>];\n"
        + "    30: trente[->%%alt-ones>];\n"
        + "    40: quarante[->%%alt-ones>];\n"
        + "    50: cinquante[->%%alt-ones>];\n"
               // rule for 60.  The /20 causes this rule's multiplier to be
               // 20 rather than 10, allowinhg us to recurse for all values
               // from 60 to 79...
        + "    60/20: soixante[->%%alt-ones>];\n"
               // ...except for 71, which must be special-cased
        + "    71: soixante et onze;\n"
               // at 72, we have to repeat the rule for 60 to get us to 79
        + "    72/20: soixante->%%alt-ones>;\n"
               // at 80, we state a new rule with the phrase for 80.  Since
               // it changes form when there's a ones digit, we need a second
               // rule at 81.  This rule also includes "/20," allowing it to
               // be used correctly for all values up to 99
        + "    80: quatre-vingts; 81/20: quatre-vingt->>;\n"
               // "cent" becomes plural when preceded by a multiplier, and
               // the multiplier is omitted from the singular form
        + "    100: cent[ >>];\n"
        + "    200: << cents;\n"
        + "    201: << cent[ >>];\n"
        + "    300: << cents;\n"
        + "    301: << cent[ >>];\n"
        + "    400: << cents;\n"
        + "    401: << cent[ >>];\n"
        + "    500: << cents;\n"
        + "    501: << cent[ >>];\n"
        + "    600: << cents;\n"
        + "    601: << cent[ >>];\n"
        + "    700: << cents;\n"
        + "    701: << cent[ >>];\n"
        + "    800: << cents;\n"
        + "    801: << cent[ >>];\n"
        + "    900: << cents;\n"
        + "    901: << cent[ >>];\n"
        + "    1000: mille[ >>];\n"
               // values from 1,100 to 1,199 are rendered as "onze cents..."
               // instead of "mille cent..."  The > after "1000" decreases
               // the rule's exponent, causing its multiplier to be 100 instead
               // of 1,000.  This prevents us from getting "onze cents cent
               // vingt-deux" ("eleven hundred one hundred twenty-two").
        + "    1100>: onze cents[ >>];\n"
               // at 1,200, we go back to formating in thousands, so we
               // repeat the rule for 1,000
        + "    1200: mille >>;\n"
               // at 2,000, the multiplier is added
        + "    2000: << mille[ >>];\n"
        + "    1,000,000: << million[ >>];\n"
        + "    2,000,000: << millions[ >>];\n"
        + "    1,000,000,000: << milliard[ >>];\n"
        + "    2,000,000,000: << milliards[ >>];\n"
        + "    1,000,000,000,000: << billion[ >>];\n"
        + "    2,000,000,000,000: << billions[ >>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        // %%alt-ones is used to insert "et" when the ones digit is 1
        + "%%alt-ones:\n"
        + "    ; et-un; =%main=;";
	
	// only static methods
	private AEMath() {
	}

	public static double round(double d, int decimalPlace){
		// see the Javadoc about why we use a String in the constructor
		// http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
		BigDecimal bd = new BigDecimal(Double.toString(d));
		bd = bd.setScale(decimalPlace,BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}
	
	public static String toAmountString(double d) {
		String ret = null;
		if(Double.isNaN(d) || Double.isInfinite(d)) {
			ret = Double.toString(Double.NaN);
		} else {
			ret = amountFormat.format(d);
		}
		return ret;
	}
	
	public static String toAmountFrenchString(double d) {
		String ret = null;
		if(Double.isNaN(d) || Double.isInfinite(d)) {
			ret = Double.toString(Double.NaN);
		} else {
			NumberFormat f = NumberFormat.getInstance(Locale.FRENCH);
			if (f instanceof DecimalFormat) {
				// decimal format
				DecimalFormat df = (DecimalFormat) f;				
				df.applyPattern("#,##0.00");
	         }
			ret = f.format(d);
		}
		return ret;
	}
	
	public static String toAmountKiloString(double d) {
		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
		formatSymbols.setDecimalSeparator('.');
		formatSymbols.setGroupingSeparator(',');
		
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(formatSymbols);
		format.applyPattern("#,###");
		
		return format.format(d);
	}
	
	public static String toRateString(double d) {
		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
		formatSymbols.setDecimalSeparator('.');
		formatSymbols.setGroupingSeparator(',');
		
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(formatSymbols);
		format.applyPattern("#,###.0");
		
		String ret = null;
		if(Double.isNaN(d) || Double.isInfinite(d)) {
			ret = Double.toString(Double.NaN);
		} else {
			ret = format.format(d);
		}
		return ret;
	}
	
	public static String toPrice2String(double d) {
		return priceFormat2.format(d);
	}
	
	public static String toPrice3String(double d) {
		return priceFormat3.format(d);
	}
	
	public static double doubleValue(Number n) {
		return n != null ? n.doubleValue() : 0.0;
	}
	
	public static long longValue(Number n) {
		return n != null ? n.longValue() : 0;
	}
	
	public static long getSizeInKB(long bytes) {
		return (long) (bytes / 1024 + 1);
	}
	
	public static boolean isZeroAmount(double amount) {
		return round(Math.abs(amount), 3) < ZERO_AMOUNT_LIMIT;
	}
	
	public static boolean isNegativeAmount(double amount) {
		return amount < 0.0 && !isZeroAmount(amount);
	}
	
	public static boolean isPositiveAmount(double amount) {
		return amount > 0.0 && !isZeroAmount(amount);
	}
	
	public static double parseDouble(String str, boolean strict) throws AEException {
		Number number = parseNumber(str, strict);
		return doubleValue(number);
	}
	
	public static Number parseNumber(String str, boolean strict) throws AEException {
		Number number = null;
		
		if(!AEStringUtil.isEmpty(str)) {
			ParsePosition pp = new ParsePosition(0);
			
			// try to parse with amountFormat
			number = amountFormat.parse(str, pp);
			if(str.length() != pp.getIndex() || number == null) {
				number = null;
			}
			
			if(number == null) {
				// try to parse with amountFormatComma
				pp.setIndex(0);
				number = amountFormatComma.parse(str, pp);
				if(str.length() != pp.getIndex() || number == null) {
					number = null;
				}
			}
			
			if(number == null) {
				// try to parse with french formating
				pp.setIndex(0);
				NumberFormat f = NumberFormat.getInstance(Locale.FRENCH);
				if (f instanceof DecimalFormat) {
					// decimal format
					DecimalFormat df = (DecimalFormat) f;				
					df.applyPattern("#,##0.00");
					number = df.parse(str, pp);
				}
				if(str.length() != pp.getIndex() || number == null) {
					number = null;
				}
			}
			
			if(number == null && strict) {
				throw new AEException("Exception de format Nombre de chaîne d'entrée '" + str + "'");
			}
		}
		
		return number;
	}
	
	/**
	 * Returns specified <code>d</code> as spellout strring (in words).
	 * 
	 * @param d
	 * @return
	 */
	public static String spellout(double d) {
		RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(french, Locale.FRANCE);
		return rbnf.format(d);
	}
	
	public static String spelloutLeftOfTheDecimalPoint(double d) {
		long l = (long) d;
		RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(french, Locale.FRANCE);
		return rbnf.format(l);
	}
	
	public static String spelloutRightOfTheDecimalPoint(double d, int decimalPlace) {
		double rounded = round(d - ((long) d), decimalPlace);
		double f = round(rounded * Math.pow(10, decimalPlace), 0);
		RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(french, Locale.FRANCE);
		return rbnf.format(f);
	}
	
	public static String[] spelloutMoney(double d) {
		String[] retArr = new String[2];
		retArr[0] = spelloutLeftOfTheDecimalPoint(d);
		retArr[1] = spelloutRightOfTheDecimalPoint(d, 2);
		return retArr;
	}
	
	public static boolean isRightOfTheDecimalPointZeroAmounnt(double d) {
		double rounded = round(d - ((long) d), 2);
		return isZeroAmount(rounded);
	}
}
