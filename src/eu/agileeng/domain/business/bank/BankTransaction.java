package eu.agileeng.domain.business.bank;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class BankTransaction extends AEDomainObject {
	
	public final static String CODE_OPENING_BALANCE = "01";
	
	public final static String CODE_MOVEMENT = "04";
	
	public final static String CODE_FINAL_BALANCE = "07";
	
	private AEDescriptor bankAccount;
	
	private AEDescriptor journal;
	
	private AEDescriptor accPeriod;
	
	private AEDescriptor statementRef;
	
	private Date date;
	
	private AEDescriptor account;
	
	private boolean recognised;

	private AEDescriptor auxiliary;
	
	private AEDocumentDescriptor reference;
	
	private AEDescriptor currency;
	
	private Double dtAmount;
	
	private Double ctAmount;
	
	/**
	 * this is
	 * AFB code or
	 * Code opération interbancaire or
	 * 
	 */
	private String codeOperation;
	
	private Date periodFinalDate;
		
	/**
	 * 
	 */
	private static final long serialVersionUID = -5704974936062383568L;

	public BankTransaction() {
		super(DomainClass.BankTransaction);
		setID(AEPersistentUtil.getTmpID());
		setCode(CODE_MOVEMENT);
	}

	public AEDescriptor getBankAccount() {
		return bankAccount;
	}

	public void setBankAccount(AEDescriptor bankAccount) {
		this.bankAccount = bankAccount;
	}

	public AEDescriptor getJournal() {
		return journal;
	}

	public void setJournal(AEDescriptor journal) {
		this.journal = journal;
	}

	public AEDescriptor getAccPeriod() {
		return accPeriod;
	}

	public void setAccPeriod(AEDescriptor accPeriod) {
		this.accPeriod = accPeriod;
	}

	public AEDescriptor getStatementRef() {
		return statementRef;
	}

	public void setStatementRef(AEDescriptor statementRef) {
		this.statementRef = statementRef;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public AEDescriptor getAccount() {
		return account;
	}

	public void setAccount(AEDescriptor account) {
		this.account = account;
	}

	public AEDescriptor getAuxiliary() {
		return auxiliary;
	}

	public void setAuxiliary(AEDescriptor auxiliary) {
		this.auxiliary = auxiliary;
	}

	public AEDocumentDescriptor getReference() {
		return reference;
	}

	public void setReference(AEDocumentDescriptor reference) {
		this.reference = reference;
	}

	public AEDescriptor getCurrency() {
		return currency;
	}

	public void setCurrency(AEDescriptor currency) {
		this.currency = currency;
	}

	public Double getDtAmount() {
		return dtAmount;
	}

	public void setDtAmount(Double dtAmount) {
		this.dtAmount = dtAmount;
	}

	public Double getCtAmount() {
		return ctAmount;
	}

	public void setCtAmount(Double ctAmount) {
		this.ctAmount = ctAmount;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		if(getBankAccount() != null) {
			json.put("bankAccountId", getBankAccount().getID());
		} else {
			json.put("bankAccountId", AEStringUtil.EMPTY_STRING);
		}
		
		if(getAccount() != null) {
			json.put("abstractAccId", -1 * getAccount().getID());
			json.put("accId", getAccount().getID());
			json.put("accCode", getAccount().getCode());
			json.put("accDescription", getAccount().getDescription());
		} else {
			json.put("accId", AEStringUtil.EMPTY_STRING);
			json.put("accCode", AEStringUtil.EMPTY_STRING);
			json.put("accDescription", AEStringUtil.EMPTY_STRING);
		}
		
		if(getDate() != null) {
			json.put("entryDate", AEDateUtil.convertToString(getDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		
		if(getJournal() != null) {
			json.put("journalCode", getJournal().getCode());
		} else {
			json.put("journalCode", AEStringUtil.EMPTY_STRING);
		}
		
		if(getAuxiliary() != null) {
			// abstractAccId should replace existing one
			json.put("abstractAccId", getAuxiliary().getDescriptor().getID());
			json.put("auxiliaryId", getAuxiliary().getID());
			json.put("auxiliary", getAuxiliary().getCode());
		} else {
			json.put("auxiliaryId", -1);
			json.put("auxiliary", AEStringUtil.EMPTY_STRING);
		}
		
		if(getReference() != null) {
			json.put("reference", getReference().getDescription());
			json.put("referenceId", getReference().getID());
			if(getReference().getClass() != null) {
				json.put("referenceType", getReference().getClazz().getID());
			}
		} else {
			json.put("reference", AEStringUtil.EMPTY_STRING);
			json.put("referenceId", AEPersistentUtil.NEW_ID);
			json.put("referenceType", AEPersistentUtil.NEW_ID);
		}
		
		json.put("currency", "EUR"); 
		
		if(!AEStringUtil.isEmpty(getCodeOperation())) {
			json.put("codeOperation", getCodeOperation()); 
		}
		
		if(getDtAmount() != null) {
			json.put("debitAmount", getDtAmount());
		} else {
			json.put("debitAmount", 0.0);
		}
		
		if(getCtAmount() != null) {
			json.put("creditAmount", getCtAmount());
		} else {
			json.put("creditAmount", 0.0); 
		}
		
		json.put("codeOperation", getCodeOperation());
		
		if(getPeriodFinalDate() != null) {
			json.put("periodFinalDate", AEDateUtil.convertToString(getPeriodFinalDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		
		json.put("isRecognised", isRecognised());
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		if(jsonObject.has("bankAccountId")) {
			 try {
				 long bankAccId = jsonObject.getLong("bankAccountId");
				 if(bankAccId > 0) {
					 AEDescriptor bankAccDescr = BankAccount.lazyDescriptor(bankAccId);
					 setBankAccount(bankAccDescr);
				 }
			 } catch (JSONException e) {
			 }
		}
		
		if(jsonObject.has("accId")) {
			 try {
				 long accId = jsonObject.getLong("accId");
				 if(accId > 0) {
					 AEDescriptor accDescr = AccAccount.lazyDescriptor(accId);
					 setAccount(accDescr);
				 }
			 } catch (JSONException e) {
			 }
		}
		
		if(jsonObject.has("auxiliaryId")) {
			long partyId = jsonObject.optLong("auxiliaryId");
			if(partyId > 0) {
				AEDescriptor auxDescr = new AEDescriptorImp();
				auxDescr.setCode(jsonObject.optString("auxiliary"));
				auxDescr.setID(partyId);
				setAuxiliary(auxDescr);
			}
		}
		
		if(jsonObject.has("journalCode")) {
			AEDescriptor journalDescr = new AEDescriptorImp();
			journalDescr.setCode(jsonObject.optString("journalCode"));
			setJournal(journalDescr);
		}
		
		if(jsonObject.has("entryDate")) {
			setDate(AEDateUtil.parseDateStrict(jsonObject.optString("entryDate")));
		}
		
		if(jsonObject.has("codeOperation")) {
			setCodeOperation(jsonObject.optString("codeOperation"));
		}
		
		AEDocumentDescriptor refDescr = new AEDocumentDescriptorImp();
		if(jsonObject.has("reference")) {
			refDescr.setDescription(jsonObject.optString("reference"));
		}
		if(jsonObject.has("referenceId")) {
			long refId = jsonObject.optLong("referenceId");
			if(refId > 0) {
				refDescr.setID(refId);
			}
		}
		if(jsonObject.has("referenceType")) {
			long refType = jsonObject.optLong("referenceType");
			if(refType > 0) {
				refDescr.setClazz(DomainClass.valueOf(refType));
			}
		}
		setReference(refDescr);
		
		if(jsonObject.has("description")) {
			setDescription(jsonObject.optString("description"));
		}

		if(jsonObject.has("debitAmount")) {
			 try {
				 double debit = jsonObject.getDouble("debitAmount");
				 setDtAmount(new Double(debit));
			 } catch (JSONException e) {
			 }
		}

		if(jsonObject.has("creditAmount")) {
			 try {
				 double credit = jsonObject.getDouble("creditAmount");
				 setCtAmount(new Double(credit));
			 } catch (JSONException e) {
			 }
		}
		
		AEDescriptor currDescr = new AEDescriptorImp();
		currDescr.setCode("EUR");
		setCurrency(currDescr);
		
		setCodeOperation(jsonObject.optString("codeOperation"));
		
		if(jsonObject.has("periodFinalDate")) {
			setPeriodFinalDate(AEDateUtil.parseDateStrict(jsonObject.optString("periodFinalDate")));
		}
		
		if(jsonObject.has("isRecognised")) {
			setRecognised(jsonObject.optBoolean("isRecognised"));
		}
	}

	public String getCodeOperation() {
		return codeOperation;
	}

	public void setCodeOperation(String codeOperation) {
		this.codeOperation = codeOperation;
	}

	public Date getPeriodFinalDate() {
		return periodFinalDate;
	}

	public void setPeriodFinalDate(Date periodFinalDate) {
		this.periodFinalDate = periodFinalDate;
	}
	
	public boolean isRecognised() {
		return recognised;
	}

	public void setRecognised(boolean recognised) {
		this.recognised = recognised;
	}
}
