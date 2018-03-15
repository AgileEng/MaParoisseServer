/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.11.2009 15:20:25
 */
package eu.agileeng.persistent.imp;

import java.util.Date;

import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistentUtil;


/**
 *
 */
@SuppressWarnings("serial")
public class AEPersistentImp implements AEPersistent {

	private long id;
	
	private String creator;
	
	private Date timeCreated;
	
	private String modifier;
	
	private Date timeModified;
	
	private AEPersistent.State persistentState;
	
	public AEPersistentImp() {
		this.id = AEPersistent.ID.NA.getID();
		this.persistentState = AEPersistent.State.NEW;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#getCreator()
	 */
	@Override
	public String getCreator() {
		return this.creator;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#getID()
	 */
	@Override
	public long getID() {
		return this.id;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#getModifier()
	 */
	@Override
	public String getModifier() {
		return this.modifier;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#getTimeCreated()
	 */
	@Override
	public Date getTimeCreated() {
		return this.timeCreated;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#getTimeModified()
	 */
	@Override
	public Date getTimeModified() {
		return this.timeModified;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#isNew()
	 */
	@Override
	public boolean isNew() {
		return this.persistentState == AEPersistent.State.NEW;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#isUpdated()
	 */
	@Override
	public boolean isUpdated() {
		return this.persistentState == AEPersistent.State.UPDATED;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#isView()
	 */
	@Override
	public boolean isView() {
		return this.persistentState == AEPersistent.State.VIEW;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setCreator(java.lang.String)
	 */
	@Override
	public void setCreator(String creator) {
		this.creator = creator;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setID(long)
	 */
	@Override
	public void setID(long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setModifier(java.lang.String)
	 */
	@Override
	public void setModifier(String modifier) {
		this.modifier = modifier;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setTimeCreated(java.util.Date)
	 */
	@Override
	public void setTimeCreated(Date timeCreated) {
		this.timeCreated = timeCreated;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setTimeModified(java.util.Date)
	 */
	@Override
	public void setTimeModified(Date timeModified) {
		this.timeModified = timeModified;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setUpdated()
	 */
	@Override
	public void setUpdated() {
		if(isView()) {
			this.persistentState  = AEPersistent.State.UPDATED;
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setView()
	 */
	@Override
	public void setView() {
		this.persistentState  = AEPersistent.State.VIEW;
	}

	@Override
	public boolean isPersistent() {
		return AEPersistent.ID.isPersistent(getID());
	}

	/**
	 * @return the persistentState
	 */
	public AEPersistent.State getPersistentState() {
		return persistentState;
	}

	@Override
	public void setPersistentState(State persistentState) {
		this.persistentState = persistentState;
	}

	@Override
	public void resetAsNew() {
		this.id = AEPersistentUtil.getTmpID();
		this.creator = null;
		this.timeCreated = null;
		this.modifier = null;
		this.timeModified = null;
		this.persistentState = AEPersistent.State.NEW;
	}

	@Override
	public AEPersistent withTmpId() {
		setID(AEPersistentUtil.getTmpID());
		return this;
	}
}
