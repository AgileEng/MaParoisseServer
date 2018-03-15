package eu.agileeng.persistent.dao.document.contractdeTravail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.business.ContractDeTravailReason;
import eu.agileeng.domain.business.EmploymentClassification;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.Person.NationalityType;
import eu.agileeng.domain.contact.Person.SalutationType;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.domain.document.social.AESocialDocumentFilter;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravail;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailType;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class MBContratDeTravailDAO extends AEDocumentDAO {

	public MBContratDeTravailDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
		// TODO Auto-generated constructor stub
	}

	private static String selectSQLContratDeTravail = 
		"select ct.*, u.UserName from [ContratTravail] ct inner join [Users] u on ct.User_ID = u.User_ID "
		+ " where ct.Company_ID = ? order by ct.AtEmbauche_ID asc";
	
	private static String selectSQLAttestation = 
		"select ct.*, u.UserName from [AtEmbauche2] ct inner join [Users] u on ct.User_ID = u.User_ID "
		+ " where ct.Company_ID = ? order by ct.AtEmbauche_ID asc"; 

	public AEDocumentsList loadContratDeTravail(AEDocumentFilter filter) throws AEException {
		AESocialDocumentFilter socialFilter = (AESocialDocumentFilter) filter;
		AEDocumentsList docsList = new AEDocumentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = selectSQLContratDeTravail;
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, socialFilter.getCompany().getDescriptor().getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				ContractDeTravail doc = new ContractDeTravail();
				buildContratDeTravail(doc, rs);
				docsList.add(doc);
			}
			return docsList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public void buildContratDeTravail(ContractDeTravail doc, ResultSet rs) throws SQLException, AEException {
		Employee empl = new Employee();
		empl.setValidateUIN(true);
		
		Address address = new Address(Address.Type.BUSINESS);
		empl.setAddress(address);
		
		Contact contact = new Contact(Contact.Type.BUSINESS);
		empl.setContact(contact);
		
		doc.setEmployee(empl);
		
		String strDate = null;
		
		/**
		 * 
		 */
		
		//[AtEmbauche_ID]
		doc.setID(rs.getLong("AtEmbauche_ID"));
		AEDescriptor srcDescr = new AEDescriptorImp();
		srcDescr.setID(doc.getID());
		srcDescr.setCode("ContratDeTravail");
		doc.setJournal(srcDescr);
		
		//[DateOfInsert]
		Date date = rs.getDate("DateOfInsert");
		doc.setDate(date);
		doc.setRegDate(date);
		
		doc.setTimeCreated(rs.getTimestamp("DateOfInsert"));
		doc.setTimeModified(rs.getTimestamp("DateOfInsert"));
		
		//[User_ID]
		doc.setCreator(rs.getString("UserName"));
		doc.setModifier(rs.getString("UserName"));
		
		//[Company_ID] don't read will be set after
		
		//[ParentCompany_ID] don't read
		
		//[Matricule] don't read
		
		//[AtEmbauche_Number]
		String strNumber = rs.getString("AtEmbauche_Number");
		try {
			doc.setNumber(Long.parseLong(strNumber));
			doc.setRegNumber(Long.parseLong(strNumber));
		} catch(Exception e) {
			
		}
		doc.setNumberString(strNumber);
		doc.setRegNumberString(strNumber);
		
		//[S_Title]
		String title = rs.getString("S_Title");
		if("Monsieur".equalsIgnoreCase(title)) {
			empl.setSalutation(SalutationType.Mr);
		} else if("Madame".equalsIgnoreCase(title)) {
			empl.setSalutation(SalutationType.Mrs);
		} else if("Mademoiselle".equalsIgnoreCase(title)) {
			empl.setSalutation(SalutationType.Ms);
		}
		//[S_FirstName]
		empl.setFirstName(rs.getString("S_FirstName"));
		//[S_MaidenName]
		empl.setGirlName(rs.getString("S_MaidenName"));
		//[S_LastName]
		empl.setLastName(rs.getString("S_LastName"));
		//[S_Address]
		address.setStreet(rs.getString("S_Address"));
		//[S_PostalCode]
		address.setPostalCode(rs.getString("S_PostalCode"));
		//[S_City]
		address.setCity(rs.getString("S_City"));
		//[S_PhoneNumber]
		contact.setPhone(rs.getString("S_PhoneNumber"));
		//[S_DateOfBirth]
		strDate = rs.getString("S_DateOfBirth");
		if(!rs.wasNull()) {
			empl.setDateOfBirth(AEDateUtil.parseDateStrict(strDate));
		}
		//[S_CityOfBirth]
		empl.setPlaceOfBirth(rs.getString("S_CityOfBirth"));
		//[S_handicape] ???
		//[S_NumberSS]
		String uin = rs.getString("S_NumberSS");
		if(AEStringUtil.isEmpty(uin)) {
			empl.setHasUIN(false);
		}
		empl.setUIN(uin);
		//[S_Nationality]
		empl.setNationality(rs.getString("S_Nationality"));
		//[S_Francais]
		int natType = rs.getInt("S_Francais");
		if(natType == 1) {
			empl.setNationalityType(NationalityType.French);
		} else if(natType == 2) {
			empl.setNationalityType(NationalityType.Europian);
		} else {
			empl.setNationalityType(NationalityType.Other);
		}
		//[S_Student]
		doc.setStudent(rs.getInt("S_Student") != 0);
		//[S_StudentCS]
		doc.setWorkDuringVacation(rs.getInt("S_StudentCS") != 0);
		//[S_Residant]
		int rDocType = rs.getInt("S_Residant");
		if(!rs.wasNull()) {
			if(rDocType == 1) {
				empl.setDocType(AEDocumentType.valueOf(AEDocumentType.System.ResidenceCardPerm));
			} else {
				empl.setDocType(AEDocumentType.valueOf(AEDocumentType.System.ResidenceCardTmp));
			}
		}
		//[S_CSNumero]
		empl.setDocNumber(rs.getString("S_CSNumero"));
		//[S_DateFinValide]
		strDate = rs.getString("S_DateFinValide");
		if(!rs.wasNull()) {
			empl.setDocDateOfExpiry(AEDateUtil.parseDateStrict(strDate));
		}
		//[S_Delivre]
		empl.setDocIssuedByString(rs.getString("S_Delivre"));
		//[S_TxtFull]
		empl.setLeTexte(rs.getInt("S_TxtFull") != 0);
		//[S_Anciennete]
		try {
			doc.setProfessionDuration(rs.getDouble("S_Anciennete")); //FIXME
		} catch(Exception e) {
			
		}
		//[S_NEntreprise]
		doc.setFormerEmployee(rs.getInt("S_NEntreprise") != 0);
		//[E_DateEmbauche]
		Date dateEmbauche = null;
		strDate = rs.getString("E_DateEmbauche");
		if(!rs.wasNull()) {
			dateEmbauche = AEDateUtil.parseDateStrict(strDate);
		}
		//[E_HeureEmbauche]
		Date timeEmbauche = rs.getTimestamp("E_HeureEmbauche");
		
		Date dateOfEntry = AEDateUtil.createDateTime(dateEmbauche, timeEmbauche);
		doc.setDateOfEntry(dateOfEntry);
		//[E_Due]
		doc.setDeclarationDUE(rs.getInt("E_Due") != 0);
		//[E_DueFaitLe]
		strDate = rs.getString("E_DueFaitLe");
		if(!rs.wasNull()) {
			doc.setDeclarationDUEDate(AEDateUtil.parseDateStrict(strDate));
		}
		//[E_DueReference]
		doc.setDeclarationDUENumber(rs.getString("E_DueReference"));
		//[E_Emploi]
		doc.setEmployment(rs.getString("E_Emploi"));
		//[E_Qualification]
		doc.setQualification(rs.getString("E_Qualification"));
		//[E_Classification]
		int eClassification = rs.getInt("E_Classification");
		switch (eClassification) {
			case 1: {
				doc.setEmploymentClassification(
						new EmploymentClassification(
								EmploymentClassification.System.valueOf(EmploymentClassification.System.EMPLOYEE.getSystemID())));
				break;
			}
			case 2: {
				doc.setEmploymentClassification(
						new EmploymentClassification(
								EmploymentClassification.System.valueOf(EmploymentClassification.System.WORKER.getSystemID())));
				break;
			}
			case 3: {
				doc.setEmploymentClassification(
						new EmploymentClassification(
								EmploymentClassification.System.valueOf(EmploymentClassification.System.FOREMAN.getSystemID())));
				break;
			}
			case 4: {
				doc.setEmploymentClassification(
						new EmploymentClassification(
								EmploymentClassification.System.valueOf(EmploymentClassification.System.CADRE.getSystemID())));
				break;
			}
		}
		//[E_Coefficient]
		String eCoefficient = rs.getString("E_Coefficient");
		try {
			doc.setCoefficient(Double.parseDouble(eCoefficient));
		} catch (Exception e) {
			
		}
		//[E_Niveau]
		doc.setLevel(rs.getString("E_Niveau"));
		//[E_Echelon]
		doc.setEchelon(rs.getString("E_Echelon"));
		//[E_Position]
		doc.setPosition(rs.getString("E_Position"));
		//[E_Indice]
		doc.setIndex(rs.getString("E_Indice"));
		//[E_Travail_Nuit]
		doc.setNightWork(rs.getInt("E_Travail_Nuit") != 0);
		//[TC_Type]
		//[TC_MotifCDD]
		int tcType = rs.getInt("TC_Type");
		int tcMotifCDD = rs.getInt("TC_MotifCDD");
		if(tcType == 0) {
			doc.setSubType(new ContractDeTravailType(ContractDeTravailType.System.PERMANENT));
		} else {
			if(tcMotifCDD == 0) {
				// replacement
				doc.setSubType(new ContractDeTravailType(ContractDeTravailType.System.TEMPORARY_REPLACEMENT));
			} else if(tcMotifCDD == 1) {
				// seasonal
				doc.setSubType(new ContractDeTravailType(ContractDeTravailType.System.TEMPORARY_SEASONAL));
			} else {
				// peak
				doc.setSubType(new ContractDeTravailType(ContractDeTravailType.System.TEMPORARY_PEAK));
			}
		}
		//[TC_Horaire]
		doc.setFullTime(rs.getInt("TC_Horaire") != 0);
		//[TC_Aide] FIXME
		
		//[TC_MotifCDD_Du]
		strDate = rs.getString("TC_MotifCDD_Du");
		if(!rs.wasNull()) {
			doc.setAppointmentFrom(AEDateUtil.parseDateStrict(strDate));
		}
		//[TC_MotifCDD_Au]
		strDate = rs.getString("TC_MotifCDD_Au");
		if(!rs.wasNull()) {
			doc.setAppointmentTo(AEDateUtil.parseDateStrict(strDate));
		}
		//[TC_MotifSurcroit]
		doc.setDescription(rs.getString("TC_MotifSurcroit"));
		//[TC_MotifCDD_NP]
		AEDescriptor absentEmpl = new AEDescriptorImp();
		absentEmpl.setName(rs.getString("TC_MotifCDD_NP"));
		doc.setAbsentEmployee(absentEmpl);
		//[TC_MotifCDD_Qualif]
		doc.setAbsentQualification(rs.getString("TC_MotifCDD_Qualif"));
		//[TC_MotifCDD_Echelon]
		doc.setAbsentEchelon(rs.getString("TC_MotifCDD_Echelon"));
		//[TC_MotifCDD_MAbsc]
		String absReason = rs.getString("TC_MotifCDD_MAbsc");
		if(!rs.wasNull()) {
			doc.setReason(new ContractDeTravailReason(ContractDeTravailReason.System.valueOfDescr(absReason)));
		}
		//[TC_MotifCDD_MAbsc_Autre]
		doc.setDescription(rs.getString("TC_MotifCDD_MAbsc_Autre"));
		
		//[TC_MotifCDD_DMin]
		int appointmentPeriod = rs.getInt("TC_MotifCDD_DMin");
		if(!rs.wasNull()) {
			doc.setAppointmentPeriod(appointmentPeriod);
		}
		
		//[TC_NombreHeures]
		try {
			doc.setHoursPerWeek(rs.getDouble("TC_NombreHeures"));
		} catch(Exception e) {
			
		}
		//[TC_TypeCDDRempl]
		doc.setAppointmentFixed(rs.getInt("TC_TypeCDDRempl") == 0);
		
		//[TC_TypeCDDRempl_Du]
		strDate = rs.getString("TC_TypeCDDRempl_Du");
		if(!rs.wasNull()) {
			doc.setAppointmentFrom(AEDateUtil.parseDateStrict(strDate));
		}
		//[TC_TypeCDDRempl_Au]
		strDate = rs.getString("TC_TypeCDDRempl_Au");
		if(!rs.wasNull()) {
			doc.setAppointmentTo(AEDateUtil.parseDateStrict(strDate));
		}
		
		//[TC_Mutuelle]
		doc.setMutuelle(rs.getInt("TC_Mutuelle") != 0);
		
		//[RB_Salaire] //FIXME
		
		//[RB_RemBrute]
		String grossAmount = rs.getString("RB_RemBrute");
		try {
			doc.setGrossAmount(Double.parseDouble(grossAmount));
		} catch (Exception e) {
			
		}
		
		//[RB_Essais]
		try {
			doc.setTrialPeriod(rs.getDouble("RB_Essais"));
		} catch(Exception e) {
			
		}
		//[RB_EssaisMJ] //fixme
		
		//[RB_Renouvelable]
		doc.setRenewable(rs.getInt("RB_Renouvelable") != 0);

		//[C_CFConfEmpl]
		AEDescriptor verifiedByEmpl = Employee.lazyDescriptor(-1);
		verifiedByEmpl.setName(rs.getString("C_CFConfEmpl"));
		doc.setVerifiedBy(verifiedByEmpl);

		//[C_DateE]
		strDate = rs.getString("C_DateE");
		if(!rs.wasNull()) {
			doc.setValidatedTime(AEDateUtil.parseDateStrict(strDate));
		}
		//[Active]
		//[RIB_B]
		//[RIB_D]
		//[RIB_E]
		//[RIB_G]
		//[RIB_C]
		//[RIB_Cl]
		//[TC_CDITPNA_NJours_Tr]

		// set this record in view state
		doc.setView();
	}
	
	public AEDocumentsList loadAttestation2(AEDocumentFilter filter) throws AEException {
		AESocialDocumentFilter socialFilter = (AESocialDocumentFilter) filter;
		AEDocumentsList docsList = new AEDocumentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = selectSQLAttestation;
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, socialFilter.getCompany().getDescriptor().getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				ContractDeTravail doc = new ContractDeTravail();
				buildAttestation2(doc, rs);
				docsList.add(doc);
			}
			return docsList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public void buildAttestation2(ContractDeTravail doc, ResultSet rs) throws SQLException, AEException {
		Employee empl = new Employee();
		empl.setValidateUIN(true);
		
		Address address = new Address(Address.Type.BUSINESS);
		empl.setAddress(address);
		
		Contact contact = new Contact(Contact.Type.BUSINESS);
		empl.setContact(contact);
		
		doc.setEmployee(empl);
		
		String strDate = null;
		
		/**
		 * 
		 */
		
		//[AtEmbauche_ID]
		doc.setID(rs.getLong("AtEmbauche_ID"));
		AEDescriptor srcDescr = new AEDescriptorImp();
		srcDescr.setID(doc.getID());
		srcDescr.setCode("Attestation2");
		doc.setJournal(srcDescr);
		
		//[DateOfInsert]
		Date date = rs.getDate("DateOfInsert");
		doc.setDate(date);
		doc.setRegDate(date);
		
		doc.setTimeCreated(rs.getTimestamp("DateOfInsert"));
		doc.setTimeModified(rs.getTimestamp("DateOfInsert"));
		
		//[User_ID]
		doc.setCreator(rs.getString("UserName"));
		doc.setModifier(rs.getString("UserName"));
		
		//[Company_ID] don't read will be set after
		
		//[AtEmbauche_Number]
		String strNumber = rs.getString("AtEmbauche_Number");
		try {
			doc.setNumber(Long.parseLong(strNumber));
			doc.setRegNumber(Long.parseLong(strNumber));
		} catch(Exception e) {
			
		}
		doc.setNumberString(strNumber);
		doc.setRegNumberString(strNumber);
		
		//[S_Title]
		String title = rs.getString("S_Title");
		if("Monsieur".equalsIgnoreCase(title)) {
			empl.setSalutation(SalutationType.Mr);
		} else if("Madame".equalsIgnoreCase(title)) {
			empl.setSalutation(SalutationType.Mrs);
		} else if("Mademoiselle".equalsIgnoreCase(title)) {
			empl.setSalutation(SalutationType.Ms);
		}
		//[S_FirstName]
		empl.setFirstName(rs.getString("S_FirstName"));
		//[S_MaidenName]
		empl.setGirlName(rs.getString("S_MaidenName"));
		//[S_LastName]
		empl.setLastName(rs.getString("S_LastName"));
		//[S_Address]
		address.setStreet(rs.getString("S_Address"));
		//[S_PostalCode]
		address.setPostalCode(rs.getString("S_PostalCode"));
		//[S_City]
		address.setCity(rs.getString("S_City"));
		//[S_PhoneNumber]
		contact.setPhone(rs.getString("S_PhoneNumber"));
		//[S_DateOfBirth]
		strDate = rs.getString("S_DateOfBirth");
		if(!rs.wasNull()) {
			empl.setDateOfBirth(AEDateUtil.parseDateStrict(strDate));
		}
		//[S_CityOfBirth]
		empl.setPlaceOfBirth(rs.getString("S_CityOfBirth"));
		//[S_handicape] ???
		//[S_NumberSS]
		String uin = rs.getString("S_NumberSS");
		if(AEStringUtil.isEmpty(uin)) {
			empl.setHasUIN(false);
		}
		empl.setUIN(uin);
		//[S_Nationality]
		empl.setNationality(rs.getString("S_Nationality"));
		//[S_Francais]
		int natType = rs.getInt("S_Francais");
		if(natType == 1) {
			empl.setNationalityType(NationalityType.French);
		} else if(natType == 2) {
			empl.setNationalityType(NationalityType.Europian);
		} else {
			empl.setNationalityType(NationalityType.Other);
		}
		//[S_Student]
		doc.setStudent(rs.getInt("S_Student") != 0);
		
		//[S_StudentCS]
//		doc.setWorkDuringVacation(rs.getInt("S_StudentCS") != 0);
		
		//[S_Residant]
		int rDocType = rs.getInt("S_Residant");
		if(!rs.wasNull()) {
			if(rDocType == 1) {
				empl.setDocType(AEDocumentType.valueOf(AEDocumentType.System.ResidenceCardPerm));
			} else {
				empl.setDocType(AEDocumentType.valueOf(AEDocumentType.System.ResidenceCardTmp));
			}
		}
		//[S_CSNumero]
		empl.setDocNumber(rs.getString("S_CSNumero"));
		//[S_DateFinValide]
		strDate = rs.getString("S_DateFinValide");
		if(!rs.wasNull()) {
			empl.setDocDateOfExpiry(AEDateUtil.parseDateStrict(strDate));
		}
		//[S_Delivre]
		empl.setDocIssuedByString(rs.getString("S_Delivre"));
		//[S_TxtFull]
		empl.setLeTexte(rs.getInt("S_TxtFull") != 0);
		//[S_Anciennete]
		try {
			doc.setProfessionDuration(rs.getDouble("S_Anciennete")); //FIXME
		} catch(Exception e) {
			
		}
		//[S_NEntreprise]
		doc.setFormerEmployee(rs.getInt("S_NEntreprise") != 0);
		//[E_DateEmbauche]
		Date dateEmbauche = null;
		strDate = rs.getString("E_DateEmbauche");
		if(!rs.wasNull()) {
			dateEmbauche = AEDateUtil.parseDateStrict(strDate);
		}
		//[E_HeureEmbauche]
		Date timeEmbauche = rs.getTimestamp("E_HeureEmbauche");
		
		Date dateOfEntry = AEDateUtil.createDateTime(dateEmbauche, timeEmbauche);
		doc.setDateOfEntry(dateOfEntry);
		//[E_Due]
		doc.setDeclarationDUE(rs.getInt("E_Due") != 0);
		//[E_DueFaitLe]
		strDate = rs.getString("E_DueFaitLe");
		if(!rs.wasNull()) {
			doc.setDeclarationDUEDate(AEDateUtil.parseDateStrict(strDate));
		}
		//[E_DueReference]
		doc.setDeclarationDUENumber(rs.getString("E_DueReference"));
		//[E_Emploi]
		doc.setEmployment(rs.getString("E_Emploi"));
		//[E_Qualification]
//		doc.setQualification(rs.getString("E_Qualification"));
		//[E_Classification]
		int eClassification = rs.getInt("E_Classification");
		switch (eClassification) {
			case 1: {
				doc.setEmploymentClassification(
						new EmploymentClassification(
								EmploymentClassification.System.valueOf(EmploymentClassification.System.EMPLOYEE.getSystemID())));
				break;
			}
			case 2: {
				doc.setEmploymentClassification(
						new EmploymentClassification(
								EmploymentClassification.System.valueOf(EmploymentClassification.System.WORKER.getSystemID())));
				break;
			}
			case 3: {
				doc.setEmploymentClassification(
						new EmploymentClassification(
								EmploymentClassification.System.valueOf(EmploymentClassification.System.FOREMAN.getSystemID())));
				break;
			}
			case 4: {
				doc.setEmploymentClassification(
						new EmploymentClassification(
								EmploymentClassification.System.valueOf(EmploymentClassification.System.CADRE.getSystemID())));
				break;
			}
		}
		//[E_Coefficient]
		String eCoefficient = rs.getString("E_Coefficient");
		try {
			doc.setCoefficient(Double.parseDouble(eCoefficient));
		} catch (Exception e) {
			
		}
		//[E_Niveau]
		doc.setLevel(rs.getString("E_Niveau"));
		//[E_Echelon]
		doc.setEchelon(rs.getString("E_Echelon"));
		//[E_Position]
		doc.setPosition(rs.getString("E_Position"));
		//[E_Indice]
		doc.setIndex(rs.getString("E_Indice"));
		//[E_Travail_Nuit]
		doc.setNightWork(rs.getInt("E_Travail_Nuit") != 0);
		//[TC_Type]
		//[TC_MotifCDD]
		int tcType = rs.getInt("TC_Type");
		int tcMotifCDD = rs.getInt("TC_MotifCDD");
		if(tcType == 0) {
			doc.setSubType(new ContractDeTravailType(ContractDeTravailType.System.PERMANENT));
		} else {
			if(tcMotifCDD == 0) {
				// replacement
				doc.setSubType(new ContractDeTravailType(ContractDeTravailType.System.TEMPORARY_REPLACEMENT));
			} else if(tcMotifCDD == 1) {
				// seasonal
				doc.setSubType(new ContractDeTravailType(ContractDeTravailType.System.TEMPORARY_SEASONAL));
			} else {
				// peak
				doc.setSubType(new ContractDeTravailType(ContractDeTravailType.System.TEMPORARY_PEAK));
			}
		}
		//[TC_Horaire]
		doc.setFullTime(rs.getInt("TC_Horaire") != 0);
		//[TC_Aide] FIXME
		
		//[TC_MotifCDD_Du]
		strDate = rs.getString("TC_MotifCDD_Du");
		if(!rs.wasNull()) {
			doc.setAppointmentFrom(AEDateUtil.parseDateStrict(strDate));
		}
		//[TC_MotifCDD_Au]
		strDate = rs.getString("TC_MotifCDD_Au");
		if(!rs.wasNull()) {
			doc.setAppointmentTo(AEDateUtil.parseDateStrict(strDate));
		}
		//[TC_MotifSurcroit]
		doc.setDescription(rs.getString("TC_MotifSurcroit"));
		//[TC_MotifCDD_NP]
		AEDescriptor absentEmpl = new AEDescriptorImp();
		absentEmpl.setName(rs.getString("TC_MotifCDD_NP"));
		doc.setAbsentEmployee(absentEmpl);
		//[TC_MotifCDD_Qualif]
		doc.setAbsentQualification(rs.getString("TC_MotifCDD_Qualif"));

		//[TC_MotifCDD_Echelon]
//		doc.setAbsentEchelon(rs.getString("TC_MotifCDD_Echelon"));
		
		//[TC_MotifCDD_MAbsc]
		String absReason = rs.getString("TC_MotifCDD_MAbsc");
		if(!rs.wasNull()) {
			doc.setReason(new ContractDeTravailReason(ContractDeTravailReason.System.valueOfDescr(absReason)));
		}
		
		//[TC_MotifCDD_DMin]
		int appointmentPeriod = rs.getInt("TC_MotifCDD_DMin");
		if(!rs.wasNull()) {
			doc.setAppointmentPeriod(appointmentPeriod);	
		}
 		
		//[TC_NombreHeures]
		try {
			doc.setHoursPerWeek(rs.getDouble("TC_NombreHeures"));
		} catch(Exception e) {
			
		}
		//[TC_TypeCDDRempl]
		doc.setAppointmentFixed(rs.getInt("TC_TypeCDDRempl") == 0);
		
		//[TC_TypeCDDRempl_Du]
		strDate = rs.getString("TC_TypeCDDRempl_Du");
		if(!rs.wasNull()) {
			doc.setAppointmentFrom(AEDateUtil.parseDateStrict(strDate));
		}
		//[TC_TypeCDDRempl_Au]
		strDate = rs.getString("TC_TypeCDDRempl_Au");
		if(!rs.wasNull()) {
			doc.setAppointmentTo(AEDateUtil.parseDateStrict(strDate));
		}

		//[TC_Mutuelle]
//		doc.setMutuelle(rs.getInt("TC_Mutuelle") != 0);

		//[RB_Salaire] //FIXME
		
		//[RB_RemBrute]
		String grossAmount = rs.getString("RB_RemBrute");
		try {
			doc.setGrossAmount(Double.parseDouble(grossAmount));
		} catch (Exception e) {
			
		}
		//[RB_Essais]
		try {
			doc.setTrialPeriod(rs.getDouble("RB_Essais"));
		} catch(Exception e) {
			
		}
		
		//[RB_EssaisMJ] //fixme
		
		//[RB_Renouvelable]
		doc.setRenewable(rs.getInt("RB_Renouvelable") != 0);
		
		//[C_CFConfEmpl]
		AEDescriptor verifiedByEmpl = Employee.lazyDescriptor(-1);
		verifiedByEmpl.setName(rs.getString("C_CFConfEmpl"));
		doc.setVerifiedBy(verifiedByEmpl);
		
		//[C_DateE]
		strDate = rs.getString("C_DateE");
		if(!rs.wasNull()) {
			doc.setValidatedTime(AEDateUtil.parseDateStrict(strDate));
		}
		//[Active]
		//[RIB_B]
		//[RIB_D]
		//[RIB_E]
		//[RIB_G]
		//[RIB_C]
		//[RIB_Cl]
		//[TC_CDITPNA_NJours_Tr]

		// set this record in view state
		doc.setView();
	}
}
