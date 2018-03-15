/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 26.10.2009 21:53:29
 */
package eu.agileeng.persistent;

import java.io.Serializable;
import java.util.Date;

/**
 * Every persistence-capable class must implement 
 * the AEPersistent interface or be a subclass of a class that implements it.
 */
public interface AEPersistent extends Serializable {
	
	static public enum State {
		NEW,
		VIEW,
		UPDATED,
		DELETED
	}
	
	static public enum ID {
		NA(0L);
		
		private long id;
		
		private ID(long id) {
			this.id = id;
		}
		
		public final long getID() {
			return this.id;
		}
		
		public static final boolean isPersistent(long id) {
			return id > 0;
		}
	}
	
	/*
	 * Identification
	 */
	public void setID(long id);
	
	public long getID();
	
	/*
	 * Database related state
	 */
	public boolean isNew();
	
	public void setUpdated();
	
	public boolean isUpdated();
	
	public void setView();
	
	public boolean isView();
	
	public boolean isPersistent();
	
	/*
	 * Temporary information 
	 */
	public void setTimeModified(Date timeModified);
	
	public Date getTimeModified();
	
	public void setModifier(String modifier);
	
	public String getModifier();
	
	public void setTimeCreated(Date timeCreated);
	
	public Date getTimeCreated();
	
	public void setCreator(String creator);
	
	public String getCreator();
	
	public AEPersistent.State getPersistentState();
	
	public void setPersistentState(AEPersistent.State persistentState);
	
	public void resetAsNew();
	
	public AEPersistent withTmpId();
}
