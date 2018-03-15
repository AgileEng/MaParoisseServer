/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.11.2009 16:30:46
 */
package eu.agileeng.util;

import java.io.Serializable;

/**
 *
 */
public interface AEPredicate extends Serializable {
	public boolean evaluate(Object o);
}
