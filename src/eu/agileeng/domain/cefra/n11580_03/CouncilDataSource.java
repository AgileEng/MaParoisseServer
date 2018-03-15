package eu.agileeng.domain.cefra.n11580_03;

import java.util.Date;

import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.council.Council;
import eu.agileeng.domain.council.CouncilMember;
import eu.agileeng.domain.council.CouncilMembersList;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class CouncilDataSource extends ReportDataSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1787487640177796628L;

	private Date date;

	private Council council;

	public CouncilDataSource() {

	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @param date
	 *            the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * @return the council
	 */
	public Council getCouncil() {
		return council;
	}

	/**
	 * @param council
	 *            the council to set
	 */
	public void setCouncil(Council council) {
		this.council = council;
	}
	
	public CouncilMember getCure() {
		CouncilMember cure = null;
		CouncilMembersList members = council.getMembers();
		
		if(members != null){
			for(CouncilMember member : members){
				if(member.getTypeId() == CouncilMember.MemberType.cure){
					cure = member;
				}
			}
		}
		return cure;
	}
	
	public CouncilMember getMaire() {
		CouncilMember maire = null;
		CouncilMembersList members = council.getMembers();
		
		if(members != null){
			for(CouncilMember member : members){
				if(member.getTypeId() == CouncilMember.MemberType.maire){
					maire = member;
				}
			}
		}
		return maire;
	}
	
	public CouncilMember getMaireAnnexe() {
		CouncilMember maireAnnexe = null;
		CouncilMembersList members = council.getMembers();
		
		if(members != null){
			for(CouncilMember member : members){
				if(member.getTypeId() == CouncilMember.MemberType.maire_annex){
					maireAnnexe = member;
				}
			}
		}
		return maireAnnexe;
	}
	
	public CouncilMember getPresident() {
		CouncilMember president = null;
		CouncilMembersList members = council.getMembers();
		
		if(members != null){
			for(CouncilMember member : members){
				if(member.getPositionId() == CouncilMember.Position.president){
					president = member;
				}
			}
		}
		return president;
	}
	
	public CouncilMember getTresorie() {
		CouncilMember tresorie = null;
		CouncilMembersList members = council.getMembers();
		
		if(members != null){
			for(CouncilMember member : members){
				if(member.getPositionId() == CouncilMember.Position.tresorier){
					tresorie = member;
				}
			}
		}
		return tresorie;
	}
	
	public CouncilMember getSecretaire() {
		CouncilMember secretaire = null;
		CouncilMembersList members = council.getMembers();
		
		if(members != null){
			for(CouncilMember member : members){
				if(member.getPositionId() == CouncilMember.Position.secretaire){
					secretaire = member;
				}
			}
		}
		return secretaire;
	}
	
	public CouncilMembersList getOrdinaryMembers() {
		CouncilMembersList ordinaryMembers = new CouncilMembersList();
		CouncilMembersList members = council.getMembers();
		if(members != null) {
			for(CouncilMember member : members){
				if(member.getTypeId() == CouncilMember.MemberType.ordinary && member.getPositionId() == CouncilMember.Position.none){
					ordinaryMembers.add(member);
				}
			}
		}
		return ordinaryMembers;
	}
	
	public String getEntryDate(CouncilMember member) {
		String dateAsString = "";
		if(member != null) {
			Date entryDate = member.getEntryDate();
			if(entryDate != null) {
				dateAsString = AEDateUtil.formatToFrench(entryDate);
			}
		}
		return dateAsString;
	}
	
	public String getFirstElectionDate(CouncilMember member) {
		String dateAsString = "";
		if(member != null) {
			Date firstElectionDate = member.getFirstElectionDate();
			if(firstElectionDate != null) {
				dateAsString = AEDateUtil.formatToFrench(firstElectionDate);
			}
		}
		return dateAsString;
	}
	
	public String getNextRenewalDate(CouncilMember member) {
		String dateAsString = "";
		if(member != null) {
			Date nextRenewalDate = member.getNextRenewalDate();
			if(nextRenewalDate != null) {
				dateAsString = AEDateUtil.formatToFrench(nextRenewalDate);
			}
		}
		return dateAsString;
	}
	
	public String getMemberFullName(CouncilMember member) {
		String fullName = "";
		if(member != null) {
			Employee empl = member.getEmployee();
			if(empl != null) {
				fullName = AEStringUtil.trim(empl.getName());
			}
		}
		return fullName;
	}
	
	public String getMemberEmail(CouncilMember member) {
		String email = "";		
		if(member != null) {
			Employee empl = member.getEmployee();
			if(empl != null) {
				Contact cont = empl.getContact();
				if(cont != null){
					email = AEStringUtil.trim(cont.geteMail());
				}
			}
		}
		return email;
	}
	
	public String getMemberPhone(CouncilMember member) {
		String phone = "";		
		if(member != null) {
			Employee empl = member.getEmployee();
			if(empl != null) {
				Contact cont = empl.getContact();
				if(cont != null){
					phone = AEStringUtil.trim(cont.getPhone());
				}
			}
		}
		return phone;
	}
	
	public String getMemberAddress(CouncilMember member) {
		String address = "";
		if(member != null) {
			Employee empl = member.getEmployee();
			if(empl != null) {
				Address addr = empl.getAddress();
				if(addr != null){
					StringBuilder sb = new StringBuilder("");
					sb.append(addr.getStreet());
					sb.append(", ");
					sb.append(addr.getPostalCode());
					sb.append(" ");
					sb.append(addr.getCity());
					address = AEStringUtil.trim(sb.toString());
				}
			}
		}
		return address;
	}
	
	public String getNextRenewalDate(){
		String dateAsString = "";
		if(council.getEndDate() != null) {
			Date nextRenewalDate = council.getEndDate();
			dateAsString = AEDateUtil.formatToFrench(nextRenewalDate);
		}
		return dateAsString;
	}
}
