package eu.agileeng.domain.document.social;

import java.util.Date;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.document.social.accidentdutravail.AccidentDuTravail;
import eu.agileeng.domain.document.social.accidentdutravail.AccidentDuTravail.JSONKey;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravail;
import eu.agileeng.domain.document.social.findutravail.FinDuTravail;
import eu.agileeng.domain.document.social.rib.Rib;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class AESocialDocumentView {

	private JSONObject jsonDocument;
	
	public AESocialDocumentView(JSONObject jsonDocument) {
		this.jsonDocument = jsonDocument;
	}

	public JSONObject getJsonDocument() {
		return jsonDocument;
	}
	
	public final String getTypeDeDeclaration() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AccidentDuTravail.JSONKey.typeDeDeclaration.toString())) {
			str = AEStringUtil.trim(jsonDocument.optString(AccidentDuTravail.JSONKey.typeDeDeclaration.toString()));
		}
		
		return str;
	}
	
	public final int getTypeArret() {
		int i = 0;
		
		if (jsonDocument != null && jsonDocument.has(AccidentDuTravail.JSONKey.typeArret.toString())) {
			i = jsonDocument.optInt(AccidentDuTravail.JSONKey.typeArret.toString());
		}
		
		return i;
	}
	
	public final String getEmployeeLastName() {
		String res = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee)) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee);
			if(jsonEmployee != null && jsonEmployee.has(Person.JSONKey.lastName)) {
				res = AEStringUtil.trim(jsonEmployee.optString(Person.JSONKey.lastName));
			}
		}
		
		return res;
	}
	
	public final String getEmployeeFirstName() {
		String res = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee)) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee);
			if(jsonEmployee != null && jsonEmployee.has(Person.JSONKey.firstName)) {
				res = AEStringUtil.trim(jsonEmployee.optString(Person.JSONKey.firstName));
			}
		}
		
		return res;
	}
	
	public final String getDateDuDernier() {
		String res = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AccidentDuTravail.JSONKey.dateDuDernier.toString())) {
			Date dateDuDernier = AEDateUtil.parseDateStrict(jsonDocument.optString(JSONKey.dateDuDernier.toString()));
			if(dateDuDernier != null) {
				res = AEDateUtil.convertToString(dateDuDernier, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		
		return res;
	}
	
	public final String getDateDeAccident() {
		String res = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AccidentDuTravail.JSONKey.dateDeAccident.toString())) {
			Date dateDeAccident = AEDateUtil.parseDateStrict(jsonDocument.optString(JSONKey.dateDeAccident.toString()));
			if(dateDeAccident != null) {
				res = AEDateUtil.convertToString(dateDeAccident, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		
		return res;
	}
	
	public final boolean getTravailNonRepris() {
		boolean bool = false;
		
		if (jsonDocument != null && jsonDocument.has(AccidentDuTravail.JSONKey.travailNonRepris.toString())) {
			bool = jsonDocument.optBoolean(AccidentDuTravail.JSONKey.travailNonRepris.toString());
		}
		
		return bool;
	}
	
	public final String getDateDeFin() {
		String res = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AccidentDuTravail.JSONKey.dateDeFin.toString())) {
			Date dateDeFin = AEDateUtil.parseDateStrict(jsonDocument.optString(JSONKey.dateDeFin.toString()));
			if(dateDeFin != null) {
				res = AEDateUtil.convertToString(dateDeFin, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		
		return res;
	}
	
	public final String getDateDeReprise() {
		String res = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AccidentDuTravail.JSONKey.dateDeReprise.toString())) {
			Date dateDeReprise = AEDateUtil.parseDateStrict(jsonDocument.optString(JSONKey.dateDeReprise.toString()));
			if(dateDeReprise != null) {
				res = AEDateUtil.convertToString(dateDeReprise, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		
		return res;
	}
	
	public final boolean getMiTemps() {
		boolean bool = false;
		
		if (jsonDocument != null && jsonDocument.has(AccidentDuTravail.JSONKey.miTemps.toString())) {
			bool = jsonDocument.optBoolean(AccidentDuTravail.JSONKey.miTemps.toString());
		}
		
		return bool;
	}
	
	public final boolean getFullTimeAttestation() {
		boolean halfTime = false;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.fullTime.toString())) {
			halfTime = jsonDocument.optBoolean(ContractDeTravail.JSONKey.fullTime.toString());
		}
		
		return halfTime;
	}
	
	/* Fin du Travail == Solde de tout compte */
	public final String getResponsableName() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(FinDuTravail.JSONKey.responsibleName.toString())) {
			str = AEStringUtil.trim(jsonDocument.optString(FinDuTravail.JSONKey.responsibleName.toString()));
		}
		
		return str;
	}
	
	public final String getDateEntry() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(FinDuTravail.JSONKey.dateDeAccident.toString())) {
			Date d = AEDateUtil.parseDateStrict(jsonDocument.optString(FinDuTravail.JSONKey.dateDeAccident.toString()));
			if (d != null) {
				str = AEDateUtil.convertToString(d, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		
		return str;
	}
	
	public final String getReleaseDate() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(FinDuTravail.JSONKey.dateDuDernier.toString())) {
			Date d = AEDateUtil.parseDateStrict(jsonDocument.optString(FinDuTravail.JSONKey.dateDuDernier.toString()));
			if (d != null) {
				str = AEDateUtil.convertToString(d, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		
		return str;
	}
	
	public final boolean isStudent() {
		boolean isStudent = false;
		
		if (jsonDocument != null && jsonDocument.has(FinDuTravail.JSONKey.student.toString())) {
			isStudent = jsonDocument.optBoolean(FinDuTravail.JSONKey.student.toString());
		}
		
		return isStudent;
	}
	
	public final long getMotif() {
		long motif = 0;

		if (jsonDocument != null && jsonDocument.has(FinDuTravail.JSONKey.leavingReason.toString())) {
			motif = jsonDocument.optLong(FinDuTravail.JSONKey.leavingReason.toString());
		}
		
		return motif;
	}
	
	public final String getLeavingReason() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(FinDuTravail.JSONKey.leavingReason.toString())) {
			str = AEStringUtil.trim(jsonDocument.optString(FinDuTravail.JSONKey.leavingReason.toString()));
		}
		
		return str;
	}
	
	public final String getLeavingReasonDesc() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(FinDuTravail.JSONKey.leavingReasonDescription.toString())) {
			str = AEStringUtil.trim(jsonDocument.optString(FinDuTravail.JSONKey.leavingReasonDescription.toString()));
		}
		
		return str;
	}
	
	public final String getDescription() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has("description")) {
			str = AEStringUtil.trim(jsonDocument.optString("description"));
			str = str.replace("\n", "<br />");
		}
		
		return str;
	}
	
	/* CertificatDeTravail (in FinDeTravail.java file) */
	public final String getAddress() {
		String address = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.address.toString())) {
				JSONObject jsonContact = jsonEmployee.optJSONObject(Person.JSONKey.address.toString());
				address = AEStringUtil.trim(jsonContact.optString("address"));
			}
		}
		
		return address;
	}
	
	public final String getPostalCode() {
		String postalCode = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.address.toString())) {
				JSONObject jsonPostalCode = jsonEmployee.optJSONObject(Person.JSONKey.address.toString());
				postalCode = AEStringUtil.trim(jsonPostalCode.optString("postCode"));
			}
		}
		
		return postalCode;
	}
	
	public final String getTown() {
		String town = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.address.toString())) {
				JSONObject jsonPostalCode = jsonEmployee.optJSONObject(Person.JSONKey.address.toString());
				town = AEStringUtil.trim(jsonPostalCode.optString("town"));
			}
		}
		
		return town;
	}
	
	public final String getSocialNumber() {
		String socialNumber = AEStringUtil.EMPTY_STRING;

		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if(jsonEmployee != null) {
				boolean hasIdentityNo = true;
				if(jsonEmployee.has(Person.JSONKey.hasIdentityNo.toString())) {
					hasIdentityNo = jsonEmployee.optBoolean(Person.JSONKey.hasIdentityNo.toString());
				}
				if(hasIdentityNo) {
					if (jsonEmployee.has(Person.JSONKey.identityNo.toString())) {
						socialNumber = jsonEmployee.optString(Person.JSONKey.identityNo.toString());
					}
				} else {
					//"numéro de sécurité sociale en cours"
					socialNumber = "en cours d'immatriculation"; 
				}
			}
		}

		return socialNumber;
	}
	
	public final String getEmployedAs() {
		String employedAs = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has("employedAs")) {
			employedAs = jsonDocument.optString("employedAs");
		}
		
		return employedAs;
	}
	
	public final String getRegDate() {
		String regDate = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.regDate.toString())) {
			Date date = AEDateUtil.parseDateStrict(jsonDocument.optString(AESocialDocument.JSONKey.regDate.toString()));
			if (date != null) {
				regDate = AEDateUtil.convertToString(date, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		
		return regDate;
	}
	
	/* ContractDeTravail */
	public final String getSalutation() {
		long salutationID = 0;
		String salutation = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.salutationID.toString())) {
				salutationID = jsonEmployee.optLong(Person.JSONKey.salutationID.toString());
				switch ((int) salutationID) {
					case 10:
						salutation = "Monsieur";
						break;
					case 20:
						salutation = "Madame";
						break;
					case 30:
						salutation = "Mademoiselle";
						break;
					default:
						salutation = AEStringUtil.EMPTY_STRING;
				}
			}
		}
		
		return salutation;
	}
	
	public final String getSalutationShort() {
		long salutationID = 0;
		String salutation = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.salutationID.toString())) {
				salutationID = jsonEmployee.optLong(Person.JSONKey.salutationID.toString());
				switch ((int) salutationID) {
					case 10:
						salutation = "M.";
						break;
					case 20:
						salutation = "Mme.";
						break;
					case 30:
						salutation = "Melle.";
						break;
					default:
						salutation = AEStringUtil.EMPTY_STRING;
				}
			}
		}
		
		return salutation;
	}
	
	public final boolean isMadam() {
		boolean isMadam = true;
		long salutationID = 0;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.salutationID.toString())) {
				salutationID = jsonEmployee.optLong(Person.JSONKey.salutationID.toString());
				isMadam = salutationID != 10;
			}
		}
		
		return isMadam;
	}
	
	public final String getTelephone() {
		String tel = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.contact.toString())) {
				JSONObject jsonContact = jsonEmployee.optJSONObject(Person.JSONKey.contact.toString());
				if (jsonContact != null && jsonContact.has("phone")) {
					tel = jsonContact.optString("phone");
				}
			}
		}
		
		return tel;
	}
	
	public final String getDateOfBirth() {
		String dateOfBirth = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.dateOfBirth.toString())) {
				Date date = AEDateUtil.parseDateStrict(jsonEmployee.optString(Person.JSONKey.dateOfBirth.toString()));
				if (date != null) {
					dateOfBirth = AEDateUtil.convertToString(date, AEDateUtil.FRENCH_DATE_FORMAT);
				}
			}
		}
		
		return dateOfBirth;
	}
	
	public final String getPlaceOfBirth() {
		String placeOfBirth = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.placeOfBirth.toString())) {
				placeOfBirth = jsonEmployee.optString(Person.JSONKey.placeOfBirth.toString());
			}
		}
		
		return placeOfBirth;
	}
	
	public final boolean isFrench() {
		boolean isFrench = true;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.nationalityTypeID.toString())) {
				isFrench = jsonEmployee.optInt(Person.JSONKey.nationalityTypeID.toString()) == 5 ? true : false;
			}
		}
		
		return isFrench;
	}
	
	public final String getIdentityCardType() {
		String cardType = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.docTypeID.toString())) {
				if (jsonEmployee.optLong(Person.JSONKey.docTypeID.toString()) == 3) {
					cardType = "Carte de résident";
				} else if (jsonEmployee.optLong(Person.JSONKey.docTypeID.toString()) == 4) {
					cardType = "Carte de séjour";
				}
				
			}
		}
		
		return cardType;
	}
	
	public final String getNationality() {
		String nationality = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee.toString())) {
			JSONObject jsonEmployee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee.toString());
			if (jsonEmployee != null && jsonEmployee.has(Person.JSONKey.nationalityStr.toString())) {
				nationality = jsonEmployee.optString(Person.JSONKey.nationalityStr.toString());
			}
		}
		
		return nationality;
	}
	
	public final boolean isWorkingDuringVacation() {
		boolean workingDuringVacation = false;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.workingDuringVacation.toString())) {
			workingDuringVacation = jsonDocument.optBoolean(ContractDeTravail.JSONKey.workingDuringVacation.toString());
		}
		
		return workingDuringVacation;
	}
	
	public final int getProfessionDuration() {
		int professionDuration = 0;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.proffesionDuration.toString())) {
			professionDuration = jsonDocument.optInt(ContractDeTravail.JSONKey.proffesionDuration.toString());
		}
		
		return professionDuration;
	}
	
	public final boolean isFormerEmployee() {
		boolean formerEmployee = false;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.formerEmployee.toString())) {
			formerEmployee = jsonDocument.optBoolean(ContractDeTravail.JSONKey.formerEmployee.toString());
		}
		
		return formerEmployee;
	}
	
	public final String getDateOfEntry() {
		String dateOfEntry = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.dateOfEntry.toString())) {
			Date date = AEDateUtil.parseDateStrict(jsonDocument.optString(ContractDeTravail.JSONKey.dateOfEntry.toString()));
			if (date != null) {
				dateOfEntry = AEDateUtil.convertToString(date, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		
		return dateOfEntry;
	}
	
	public final String getTimeOfEntry() {
		String timeOfEntry = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.timeOfEntry.toString())) {
//			Date time = AEDateUtil.parseTimeStrict(jsonDocument.optString(ContractDeTravail.JSONKey.timeOfEntry));
//			if (time != null) {
//				timeOfEntry = AEDateUtil.convertToString(time, AEDateUtil.FRENCH_DATE_FORMAT);
//			}
			timeOfEntry = jsonDocument.optString(ContractDeTravail.JSONKey.timeOfEntry);
		}
		
		return timeOfEntry;
	}
	
	public final boolean hasDeclarationDUE() {
		boolean hasDeclaration = false;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.declarationDUE.toString())) {
			hasDeclaration = jsonDocument.optBoolean(ContractDeTravail.JSONKey.declarationDUE.toString());
		}
		
		return hasDeclaration;
	}
	
	public final String getDUEDate() {
		String dueDate = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.declarationDUEDate.toString())) {
			Date date = AEDateUtil.parseDateStrict(jsonDocument.optString(ContractDeTravail.JSONKey.declarationDUEDate.toString()));
			if (date != null) {
				dueDate = AEDateUtil.convertToString(date, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		
		return dueDate;
	}
	
	public final String getDUENumber() {
		String dueNumber = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.declarationDUENumber.toString())) {
			dueNumber = jsonDocument.optString(ContractDeTravail.JSONKey.declarationDUENumber.toString());
		}
		
		return dueNumber;
	}
	
	public final String getEmploymentPosition() {
		String emplPosition = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.employment.toString())) {
			emplPosition = jsonDocument.optString(ContractDeTravail.JSONKey.employment.toString());
		}
		
		return emplPosition;
	}
	
	public final String getEmploymentClassification() {
		long emplClassification = 0;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.employmentClassification_ID.toString())) {
			emplClassification = jsonDocument.optLong(ContractDeTravail.JSONKey.employmentClassification_ID.toString());
			switch((int) emplClassification) {
			case 10:
				return "Employé";
			case 20:
				return "Ouvrier";
			case 30:
				return "Agent de  maîtrise";
			case 40:
				return "Cadre";
			default:
				return "";
			}
		}
		return "";
	}
	
	public final int getCoefficient() {
		int coefficient = -1;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.coefficient.toString())) {
			coefficient = jsonDocument.optInt(ContractDeTravail.JSONKey.coefficient.toString());
		}
		
		return coefficient;
	}
	
	public final String getNiveau() {
		String niveau = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.ae_level.toString())) {
			niveau = jsonDocument.optString(ContractDeTravail.JSONKey.ae_level.toString());
		}
		
		return niveau;
	}
	
	public final String getEchelon() {
		String echelon = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.echelon.toString())) {
			echelon = jsonDocument.optString(ContractDeTravail.JSONKey.echelon.toString());
		}
		
		return echelon;
	}
	
	public final String getPosition() {
		String position = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.position.toString())) {
			position = jsonDocument.optString(ContractDeTravail.JSONKey.position.toString());
		}
		
		return position;
	}
	
	public final String getIndice() {
		String indice = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.ae_index.toString())) {
			indice = jsonDocument.optString(ContractDeTravail.JSONKey.ae_index.toString());
		}
		
		return indice;
	}
	
	public final boolean isNightWork() {
		boolean isNightWork = false;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.nightWork.toString())) {
			isNightWork = jsonDocument.optBoolean(ContractDeTravail.JSONKey.nightWork.toString());
		}
		
		return isNightWork;
	}
	
	public final boolean isCDD() {
		boolean isCDD = false;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.subTypeId.toString())) {
			isCDD = jsonDocument.optLong(ContractDeTravail.JSONKey.subTypeId.toString()) == 100 ? false : true;
		}
		
		return isCDD;
	}
	
	//10 for seasonal, 11 for replacement
	public final int getCDDSubType() {
		int cddSubType = 10;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.subTypeId.toString())) {
			cddSubType = jsonDocument.optInt(ContractDeTravail.JSONKey.subTypeId.toString());
		}
		
		return cddSubType;
	}
	
	public final String getAppointmentFrom() {
		String from = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.appointmentFrom.toString())) {
			Date date = AEDateUtil.parseDateStrict(jsonDocument.optString(ContractDeTravail.JSONKey.appointmentFrom.toString()));
			from = AEDateUtil.convertToString(date, AEDateUtil.FRENCH_DATE_FORMAT);
		}
		
		return from;
	}
	
	public final String getAppointmentFromUOM(int duration) {
		String uom = AEStringUtil.EMPTY_STRING;
		long uomID = 0;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.cddReplacementNpDurationUoM.toString())) {
			uomID = jsonDocument.optLong(ContractDeTravail.JSONKey.cddReplacementNpDurationUoM.toString());
			if (uomID == 110 && duration <= 1) {
				uom = "jour";
			} else if (uomID == 110 && duration > 1) {
				uom = "jours";
			} else if (uomID == 130) {
				uom = "mois";
			}
		}
		
		return uom;
	}
	
	public final String getAppointmentTo() {
		String to = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.appointmentTo.toString())) {
			Date date = AEDateUtil.parseDateStrict(jsonDocument.optString(ContractDeTravail.JSONKey.appointmentTo.toString()));
			to = AEDateUtil.convertToString(date, AEDateUtil.FRENCH_DATE_FORMAT);
		}
		
		return to;
	}
	
	public final String getCDDMotif() {
		String motif = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has("description")) {
			motif = jsonDocument.optString("description");
		}
		
		return motif;
	}
	
	/**
	 * Don't change return values, they are used in the logic
	 * @return
	 */
	public final String getTrialPeriodType() {
		String type = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.fullTime.toString())) {
			if (jsonDocument.optBoolean(ContractDeTravail.JSONKey.fullTime.toString())) {
				type= "temps complet";
			} else if (!jsonDocument.optBoolean(ContractDeTravail.JSONKey.fullTime.toString())) {
				type = "temps partiel";
			}
		}
		
		return type;
	}
	
	public final String getAbsentQualification() {
		String absentQualification = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.absentQualification.toString())) {
			absentQualification = jsonDocument.optString(ContractDeTravail.JSONKey.absentQualification.toString());
		}
		
		return absentQualification;
	}
	
	public final String getAbsentEmployeeName() {
		String absentEmployeeName = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.absentEmployeeName.toString())) {
			absentEmployeeName = jsonDocument.optString(ContractDeTravail.JSONKey.absentEmployeeName.toString());
		}
		
		return absentEmployeeName;
	}
	
	public final String getAbsentTime() {
		String absentTime = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.appointmentPeriod.toString())) {
			absentTime = "jusqu'au retour du salarie avec une duree minimale de "+jsonDocument.optLong(ContractDeTravail.JSONKey.appointmentPeriod.toString());
		} else {
			absentTime = "du "+this.getAppointmentFrom()+" au "+this.getAppointmentTo();
		}
		
		return absentTime;
	}
	
	public final int getAbsentMinimumDuration() {
		int duration = 0;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.appointmentPeriod.toString())) {
			duration = jsonDocument.optInt(ContractDeTravail.JSONKey.appointmentPeriod.toString());
		}
		
		return duration;
	}
	
	public final double getHoursPerSWeek() {
		double hoursPerWeek = 0.0;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.hoursPerWeek.toString())) {
			hoursPerWeek = jsonDocument.optDouble(ContractDeTravail.JSONKey.hoursPerWeek.toString());
		}
		
		return hoursPerWeek;
	}
	
	public final int getTrialPeriodDuration() {
		int trialPeriodDuration = 0;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.trialPeriodQty.toString())) {
			trialPeriodDuration = jsonDocument.optInt(ContractDeTravail.JSONKey.trialPeriodQty.toString());
		}
		
		return trialPeriodDuration;
	}
	
	public final String getProbationType() {
		String trialPeriodType = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.trialPeriodUOMId.toString())) {
			if (jsonDocument.optInt(ContractDeTravail.JSONKey.trialPeriodUOMId.toString()) == 110) {
				trialPeriodType = "jour(s)";
			} else {
				trialPeriodType = "mois";
			}
		}
		
		return trialPeriodType;
	}
	
	public final boolean isRenewable() {
		boolean isRenewable = false;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.renewable.toString())) {
			isRenewable = jsonDocument.optBoolean(ContractDeTravail.JSONKey.renewable.toString());
		}
		
		return isRenewable;
	}
	
	public final String getSalaryType() {
		String remunerationType = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.perUOMId.toString())) {
			if (jsonDocument.optInt(ContractDeTravail.JSONKey.perUOMId.toString()) == 130) {
				remunerationType = "Salaire mensuel";
			} else {
				remunerationType = "Salaire horaire";
			}
		}
		
		return remunerationType;
	}
	
	public final double getSalaryAmount() {
		double salaryAmount = 0;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.grossAmount.toString())) {
			salaryAmount = jsonDocument.optDouble(ContractDeTravail.JSONKey.grossAmount.toString());
		}
		
		return salaryAmount;
	}
	
	public final String getVerifiedBy() {
		String verifiedBy = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.verifiedByName.toString())) {
			verifiedBy = jsonDocument.optString(ContractDeTravail.JSONKey.verifiedByName.toString());
		}
		
		return verifiedBy;
	}
	
	public final String getRepresentant() {
		String repr = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.verifiedBySalutation.toString())) {
			//10 for Mr
			repr = jsonDocument.optInt(ContractDeTravail.JSONKey.verifiedBySalutation) == 10 ? "Gérant" : "Gérante";
		}
		
		return repr;
	}
	
	public final String getRepresentantSalutationShort() {
		String repr = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.verifiedBySalutation.toString())) {
			//10 for Mr 20 for Mrs 30 for Ms
			repr = jsonDocument.optInt(ContractDeTravail.JSONKey.verifiedBySalutation) == 10 ? "M. " : jsonDocument.optInt(ContractDeTravail.JSONKey.verifiedBySalutation) == 20 ? "Mme. " : "Melle. "; 
		}
		
		return repr;
	}
	
	public final String getDateOfSignup() {
		String dateOfSignup = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.regDate.toString())) {
			Date date = AEDateUtil.parseDateStrict(jsonDocument.optString(AESocialDocument.JSONKey.regDate.toString()));
			dateOfSignup = AEDateUtil.convertToString(date, AEDateUtil.FRENCH_DATE_FORMAT);
		}
		
		return dateOfSignup;
	}
	
	public final String getNumber() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has("number")) {
			str = AEStringUtil.trim(jsonDocument.optString("number"));
		}
		
		return str;
	}
	
	public final int getReasonID() {
		int rID = 0;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.reasonId.toString())) {
			rID = jsonDocument.optInt(ContractDeTravail.JSONKey.reasonId.toString());
		}
		
		return rID;
	}
	
	public final boolean isAppointmentFixed() {
		boolean isFixed = true;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.appointmentFixed.toString())) {
			isFixed = jsonDocument.optBoolean(ContractDeTravail.JSONKey.appointmentFixed.toString());
		}
		
		return isFixed;
	}
	
	public final String getReasonString() {
		String rStr = AEStringUtil.EMPTY_STRING;
		
		switch (this.getReasonID()) {
			case 1: rStr = this.getDescription();
			break;
			case 150: rStr = "Mise à pied";
			break;
			case 140: rStr = "Formation professionnelle continue";
			break;
			case 130: rStr = "Congés évenements familiaux";
			break;
			case 120: rStr = "Absence injustifiée";
			break;
			case 110: rStr = "Congés sans solde";
			break;
			case 100: rStr = "Paternité";
			break;
			case 90: rStr = "Maternité";
			break;
			case 80: rStr = "Congé parental";
			break;
			case 70: rStr = "Accident du travail";
			break;
			case 60: rStr = "Maladie";
			break;
			case 50: rStr = "Congés payés";
			break;
		}
		
		return rStr;
	}
	
	public final String getBankNameRIB() {
		String bank = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(Rib.JSONKey.bankName.toString())) {
			bank = jsonDocument.optString(Rib.JSONKey.bankName.toString());
		}
		
		return bank;
	}
	
	public final String getBankAddressRIB() {
		String address = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(Rib.JSONKey.address.toString())) {
			address = jsonDocument.optString(Rib.JSONKey.address.toString());
		}
		
		return address;
	}
	
	public final String getContactNameRIB() {
		String name = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(Rib.JSONKey.contactName.toString())) {
			name = jsonDocument.optString(Rib.JSONKey.contactName.toString());
		}
		
		return name;
	}
	
	public final String getContactPhoneRIB() {
		String phone = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(Rib.JSONKey.contactPhone.toString())) {
			phone = jsonDocument.optString(Rib.JSONKey.contactPhone.toString());
		}
		
		return phone;
	}
	
	public final String getContactMailRIB() {
		String mail = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(Rib.JSONKey.contactEMail.toString())) {
			mail = jsonDocument.optString(Rib.JSONKey.contactEMail.toString());
		}
		
		return mail;
	}
	
	public final String getSiteRIB() {
		String site = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(Rib.JSONKey.webSite.toString())) {
			site = jsonDocument.optString(Rib.JSONKey.webSite.toString());
		}
		
		return site;
	}
	
	public final String getRIBCodeRIB() {
		String rib = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(Rib.JSONKey.rib.toString())) {
			rib = jsonDocument.optString(Rib.JSONKey.rib.toString());
		}
		
		return rib;
	}
	
	public final String getIBANRIB() {
		String iban = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(Rib.JSONKey.iban.toString())) {
			iban = jsonDocument.optString(Rib.JSONKey.iban.toString());
		}
		
		return iban;
	}
	
	public final String getBICRIB() {
		String bic = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(Rib.JSONKey.bic.toString())) {
			bic = jsonDocument.optString(Rib.JSONKey.bic.toString());
		}
		
		return bic;
	}
	
	public final String getAbsentEmployeeFirstName() {
		String name = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.absentEmployeeFirstName.toString())) {
			name = jsonDocument.optString(ContractDeTravail.JSONKey.absentEmployeeFirstName.toString());
		}
		
		return name;
	}
	
	public final String getAbsentEmployeeTitle() {
		int title = 0;
		String titleString = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.absentEmployeeTitle.toString())) {
			title = jsonDocument.optInt(ContractDeTravail.JSONKey.absentEmployeeTitle.toString());
		}
		switch(title) {
			case 10: titleString = "Monsieur";
			break;
			case 20: titleString = "Madame";
			break;
			case 30: titleString = "Mademoiselle";
			break;
		}
		
		return titleString;
	}
	
	public final boolean getAbsentEmployeeIsMadam() {
		int title = 0;
		boolean isMadam = true;
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.absentEmployeeTitle.toString())) {
			title = jsonDocument.optInt(ContractDeTravail.JSONKey.absentEmployeeTitle.toString());
		}
		isMadam = title == 10 ? false : true;
		return isMadam;
	}
	
	public final String getAbsentEchelon() {
		String echelon = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.absentEchelon.toString())) {
			echelon = jsonDocument.optString(ContractDeTravail.JSONKey.absentEchelon.toString());
		}
		
		return echelon;
	}
	
	public final boolean getRemplacementPartiel() {
		boolean remplacement = false;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.replacementPartiel.toString())) {
			remplacement = jsonDocument.optBoolean(ContractDeTravail.JSONKey.replacementPartiel.toString());
		}
		
		return remplacement;
	}
	
	public final boolean getMutuelle() {
		boolean mutuelle = false;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.mutuelle.toString())) {
			mutuelle = jsonDocument.optBoolean(ContractDeTravail.JSONKey.mutuelle.toString());
		}
		
		return mutuelle;
	}
	
	public final String getAppointmentStartDate() {
		String date = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.appointmentStartDate.toString())) {
			Date d = AEDateUtil.parseDateStrict(jsonDocument.optString(ContractDeTravail.JSONKey.appointmentStartDate.toString()));
			date = AEDateUtil.convertToString(d, AEDateUtil.FRENCH_DATE_FORMAT);
		}
		
		return date;
	}
	
	public final String getAppointmentStartTime() {
		String time = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.appointmentStartTime.toString())) {
			time = jsonDocument.optString(ContractDeTravail.JSONKey.appointmentStartTime.toString());
		}
		
		return time;
	}
	
	public final boolean isCDINonAnnualise() {
		boolean cdiNonAnnualise = false;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.cdiNonAnnualise.toString())) {
			cdiNonAnnualise = jsonDocument.optBoolean(ContractDeTravail.JSONKey.cdiNonAnnualise.toString());
		}
		
		return cdiNonAnnualise;
	}
	
	public final int getDaysPerWeek() {
		int daysPerWeek = 0;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.daysPerWeek.toString())) {
			daysPerWeek = jsonDocument.optInt(ContractDeTravail.JSONKey.daysPerWeek.toString());
		}
		
		return daysPerWeek;
	}
	
	public final String getQualification() {
		String qualification = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.qualification.toString())) {
			qualification = jsonDocument.optString(ContractDeTravail.JSONKey.qualification.toString());
		}
		
		return qualification;
	}
	
	public final String getContratAideString() {
		int contratAideId = 0;
		String contratAide = AEStringUtil.EMPTY_STRING;
		
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.contractAssistenceId.toString())) {
			contratAideId = jsonDocument.optInt(ContractDeTravail.JSONKey.contractAssistenceId.toString());
			switch(contratAideId) {
				case 50: contratAide = "Aucun";
				break;
				case 60: contratAide = "CIE";
				break;
				case 70: contratAide = "Apprentissage";
				break;
				case 80: contratAide = "Contrat de professionnalisation jeune";
				break;
				case 90: contratAide = "Contrat de professionnalisation adulte";
				break;
				case 100: contratAide = "Contrat jeune entreprise";
				break;
				case 110: contratAide = "ZRR";
				break;
				case 120: contratAide = "ZRU";
				break;
				case 130: contratAide = "ZFU";
				break;
				case 140: contratAide = "Contrat d'adaptation";
				break;
				case 150: contratAide = "Contrat d'orientation";
				break;
				case 160: contratAide = "CES";
				break;
				case 170: contratAide = "CEC";
				break;
				case 180: contratAide = "Emploi-Jeunes";
				break;
				case 190: contratAide = "ACCRE";
				break;
				case 200: contratAide = "Prime handicapé";
				break;
				case 1: contratAide = "Autres";
				break;
			}
		}
		
		return contratAide;
	}
	
	public final boolean stillPresent() {
		boolean stillPresent = true;
		
		if (jsonDocument != null && jsonDocument.has(FinDuTravail.JSONKey.stillPresent.toString())) {
			stillPresent = jsonDocument.optBoolean(FinDuTravail.JSONKey.stillPresent.toString());
		}
		
		return stillPresent;
	}
	
	public final String getNdOrdre() {
		String nDOrdre = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("employee")) {
			JSONObject empl = jsonDocument.optJSONObject("employee");
			if (empl != null && empl.has("docNumber")) {
				nDOrdre = empl.optString("docNumber");
			}
		}
		return nDOrdre;
	}
	
	public final String getValidityDate() {
		String validityDate = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("employee")) {
			JSONObject empl = jsonDocument.optJSONObject("employee");
			if (empl != null && empl.has("docDateOfExpiry")) {
				validityDate = empl.optString("docDateOfExpiry");
			}
		}
		return validityDate;
	}
	
	public final String getAbsentEmployment() {
		String absEmpl = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has(ContractDeTravail.JSONKey.absentEmployment.toString())) {
			absEmpl = jsonDocument.optString(ContractDeTravail.JSONKey.absentEmployment.toString());
		}
		return absEmpl;
	}
	
	public final long getEmployeeId() {
		Long id = 0L;
		if (jsonDocument != null && jsonDocument.has("employee")) {
			JSONObject empl = jsonDocument.optJSONObject("employee");
			if (empl != null && empl.has("id")) {
				id = empl.optLong("id");
			}
		}
		return id;
	}
	
	public final long getDomainId() {
		Long domainId = 0L;
		if (jsonDocument != null && jsonDocument.has(AESocialDocument.JSONKey.employee)) {
			JSONObject employee = jsonDocument.optJSONObject(AESocialDocument.JSONKey.employee);
			if (employee != null && employee.has(Employee.JSONKey.ftpId)) {
				domainId = employee.optLong(Employee.JSONKey.ftpId);
			}
		}
		return domainId;
	}
}
