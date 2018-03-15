/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 12.06.2010 10:38:48
 */
package eu.agileeng.domain;

import java.util.EventListener;
import java.util.EventObject;

/**
 * Specifies the method that a listener of a document change event must implement.
 * An <code>documentChanged</code> is fired when 
 * (the contents of) an document has changed. 
 * This might mean that its attributes have been modified, added, or removed, 
 * and/or that the object itself has been replaced. 
 * How the object has changed can be determined by examining the NamingEvent's old and new bindings. 
 */
public interface AEListener extends EventListener{
	public void modelChanged(EventObject evt);
}
