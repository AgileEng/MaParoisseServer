package eu.agileeng.util;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

/**
 * @author Vesko Vatov
 *
 */
@SuppressWarnings("serial")
public class StringAlign extends Format {
	
	public static enum Align {
		LEFT,
		CENTER,
		RIGHT;
	}

	/** Current justification */
	private Align just;
	
	/** Current max length */
	private int maxChars;

	/** Construct a StringAlign formatter; length and alignment are
	 * passed to the Constructor instead of each format() call as the
	 * expected common use is in repetitive formatting e.g., page numbers.
	 * @param nChars - the length of the output
	 * @param just - one of LEFT, CENTER or RIGHT
	 */
	public StringAlign(int maxChars, Align just) {
		this.just = just;
		if (maxChars < 0) {
			throw new IllegalArgumentException("maxChars must be positive.");
		}
		this.maxChars = maxChars;
	}

	/** Format a String.
	 * @param input _ the string to be aligned.
	 * @parm where - the StringBuilder to append it to.
	 * @param ignore - a FieldPosition (may be null, not used but
	 * specified by the general contract of Format).
	 */
	public StringBuilder format(Object obj, StringBuilder where, FieldPosition ignore)  {
		String s = (String) obj;
		String wanted = s.substring(0, Math.min(s.length(), maxChars));

		// Get the spaces in the right place.
		switch (just) {
			case RIGHT:
				pad(where, maxChars - wanted.length());
				where.append(wanted);
				break;
			case CENTER:
				int toAdd = maxChars - wanted.length();
				pad(where, toAdd/2);
				where.append(wanted);
				pad(where, toAdd - toAdd/2);
				break;
			case LEFT:
				where.append(wanted);
				pad(where, maxChars - wanted.length());
				break;
		}
		return where;
	}

	protected final void pad(StringBuilder to, int howMany) {
		for (int i=0; i < howMany; i++) {
			to.append(' ');
		}
	}

	/** 
	 * Convenience Routine 
	 */
	String format(String s) {
		return format(s, new StringBuilder(), null).toString();
	}

	/** 
	 * ParseObject is required, but not useful here. 
	 */
	public Object parseObject (String source, ParsePosition pos)  {
		return source;
	}

	@Override
	public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
		return new StringBuffer(format(obj, new StringBuilder(toAppendTo), pos));
	}
}
