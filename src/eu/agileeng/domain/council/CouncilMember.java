package eu.agileeng.domain.council;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.util.AEDateUtil;

public class CouncilMember extends AEDomainObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6979687955518858405L;
	
	private long councilId;
	private Date startDate;
	private Date endDate;
	private MemberType typeId;
	private Position positionId;
	private Date entryDate;
	private Date firstElectionDate;
	private Date nextRenewalDate;
	
	private Employee employee;


	public CouncilMember() {
		super(DomainClass.CouncilMember);
		// TODO Auto-generated constructor stub
	}
	
	public static enum MemberType {
		ordinary(10L),
		cure(20L),
		maire(30L),
		maire_annex(40L);
		
		private long id;
		
		MemberType(long id) {
			this.id = id;
		}
		
		public long getId() {
			return this.id;
		}
		
		public static MemberType findById(long id) {
			for (MemberType pg: MemberType.values()) {
				if (pg.getId() == id) return pg;
			}
			
			return null;
		}
		
		static boolean isOfficio(MemberType mt) {
			if (mt.getId() > 10L) return true;
			else return false;
		}
	}
	
	public static enum Position {
		none(10L),
		president(20L),
		tresorier(30L),
		secretaire(40L);
		
		private long id;
		
		Position(long id) {
			this.id = id;
		}
		
		public long getId() {
			return this.id;
		}
		
		public static Position findById(long id) {
			for (Position p: Position.values()) {
				if (p.getId() == id) return p;
			}
			
			return null;
		}
	}
	
	

	public long getCouncilId() {
		return councilId;
	}

	public void setCouncilId(long councilId) {
		this.councilId = councilId;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Position getPositionId() {
		return positionId;
	}

	public void setPositionId(Position positionId) {
		this.positionId = positionId;
	}
	
	public MemberType getTypeId() {
		return typeId;
	}

	public void setTypeId(MemberType typeId) {
		this.typeId = typeId;
	}

	public Date getEntryDate() {
		return entryDate;
	}

	public void setEntryDate(Date entryDate) {
		this.entryDate = entryDate;
	}

	public Date getFirstElectionDate() {
		return firstElectionDate;
	}

	public void setFirstElectionDate(Date firstElectionDate) {
		this.firstElectionDate = firstElectionDate;
	}

	public Date getNextRenewalDate() {
		return nextRenewalDate;
	}

	public void setNextRenewalDate(Date nextRenewalDate) {
		this.nextRenewalDate = nextRenewalDate;
	}

	@Override
	public void create(JSONObject json) throws JSONException {
		super.create(json);
		//councilId
		this.setCouncilId(json.optLong("councilId"));
		//employee
		Employee emp = new Employee();
		emp.create(json.getJSONObject("employee"));
		this.setEmployee(emp);
		//start date
		if (json.has("startDate")) {
			this.setStartDate(AEDateUtil.parseDateStrict(json.optString("startDate")));
		}
		//end date
		if (json.has("endDate")) {
			this.setEndDate(AEDateUtil.parseDateStrict(json.optString("endDate")));
		}
		//member type id
		MemberType mt = MemberType.findById(json.getLong("typeId"));
		//if (mt == null) throw new AEException("There is no member type with the ID = "+json.getLong("typeId"));
		this.setTypeId(mt);
		
		//position id
		Position p = Position.findById(json.getLong("positionId"));
		this.setPositionId(p);
		
		//entry date
		if (json.has("entryDate")) {
			this.setEntryDate(AEDateUtil.parseDateStrict(json.optString("entryDate")));
		}
		
		//first election date
		if (json.has("firstElectionDate")) {
			this.setFirstElectionDate(AEDateUtil.parseDateStrict(json.optString("firstElectionDate")));
		}
		
		//next renewal date
		if (json.has("nextRenewalDate")) {
			this.setNextRenewalDate(AEDateUtil.parseDateStrict(json.optString("nextRenewalDate")));
		}
		
		
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		
		
		json.put("councilId", this.getCouncilId());
		if(this.getEmployee() != null) {
			json.put("employee", this.getEmployee().toJSONObject());
		}
		if (this.getStartDate() != null) json.put("startDate", sdf.format(this.getStartDate()));
		if (this.getEndDate() != null) json.put("endDate", sdf.format(this.getEndDate()));
		json.put("typeId", this.getTypeId().getId());
		json.put("positionId", this.getPositionId().getId());
		if (this.getEntryDate() != null) json.put("entryDate", sdf.format(this.getEntryDate()));
		if (this.getFirstElectionDate() != null) json.put("firstElectionDate", sdf.format(this.getFirstElectionDate()));
		if (this.getNextRenewalDate() != null) json.put("nextRenewalDate", sdf.format(this.getNextRenewalDate()));
		
		json.put("guiGroupId", (MemberType.isOfficio(this.getTypeId()) ? 20L : 10L));
		
		return json;
		
	}

}
