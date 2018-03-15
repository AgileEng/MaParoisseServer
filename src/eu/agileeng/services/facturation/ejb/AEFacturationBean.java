package eu.agileeng.services.facturation.ejb;

import java.util.Date;
import java.util.Iterator;

import javax.ejb.EJB;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.business.AEPaymentTerms;
import eu.agileeng.domain.business.AEPaymentTermsList;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentFactory;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.trade.AEDocumentItem;
import eu.agileeng.domain.facturation.AEArticle;
import eu.agileeng.domain.facturation.AEArticleValidator;
import eu.agileeng.domain.facturation.AEArticlesList;
import eu.agileeng.domain.facturation.AEClient;
import eu.agileeng.domain.facturation.AEClientValidator;
import eu.agileeng.domain.facturation.AEClientsList;
import eu.agileeng.domain.facturation.AEFacture;
import eu.agileeng.domain.facturation.AEFactureDescr;
import eu.agileeng.domain.facturation.AEFactureFactory;
import eu.agileeng.domain.facturation.AEFactureFilter;
import eu.agileeng.domain.facturation.AEFactureItem;
import eu.agileeng.domain.facturation.AEFactureItemsList;
import eu.agileeng.domain.facturation.AEFacturePrintTemplate;
import eu.agileeng.domain.facturation.AEFacturePrintTemplatesList;
import eu.agileeng.domain.facturation.AEFactureSaveValidator;
import eu.agileeng.domain.facturation.AEFactureUtil;
import eu.agileeng.domain.facturation.AEFactureUtil.FactureState;
import eu.agileeng.domain.facturation.AEFactureUtil.PayableType;
import eu.agileeng.domain.facturation.AEFactureUtil.PaymentType;
import eu.agileeng.domain.facturation.AEFacturesList;
import eu.agileeng.domain.facturation.AEPayment;
import eu.agileeng.domain.facturation.AEPaymentsFilter;
import eu.agileeng.domain.facturation.AEPaymentsList;
import eu.agileeng.domain.facturation.AEPaymentsListValidator;
import eu.agileeng.domain.measurement.UnitOfMeasurement;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.acc.AccPeriodDAO;
import eu.agileeng.persistent.dao.acc.VATItemDAO;
import eu.agileeng.persistent.dao.facturation.AEFacturationDAO;
import eu.agileeng.persistent.dao.facturation.AEFactureDAO;
import eu.agileeng.persistent.dao.facturation.AEFactureItemDAO;
import eu.agileeng.persistent.dao.facturation.AEPaymentDAO;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.acc.ejb.AccLocal;
import eu.agileeng.services.party.ejb.PartyLocal;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;

public class AEFacturationBean extends AEBean implements AEFacturationLocal, AEFacturationRemote {

	@EJB private AccLocal accLocal;
	@EJB private PartyLocal partyService;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6276510944336607346L;

