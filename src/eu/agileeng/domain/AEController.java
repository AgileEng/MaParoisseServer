/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 12.06.2010 10:35:51
 */
package eu.agileeng.domain;

import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public abstract class AEController {

	private Object model;
	
	private Set<AEListener> listeners = new HashSet<AEListener>();
	
	/**
	 * 
	 */
	protected AEController(Object model) {
		assert(model != null);
		this.model = model;
	}

	/**
	 * @return the model
	 */
	public final Object getModel() {
		assert(this.model != null);
		return this.model;
	}
	
	public final boolean addListener(AEListener l) {
		synchronized (this.listeners) {
			return this.listeners.add(l);
		}
	}
	
	public final boolean removeListener(AEListener l) {
		synchronized (this.listeners) {
			return this.listeners.remove(l);
		}
	}
	
	public final void clear() {
		synchronized (this.listeners) {
			this.listeners.clear();
			this.model = null;
		}
	}
	
	public final void fireEvent(EventObject e) {
		AEListener[] listeners;
		synchronized (this.listeners) {
			listeners = (AEListener[]) this.listeners.toArray();
		}
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].modelChanged(e);
		}
	}
}
