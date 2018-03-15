package eu.agileeng.domain.cefra.n11580_03;

import java.util.Date;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 * CefraForm?
 * 	number=11580-03&
 *  ownerId=<long>&
 *  contributorId=<long>&
 *  year=<int>&
 *  dateTo=YYYY-MM-DD&
 *  nature=<10: cashe; 20: other>&
 *  paymentMethod=<10: Cash transfer; 20 = check>
 * @author vvatov
 *
 */
public class Cefra11580_03Request extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4750315808595972814L;
	
//	static public enum Nature {
//		NA(0L),
//		Cashe(10L),
//		Other(20L);
//
//		private long id;
//
//		private Nature(long id) {
//			this.id = id;
//		}
//
//		public final long getId() {
//			return this.id;
//		}
//
//		public static Nature valueOf(long id) {
//			Nature ret = null;
//			for (Nature inst : Nature.values()) {
//				if(inst.getId() == id) {
//					ret = inst;
//					break;
//				}
//			}
//			if(ret == null) {
//				ret = NA;
//			}
//			return ret;
//		}
//	}
//	
//	static public enum PaymentMethod {
//		NA(0L),
//		Cashe(10L),
//		Check(20L);
//
//		private long id;
//
//		private PaymentMethod(long id) {
//			this.id = id;
//		}
//
//		public final long getId() {
//			return this.id;
//		}
//
//		public static PaymentMethod valueOf(long id) {
//			PaymentMethod ret = null;
//			for (PaymentMethod inst : PaymentMethod.values()) {
//				if(inst.getId() == id) {
//					ret = inst;
//					break;
//				}
//			}
//			if(ret == null) {
//				ret = NA;
//			}
//			return ret;
//		}
//	}
	
	private AEDescriptor contributor;
	
	private int year;
	
	private Date dateTo;
	
	private String docNumber;
	
//	private Nature nature;
//	
//	private PaymentMethod paymentMethod;
	
	public Cefra11580_03Request() {
		super(DomainClass.TRANSIENT);
	}

	/**
	 * @return the contributor
	 */
	public AEDescriptor getContributor() {
		return contributor;
	}

	/**
	 * @param contributor the contributor to set
	 */
	public void setContributor(AEDescriptor contributor) {
		this.contributor = contributor;
	}

	/**
	 * @return the year
	 */
	public int getYear() {
		return year;
	}

	/**
	 * @param year the year to set
	 */
	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * @return the dateTo
	 */
	public Date getDateTo() {
		return dateTo;
	}

	/**
	 * @param dateTo the dateTo to set
	 */
	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}

	/**
	 * @return the docNumber
	 */
	public String getDocNumber() {
		return docNumber;
	}

	/**
	 * @param docNumber the docNumber to set
	 */
	public void setDocNumber(String docNumber) {
		this.docNumber = docNumber;
	}

//	/**
//	 * @return the nature
//	 */
//	public Cefra11580_03Request.Nature getNature() {
//		return nature;
//	}
//
//	/**
//	 * @param nature the nature to set
//	 */
//	public void setNature(Cefra11580_03Request.Nature nature) {
//		this.nature = nature;
//	}
//
//	/**
//	 * @return the paymentMethod
//	 */
//	public Cefra11580_03Request.PaymentMethod getPaymentMethod() {
//		return paymentMethod;
//	}
//
//	/**
//	 * @param paymentMethod the paymentMethod to set
//	 */
//	public void setPaymentMethod(Cefra11580_03Request.PaymentMethod paymentMethod) {
//		this.paymentMethod = paymentMethod;
//	}
}