	@Override
	public AEResponse saveArticle(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject articleJson = arguments.optJSONObject("article");
			long ownerId = arguments.optLong("sOwnerId");
			
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// whether this article is ownered by this customer
			if(ownerId != articleJson.optLong("ownerId")) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Create article
			 */
			AEArticle art = new AEArticle();
			art.create(articleJson);
			
			/**
			 * Validate
			 */
			art.validateWith(AEArticleValidator.getInstance());
			
			/**
			 * Save in transaction
			 */
			localConnection.beginTransaction();
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			switch(art.getPersistentState()) {
			case NEW:
				setNextNumber(art, localConnection, invContext);
				factDAO.insert(art);
				if(art.getCodeNumber() > 0) {
					factDAO.updateCodeNumber(art);
				}
				break;
			case UPDATED:
				factDAO.update(art);
				break;
			case DELETED:
				assert(false);
				break;
			default:
				// internal error
				assert(false);
				break;
			}
			
			/**
			 * After Save
			 */
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put("article", art.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadArticles(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("ownerId");
			boolean dontLoadInfo = arguments.optBoolean("dontLoadInfo");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Validate
			 */
			
			/**
			 * Load
			 */
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			AEArticlesList articles = factDAO.loadArticlesList(ownerDescr);
			
			/**
			 * Load sale accounts
			 */
			JSONArray saleAccounts = null;
			if(!dontLoadInfo) {	
				saleAccounts = accLocal.loadSaleAccountsByOwner(ownerId, localConnection);
			}
			
			/**
			 * Load VAT Items
			 */
			JSONArray vatItems = null;
			if(!dontLoadInfo) {	
				VATItemDAO vatItemsDAO = daoFactory.getVATItemDAO(localConnection);
				vatItems = vatItemsDAO.load();
			}
			
			/**
			 * After Load
			 */
			
			JSONObject payload = new JSONObject();
			
			/**
			 * articlesInfo
			 */
			JSONObject articlesInfo = new JSONObject();
			if(!dontLoadInfo) {	
				articlesInfo.put("units", UnitOfMeasurement.toJSONArray(UnitOfMeasurement.getFacturationUnits()));
			}
			if(saleAccounts != null) {
				articlesInfo.put("accounts", saleAccounts);
			}
			if(vatItems != null) {
				articlesInfo.put("vatItems", vatItems);
			}
			payload.put("articlesInfo", articlesInfo); 
			
			/**
			 * articles
			 */
			payload.put("articles", articles.toJSONArray()); 
			
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse deleteArticle(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Get arguments
			 */
			JSONObject arguments = aeRequest.getArguments();
			long sOwnerId = arguments.optLong("sOwnerId");
			JSONObject articleJson = arguments.optJSONObject("article");
			long articleId = articleJson.optLong("id");
			
			/**
			 * Validation
			 */
			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);
			
			// whether this article is ownered by this customer
			if(sOwnerId != articleJson.optLong("ownerId")) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * DB connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// begin transaction
			localConnection.beginTransaction();
			
			// delete document
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			factDAO.deleteArticle(AEArticle.lazyDescriptor(articleId));
			
			// commit
			localConnection.commit();
			
			// return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			localConnection.rollback();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveClient(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject clientJson = arguments.optJSONObject(AEClient.JSONKey.client);
			long ownerId = arguments.optLong("sOwnerId");
			
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// whether this article is ownered by this customer
			if(ownerId != clientJson.optLong("ownerId")) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Create article
			 */
			AEClient client = new AEClient();
			client.create(clientJson);
			
			/**
			 * Validate
			 */
			client.validateWith(AEClientValidator.getInstance());
			
			/**
			 * Save in transaction
			 */
			localConnection.beginTransaction();
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			switch(client.getPersistentState()) {
			case NEW:
				setNextNumber(client, localConnection, invContext);
				factDAO.insert(client);
				if(client.getCodeNumber() > 0) {
					factDAO.updateCodeNumber(client);
				}
				break;
			case UPDATED:
				factDAO.update(client);
				break;
			case DELETED:
				assert(false);
				break;
			default:
				// internal error
				assert(false);
				break;
			}
			
			/**
			 * After Save
			 */
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put(AEClient.JSONKey.client, client.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadClients(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("ownerId");
			boolean dontLoadInfo = arguments.optBoolean("dontLoadInfo");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Validate
			 */
			
			/**
			 * Load
			 */
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			AEClientsList clients = factDAO.loadClientsList(ownerDescr);
			
			/**
			 * Load sale accounts
			 */
			JSONArray saleAccounts = null;
			if(!dontLoadInfo) {
				saleAccounts = accLocal.loadSaleAccountsByOwner(ownerId, localConnection);
			}
			
			/**
			 * Load Payment Terms Items
			 */
			AEPaymentTermsList ptList = null;
			if(!dontLoadInfo) {
				ptList = factDAO.loadPaymentTerms(ownerDescr);
			}
			
			/**
			 * After Load
			 */
			
			JSONObject payload = new JSONObject();
			
			/**
			 * clientsInfo
			 */
			JSONObject clientsInfo = new JSONObject();
			if(saleAccounts != null) {
				clientsInfo.put("accounts", saleAccounts);
			}
			if(ptList != null) {
				clientsInfo.put("paymentTerms", ptList.toJSONArray());
			}
			payload.put("clientsInfo", clientsInfo); 
			
			/**
			 * articles
			 */
			payload.put(AEClient.JSONKey.clients, clients.toJSONArray()); 
			
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse deleteClient(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Get arguments
			 */
			JSONObject arguments = aeRequest.getArguments();
			long sOwnerId = arguments.optLong("sOwnerId");
			JSONObject clientJson = arguments.optJSONObject(AEClient.JSONKey.client);
			long clientId = clientJson.optLong("id");
			
			/**
			 * Validation
			 */
			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);
			
			// whether this article is ownered by this customer
			if(sOwnerId != clientJson.optLong("ownerId")) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * DB connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// begin transaction
			localConnection.beginTransaction();
			
			// delete document
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			factDAO.deleteClient(AEClient.lazyDescriptor(clientId));
			
			// commit
			localConnection.commit();
			
			// return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			localConnection.rollback();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveFacture(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject factureJson = arguments.optJSONObject(AEFacture.JSONKey.facture);
			long docType = factureJson.getLong("docType");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// whether this article is ownered by this customer
			if(ownerId != factureJson.optLong("ownerId")) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Detect document type
			 */
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			/**
			 * Create facture
			 */
			
			// create from JSON
			AEDocumentFactory docFactory = AEDocumentFactory.getInstance(aeDocType);
			if(!(docFactory instanceof AEFactureFactory)) {
				throw new AEException("System Error: Unknnown document factory!");
			}
			AEDocument aeDocument = docFactory.createDocument(aeDocType);
			if(!(aeDocument instanceof AEFacture)) {
				throw new AEException("System Error: The document is not type of AEFacture!");
			}
			AEFacture facture = (AEFacture) aeDocument;
			facture.create(factureJson);
			
			// Load foreign records
			VATItemDAO vatItemsDAO = daoFactory.getVATItemDAO(localConnection);
			JSONArray vatItems = vatItemsDAO.load();
			loadVatItems(facture, vatItems);
			
			// Create dateOfExpiry
			createDateOfExpiry(facture);
			
			// begin transaction
			localConnection.beginTransaction();
			
			// save facrure
			saveFacture(facture, invContext, localConnection);
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put(AEFacture.JSONKey.facture, facture.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadFactures(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		return null;
	}

	@Override
	public AEResponse deleteFacture(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long factureId = arguments.getLong("id");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Load and validate
			 */
			AEFacture facture = loadFacture(AEFacture.lazyDescriptor(factureId), invContext, localConnection);
			
			// whether this facture is ownered by this customer
			if(ownerId != facture.getCompany().getDescriptor().getID()) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Business rules validations
			 */
			
			// validate in open period
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			boolean bRet = false;
			if(!facture.isTemplate()) {
				// check closed period
				if(AEDocumentType.valueOf(AEDocumentType.System.AEFactureSale).equals(facture.getType())) {
					bRet = accPeriodDAO.isInClosedPeriod(
							facture.getDate(), 
							facture.getCompany() != null ? 
									facture.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
							AEApp.FACTURATION_FACTURE);
				}
			}
			if(bRet) {
				throw new AEException(
						(int) AEError.System.CANNOT_INSERT_UPDATE_CLOSED_PERIOD.getSystemID(), 
						"Période close, veuillez saisir la date de facture en reference!");
			}
			
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConnection);
			AEPaymentDAO paymentDAO = daoFactory.getPaymentDAO(localConnection);
			
			// invoiced devis cannot be deleted
			factureDAO.deleteInvoicedDevisValidator(facture);
			
			/**
			 * Delete in transaction
			 */
			localConnection.beginTransaction();
			
			// use case delete advance invoice 
			if(facture.isFacture() && facture.isAdvance()) {
				// advance invoice will be deleted
				
				// load the source document of the advance facture
				if(facture.getSrcDoc() == null) {
					throw new AEException("System error: Advance invoice without source");
				}
				AEFacture srcDocumentOfAdvancedFacture = loadFacture(facture.getSrcDoc().getDescriptor(), invContext, localConnection);
				if(srcDocumentOfAdvancedFacture == null) {
					throw new AEException("System error: Cannot found the source for advanced invoice");
				}
				
				// detetect the source document's type
				if(srcDocumentOfAdvancedFacture.isDevis()) {
					// advance facture over devis
					boolean hasFinalFacture = false;
					
					// process final facture
					AEFactureItemsList itemsList = facture.getFactureItems();
					for (Iterator<AEDocumentItem> iterator = itemsList.iterator(); iterator.hasNext();) {
						AEFactureItem advItem = (AEFactureItem) iterator.next();

						// process final facture
						AEDescriptor finalFactureDescr = factureDAO.loadWhereItemIsDeducted(advItem.getDescriptor());
						if(finalFactureDescr != null) {
							AEFacture finalFacture = loadFacture(finalFactureDescr, invContext, localConnection);
							if(finalFacture.isFormalFacture()) {
								// transfer advance payment to regular requisition
								AEPayment stillNotPaidPayment = new AEPayment();

								stillNotPaidPayment.setToFacture(finalFactureDescr);
								stillNotPaidPayment.setPayer(facture.getClient() != null ? facture.getClient().getDescriptor() : null);
								stillNotPaidPayment.setPaymentTerms(facture.getPaymentTerms());
								stillNotPaidPayment.setPayableType(AEFactureUtil.PayableType.REQUISITION);
								stillNotPaidPayment.setPaymentType(AEFactureUtil.PaymentType.REGULAR);
								stillNotPaidPayment.setDate(facture.getDate());
								if(stillNotPaidPayment.getPaymentTerms() instanceof AEPaymentTerms) {
									AEPaymentTerms pTerms = (AEPaymentTerms) stillNotPaidPayment.getPaymentTerms();
									stillNotPaidPayment.setDueDate(pTerms.applyTerms(facture.getDate()));
								} else {
									stillNotPaidPayment.setDueDate(facture.getDate());
								}
								stillNotPaidPayment.setAmount(facture.getAmount());
								paymentDAO.insert(stillNotPaidPayment);
							}

							// remove advance deductions
							AEFactureItemsList deductionFactureItems = finalFacture.grantFactureItems();
							for (Iterator<AEDocumentItem> iterator2 = deductionFactureItems.iterator(); iterator2.hasNext();) {
								AEFactureItem deductedItem = (AEFactureItem) iterator2.next();
								if(AEFactureUtil.FactureItemType.ADVANCE_DEDUCTION.equals(deductedItem.getTypeItem())
										&& deductedItem.getItemDeducted() != null 
										&& deductedItem.getItemDeducted().getDescriptor().getID() == advItem.getID()) {
									
									deductedItem.setPersistentState(AEPersistent.State.DELETED);
									hasFinalFacture = true;
								}
							}
							
							// update final invoice
							finalFacture.setUpdated();
							saveFacture(finalFacture, invContext, localConnection);
						}
					}
					
					// devis'es advance payment
					AEPaymentsList pList = paymentDAO.loadToFacture(srcDocumentOfAdvancedFacture.getDescriptor());
					for (Iterator<AEPayment> iterator = pList.iterator(); iterator.hasNext();) {
						AEPayment aePayment = (AEPayment) iterator.next();
						if(AEFactureUtil.PaymentType.ADVANCE.equals(aePayment.getPaymentType())) {
							paymentDAO.updateToUnpaid(aePayment.getID());
							if(hasFinalFacture) {
								paymentDAO.updatePayableType(AEFactureUtil.PayableType.SCHEDULE.getTypeId(), aePayment.getID());
							} else {
								paymentDAO.updatePayableType(AEFactureUtil.PayableType.REQUISITION.getTypeId(), aePayment.getID());
							}
						}
					}
				} else if(srcDocumentOfAdvancedFacture.isFacture()) {
					// advance facture over facture
					AEFacture finalFacture = srcDocumentOfAdvancedFacture;
					if(finalFacture.isFormalFacture()) {
						// transfer advance payment to regular requisition
						AEPayment stillNotPaidPayment = new AEPayment();
						stillNotPaidPayment.setToFacture(finalFacture.getDescriptor());
						stillNotPaidPayment.setPayer(facture.getClient() != null ? facture.getClient().getDescriptor() : null);
						stillNotPaidPayment.setPaymentTerms(facture.getPaymentTerms());
						stillNotPaidPayment.setPayableType(AEFactureUtil.PayableType.REQUISITION);
						stillNotPaidPayment.setPaymentType(AEFactureUtil.PaymentType.REGULAR);
						stillNotPaidPayment.setDate(facture.getDate());
						if(stillNotPaidPayment.getPaymentTerms() instanceof AEPaymentTerms) {
							AEPaymentTerms pTerms = (AEPaymentTerms) stillNotPaidPayment.getPaymentTerms();
							stillNotPaidPayment.setDueDate(pTerms.applyTerms(facture.getDate()));
						} else {
							stillNotPaidPayment.setDueDate(facture.getDate());
						}
						stillNotPaidPayment.setAmount(facture.getAmount());
						paymentDAO.insert(stillNotPaidPayment);
						
						// remove advance deducted items
						AEFactureItemsList itemsList = facture.getFactureItems();
						for (Iterator<AEDocumentItem> iterator = itemsList.iterator(); iterator.hasNext();) {
							AEFactureItem advItem = (AEFactureItem) iterator.next();
							AEFactureItemsList deductionFactureItems = finalFacture.grantFactureItems();
							for (Iterator<AEDocumentItem> iterator2 = deductionFactureItems.iterator(); iterator2.hasNext();) {
								AEFactureItem deductedItem = (AEFactureItem) iterator2.next();
								if(AEFactureUtil.FactureItemType.ADVANCE_DEDUCTION.equals(deductedItem.getTypeItem())
										&& deductedItem.getItemDeducted() != null 
										&& deductedItem.getItemDeducted().getDescriptor().getID() == advItem.getID()) {
									
									deductedItem.setPersistentState(AEPersistent.State.DELETED);
								}
							}
						}
						
						// update final invoice
						finalFacture.setUpdated();
						saveFacture(finalFacture, invContext, localConnection);
					} else {
						double advAmount = 0.0;
						AEFactureItemsList items = finalFacture.grantFactureItems();
						for (Iterator<AEDocumentItem> iterator = items.iterator(); iterator.hasNext();) {
							AEFactureItem item = (AEFactureItem) iterator.next();
							if(AEFactureUtil.FactureItemType.ADVANCE_DEDUCTION.equals(item.getTypeItem())) {
								advAmount += Math.abs(item.getAmount());
								item.setPersistentState(AEPersistent.State.DELETED);
							}
						}
						finalFacture.setAdvanceMode(AEFactureUtil.PercentAmountMode.AMOUNT);
						finalFacture.setAdvanceAmount(AEMath.round(advAmount, 2));

						// update and save regular invoice
						finalFacture.setUpdated();
						saveFacture(finalFacture, invContext, localConnection);
					}
				} else {
					
				}
				
				// recalculate srcFacture
				factureDAO.updatePaidAmount(srcDocumentOfAdvancedFacture.getDescriptor().getID());
			} else if(facture.isFacture()) {
				// regular facture will be deleted
				
				// update payments
				AEFactureItemsList items = facture.grantFactureItems();
				for (AEDocumentItem item : items) {
					if(item instanceof AEFactureItem) {
						AEFactureItem fItem = (AEFactureItem) item;
						if(fItem.getPaymentInvoiced() != null) {
							paymentDAO.updateToUnpaid(fItem.getPaymentInvoiced().getDescriptor().getID());
						}
					}
				}
				
				// update state
				if(facture.getSrcDoc() != null) {
					AEFacture srcDocumentOfAdvancedFacture = loadFacture(facture.getSrcDoc().getDescriptor(), invContext, localConnection);
					if(srcDocumentOfAdvancedFacture != null) {
						// detetect the source document's type
						if(srcDocumentOfAdvancedFacture.isDevis()) {
							factureDAO.updateInvoicedAmount(srcDocumentOfAdvancedFacture.getID());
						}
					}
				}
			}
			
			deleteFacture(facture, invContext, localConnection);
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put("id", facture.getID());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void prepareInsert(AEFacture facture, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate insert
		 */
		
		
		/**
		 * prepare
		 */
		Date dateNow = new Date();

		// creator
		facture.setCreator(invContext.getAuthPrincipal().getName());
		facture.setTimeCreated(dateNow);
		
		// modifier
		facture.setModifier(invContext.getAuthPrincipal().getName());
		facture.setTimeModified(dateNow);
		
		// date
		if(facture.getDate() == null) {
			facture.setDate(dateNow);
		}
		facture.setRegDate(dateNow);
		
		// company
		if(facture.getCompany() == null) {
			facture.setCompany(invContext.getAuthPrincipal().getCompany());
		}
		
		// number
		setNextNumber(facture, aeConnection, invContext);
		
		// payments
		if(AECollectionUtil.isEmpty(facture.getPayments())) {
			// new facture without payments
			createPayments(facture, invContext, aeConnection);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void insert(AEFacture facture, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();
			
			// prepare concrete document type
			prepareInsert(facture, invContext, localConnection);
			
			// insert
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConnection);
			factureDAO.insert(facture);
			
			// commit
			localConnection.commit();
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void prepareUpdate(AEFacture facture, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate update
		 */
		
		/**
		 * prepare
		 */
		Date dateNow = new Date();
		if(AEStringUtil.isEmpty(facture.getCreator())) {
			facture.setCreator(invContext.getAuthPrincipal().getName());
		}
		if(facture.getTimeCreated() == null) {
			facture.setTimeCreated(dateNow);
		}
		facture.setModifier(invContext.getAuthPrincipal().getName());
		facture.setTimeModified(dateNow);
		if(facture.getCompany() == null) {
			facture.setCompany(invContext.getAuthPrincipal().getCompany());
		}
		
		// number
		if(AEStringUtil.isEmpty(facture.getNumberString())) {
			setNextNumber(facture, aeConnection, invContext);
		} else {
			setCurrentNumber(facture, aeConnection, invContext);
		}
		
		// payments
		updatePayments(facture, invContext, aeConnection);
	}
	
	/**
 	 * private where no need for different  
	 */
	private void update(AEFacture facture, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// prepare
			prepareUpdate(facture, invContext, aeConnection);
			
			// update
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConn);
			factureDAO.update(facture);
			
			// commit
			localConn.commit();
		} catch (Throwable t) {
			t.printStackTrace();
			localConn.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConn);
		}
	}

	@Override
	public AEResponse loadFacturesInfo(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("ownerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Load VAT Items
			 */
			VATItemDAO vatItemsDAO = daoFactory.getVATItemDAO(localConnection);
			JSONArray vatItems = vatItemsDAO.load();
			
			/**
			 * Load Payment terms
			 */
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			AEPaymentTermsList ptList = factDAO.loadPaymentTerms(ownerDescr);
			
			/**
			 * After Load
			 */
			
			JSONObject payload = new JSONObject();
			JSONObject facturesInfo = new JSONObject();
			facturesInfo.put("units", UnitOfMeasurement.toJSONArray(UnitOfMeasurement.getFacturationUnits()));
			facturesInfo.put("vatItems", vatItems);
			facturesInfo.put("paymentTerms", ptList.toJSONArray());
			payload.put("facturesInfo", facturesInfo); 
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	protected void processItems(AEFacture facture, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();
			
			// save in transaction
			AEFactureItemDAO factureItemDAO = daoFactory.getFactureItemDAO(localConnection);
			AEFactureItemsList items = facture.getFactureItems();
			for (Iterator<AEDocumentItem> iterator = items.iterator(); iterator.hasNext();) {
				AEFactureItem aeDocumentItem = (AEFactureItem) iterator.next();
				aeDocumentItem.setDocument(facture.getDescriptor());
				switch(aeDocumentItem.getPersistentState()) {
				case NEW:
					factureItemDAO.insert(aeDocumentItem);
					break;
				case UPDATED:
					factureItemDAO.update(aeDocumentItem);
					break;
				case DELETED:
					factureItemDAO.delete(aeDocumentItem.getID());
					iterator.remove();
					break;
				default:
					// internal error
					assert(false);
					break;
				}
			}
			localConnection.commit();
		} catch (AEException e) {
			localConnection.rollback();
			throw e;
		} catch (Throwable t) {
			localConnection.rollback();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private void processPayments(AEFacture facture, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();
			
			// facture payments
			AEPaymentsList payments = facture.grantPayments();
			
			// validate
			facture.grantPayments().validateWith(AEPaymentsListValidator.getInstance());
			
			// save in transaction
			AEPaymentDAO paymentDAO = daoFactory.getPaymentDAO(localConnection);
			for (Iterator<AEPayment> iterator = payments.iterator(); iterator.hasNext();) {
				AEPayment p = (AEPayment) iterator.next();
				p.setToFacture(facture.getDescriptor());
				p.setPayer(facture.getClient() != null ? facture.getClient().getDescriptor() : null);
//				if(p.getPaymentTerms() == null) {
//					p.setPaymentTerms(facture.getPaymentTerms());
//				}
				switch(p.getPersistentState()) {
				case NEW:
					paymentDAO.insert(p);
					break;
				case UPDATED:
					paymentDAO.update(p);
					break;
				case DELETED:
					paymentDAO.delete(p.getID());
					iterator.remove();
					break;
				default:
					// internal error
					assert(false);
					break;
				}
			}
			localConnection.commit();
		} catch (AEException e) {
			localConnection.rollback();
			throw e;
		} catch (Throwable t) {
			localConnection.rollback();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private void loadVatItems(AEFacture facture, JSONArray vatItems) throws AEException {
		try {
			AEFactureItemsList items = facture.getFactureItems();
			for (Iterator<AEDocumentItem> iterator = items.iterator(); iterator.hasNext();) {
				AEFactureItem fItem = (AEFactureItem) iterator.next();
				long vatId = fItem.getVatId();
				for (int i = 0; i < vatItems.length(); i++) {
					JSONObject vatItemJson = vatItems.optJSONObject(i);
					if(vatItemJson != null && vatItemJson.optLong("id") == vatId) {
						fItem.grantVatItem().create(vatItemJson);
						break;
					}
				}
			}
		} catch (Exception e) {
			throw new AEException(e);
		}
	}
	
	@Override
	public AEResponse loadFacture(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject factureJson = arguments.getJSONObject(AEFacture.JSONKey.facture);
			long factureId = factureJson.getLong("id");
			AEDescriptor factureDescr = AEFacture.lazyDescriptor(factureId);
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// load facture
			AEFacture facture = loadFacture(factureDescr, invContext, localConnection);
			
			// validate view
			if(ownerId != facture.getCompany().getDescriptor().getID()) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			// return response
			JSONObject payload = new JSONObject();
			payload.put(AEFacture.JSONKey.facture, facture.toJSONObject());
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEResponse loadByFilter(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject filterJSON = arguments.optJSONObject("filter");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// parse request
			AEFactureFilter filter = new AEFactureFilter();
			filter.create(filterJSON);
			
			// load results
			AEFactureDAO docDAO = daoFactory.getFactureDAO(localConnection);
			AEFacturesList resultsList = docDAO.load(filter);

			// return response
			JSONObject payload = new JSONObject();
			payload.put(AEFacture.JSONKey.factures, resultsList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public void setNextNumber(AEDescriptive descr, AEConnection aeConnection, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			
			// get next number
			long nextNumber = 0;
			AEFactureUtil.ReceiptBook rb = getReceiptBook(descr, localConnection, invContext);
			switch(rb) {
			case DEVIS: 
			case FACTURE: 
			case AVOIR: 
			case BROUILLON: {
				AEFacture facture = (AEFacture) descr;
				AEFactureDAO factDAO = daoFactory.getFactureDAO(localConnection);
				nextNumber = factDAO.getNextNumber(rb, facture.getCompany().getDescriptor());
				break;
			}
			case CLIENT: {
				AEClient client = (AEClient) descr;
				AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
				nextNumber = factDAO.getNextNumberClient(client.getCompany().getDescriptor());
				break;
			}
			case ARTICLE: {
				AEArticle article = (AEArticle) descr;
				AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
				nextNumber = factDAO.getNextNumberArticle(article.getCompany().getDescriptor());
				break;
			}
			default: 
				nextNumber = 0;
				break;
			}
			
			// format
			String nextNumberString = rb.toString(nextNumber);
			
			if(descr instanceof AEFacture) {
				AEFacture facture = (AEFacture) descr;
				facture.setNumber(nextNumber);
				facture.setNumberString(nextNumberString);
			} else if(descr instanceof AEClient) {
				AEClient o = (AEClient) descr;
				if(AEStringUtil.isEmpty(o.getCode())) {
					o.setCode(nextNumberString);
					o.setCodeNumber(nextNumber);
				} else {
					o.setCodeNumber(0);
				}
			} else if(descr instanceof AEArticle) {
				AEArticle o = (AEArticle) descr;
				if(AEStringUtil.isEmpty(o.getCode())) {
					o.setCode(nextNumberString);
					o.setCodeNumber(nextNumber);
				} else {
					o.setCodeNumber(0);
				}
			}
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private void setCurrentNumber(AEDescriptive descr, AEConnection aeConnection, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			
			// get current number
			if(descr instanceof AEFacture) {
				AEFacture facture = (AEFacture) descr;
				AEFactureDAO factDAO = daoFactory.getFactureDAO(localConnection);
				facture.setNumber(factDAO.getCurrentNumber(facture.getDescriptor()));
			}
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private AEFactureUtil.ReceiptBook getReceiptBook(AEDescriptive descr, AEConnection aeConnection, AEInvocationContext invContext) {
		AEFactureUtil.ReceiptBook rb = AEFactureUtil.ReceiptBook.NA;
		
		if(descr instanceof AEClient) {
			rb = AEFactureUtil.ReceiptBook.CLIENT;
		} else if(descr instanceof AEArticle) {
			rb = AEFactureUtil.ReceiptBook.ARTICLE;
		} else if(descr instanceof AEFacture) {
			AEFacture facture = (AEFacture) descr;
			if(AEDocumentType.System.AEDevisSale.getSystemID() == facture.getType().getSystemID()) {
				rb = AEFactureUtil.ReceiptBook.DEVIS;
			} else if(AEDocumentType.System.AEFactureSale.getSystemID() == facture.getType().getSystemID()) {
				if(AEFactureUtil.FactureSubType.REGULAR.equals(facture.getSubType())
						|| AEFactureUtil.FactureSubType.ADVANCE.equals(facture.getSubType())) {
					
					if(AEFactureUtil.FactureState.DRAFT.equals(facture.getState())) {
						rb = AEFactureUtil.ReceiptBook.BROUILLON;
					} else if(AEFactureUtil.FactureState.VALIDATED.equals(facture.getState()) ||
							AEFactureUtil.FactureState.PAID.equals(facture.getState())) {
						
						rb = AEFactureUtil.ReceiptBook.FACTURE;
					}
				} else if(AEFactureUtil.FactureSubType.CREDIT_NOTE.equals(facture.getSubType())) {
					rb = AEFactureUtil.ReceiptBook.AVOIR;
				}
			}
		}
		
		return rb;
	}
	
	public void createPayments(AEFacture facture, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		// advance payment
		if(facture.getAdvanceMode() != null 
				&& facture.getAdvanceMode() != AEFactureUtil.PercentAmountMode.NA 
				&& !AEMath.isZeroAmount(facture.getAdvanceAmount())) {
			
			AEPayment advancePayment = new AEPayment();
			
			advancePayment.setToFacture(facture.getDescriptor());
			advancePayment.setPayer(facture.getClient() != null ? facture.getClient().getDescriptor() : null);
			advancePayment.setPaymentTerms(facture.getPaymentTerms());
			advancePayment.setPayableType(facture.getInitialAdvancePayableType());
			advancePayment.setPaymentType(AEFactureUtil.PaymentType.ADVANCE);
			advancePayment.setDate(facture.getDate());
			if(advancePayment.getPaymentTerms() instanceof AEPaymentTerms) {
				AEPaymentTerms pTerms = (AEPaymentTerms) advancePayment.getPaymentTerms();
				advancePayment.setDueDate(pTerms.applyTerms(facture.getDate()));
			} else {
				advancePayment.setDueDate(facture.getDate());
			}
			advancePayment.setAmount(facture.getAdvanceAmount());
			
			facture.grantPayments().add(advancePayment);
		}
		
		// balance		
		AEPayment balancePayment = new AEPayment();
		balancePayment.setToFacture(facture.getDescriptor());
		balancePayment.setPayer(facture.getClient() != null ? facture.getClient().getDescriptor() : null);
		balancePayment.setPaymentTerms(facture.getPaymentTerms());
		if(facture.getSubType() == AEFactureUtil.FactureSubType.ADVANCE) {
			// create the one payment advance facture
			balancePayment.setPayableType(AEFactureUtil.PayableType.JUNK);
			balancePayment.setPaid(true);
			balancePayment.setPaidDate(facture.getDate());
		} else {
			balancePayment.setPayableType(facture.getInitialPayableType());
		}
		balancePayment.setPaymentType(AEFactureUtil.PaymentType.BALANCE);
		balancePayment.setDate(facture.getDate());
		if(facture.isFacture()) {
			if(balancePayment.getPaymentTerms() instanceof AEPaymentTerms) {
				AEPaymentTerms pTerms = (AEPaymentTerms) balancePayment.getPaymentTerms();
				balancePayment.setDueDate(pTerms.applyTerms(facture.getDate()));
			} else {
				balancePayment.setDueDate(facture.getDate());
			}
		}
		
		facture.grantPayments().add(balancePayment);
		
		// at the end
		facture.grantPayments().distributeAmount(facture.getAmount());
	}
	
	public void updatePayments(AEFacture facture, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get payments
			AEPaymentsList pList = facture.getPayments();
			
			// advance payment
			double advanceAmount = 0.0;
			if(facture.hasAdvance()) {
				// the facture has advance payment
				advanceAmount = facture.getAdvanceAmount();
				AEPayment advancePayment = pList.getAdvancePayment();
				if(advancePayment == null) {
					// create advance payment
					advancePayment = new AEPayment();

					// initial update
					advancePayment.setPayableType(facture.getInitialAdvancePayableType());
					advancePayment.setPaymentType(AEFactureUtil.PaymentType.ADVANCE);

					pList.add(advancePayment);
				} else {
					advancePayment.setUpdated();
				}
				
				// update advance payment
				advancePayment.setToFacture(facture.getDescriptor());
				advancePayment.setPayer(facture.getClient() != null ? facture.getClient().getDescriptor() : null);
				advancePayment.setPaymentTerms(facture.getPaymentTerms());
				advancePayment.setDate(facture.getDate());
				if(advancePayment.getPaymentTerms() instanceof AEPaymentTerms) {
					AEPaymentTerms pTerms = (AEPaymentTerms) advancePayment.getPaymentTerms();
					advancePayment.setDueDate(pTerms.applyTerms(facture.getDate()));
				} else {
					advancePayment.setDueDate(facture.getDate());
				}
				advancePayment.setAmount(advanceAmount);
			} else {
				// the facture has not advance payment
				AEPayment advancePayment = pList.getAdvancePayment();
				if(advancePayment != null) {
					advancePayment.setPersistentState(AEPersistent.State.DELETED);
				}
			}
			
			// regular payments
			AEPaymentsList payList = pList.getRegularPayments();
			for (Iterator<AEPayment> iterator = payList.iterator(); iterator.hasNext();) {
				AEPayment reqularPayment = (AEPayment) iterator.next();
				switch(reqularPayment.getPersistentState()) {
				case NEW:
					// should be initialized 
					reqularPayment.setPayableType(facture.getInitialPayableType());
					reqularPayment.setDate(facture.getDate());
					// dont use break, the iitialization should continue
				case UPDATED:
					if(facture.isFacture()) {
						if(reqularPayment.getPaymentTerms() instanceof AEPaymentTerms) {
							AEPaymentTerms pTerms = (AEPaymentTerms) reqularPayment.getPaymentTerms();
							reqularPayment.setDueDate(pTerms.applyTerms(facture.getDate()));
						}
					}
					break;
				case DELETED:
					break;
				default:
					break;
				}
			}
			
			// balance
			AEPayment balancePayment = pList.getBalancePayment();
			if(balancePayment == null) {
				balancePayment = new AEPayment();
				balancePayment.setPayableType(facture.getInitialPayableType());
				balancePayment.setPaymentType(AEFactureUtil.PaymentType.BALANCE);
				pList.add(balancePayment);
			} else {
				if(facture.isFormalFacture()) {
					balancePayment.setPayableType(AEFactureUtil.PayableType.REQUISITION);
				}
				balancePayment.setUpdated();
			}
			
			// update balance payment
			balancePayment.setToFacture(facture.getDescriptor());
			balancePayment.setPayer(facture.getClient() != null ? facture.getClient().getDescriptor() : null);
			balancePayment.setPaymentTerms(facture.getPaymentTerms());
			balancePayment.setDate(facture.getDate());
			if(facture.isFacture()) {
				if(balancePayment.getPaymentTerms() instanceof AEPaymentTerms) {
					AEPaymentTerms pTerms = (AEPaymentTerms) balancePayment.getPaymentTerms();
					balancePayment.setDueDate(pTerms.applyTerms(facture.getDate()));
				} else {
					balancePayment.setDueDate(facture.getDate());
				}
			}

			// at the end
			facture.setPayments(pList);
			facture.grantPayments().distributeAmount(facture.getAmount());
		} catch (AEException e) {
			throw e;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadPaymentsByFilter(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject filterJSON = arguments.optJSONObject("filter");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// parse request
			AEPaymentsFilter filter = new AEPaymentsFilter();
			filter.create(filterJSON);
			
			// load results
			AEPaymentDAO paymentDAO = daoFactory.getPaymentDAO(localConnection);
			AEPaymentsList resultsList = paymentDAO.load(filter);

			// return response
			JSONObject payload = new JSONObject();
			payload.put(AEPayment.JSONKey.payments, resultsList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private void createDateOfExpiry(AEFacture facture) {
		Date dateOfExpiry = null;
		if(facture.isDevis()) {
			dateOfExpiry = AEDateUtil.getClearDateTime(AEDateUtil.addDaysToDate(facture.getDate(), 30));
		} else if(facture.isFacture()) {
			AEPaymentTerms pTerms = facture.getPaymentTerms();
			if(pTerms != null) {
				dateOfExpiry = AEDateUtil.getClearDateTime(pTerms.applyTerms(facture.getDate()));
			}
		}
		facture.setDateOfExpiry(dateOfExpiry);
	}

	@Override
	public AEResponse devisTransitionToAccepted(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long devisId = arguments.optLong("id");
			long ownerId = arguments.optLong("sOwnerId");
			boolean createFacture = arguments.optBoolean("createFacture");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * validate owner
			 */
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConnection);
			factureDAO.validateOwner(devisId, ownerId);
			
			/**
			 * Lazzy load devis (only devis)
			 */
			AEFacture devis = factureDAO.load(AEFacture.lazyDescriptor(devisId));
			if(devis == null) {
				throw new AEException("System error: The devis cannot be found");
			}
			
			/**
			 * validate transition
			 */
			if(AEDocumentType.System.AEDevisSale.getSystemID() != devis.getType().getSystemID()
					|| !AEFactureUtil.FactureState.IN_PROGRESS.equals(devis.getState())) {
				
				throw AEError.System.FACTURATION_TRANSITION_ERROR.toException();
			}
			
			/**
			 * Begin transaction
			 */
			localConnection.beginTransaction();
			
			/**
			 * Execute transition
			 */
			AEPaymentDAO paymentDAO = daoFactory.getPaymentDAO(localConnection);
			AEFactureDescr devisDescr = (AEFactureDescr) devis.getDescriptor();
			AEPaymentsList pList = paymentDAO.loadToFacture(devisDescr);
			
			// advance payment
			if(devis.hasAdvance()) {
				// set advance payment as Reqisition
				AEPayment advPayment = pList.getAdvancePayment();
				if(advPayment != null) {
					advPayment.setPayableType(AEFactureUtil.PayableType.REQUISITION);
				} else {
					throw new AEException("System error: Advance payment cannot be found");
				}

				if(createFacture) {
					// create and save advance facture
					AEFacture advFacture = createAdvanceFacture(devisDescr, advPayment, invContext, localConnection);
					saveFacture(advFacture, invContext, localConnection);
					
					// update advance payment
					advPayment.setPaid(true);
					advPayment.setPaidDate(new Date());
				}
				
				// update advance payment
				paymentDAO.update(advPayment);
			}
			
//			// regular payments
//			AEPaymentsList payList = pList.getRegularPayments();
//			if(!payList.isEmpty()) {
//				for (Iterator<AEPayment> iterator = payList.iterator(); iterator.hasNext();) {
//					AEPayment reqularPayment = (AEPayment) iterator.next();
//					reqularPayment.setPayableType(AEFactureUtil.PayableType.REQUISITION);
//
//					// update regular payment
//					paymentDAO.update(reqularPayment);
//				}
//
//				// balance payment
//				AEPayment balancePayment = pList.getBalancePayment();
//				if(balancePayment != null) {
//					balancePayment.setPayableType(AEFactureUtil.PayableType.REQUISITION);
//					
//					// update regular payment
//					paymentDAO.update(balancePayment);
//				} else {
//					throw new AEException("System error: Balance payment cannot be found");
//				}
//			}
			
			// set the facture as accepted
			devis.setState(AEFactureUtil.FactureState.ACCEPTED);
			factureDAO.update(devis);
			
			/**
			 * After transition
			 */
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			/**
			 *  Return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEFacture.JSONKey.facture, devis.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
	 * Saves specified <code>facture</code>.
	 * 
	 * @param facture
	 * @param invContext
	 * @param aeConnection
	 * @return
	 * @throws AEException
	 */
	private AEResponse saveFacture(AEFacture facture, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			
			/**
			 * Validate 
			 * 
			 */
			facture.validateWith(AEFactureSaveValidator.getInstance());
			
			// validate in opened period
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			boolean bRet = false;
			if(!facture.isTemplate()) {
				// check closed period
				if(AEDocumentType.valueOf(AEDocumentType.System.AEFactureSale).equals(facture.getType())) {
					bRet = accPeriodDAO.isInClosedPeriod(
							facture.getDate(), 
							facture.getCompany() != null ? 
									facture.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
							AEApp.FACTURATION_FACTURE);
				}
			}
			if(bRet) {
				throw new AEException(
						(int) AEError.System.CANNOT_INSERT_UPDATE_CLOSED_PERIOD.getSystemID(), 
						"Période close, veuillez saisir la date de facture en reference!!");
			}
			
			/**
			 * Prepare Save
			 */
			facture.calculate();
			facture.calculateAdvance();
			
			/**
			 * Save Facture in transaction
			 */
			localConnection.beginTransaction();
			switch(facture.getPersistentState()) {
			case NEW:
				insert(facture, invContext, localConnection);
				break;
			case UPDATED:
				update(facture, invContext, localConnection);
				break;
			case DELETED:
				// internal error
				throw new AEException("System error: Delete is not an available operation");
			default:
				// may be internal error
				assert(false);
				break;
			}
			
			/**
			 * Proccess items
			 */
			processItems(facture, invContext, localConnection);
			
			/**
			 * Proccess payments
			 */
			processPayments(facture, invContext, localConnection);
			
			/**
			 * After Save
			 */
			// recalculate paid amount
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConnection);
			if(facture.isFacture()) {
				factureDAO.updatePaidAmount(facture.getID());
			} else if(facture.isDevis()) {
				factureDAO.updateInvoicedAmount(facture.getID());
			}
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put(AEFacture.JSONKey.facture, facture.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
	 * Load facture, only facture and items
	 * 
	 * @param factureDescr
	 * @param invContext
	 * @param aeConnection
	 * @return
	 * @throws AEException
	 */
	private AEFacture loadFacture(AEDescriptor factureDescr, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			// load facture
			AEFactureDAO docDAO = daoFactory.getFactureDAO(localConnection);
			AEFacture facture = docDAO.load(factureDescr);

			if(facture != null) {
				// load items
				AEFactureItemDAO itemsDAO = daoFactory.getFactureItemDAO(localConnection);
				facture.setFactureItems(itemsDAO.loadToFacture(factureDescr));
				
				// load paymets
				AEPaymentDAO paymentDAO = daoFactory.getPaymentDAO(localConnection);
				facture.setPayments(paymentDAO.loadToFacture(factureDescr));
			}

			// after load
			facture.calcLocked();
			
			// return response
			return facture;
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
//	private AEFacture loadFactureFull(AEDescriptor factureDescr, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
//		AEConnection localConnection = null;
//		try {
//			// get connection
//			DAOFactory daoFactory = DAOFactory.getInstance();
//			localConnection = daoFactory.getConnection(
//					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
//			
//			// load facture and items
//			AEFacture facture = loadFacture(factureDescr, invContext, localConnection);
//			
//			// load payments
//			AEPaymentDAO paymentsDAO = daoFactory.getPaymentDAO(localConnection);
//			facture.setPayments(paymentsDAO.loadToFacture(factureDescr));
//			
//			// return response
//			return facture;
//		} catch (Throwable t) {
//			throw new AEException(t.getMessage(), t);
//		} finally {
//			AEConnection.close(localConnection);
//		}
//	}
	
	/**
	 * Create Advance facture over specified <code>devis</code>.
	 */
	private AEFacture createAdvanceFacture(AEFactureDescr factDescr, AEPayment advPayment, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		
		/**
		 * Load the facture to be used as template
		 */
		AEFacture factureTemplate = loadFacture(factDescr, invContext, aeConnection);
		
		// Predicate validation: advanced facture is created only for unpaid advance payment in unformal facture
		if(factureTemplate == null || factureTemplate.isFormalFacture() || !factureTemplate.hasAdvance()
				|| advPayment == null || advPayment.isPaid() || !AEFactureUtil.PaymentType.ADVANCE.equals(advPayment.getPaymentType())) {
			
			throw new AEException("System error: Advance invoice cannot be created: Unconsistent internal state");
		}
		
		/**
		 * Keep some data before create
		 */
		double advAmount = advPayment.getAmount();
		
		/**
		 * Reset devis as new instance
		 */
		factureTemplate.resetAsNew();
		
		/**
		 * Init facture
		 */
		factureTemplate.setType(AEDocumentType.valueOf(AEDocumentType.System.AEFactureSale));
		factureTemplate.setSubType(AEFactureUtil.FactureSubType.ADVANCE);
		factureTemplate.setState(AEFactureUtil.FactureState.PAID);
		factureTemplate.setCode(null);
		factureTemplate.setName(null);
		factureTemplate.setDescription("Acompte sur devis No." + factDescr.getDescription());
		factureTemplate.setProperties(0);
		factureTemplate.setDateOfExpiry(new Date());
		// client is the same
		factureTemplate.setSrcDoc(factDescr);
		factureTemplate.setNumber(0);
		factureTemplate.setNumberString(null);
		factureTemplate.setDate(new Date());
		factureTemplate.setJournal(null);
		factureTemplate.setShipping(null);
		factureTemplate.setItemsDiscountAmount(0.0);
		factureTemplate.setDiscountAmount(0.0);
		factureTemplate.setAdvanceMode(AEFactureUtil.PercentAmountMode.NA);
		factureTemplate.setAdvanceAmount(0.0);
		factureTemplate.setAdvancePercent(0.0);
		factureTemplate.setBrutoAmount(0.0);
		factureTemplate.setTaxableAmount(0.0);
		factureTemplate.setVatAmount(0.0);
		factureTemplate.setAmount(0.0);
		
		// items
		AEFactureItemsList itemsList = factureTemplate.grantFactureItems();
		itemsList.clear();
		
		// create the one advance item
		AEFactureItem advItem = new AEFactureItem();
		advItem.setName("Acompte");
		advItem.setDescription(factDescr.getDescription());
		advItem.setTypeItem( AEFactureUtil.FactureItemType.ADVANCE);
		if(factDescr.isDevis()) {
			advItem.setPaymentInvoiced(advPayment.getDescriptor());
		}
		advItem.setQty(1);
		advItem.setUom(UnitOfMeasurement.getByID(UnitOfMeasurement.piece2.getID()));
		// VAT
		String vatCode = AEApp.getInstance().getProperties().getProperty(AEApp.defaultVatCode);
		vatCode = AEStringUtil.trim(vatCode);
		DAOFactory daoFactory = DAOFactory.getInstance();
		VATItemDAO vatItemsDAO = daoFactory.getVATItemDAO(aeConnection);
		JSONObject vatItemJson = null;
		try {
			JSONArray vatItems = vatItemsDAO.load();
			for (int i = 0; i < vatItems.length(); i++) {
				JSONObject nextItemJson = vatItems.optJSONObject(i);
				if(nextItemJson != null && vatCode.equalsIgnoreCase(nextItemJson.optString("code"))) {
					vatItemJson = nextItemJson;
					break;
				}
			}
			if(vatItemJson != null) {
				advItem.setVatId(vatItemJson.optLong("id"));
				advItem.grantVatItem().create(vatItemJson);
			} else {
				throw new AEException("System error: Default VAT cannot be found");
			}
		} catch (JSONException e) {
			throw new AEException(e);
		}
		advItem.setPriceInVat(advAmount);
		advItem.setPriceInVatPinned(false);
		
		// add the row
		itemsList.add(advItem);
		
		// calculate facture
		factureTemplate.calculate();
		
		return factureTemplate;
	}

	/**
	 * Creates invoice over specified paymet
	 * 
	 * @param devisDescr
	 * @param payment
	 * @param invContext
	 * @param aeConnection
	 * @return
	 * @throws AEException
	 */
	private AEFacture createFacture(AEFactureDescr devisDescr, AEPayment payment, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		
		/**
		 * Load the facture to be used as template
		 */
		AEFacture factureTemplate = loadFacture(devisDescr, invContext, aeConnection);
		
		// Predicate validation: invoice can be created only for not invoiced and not advance devises schedule 
		if(factureTemplate == null || payment == null) {
			throw new AEException("System error: Invoice cannot be created: Unconsistent internal state");
		}
		if(!factureTemplate.isDevis() || !FactureState.ACCEPTED.equals(factureTemplate.getState())) {
			throw AEError.System.FACTURATION_TRANSITION_ERROR.toException();
		}
		if(!PayableType.SCHEDULE.equals(payment.getPayableType()) || PaymentType.ADVANCE.equals(payment.getPaymentType())
				|| payment.isPaid()) {
			
			throw new AEException("System error: Invoice cannot be created: Unconsistent payment state");
		}
		
		/**
		 * Keep some data before create
		 */
		double factAmount = factureTemplate.getAmount();
		double paymentAmount = payment.getAmount();

		String factDescription = null;
		if(AEFactureUtil.PaymentType.REGULAR.equals(payment.getPaymentType())) {
			factDescription = new StringBuilder("Facturation selon avancée des travaux ")
			.append(AEMath.isZeroAmount(factAmount) ? AEStringUtil.EMPTY_STRING : AEMath.toRateString(paymentAmount / factAmount * 100))
			.append("% - Devis N° ") 
			.append(devisDescr.getNumber()).toString();
		} else if(AEFactureUtil.PaymentType.BALANCE.equals(payment.getPaymentType())) {
			factDescription = new StringBuilder("Appel de solde – Devis N° ") 
				.append(devisDescr.getNumber()).toString();
		} else {
			factDescription = AEStringUtil.EMPTY_STRING;
		}
		
		/**
		 * Reset devis as new instance
		 */
		factureTemplate.resetAsNew();
		
		/**
		 * Init facture
		 */
		factureTemplate.setType(AEDocumentType.valueOf(AEDocumentType.System.AEFactureSale));
		factureTemplate.setSubType(AEFactureUtil.FactureSubType.REGULAR);
		factureTemplate.setState(AEFactureUtil.FactureState.DRAFT);
		factureTemplate.setCode(null);
		factureTemplate.setName(null);
		factureTemplate.setDescription(factDescription);
		factureTemplate.setProperties(0);
		factureTemplate.setDateOfExpiry(new Date());
		factureTemplate.setPaymentTerms((AEPaymentTerms)payment.getPaymentTerms());
		createDateOfExpiry(factureTemplate);
		// client is the same
		factureTemplate.setSrcDoc(devisDescr);
		factureTemplate.setNumber(0);
		factureTemplate.setNumberString(null);
		factureTemplate.setDate(new Date());
		factureTemplate.setJournal(null);
		factureTemplate.setShipping(null);
		factureTemplate.setItemsDiscountAmount(0.0);
		factureTemplate.setDiscountAmount(0.0);
		factureTemplate.setAdvanceMode(AEFactureUtil.PercentAmountMode.NA);
		factureTemplate.setAdvanceAmount(0.0);
		factureTemplate.setAdvancePercent(0.0);
		factureTemplate.setBrutoAmount(0.0);
		factureTemplate.setTaxableAmount(0.0);
		factureTemplate.setVatAmount(0.0);
		factureTemplate.setAmount(0.0);
		
		// items
		AEFactureItemsList itemsList = factureTemplate.grantFactureItems();
		itemsList.clear();
		
		// payments
		AEPaymentsList paymentsList = factureTemplate.grantPayments();
		paymentsList.clear();
		
		// create the one advance item
		AEFactureItem advItem = new AEFactureItem();
		advItem.setName(factDescription);
		advItem.setDescription(devisDescr.getDescription());
		advItem.setTypeItem(AEFactureUtil.FactureItemType.ADVANCE);
		advItem.setPaymentInvoiced(payment.getDescriptor());
		advItem.setQty(1);
		advItem.setUom(UnitOfMeasurement.getByID(UnitOfMeasurement.piece2.getID()));
		
		// VAT
		String vatCode = AEApp.getInstance().getProperties().getProperty(AEApp.defaultVatCode);
		vatCode = AEStringUtil.trim(vatCode);
		DAOFactory daoFactory = DAOFactory.getInstance();
		VATItemDAO vatItemsDAO = daoFactory.getVATItemDAO(aeConnection);
		JSONObject vatItemJson = null;
		try {
			JSONArray vatItems = vatItemsDAO.load();
			for (int i = 0; i < vatItems.length(); i++) {
				JSONObject nextItemJson = vatItems.optJSONObject(i);
				if(nextItemJson != null && vatCode.equalsIgnoreCase(nextItemJson.optString("code"))) {
					vatItemJson = nextItemJson;
					break;
				}
			}
			if(vatItemJson != null) {
				advItem.setVatId(vatItemJson.optLong("id"));
				advItem.grantVatItem().create(vatItemJson);
			} else {
				throw new AEException("System error: Default VAT cannot be found");
			}
		} catch (JSONException e) {
			throw new AEException(e);
		}
		advItem.setPriceInVat(paymentAmount);
		advItem.setPriceInVatPinned(false);
		
		// add the row
		itemsList.add(advItem);
		
		// calculate facture
		factureTemplate.calculate();
		
		return factureTemplate;
	}
	
	@Override
	public AEResponse devisTransitionToInvoiced(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long devisId = arguments.optLong("id");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * validate owner
			 */
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConnection);
			factureDAO.validateOwner(devisId, ownerId);
			
			/**
			 * Lazzy load facture (only facture)
			 */
			AEFacture devis = factureDAO.load(AEFacture.lazyDescriptor(devisId));
			if(devis == null) {
				throw new AEException("System error: The devis cannot be found");
			}
			AEDescriptor devisDescr = devis.getDescriptorShort();
			
			/**
			 * Validate Transition
			 */
			if(!AEFactureUtil.FactureState.ACCEPTED.equals(devis.getState())) {
				throw AEError.System.FACTURATION_TRANSITION_ERROR.toException();
			}
			AEPaymentDAO paymentDAO = daoFactory.getPaymentDAO(localConnection);
			AEPaymentsList paymentsList = paymentDAO.loadToFacture(devisDescr);
			for (AEPayment aePayment : paymentsList) {
				if(PaymentType.REGULAR.equals(aePayment.getPaymentType())) {
					throw AEError.System.FACTURATION_TRANSITION_ERROR.toException();
				}
			}
			
			/**
			 * Begin transaction
			 */
			localConnection.beginTransaction();
			
			/**
			 * Execute transition
			 */
			
			// create facture but don't save it
			// because of the additional initialization
			AEFacture facture = createFacture(devisDescr, invContext, localConnection);
			
			// process advance payment
			paymentDAO = daoFactory.getPaymentDAO(localConnection);
			AEPayment advDevisPayment = paymentDAO.loadAdvancePaymentToFacture(devisDescr);
			
			// validate internal devis state
			if(advDevisPayment != null ^ devis.hasAdvance()) {
				throw new AEException("System error: The devis'es advance payment inconsistency");
			}
			
			if(advDevisPayment != null) {
				// set devis's advance payment as schedule
				// the advance requisition will be transfered to invoice
				advDevisPayment.setPayableType(AEFactureUtil.PayableType.SCHEDULE);
				paymentDAO.updatePayableType(AEFactureUtil.PayableType.SCHEDULE.getTypeId(), advDevisPayment.getID());
				
				// there is advance payment to be processed
				if(advDevisPayment.isPaid()) {
					// advance payment is paid, so create advance deduction
					
					// remove advance from facture
					facture.setAdvanceMode(AEFactureUtil.PercentAmountMode.NA);
					facture.setAdvanceAmount(0.0);
					facture.setAdvancePercent(0.0);
					
					// set the advanced and invoiced devises payments as Requisition
					AEPaymentsList advInvoicedToDevis = paymentDAO.loadAdvancedInvoicedToDevise(devis.getDescriptor());
					for (Iterator<AEPayment> iterator = advInvoicedToDevis.iterator(); iterator.hasNext();) {
						AEPayment p = (AEPayment) iterator.next();
						p.setPayableType(AEFactureUtil.PayableType.REQUISITION);
						paymentDAO.updatePayableType(AEFactureUtil.PayableType.REQUISITION.getTypeId(), p.getID());
					}
					
					// create advance items deduction
					AEFactureItemDAO itemDAO = daoFactory.getFactureItemDAO(localConnection);
					AEFactureItemsList advanceItemsList = itemDAO.loadAdvancedInvoicedToDevis(devisDescr);
					for (Iterator<AEDocumentItem> iterator = advanceItemsList.iterator(); iterator.hasNext();) {
						AEFactureItem item = (AEFactureItem) iterator.next();
						item.setItemDeducted(item.getDescriptor()); // must be before resetAsNew to keep original item Id
						item.resetAsNew();
						item.setTypeItem(AEFactureUtil.FactureItemType.ADVANCE_DEDUCTION);
						item.setQty(item.getQty() * (-1.0));
					}
					facture.grantFactureItems().addAll(advanceItemsList);
				}
			}
			
			// set the devis as invoiced
			devis.setState(AEFactureUtil.FactureState.INVOICED);
			factureDAO.updateState(AEFactureUtil.FactureState.INVOICED.getStateId(), devis.getID());
			
			// finally save facture
			// this method will create payments
			saveFacture(facture, invContext, localConnection);
			
			/**
			 * After transition
			 */
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			/**
			 *  Return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEFacture.JSONKey.facture, devis.toJSONObject());
			payload.put(
					AEFacture.JSONKey.createdFacture, 
					facture != null ? facture.getDescriptor().getDescription() : "XXX - dd/mm/yyyy");
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
	 * Creates Facture over specified <code>devis</code>.
	 */
	private AEFacture createFacture(AEDescriptor devisDescr, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		
		/**
		 * load the advise to be used as template
		 */
		AEFacture facture = loadFacture(devisDescr, invContext, aeConnection);
		
		// paranoic internal state validation 
		if(!(facture.isDevis())) {
			throw new AEException("System error: The devis cannot be invoiced: Unconsistent internal state");
		}
		
		/**
		 * Keep some data before create
		 */
//		double advAmount = facture.getAdvanceAmount();
		
		/**
		 * Reset devis as new instance
		 */
		facture.resetAsNew();
		
		/**
		 * Init facture
		 */
		facture.setType(AEDocumentType.valueOf(AEDocumentType.System.AEFactureSale));
		facture.setSubType(AEFactureUtil.FactureSubType.REGULAR);
		facture.setState(AEFactureUtil.FactureState.DRAFT);
//		facture.setCode(null);
//		facture.setName(null);
		facture.setDescription("Facture sur base devis No." + devisDescr.getDescription());
//		facture.setProperties(0);
		if(facture.getPaymentTerms() != null) {
			facture.setDateOfExpiry(facture.getPaymentTerms().applyTerms(new Date()));
		}
		// client is the same
		facture.setSrcDoc(devisDescr);
		facture.setNumber(0);
		facture.setNumberString(null);
		facture.setDate(new Date());
		facture.setJournal(null);
//		facture.setShipping(null);
		facture.setItemsDiscountAmount(0.0);
		facture.setDiscountAmount(0.0);
//		facture.setAdvanceMode(AEFactureUtil.PercentAmountMode.NA);
//		facture.setAdvanceAmount(0.0);
//		facture.setAdvancePercent(0.0);
		facture.setBrutoAmount(0.0);
		facture.setTaxableAmount(0.0);
		facture.setVatAmount(0.0);
		facture.setAmount(0.0);
//		facture.setPaidAdvance(advAmount);
		
		// items
		AEFactureItemsList itemsList = facture.grantFactureItems();
		for (Iterator<AEDocumentItem> iterator = itemsList.iterator(); iterator.hasNext();) {
			AEFactureItem item = (AEFactureItem) iterator.next();
			item.resetAsNew();
			item.setDocument(facture.getDescriptor());
		}
		
		// payments
		AEPaymentsList paymentsList = facture.getPayments();
		for (Iterator<AEPayment> iterator = paymentsList.iterator(); iterator.hasNext();) {
			AEPayment payment = (AEPayment) iterator.next();
			if(AEFactureUtil.PaymentType.REGULAR.equals(payment.getPaymentType())
					|| AEFactureUtil.PaymentType.BALANCE.equals(payment.getPaymentType())) {
				
				payment.resetAsNew();
				payment.setPayableType(AEFactureUtil.PayableType.REQUISITION);
			}
		}
		
		// calculate facture
		facture.calculate();
		
		return facture;
	}

	@Override
	public AEResponse factureTransitionToValidated(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long factureId = arguments.optLong("id");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * validate owner
			 */
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConnection);
			factureDAO.validateOwner(factureId, ownerId);
			
			/**
			 * Lazzy load facture (only facture)
			 */
			AEFacture facture = loadFacture(AEFacture.lazyDescriptor(factureId), invContext, localConnection);
			if(facture == null) {
				throw new AEException("System error: The facture cannot be found");
			}
			
			/**
			 * Validate Transition
			 */
			if(!AEFactureUtil.FactureState.DRAFT.equals(facture.getState())) {
				throw AEError.System.FACTURATION_TRANSITION_ERROR.toException();
			}
			
			/**
			 * Begin transaction
			 */
			localConnection.beginTransaction();
			
			/**
			 * Execute transition
			 */
			
			// reset the number, it should be generared during update
			facture.setNumberString(null);
			
			// set the facture as validated
			facture.setState(AEFactureUtil.FactureState.VALIDATED);
			
			// update facture
			facture.setUpdated();
			saveFacture(facture, invContext, localConnection);
			
			/**
			 * After transition
			 */
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			/**
			 *  Return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEFacture.JSONKey.facture, facture.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse payPayment(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long paymentId = arguments.optLong("id");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * validate owner
			 */
			AEPaymentDAO paymentDAO = daoFactory.getPaymentDAO(localConnection);
			paymentDAO.validateOwner(paymentId, ownerId);
			
			/**
			 * Lazzy load payment
			 */
			AEPayment payment = paymentDAO.loadFull(AEPayment.lazyDescriptor(paymentId));
			if(payment == null) {
				throw new AEException("System error: The payment cannot be found");
			}
			
			/**
			 * Validate paying
			 */
			if(AEFactureUtil.PayableType.REQUISITION != payment.getPayableType() || payment.isPaid()) {
				throw AEError.System.FACTURATION_PAYMENT_PAY_ERROR.toException();
			}
			
			/**
			 * Begin transaction
			 */
			localConnection.beginTransaction();
			
			/**
			 * Execute transition
			 */
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConnection);
			AEFactureDescr toDocDescr = (AEFactureDescr) payment.getToFacture(); 
			if(AEFactureUtil.PaymentType.ADVANCE == payment.getPaymentType() && !toDocDescr.isFormalFacture()) {	
				// this is unpaid advance payment to unformal facture
				// so create advance facture
				AEFacture advFacture = createAdvanceFacture(
						(AEFactureDescr) payment.getToFacture().getDescriptor(), 
						payment, 
						invContext, 
						localConnection);
				saveFacture(advFacture, invContext, localConnection);
				
				// process the "draft facture" advance paying case
				if(AEDocumentType.System.AEFactureSale.getSystemID() == toDocDescr.getDocumentType().getSystemID()
						&& AEFactureUtil.FactureState.DRAFT == toDocDescr.getState()) {
					
					// update draft facture
					AEFacture draftFacture = loadFacture(toDocDescr, invContext, localConnection);
					
					// remove advance from facture
					draftFacture.setAdvanceMode(AEFactureUtil.PercentAmountMode.NA);
					draftFacture.setAdvanceAmount(0.0);
					draftFacture.setAdvancePercent(0.0);
					
					// set the advanced and invoiced devises payments as Requisition
					AEPaymentsList advInvoicedToDraftFacture = paymentDAO.loadAdvancedInvoicedToDevise(toDocDescr);
					for (Iterator<AEPayment> iterator = advInvoicedToDraftFacture.iterator(); iterator.hasNext();) {
						AEPayment p = (AEPayment) iterator.next();
						p.setPayableType(AEFactureUtil.PayableType.REQUISITION);
						paymentDAO.updatePayableType(AEFactureUtil.PayableType.REQUISITION.getTypeId(), p.getID());
					}
					
					// create advance items deduction
					AEFactureItemDAO itemDAO = daoFactory.getFactureItemDAO(localConnection);
					AEFactureItemsList advanceItemsList = itemDAO.loadAdvancedInvoicedToDevis(toDocDescr);
					for (Iterator<AEDocumentItem> iterator = advanceItemsList.iterator(); iterator.hasNext();) {
						AEFactureItem item = (AEFactureItem) iterator.next();
						item.setItemDeducted(item.getDescriptor()); // must be before resetAsNew to keep original item Id
						item.resetAsNew();
						item.setTypeItem(AEFactureUtil.FactureItemType.ADVANCE_DEDUCTION);
						item.setQty(item.getQty() * (-1.0));
					}
					draftFacture.grantFactureItems().addAll(advanceItemsList);
					draftFacture.setUpdated();
					
					// save draft facture
					saveFacture(draftFacture, invContext, localConnection);
				}
			}
			
			// update current payment to paid
			Date paidDate = new Date();
			paymentDAO.updateToPaid(paidDate, payment.getID());
			payment.setPaid(true);
			payment.setPaidDate(paidDate);
			
			// recalculate payment's facture
			factureDAO.updatePaidAmount(payment.getToFacture().getDescriptor().getID());
			
			/**
			 * After transition
			 */
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			/**
			 *  Return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEPayment.JSONKey.payment, payment.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse unpaidPayment(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long paymentId = arguments.optLong("id");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * validate owner
			 */
			AEPaymentDAO paymentDAO = daoFactory.getPaymentDAO(localConnection);
			paymentDAO.validateOwner(paymentId, ownerId);
			
			/**
			 * Lazzy load payment to be unpaid
			 */
			AEPayment payment = paymentDAO.loadFull(AEPayment.lazyDescriptor(paymentId));
			if(payment == null) {
				throw new AEException("System error: The payment cannot be found");
			}
			
			/**
			 * Validate unpaid
			 */
			if(AEFactureUtil.PayableType.REQUISITION != payment.getPayableType() || !payment.isPaid()) {
				throw AEError.System.FACTURATION_PAYMENT_PAY_ERROR.toException();
			}
			
			/**
			 * Begin transaction
			 */
			localConnection.beginTransaction();
			
			/**
			 * Process
			 */
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConnection);
			AEFactureDescr toDocumentDescr = (AEFactureDescr) payment.getToFacture(); 
			if(AEFactureUtil.FactureSubType.ADVANCE.equals(toDocumentDescr.getSubType())) {
				// this is paid advance payment to facture
				JSONObject deleteFactureArguments = new JSONObject();
				deleteFactureArguments.put("id", toDocumentDescr.getID());
				deleteFactureArguments.put("sOwnerId", ownerId);
				
				AERequest deleteFactureRequest = new AERequest(deleteFactureArguments);
				deleteFactureRequest.setAuthPrincipal(ap);
				
				deleteFacture(deleteFactureRequest, invContext);
			} else {
				// update current payment to unpaid
				paymentDAO.updateToUnpaid(payment.getID());
				payment.setPaid(false);
				payment.setPaidDate(null);

				// recalculate payment's facture
				factureDAO.updatePaidAmount(payment.getToFacture().getDescriptor().getID());
			}
			
			/**
			 * After transition
			 */
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			/**
			 *  Return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEPayment.JSONKey.payment, payment.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
	 * Unconditional deleted specified <code>facture</code>.
	 * All business rules and validation must be processed before!!!
	 * 
	 * @param facture
	 * @param invContext
	 * @param aeConnection
	 * @return
	 * @throws AEException
	 */
	private void deleteFacture(AEFacture facture, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			
			/**
			 * Prepare Delete
			 */
			
			/**
			 * Delete Facture in transaction
			 */
			localConnection.beginTransaction();
			
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConnection);
			factureDAO.delete(facture.getDescriptor());
			
			/**
			 * After Delete
			 */
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse duplicateFacture(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long factureId = arguments.getLong("id");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Duplicate and validate
			 */
			AEFacture facture = duplicateFacture(AEFacture.lazyDescriptor(factureId), invContext, localConnection);
			
			// whether this facture is ownered by this customer
			if(ownerId != facture.getCompany().getDescriptor().getID()) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Save in transaction
			 */
			localConnection.beginTransaction();
			
			saveFacture(facture, invContext, localConnection);
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put("id", facture.getID());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private AEFacture duplicateFacture(AEDescriptor factureDescr, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		
		/**
		 * load the advise to be used as template
		 */
		AEFacture facture = loadFacture(factureDescr, invContext, aeConnection);
		
		// validate duplication
		if(facture.isAdvance()) {
			throw AEError.System.FACTURATION_CANOT_BE_DUPLICATED.toException();
		}
		
		/**
		 * Reset facture as new instance
		 */
		facture.resetAsNew();
		
		/**
		 * Init facture
		 */
		if(facture.isFacture()) {
			facture.setState(AEFactureUtil.FactureState.DRAFT);
		} else if(facture.isDevis()) {
			facture.setState(AEFactureUtil.FactureState.IN_PROGRESS);
		} else {
			throw new AEException("System error: Unknown doc type");
		}
		facture.setNumber(0);
		facture.setNumberString(null);
		facture.setDate(new Date());
		facture.setSrcDoc(null);
		
		// items and items deducted
		boolean hasAdvanceDeduction = false;
		double advAmount = 0.0;
		AEFactureItemsList itemsList = facture.grantFactureItems();
		for (Iterator<AEDocumentItem> iterator = itemsList.iterator(); iterator.hasNext();) {
			AEFactureItem item = (AEFactureItem) iterator.next();
			if(AEFactureUtil.FactureItemType.ADVANCE_DEDUCTION.equals(item.getTypeItem())) {
				advAmount += Math.abs(item.getAmount());
				hasAdvanceDeduction = true;
				iterator.remove();
			} else {
				item.resetAsNew();
				item.setItemDeducted(null);
				item.setPaymentInvoiced(null);
				item.setDocument(facture.getDescriptor());
			}
		}
		if(hasAdvanceDeduction) {
			facture.setAdvanceMode(AEFactureUtil.PercentAmountMode.AMOUNT);
			facture.setAdvanceAmount(AEMath.round(advAmount, 2));
		}

		return facture;
	}

	@Override
	public AEResponse loadPrintInfo(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long factureId = arguments.getLong("id");
			long ownerId = arguments.optLong("sOwnerId");
			long printTemplateId = arguments.optLong(AEFacture.JSONKey.printTemplateId);
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// load facture
			AEDescriptor factureDescr = AEFacture.lazyDescriptor(factureId);
			AEFacture facture = loadFacture(factureDescr, invContext, localConnection);
			
			// load srcDoc in depth
			if(facture.getSrcDoc() != null) {
				AEDescriptor srcDocDescr = AEFacture.lazyDescriptor(facture.getSrcDoc().getDescriptor().getID());
				AEFacture srcDoc = loadFacture(srcDocDescr, invContext, localConnection);
				facture.setSrcDoc(srcDoc.getDescriptor());
			}
			
			// load print template
			AEFacturationDAO facturationDAO = daoFactory.getFacturationDAO(localConnection);
			AEFacturePrintTemplate printTemplate = 
				facturationDAO.loadPrintTemplate(AEFacturePrintTemplate.lazyDescriptor(printTemplateId));
			if(printTemplate == null) {
				printTemplate = new AEFacturePrintTemplate();
				printTemplate.setCompany(Organization.lazyDescriptor(ownerId));
			}
			
			// validate
			if(facture == null || ownerId != facture.getCompany().getDescriptor().getID()) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			if(printTemplate == null || ownerId != printTemplate.getCompany().getDescriptor().getID()) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			// vat items view
			JSONArray vatItemsView = facture.getVatItemsView();
			if(vatItemsView != null && vatItemsView.length() > 0) {
				VATItemDAO vatItemDAO = daoFactory.getVATItemDAO(localConnection);
				for (int i = 0; i < vatItemsView.length(); i++) {
					JSONObject vi = vatItemsView.getJSONObject(i);
					JSONObject viDB = vatItemDAO.load(vi.optLong("id"));
					if(viDB != null) {
						vi.put("option", "(" 
								+ (viDB.optString("code") != null ? viDB.optString("code") : AEStringUtil.EMPTY_STRING) 
								+ ") " 
								+ viDB.optDouble("rate") + "%");
					} else {
						vi.put("option", vi.optDouble("rate") + "%");
					}
				}
			}
			
			// load issuer
			JSONObject issuer = null;
			JSONObject issuerArguments = new JSONObject();
			issuerArguments.put("customerId", facture.getCompany().getDescriptor().getID());
			issuerArguments.put("doNotLoadCAO", true);
			AERequest issuerRequest = new AERequest(issuerArguments);
			AEResponse issuerResponse = partyService.loadCustomers(issuerRequest, invContext);
			if(issuerResponse.getPayload() != null) {
				JSONObject payload = issuerResponse.getPayload();
				JSONArray issuersArray = payload.optJSONArray("customers");
				if(issuersArray.length() > 0) {
					issuer = issuersArray.getJSONObject(0);
				}
			}
			
			// return response
			JSONObject payload = new JSONObject();
			payload.put(AEFacture.JSONKey.facture, facture.toJSONObject());
			payload.put(AEFacture.JSONKey.vatItemsView, vatItemsView);
			payload.put(AEFacture.JSONKey.printTemplate, (Object) printTemplate);
			if(issuer != null) {
				payload.put("issuer", issuer);
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse createCreditNote(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long factureId = arguments.getLong("id");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Duplicate and validate
			 */
			AEFacture creditNote = createCreditNote(AEFacture.lazyDescriptor(factureId), invContext, localConnection);
			
			// whether this note is ownered by this customer
			if(ownerId != creditNote.getCompany().getDescriptor().getID()) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Save in transaction
			 */
			localConnection.beginTransaction();
			
			saveFacture(creditNote, invContext, localConnection);
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put("id", creditNote.getID());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private AEFacture createCreditNote(AEDescriptor factureDescr, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		
		/**
		 * load the advise to be used as template
		 */
		AEFacture facture = loadFacture(factureDescr, invContext, aeConnection);
		
		// validate credit note creation
		if(facture.isDevis()) {
			throw AEError.System.FACTURATION_CANOT_CREATE_CREDIT_NOTE_OVER_DEVIS.toException();
		}
		
		/**
		 * keep some facture data
		 */
		AEDescriptor factDescr = facture.getDescriptor();
		
		// copy and initialize
		AEFacture creditNote = facture;
		
		/**
		 * Reset facture as new instance
		 */
		creditNote.resetAsNew();
		
		/**
		 * Init note
		 */
		creditNote.setSubType(AEFactureUtil.FactureSubType.CREDIT_NOTE);
		creditNote.setState(AEFactureUtil.FactureState.VALIDATED);
		creditNote.setSrcDoc(factDescr);
		creditNote.setNumber(0);
		creditNote.setNumberString(null);
		creditNote.setDate(new Date());
		creditNote.setSrcDoc(null);
		creditNote.setAdvanceMode(AEFactureUtil.PercentAmountMode.NA);
		creditNote.setAdvanceAmount(0.0);
		creditNote.setAdvancePercent(0.0);
		
		// items and items deducted
		AEFactureItemsList itemsList = creditNote.grantFactureItems();
		for (Iterator<AEDocumentItem> iterator = itemsList.iterator(); iterator.hasNext();) {
			AEFactureItem item = (AEFactureItem) iterator.next();
			
			item.resetAsNew();
			item.setItemDeducted(null);
			item.setPaymentInvoiced(null);
			item.setPriceExVat(item.getPriceExVat() * (-1));
			item.setDocument(creditNote.getDescriptor());
		}
		
		// cancel facture
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEFactureDAO factureDAO = daoFactory.getFactureDAO(aeConnection);
		factureDAO.updateState(AEFactureUtil.FactureState.CANCELLED.getStateId(), factureDescr.getID());

		return creditNote;
	}

	@Override
	public AEResponse savePrintTemplate(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject templateJson = arguments.optJSONObject(AEFacturePrintTemplate.JSONKey.printTemplate);
			long ownerId = arguments.optLong("sOwnerId");
			
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// whether this article is ownered by this customer
			if(ownerId != templateJson.optLong("ownerId")) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Create template
			 */
			AEFacturePrintTemplate t = new AEFacturePrintTemplate();
			t.create(templateJson);
			
			/**
			 * Validate
			 */
			
			/**
			 * Save in transaction
			 */
			localConnection.beginTransaction();
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			switch(t.getPersistentState()) {
			case NEW:
				factDAO.insert(t);
				break;
			case UPDATED:
				factDAO.update(t);
				if(t.isByDefault()) {
					factDAO.setByDefault(t);
				}
				break;
			case DELETED:
				assert(false);
				break;
			default:
				// internal error
				assert(false);
				break;
			}
			
			/**
			 * After Save
			 */
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put(AEFacturePrintTemplate.JSONKey.printTemplate, t.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadPrintTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("ownerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Validate
			 */
			
			/**
			 * Load
			 */
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			AEFacturePrintTemplatesList templates = factDAO.loadPrintTemplatesList(ownerDescr);
			
			/**
			 * return
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEFacturePrintTemplate.JSONKey.printTemplates, templates.toJSONArray()); 
			
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse deletePrintTemplate(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Get arguments
			 */
			JSONObject arguments = aeRequest.getArguments();
			long sOwnerId = arguments.optLong("sOwnerId");
			JSONObject templateJson = arguments.optJSONObject(AEFacturePrintTemplate.JSONKey.printTemplate);
			long templateId = templateJson.optLong("id");
			
			/**
			 * Validation
			 */
			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);
			
			// whether this article is ownered by this customer
			if(sOwnerId != templateJson.optLong("ownerId")) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * DB connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// begin transaction
			localConnection.beginTransaction();
			
			// delete document
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			factDAO.deletePrintTemplate(AEFacturePrintTemplate.lazyDescriptor(templateId));
			
			// commit
			localConnection.commit();
			
			// return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			localConnection.rollback();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadRelevantPrintTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			
			long ownerId = arguments.optLong("ownerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			
			long docType = arguments.getLong("docType");
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Validate
			 */
			
			/**
			 * Load
			 */
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			AEFacturePrintTemplatesList templates = factDAO.loadRelevantPrintTemplatesList(ownerDescr, aeDocType);
			
			/**
			 * return
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEFacturePrintTemplate.JSONKey.printTemplates, templates.toJSONArray()); 
			
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadTableauDeBord(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("sOwnerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			AEPaymentDAO paymentDAO = daoFactory.getPaymentDAO(localConnection);
			AEFactureDAO factureDAO = daoFactory.getFactureDAO(localConnection);
			
			// load customer
			JSONObject customer = null;
			JSONObject customerArguments = new JSONObject();
			customerArguments.put("customerId", ownerId);
			customerArguments.put("doNotLoadCAO", true);
			AERequest customerRequest = new AERequest(customerArguments);
			AEResponse customerResponse = partyService.loadCustomers(customerRequest, invContext);
			if(customerResponse.getPayload() != null) {
				JSONObject payload = customerResponse.getPayload();
				JSONArray customersArray = payload.optJSONArray("customers");
				if(customersArray.length() > 0) {
					customer = customersArray.getJSONObject(0);
				}
			}
			
			// period
			Date startDate = null;
			Date endDate = new Date();
			if(customer != null) {
				startDate = AEDateUtil.parseDateStrict(customer.optString("finYearStartDate"));
			}
			
			// overdue debts
			AEPaymentsList overdueDebts = paymentDAO.loadOverdueDebts(ownerDescr);
			
			// maturity debts
			AEPaymentsList maturityDebts = paymentDAO.loadMaturityDebts(ownerDescr);
			
			// most - billed customers
			JSONArray mostBilledCustomers = factureDAO.loadMostBilledCustomers(ownerDescr);
			
			// accepted devises
			JSONObject acceptedDevises = factureDAO.loadAccepted(ownerDescr, startDate, endDate);
			
			// accepted devises for current month
			JSONObject acceptedDevisesCurrentMonth = factureDAO.loadAccepted(
					ownerDescr, 
					AEDateUtil.firstDayOfMonth(endDate), 
					endDate);
			
			// finance year turnover
			JSONObject finYearTurnover = factureDAO.loadFinYearTurnover(ownerDescr, startDate, endDate);
			
			// trend
			JSONArray trend = factureDAO.loadTrend(ownerDescr);
			
			// return response
			JSONObject payload = new JSONObject();
			if(customer != null) {
				payload.put("customer", customer);
			}
			payload.put("date", (Object) new Date());
			payload.put("overdueDebts", (Object) overdueDebts);
			payload.put("maturityDebts", (Object) maturityDebts);
			payload.put("mostBilledCustomers", mostBilledCustomers);
			payload.put("trend", trend);
			payload.put("acceptedDevises", acceptedDevises);
			payload.put("acceptedDevisesCurrentMonth", acceptedDevisesCurrentMonth);
			payload.put("finYearTurnover", finYearTurnover);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse calculateFacture(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject factureJson = arguments.optJSONObject(AEFacture.JSONKey.facture);
			long docType = factureJson.getLong("docType");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// whether this article is ownered by this customer
			if(ownerId != factureJson.optLong("ownerId")) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Detect document type
			 */
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			/**
			 * Create facture
			 */
			
			// create from JSON
			AEDocumentFactory docFactory = AEDocumentFactory.getInstance(aeDocType);
			if(!(docFactory instanceof AEFactureFactory)) {
				throw new AEException("System Error: Unknnown document factory!");
			}
			AEDocument aeDocument = docFactory.createDocument(aeDocType);
			if(!(aeDocument instanceof AEFacture)) {
				throw new AEException("System Error: The document is not type of AEFacture!");
			}
			AEFacture facture = (AEFacture) aeDocument;
			facture.create(factureJson);
			
			// Load foreign records
			VATItemDAO vatItemsDAO = daoFactory.getVATItemDAO(localConnection);
			JSONArray vatItems = vatItemsDAO.load();
			loadVatItems(facture, vatItems);
			
			// Create dateOfExpiry
			createDateOfExpiry(facture);
			
			// calculate facrure
			facture.calculate();
			facture.calculateAdvance();
			
			JSONObject payload = new JSONObject();
			payload.put(AEFacture.JSONKey.facture, facture.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEResponse savePaymentTerm(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject templateJson = arguments.optJSONObject(AEPaymentTerms.JSONKey.paymentTerm.toString());
			long ownerId = arguments.optLong("sOwnerId");
			
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// whether this article is ownered by this customer
			if(ownerId != templateJson.optLong("ownerId")) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Create template
			 */
			AEPaymentTerms pt = new AEPaymentTerms();
			pt.create(templateJson);
			
			/**
			 * Validate
			 */
			
			/**
			 * Save in transaction
			 */
			localConnection.beginTransaction();
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			switch(pt.getPersistentState()) {
			case NEW:
				factDAO.insert(pt);
				if(pt.isDefault()) {
					factDAO.setByDefault(pt);
				}
				break;
			case UPDATED:
				factDAO.update(pt);
				if(pt.isDefault()) {
					factDAO.setByDefault(pt);
				}
				break;
			case DELETED:
				assert(false);
				break;
			default:
				// internal error
				assert(false);
				break;
			}
			
			/**
			 * After Save
			 */
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put(AEPaymentTerms.JSONKey.paymentTerm.toString(), pt.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEResponse loadPymentTermTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			
			long ownerId = arguments.optLong("ownerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Validate
			 */
			
			/**
			 * Load
			 */
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			AEPaymentTermsList templates = factDAO.loadPaymentTermTemplates(ownerDescr);
			
			/**
			 * return
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEPaymentTerms.JSONKey.paymentTerms.toString(), templates.toJSONArray()); 
			
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadPaymentTermTemplate(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEResponse aeResp = null;
		
		return aeResp;
	}

	@Override
	public AEResponse deletePaymentTermTemplate(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Get arguments
			 */
			JSONObject arguments = aeRequest.getArguments();
			long sOwnerId = arguments.optLong("sOwnerId");
			JSONObject templateJson = arguments.optJSONObject(AEPaymentTerms.JSONKey.paymentTerm.toString());
			long templateId = templateJson.optLong("id");
			
			/**
			 * Validation
			 */
			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);
			
			// whether this article is ownered by this customer
			if(sOwnerId != templateJson.optLong("ownerId")) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * DB connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// begin transaction
			localConnection.beginTransaction();
			
			// delete document
			AEFacturationDAO factDAO = daoFactory.getFacturationDAO(localConnection);
			factDAO.deletePaymentTermTemplate(AEPaymentTerms.lazyDescriptor(templateId));
			
			// commit
			localConnection.commit();
			
			// return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			localConnection.rollback();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse invoicePayment(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long paymentId = arguments.optLong("id");
			long ownerId = arguments.optLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * validate owner
			 */
			AEPaymentDAO paymentDAO = daoFactory.getPaymentDAO(localConnection);
			paymentDAO.validateOwner(paymentId, ownerId);
			
			/**
			 * load payment
			 */
			AEPayment payment = paymentDAO.loadFull(AEPayment.lazyDescriptor(paymentId));
			if(payment == null) {
				throw new AEException("System error: The payment cannot be found");
			}
			
			/**
			 * Validate invoicing
			 */
			if(AEFactureUtil.PayableType.SCHEDULE != payment.getPayableType() || payment.isPaid()) {
				throw AEError.System.FACTURATION_PAYMENT_PAY_ERROR.toException();
			}
			
			/**
			 * Begin transaction
			 */
			localConnection.beginTransaction();
			
			/**
			 * Create Facture
			 */
			AEFacture advFacture = createFacture(
					(AEFactureDescr) payment.getToFacture().getDescriptor(), 
					payment, 
					invContext, 
					localConnection);
			saveFacture(advFacture, invContext, localConnection);
			
			// update current payment to invoiced
			Date paidDate = new Date();
			paymentDAO.updateToPaid(paidDate, payment.getID());
			payment.setPaid(true);
			payment.setPaidDate(paidDate);
		
			// recalculate invoiced state
			AEFactureDAO factureDAO = DAOFactory.getInstance().getFactureDAO(localConnection);
			factureDAO.updateInvoicedAmount(payment.getToFacture().getDescriptor().getID());
			
			/**
			 * After transition
			 */
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			/**
			 *  Return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEFacture.JSONKey.facture, advFacture.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}
