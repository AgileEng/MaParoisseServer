/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.11.2009 16:40:50
 */
package eu.agileeng.domain.contact;

import java.util.Date;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.business.DepartmentAGIRC;
import eu.agileeng.domain.business.DepartmentARRCO;
import eu.agileeng.domain.business.TaxTypeEnum;
import eu.agileeng.domain.cash.FinancesUtil;

/**
 *
 */
@SuppressWarnings("serial")
public class Organization extends Party {

	public static class Property {
		public static final long BANK_ACCOUNTING_COMPOUND = 1L << 2;
	}
	
	public static class JSONKey {
		public static final String customer = "customer";
		public static final String customers = "customers";
		public static final String customerId = "customerId";
		public static final String defaultDiversAccCode = "defaultDiversAccCode";
		public static final String chartOfAccounts = "chartOfAccounts";
		public static final String doNotLoadContacts = "doNotLoadContacts";
		public static final String doNotLoadAddresses = "doNotLoadAddresses";
		public static final String doNotLoadCAO = "doNotLoadCAO";
		public static final String onlyCashAccounts = "onlyCashAccounts";
		public static final String doNotLoadSocialInfo = "doNotLoadSocialInfo";
		public static final String paroisseStatut = "paroisseStatut";
		public static final String paroisseDoyenne = "paroisseDoyenne";
		public static final String paroisseContactPerson = "paroisseContactPerson";
	}
	
	static public enum AppType {
		nd,
		fabrique,
		mense
	}
	
	private OrganizationFinanceCapital financeCapital;
	
	private OrganizationIndustry industry;
	
	private OrganizationEmplNumber emplNumber;
	
	/**
	 * Initialize here to be independent of constructors
	 */
	private EmployeeList employeeList = new EmployeeList();
	
	private String CEGIDNumber;
	private TaxTypeEnum taxType; //Recette des impots
	//SIRET->UIN
	private String taxNo;//FRP
	private String communiyVATNo;//TVA intracommunitaire
	private String nafCode;
	private String collectiveConvention;
	private String salaryNumber;//numero URSSAF
	private String salaryDepartment;//Lieu cotisation URSSAF
	private DepartmentAGIRC departmentAGIRC;//nom caisse de retraite AGIRC
	private DepartmentARRCO departmentARRCO;//nom caisse de retraite ARRCO
	private String retirementNoAGIRC;//numero affiliation caisse AGIRC
	private String retirementNoARRCO;//numero affiliation caisse ARRCO
	private String taxNoReceive;//impots numero recette
	private String taxNoFile;//impots numero dossier
	private String taxNoKey;//impots numero Cle
	private String taxCenter;//centre des impots
	private String taxInspectionNumber;//impots numero inspection
	private String taxRegime;//impots regime
	private String siren;
	private String nic;
	private Date   startDate;
	private long cashAccMode = FinancesUtil.CASH_ACC_MODE_MONTHLY;
	private long bankAccMode = FinancesUtil.BANK_ACC_MODE_DETAIL;
	
	/**
	 * @param clazz
	 */
	public Organization() {
		super(DomainClass.ORGANIZATION);
	}

	/**
	 * @return the financeCapital
	 */
	public OrganizationFinanceCapital getFinanceCapital() {
		return financeCapital;
	}

	/**
	 * @param financeCapital the financeCapital to set
	 */
	public void setFinanceCapital(OrganizationFinanceCapital financeCapital) {
		this.financeCapital = financeCapital;
	}

	/**
	 * @return the industry
	 */
	public OrganizationIndustry getIndustry() {
		return industry;
	}

	/**
	 * @param industry the industry to set
	 */
	public void setIndustry(OrganizationIndustry industry) {
		this.industry = industry;
	}

	/**
	 * @return the emplNumber
	 */
	public OrganizationEmplNumber getEmplNumber() {
		return emplNumber;
	}

	/**
	 * @param emplNumber the emplNumber to set
	 */
	public void setEmplNumber(OrganizationEmplNumber emplNumber) {
		this.emplNumber = emplNumber;
	}

	/**
	 * @return the employeeList
	 */
	public EmployeeList getEmployeeList() {
		return employeeList;
	}

	/**
	 * @param employeeList the employeeList to set
	 */
	public void setEmployeeList(EmployeeList employeeList) {
		this.employeeList = employeeList;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new OrganizationDescriptor(id);
	}

	public String getCEGIDNumber() {
		return CEGIDNumber;
	}

	public void setCEGIDNumber(String cEGIDNumber) {
		CEGIDNumber = cEGIDNumber;
	}

	public String getTaxNo() {
		return taxNo;
	}

	public void setTaxNo(String taxNo) {
		this.taxNo = taxNo;
	}

	public String getCommuniyVATNo() {
		return communiyVATNo;
	}

	public void setCommuniyVATNo(String communiyVATNo) {
		this.communiyVATNo = communiyVATNo;
	}

	public String getNafCode() {
		return nafCode;
	}

	public void setNafCode(String nafCode) {
		this.nafCode = nafCode;
	}

	public String getCollectiveConvention() {
		return collectiveConvention;
	}

	public void setCollectiveConvention(String collectiveConvention) {
		this.collectiveConvention = collectiveConvention;
	}

	public String getSalaryNumber() {
		return salaryNumber;
	}

	public void setSalaryNumber(String salaryNumber) {
		this.salaryNumber = salaryNumber;
	}

	public String getSalaryDepartment() {
		return salaryDepartment;
	}

	public void setSalaryDepartment(String salaryDepartment) {
		this.salaryDepartment = salaryDepartment;
	}

	public String getRetirementNoAGIRC() {
		return retirementNoAGIRC;
	}

	public void setRetirementNoAGIRC(String retirementNoAGIRC) {
		this.retirementNoAGIRC = retirementNoAGIRC;
	}

	public String getRetirementNoARRCO() {
		return retirementNoARRCO;
	}

	public void setRetirementNoARRCO(String retirementNoARRCO) {
		this.retirementNoARRCO = retirementNoARRCO;
	}

	public String getTaxNoReceive() {
		return taxNoReceive;
	}

	public void setTaxNoReceive(String taxNoReceive) {
		this.taxNoReceive = taxNoReceive;
	}

	public String getTaxNoFile() {
		return taxNoFile;
	}

	public void setTaxNoFile(String taxNoFile) {
		this.taxNoFile = taxNoFile;
	}

	public String getTaxNoKey() {
		return taxNoKey;
	}

	public void setTaxNoKey(String taxNoKey) {
		this.taxNoKey = taxNoKey;
	}

	public String getTaxCenter() {
		return taxCenter;
	}

	public void setTaxCenter(String taxCenter) {
		this.taxCenter = taxCenter;
	}

	public String getTaxInspectionNumber() {
		return taxInspectionNumber;
	}

	public void setTaxInspectionNumber(String taxInspectionNumber) {
		this.taxInspectionNumber = taxInspectionNumber;
	}

	public String getTaxRegime() {
		return taxRegime;
	}

	public void setTaxRegime(String taxRegime) {
		this.taxRegime = taxRegime;
	}

	/**
	 * @return the taxType
	 */
	public TaxTypeEnum getTaxType() {
		return taxType;
	}

	/**
	 * @param taxType the taxType to set
	 */
	public void setTaxType(TaxTypeEnum taxType) {
		this.taxType = taxType;
	}

	/**
	 * @return the departmentAGIRC
	 */
	public DepartmentAGIRC getDepartmentAGIRC() {
		return departmentAGIRC;
	}

	/**
	 * @param departmentAGIRC the departmentAGIRC to set
	 */
	public void setDepartmentAGIRC(DepartmentAGIRC departmentAGIRC) {
		this.departmentAGIRC = departmentAGIRC;
	}

	/**
	 * @return the departmentARRCO
	 */
	public DepartmentARRCO getDepartmentARRCO() {
		return departmentARRCO;
	}

	/**
	 * @param departmentARRCO the departmentARRCO to set
	 */
	public void setDepartmentARRCO(DepartmentARRCO departmentARRCO) {
		this.departmentARRCO = departmentARRCO;
	}

	public String getSiren() {
		return siren;
	}

	public void setSiren(String siren) {
		this.siren = siren;
	}

	public String getNic() {
		return nic;
	}

	public void setNic(String nic) {
		this.nic = nic;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public long getCashAccMode() {
		return cashAccMode;
	}

	public void setCashAccMode(long cashAccMode) {
		this.cashAccMode = cashAccMode;
	}

	public long getBankAccMode() {
		return bankAccMode;
	}

	public void setBankAccMode(long bankAccMode) {
		this.bankAccMode = bankAccMode;
	}
	
	/**
	 * 
	 * AppType.fabrique - From 0001 to 110x these are the factories or parishes;
	 * AppType.mense - From 9001 to 9180 they are menses.
	 * AppType.nd - when the type cannot be determined
	 * 
	 * @param code
	 * @return
	 */
	public static AppType getAppType(String code) {
		try {
			Long lCode = Long.parseLong(code);
			if(9001L <= lCode && lCode <= 9180L) {
				return AppType.mense;
			} else {
				return AppType.fabrique;
			}
		} catch (Exception e) {
			return AppType.nd;
		}
	}
}
