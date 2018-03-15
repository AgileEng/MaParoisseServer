/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.05.2010 12:51:35
 */
package eu.agileeng.services.document.ejb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import org.nfunk.jep.JEP;
import org.nfunk.jep.SymbolTable;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccJournalEntry;
import eu.agileeng.domain.acc.AccJournalItem;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.cash.CashJournalEntriesList;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFactory;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.domain.document.AEDocumentItemSIndexComparator;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.document.trade.AEDocumentItem;
import eu.agileeng.domain.document.trade.AEDocumentItemsList;
import eu.agileeng.domain.document.trade.AETradeDocument;
import eu.agileeng.domain.document.trade.AETradeDocumentFilter;
import eu.agileeng.domain.document.trade.AETradeDocumentResultsList;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.domain.jcr.JcrFile;
import eu.agileeng.domain.jcr.JcrNode;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistent.State;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.acc.AccJournalDAO;
import eu.agileeng.persistent.dao.acc.AccPeriodDAO;
import eu.agileeng.persistent.dao.acc.AccountDAO;
import eu.agileeng.persistent.dao.cash.CashModuleDAO;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.persistent.dao.document.trade.AEDocumentItemDAO;
import eu.agileeng.persistent.dao.document.trade.AETradeDocumentDAO;
import eu.agileeng.persistent.dao.file.FileAttachmentDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthRole;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.acc.ejb.AccLocal;
import eu.agileeng.services.file.ejb.FileAttachmentLocal;
import eu.agileeng.services.imp.AEInvocationContextImp;
import eu.agileeng.services.jcr.ejb.JcrLocal;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEFileUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;
import eu.agileeng.util.AEValue;
import eu.agileeng.util.LightStringTokenizer;
import eu.agileeng.util.mail.Emailer;

/**
 *
 */
@Stateless
public class AEDocumentBean extends AEBean implements AEDocumentLocal, AEDocumentRemote {

	private static final long serialVersionUID = 6744171685351184106L;
	
	@EJB private AccLocal accLocal;
	@EJB private JcrLocal jcrLocal;
	@EJB private FileAttachmentLocal fileAttachmentLocal;
//	@EJB private PartyLocal partyService;

	/* (non-Javadoc)
	 * @see eu.agileeng.services.document.AEDocumentService#save(eu.agileeng.domain.document.AEDocument)
	 */
	@Override
	public AEDocument save(AEDocument aeDocument, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEDocumentFactory docFactory = AEDocumentFactory.getInstance(aeDocument.getType());
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// doc validation
			aeDocument.validateWith(docFactory.getSaveValidator());
			
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();

			// save in transaction
			switch(aeDocument.getPersistentState()) {
			case NEW:
				insert(aeDocument, invContext, localConnection);
				break;
			case UPDATED:
				update(aeDocument, invContext, localConnection);
				break;
			default:
				// internal error
				assert(false);
				break;
			}

			/**
			 *  process items
			 */
			processItems(aeDocument, invContext, localConnection);
			
			/**
			 * commit and return
			 */
			localConnection.commit();
			return aeDocument;
		} catch (Exception e) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public void processItems(AEDocument aeDocument, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();
			
			// save in transaction
			AEDocumentItemDAO docItemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
			AEDocumentItemsList items = aeDocument.getItems();
			for (Iterator<AEDocumentItem> iterator = items.iterator(); iterator.hasNext();) {
				AEDocumentItem aeDocumentItem = (AEDocumentItem) iterator.next();
				aeDocumentItem.setDocument(aeDocument.getDescriptor());
				switch(aeDocumentItem.getPersistentState()) {
				case NEW:
					docItemDAO.insert(aeDocumentItem);
					break;
				case UPDATED:
					docItemDAO.update(aeDocumentItem);
					break;
				case DELETED:
					docItemDAO.delete(aeDocumentItem.getID());
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
			e.printStackTrace();
			throw e;
		} catch (Throwable t) {
			localConnection.rollback();
			t.printStackTrace();
			throw new AEException((int) AEError.System.NA.getSystemID(), t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void prepareInsert(AEDocument aeDocument, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate insert
		 */
		
		
		/**
		 * prepare
		 */
		Date dateNow = new Date();
		aeDocument.setCreator(invContext.getAuthPrincipal().getName());
		aeDocument.setTimeCreated(dateNow);
		aeDocument.setModifier(invContext.getAuthPrincipal().getName());
		aeDocument.setTimeModified(dateNow);
		if(aeDocument.getDate() == null) {
			aeDocument.setDate(dateNow);
		}
		if(aeDocument.getCompany() == null) {
			aeDocument.setCompany(invContext.getAuthPrincipal().getCompany());
		}
		
		aeDocument.setRegDate(dateNow);
	}
	
	/**
 	 * private where no need for different  
	 */
	@Override
	public void insert(AEDocument aeDocument, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// prepare concrete document type
			prepareInsert(aeDocument, invContext, aeConnection);
			
			// insert
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(aeDocument.getType(), localConn);
			docDAO.insert(aeDocument);
			
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
	
	/**
 	 * private where no need for different  
	 */
	private void prepareUpdate(AEDocument aeDocument, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate update
		 */
		
		/**
		 * prepare
		 */
		Date dateNow = new Date();
		if(AEStringUtil.isEmpty(aeDocument.getCreator())) {
			aeDocument.setCreator(invContext.getAuthPrincipal().getName());
		}
		if(aeDocument.getTimeCreated() == null) {
			aeDocument.setTimeCreated(dateNow);
		}
		aeDocument.setModifier(invContext.getAuthPrincipal().getName());
		aeDocument.setTimeModified(dateNow);
		if(aeDocument.getCompany() == null) {
			aeDocument.setCompany(invContext.getAuthPrincipal().getCompany());
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void update(AEDocument aeDocument, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// prepare
			prepareUpdate(aeDocument, invContext, aeConnection);
			
			// update
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(aeDocument.getType(), localConn);
			docDAO.update(aeDocument);
			
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

	/* (non-Javadoc)
	 * @see eu.agileeng.services.document.AEDocumentService#open(eu.agileeng.domain.AEDescriptor)
	 */
	@Override
	public AEDocument load(AEDescriptor docDescr, AEInvocationContext invContext) throws AEException {
		assert(docDescr instanceof AEDocumentDescriptor);
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			localConnection = daoFactory.getConnection();
			AEDocumentDescriptor documentDescr = (AEDocumentDescriptor) docDescr;
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(documentDescr.getDocumentType(), localConnection);
			AEDocument aeDocument = docDAO.load(docDescr);

			return aeDocument;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.document.AEDocumentService#load(eu.agileeng.domain.document.AEDocumentFilter)
	 */
	@Override
	public AEDocumentsList load(AEDocumentFilter docFilter, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			localConnection = daoFactory.getConnection();
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(docFilter.getDocType(), localConnection);
			AEDocumentsList docsList = docDAO.loadContratDeTravail(docFilter);

			return docsList;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse save(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {

			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			JSONObject jsonDocument = aeRequest.getArguments().getJSONObject("document");
			AEDocumentFactory docFactory = AEDocumentFactory.getInstance(
					AEDocumentType.valueOf(jsonDocument.getLong("xType")));
			
			/**
			 * Factory and transaction
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Create document
			 */
			AEDocument aeDocument = docFactory.createDocument(aeRequest.getArguments().getJSONObject("document"));
			
			/**
			 * PreSave preparation
			 */
			if(aeDocument instanceof AETradeDocument) {
				vatCalculation((AETradeDocument) aeDocument);
			}
			
			/**
			 * Validate document save operation
			 */
			aeDocument.validateWith(docFactory.getSaveValidator());
			
			// validate in opened period
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			if(!aeDocument.isTemplate()) {
				boolean bRet = false;
				
				// check for insert into closed period
				if(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoice).equals(aeDocument.getType())) {
					bRet = accPeriodDAO.isInClosedPeriod(
							aeDocument.getDate(), 
							aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
									AEApp.PURCHASE_MODULE_ID);
				} else if(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoiceFNP).equals(aeDocument.getType())) {
					bRet = accPeriodDAO.isInClosedPeriod(
							aeDocument.getDate(), 
							aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
									AEApp.PURCHASE_FNP_MODULE_ID);
				} else if(AEDocumentType.valueOf(AEDocumentType.System.AESaleInvoice).equals(aeDocument.getType())) {
					bRet = accPeriodDAO.isInClosedPeriod(
							aeDocument.getDate(), 
							aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
									AEApp.SALE_MODULE_ID);
				} else if(
						AEDocumentType.valueOf(AEDocumentType.System.AECheque).equals(aeDocument.getType())
						|| AEDocumentType.valueOf(AEDocumentType.System.AEVirement).equals(aeDocument.getType())
						|| AEDocumentType.valueOf(AEDocumentType.System.AEPrelevement).equals(aeDocument.getType())
						|| AEDocumentType.valueOf(AEDocumentType.System.AELCR).equals(aeDocument.getType())) {
					
					bRet = accPeriodDAO.isInClosedPeriod(
							aeDocument.getDate(), 
							aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
									AEApp.PAYMENT_MODULE_ID);
				}
				if(bRet) {
					throw new AEException(
							(int) AEError.System.CANNOT_INSERT_UPDATE_CLOSED_PERIOD.getSystemID(), 
							"Période close, veuillez saisir la date de facture en reference!");
				}
				
				// check for remove from closed period
				if(aeDocument.getPersistentState() == AEPersistent.State.UPDATED) {
					AEDocumentDAO docDAO = daoFactory.getDocumentDAO(aeDocument.getType(), localConnection);
					Date dbDocDate = docDAO.getDocDate(aeDocument.getID());
					if(dbDocDate != null) {
						if(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoice).equals(aeDocument.getType())) {
							bRet = accPeriodDAO.isInClosedPeriod(
									dbDocDate, 
									aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
											AEApp.PURCHASE_MODULE_ID);
						} else if(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoiceFNP).equals(aeDocument.getType())) {
							bRet = accPeriodDAO.isInClosedPeriod(
									dbDocDate, 
									aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
											AEApp.PURCHASE_FNP_MODULE_ID);
						} else if(AEDocumentType.valueOf(AEDocumentType.System.AESaleInvoice).equals(aeDocument.getType())) {
							bRet = accPeriodDAO.isInClosedPeriod(
									dbDocDate, 
									aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
											AEApp.SALE_MODULE_ID);
						} else if(
								AEDocumentType.valueOf(AEDocumentType.System.AECheque).equals(aeDocument.getType())
								|| AEDocumentType.valueOf(AEDocumentType.System.AEVirement).equals(aeDocument.getType())
								|| AEDocumentType.valueOf(AEDocumentType.System.AEPrelevement).equals(aeDocument.getType())
								|| AEDocumentType.valueOf(AEDocumentType.System.AELCR).equals(aeDocument.getType())) {

							bRet = accPeriodDAO.isInClosedPeriod(
									dbDocDate, 
									aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
											AEApp.PAYMENT_MODULE_ID);
						}
					}
					if(bRet) {
						throw new AEException(
								(int) AEError.System.CANNOT_INSERT_UPDATE_CLOSED_PERIOD.getSystemID(), 
								"Enregistrement impossible, vous tentez de modifier une facture appartenant à une période close!");
					}
				}
			}
			
			// get connection and begin transaction
			localConnection.beginTransaction();

			// save in transaction
			switch(aeDocument.getPersistentState()) {
			case NEW:
				insert(aeDocument, invContext, localConnection);
				break;
			case UPDATED:
				update(aeDocument, invContext, localConnection);
				break;
			default:
				// internal error
				assert(false);
				break;
			}

			/**
			 *  process items
			 */
			processItems(aeDocument, invContext, localConnection);
			
			/**
			 * post processing
			 */
			if(AEDocumentType.valueOf(AEDocumentType.System.SUIVI_GPL).equals(aeDocument.getType())
					|| AEDocumentType.valueOf(AEDocumentType.System.DONNEES).equals(aeDocument.getType())) {

				long moduleId = AEPersistentUtil.NEW_ID;
				if(AEDocumentType.valueOf(AEDocumentType.System.SUIVI_GPL).equals(aeDocument.getType())) {
					moduleId = AEApp.SUIVI_GPL_MODULE_ID;
				} else if(AEDocumentType.valueOf(AEDocumentType.System.DONNEES).equals(aeDocument.getType())) {
					moduleId = AEApp.DONNEES_MODULE_ID;
				} else {
					throw new AEException("System Error: Unknnown document type!");
				}
				
				// determine acc period
				AccPeriod accPeriod = getFirstOpenPeriod(
						aeDocument.getCompany().getDescriptor().getID(), 
						moduleId, 
						localConnection);

				synchronizeSOTExc((AETradeDocument) aeDocument, accPeriod, invContext);
			}
			
			// check for jcrPath to be attached
			String jcrPath = jsonDocument.optString("jcrPath");
			if(!AEStringUtil.isEmpty(jcrPath)) {
				JcrFile jcrFile = null;
				File jcrFileContent = null;
				try {
					// get jcrFile with content
					// this method will throw exception if anything with the path going wrong
					// the content of the file is saved in tmp folder
					jcrFile = (JcrFile) jcrLocal.getJcrFileWithContent(jcrPath, aeDocument.getCompany().getDescriptor().getID(), invContext);

					// move content of the file to the fileRepository folder
					String fileRepository = AEApp.getInstance().getProperties().getProperty(AEApp.fileRepository);
					jcrFileContent = new File(
							fileRepository, 
							new StringBuilder("dmtbox_att_").append((String) invContext.getProperty(AEInvocationContext.HttpSessionId))
							.append("_").append(aeDocument.getID()).append("_").append(aeDocument.getClazz().getID())
							.append(FilenameUtils.EXTENSION_SEPARATOR_STR).append(FilenameUtils.getExtension(jcrFile.getName())).toString());
					FileUtils.moveFile(jcrFile.getContent(), jcrFileContent);

					// attach to aeDocument
					JSONObject fileAttachment = new JSONObject();
					fileAttachment.put("ownerId", aeDocument.getCompany().getDescriptor().getID());
					fileAttachment.put("toId", aeDocument.getID());
					fileAttachment.put("toType", DomainModel.DomainClass.AeDocument.getID());
					fileAttachment.put("name", jcrFile.getName());
					fileAttachment.put("length", jcrFileContent.length());
					fileAttachment.put("remotePath", jcrFileContent.getParent());
					fileAttachment.put("remoteName", jcrFileContent.getName());
					fileAttachment.put("isDirty", false);

					JSONObject attachArguments = new JSONObject();
					attachArguments.put("fileAttachment", fileAttachment);
					AERequest attachRequest = new AERequest(attachArguments);
					fileAttachmentLocal.manage(attachRequest, invContext);

					// remove node
					JSONObject deleteNodeArguments = new JSONObject();
					deleteNodeArguments.put(JcrNode.JSONKey.path, jcrPath);
					AERequest deleteNodeRequest = new AERequest(deleteNodeArguments);
					jcrLocal.deleteNode(deleteNodeRequest, invContext);
				} catch (Throwable t) {
					// jcrFileContent is not needed
					if(jcrFileContent != null) {
						AEFileUtil.deleteFileQuietly(jcrFileContent);
					}
					
					// re throw exception
					throw new AEException(t);
				} finally {
					// delete tmp file content
					if(jcrFile != null && jcrFile.getContent() != null) {
						AEFileUtil.deleteParentQuietlyExt(jcrFile.getContent());
					}
				}
				
			}
			
			/**
			 * Commit and return response
			 */
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put("document", aeDocument.toJSONObject());
			return new AEResponse(payload);
		} catch (AEException e) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw e;
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			t.printStackTrace();
			throw new AEException((int) AEError.System.NA.getSystemID(), t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void vatCalculation(AETradeDocument tDoc) {
		double taxableAmount = 0.0;
		AEDocumentItemsList items = tDoc.getItems();
		for (AEDocumentItem aeDocumentItem : items) {
			taxableAmount += aeDocumentItem.getTaxableAmount();
		}
		tDoc.setTaxableAmount(taxableAmount);
	}
	
	@Override
	public AEResponse loadSupplyTemplate(AERequest aeRequest) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			localConnection = daoFactory.getConnection();
			long templateId = aeRequest.getArguments().getLong("id");
			AEDescriptor docDescr = AETradeDocument.lazyDescriptor(templateId);
			
			// load document
			AEDocumentDAO docDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			AEDocument aeDocument = docDAO.load(docDescr);

			// load items
			AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
			aeDocument.setItems(itemDAO.load(docDescr));
			
			// load attachment
			FileAttachmentDAO fileAttDAO = daoFactory.getFileAttachmentDAO(localConnection);
			aeDocument.setFileAttachment(fileAttDAO.loadTo(aeDocument.getDescriptor()));
			
			// load payment delay
			AETimePeriod payDelay = null;
			if(aeRequest.getArguments().has("loadPayDelay") 
					&& aeRequest.getArguments().getBoolean("loadPayDelay")
					&& aeDocument instanceof AETradeDocument) {
				AETradeDocument aeTradeDoc = (AETradeDocument) aeDocument;
				AEDescriptor companyDescr = null;
				if(aeDocument.getType() != null 
						&& (aeDocument.getType().getSystemID() == AEDocumentType.System.AEPurchaseInvoice.getSystemID()
								|| aeDocument.getType().getSystemID() == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID())
						&& aeTradeDoc.getIssuer() != null) {
					
					companyDescr =  aeTradeDoc.getIssuer().getDescriptor();
				} else if(aeDocument.getType() != null 
						&& aeDocument.getType().getSystemID() == AEDocumentType.System.AESaleInvoice.getSystemID()
						&& aeTradeDoc.getRecipient() != null) {
					
					companyDescr =  aeTradeDoc.getRecipient().getDescriptor();
				}
				if(companyDescr != null) {
					OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
					payDelay = orgDAO.loadPayDelay(companyDescr);
				}
			}
			
			// whether it is for new doc creationn
			if(aeRequest.getArguments().has("forNewDoc") 
					&& aeRequest.getArguments().optBoolean("forNewDoc")
					&& aeDocument instanceof AETradeDocument) {

				if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
					AccPeriod accPeriod = getFirstOpenPeriod(
							aeRequest.getArguments().getLong("ownerId"),
							AEApp.PURCHASE_FNP_MODULE_ID,
							localConnection);
					if(accPeriod == null) {
						throw new AEException("Internal Error: Cannot detect accounting period!");
					}
					aeDocument.setDate(AEDateUtil.getClearDate(accPeriod.getEndDate()));
				}
			}
			
			// return response
			JSONObject payload = new JSONObject();
			payload.put("document", aeDocument.toJSONObject());
			if(payDelay != null) {
				payload.put("payDelayDuration", payDelay.getDuration());
				payload.put("payDelayUOM", payDelay.getUnit());
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
	public AEResponse loadByFilter(AERequest aeRequest) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(aeRequest.getArguments().getLong("sOwnerId"));
			
			// get connection
			localConnection = daoFactory.getConnection();
			
			// parse request
			AETradeDocumentFilter filter = new AETradeDocumentFilter();
			filter.create(aeRequest.getArguments().getJSONObject("filter"));
			if(aeRequest.getArguments().has("openedPeriod")) {
				boolean openedPeriod = aeRequest.getArguments().getBoolean("openedPeriod");
				if(openedPeriod) {
					AccPeriod accPeriod = null;
					if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoice.getSystemID()) {
						accPeriod = getFirstOpenPeriod(
								aeRequest.getArguments().getLong("ownerId"),
								AEApp.PURCHASE_MODULE_ID,
								localConnection);
					} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
						accPeriod = getFirstOpenPeriod(
								aeRequest.getArguments().getLong("ownerId"),
								AEApp.PURCHASE_FNP_MODULE_ID,
								localConnection);
					} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AESaleInvoice.getSystemID()) {
						accPeriod = getFirstOpenPeriod(
								aeRequest.getArguments().getLong("ownerId"),
								AEApp.SALE_MODULE_ID,
								localConnection);
					}
					if(accPeriod != null) {
						filter.setDocDateFrom(accPeriod.getStartDate());
					}
				}
			}
			
			// load results
			AETradeDocumentDAO docDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			AETradeDocumentResultsList resultsList = docDAO.load(filter);

			// return response
			JSONObject payload = new JSONObject();
			payload.put("documents", resultsList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEApp.logger().error(t.getMessage(), t);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closePurchasePeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * get connection to the DB and begin transaction
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();
			
			/**
			 * prepare dao's
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
			AETradeDocumentDAO tradeDocDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			
			/**
			 * determine period
			 */
			AccPeriod accPeriod = null;
			if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoice.getSystemID()) {
				accPeriod = getFirstOpenPeriod(
						aeRequest.getArguments().getLong("ownerId"),
						AEApp.PURCHASE_MODULE_ID,
						localConnection);
			} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
				accPeriod = getFirstOpenPeriod(
						aeRequest.getArguments().getLong("ownerId"),
						AEApp.PURCHASE_FNP_MODULE_ID,
						localConnection);
			} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AESaleInvoice.getSystemID()) {
				accPeriod = getFirstOpenPeriod(
						aeRequest.getArguments().getLong("ownerId"),
						AEApp.SALE_MODULE_ID,
						localConnection);
			}
			if(accPeriod == null) {
				throw new AEException("Internal Error: Cannot detect accounting period!");
			}
			Date startDate = AEDateUtil.getClearDate(accPeriod.getStartDate());	
			Date endDate = AEDateUtil.getClearDate(accPeriod.getEndDate());
			
			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
				throw new AEException(
						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
						"La période n'est pas terminée.");
			}
			
			/**
			 * load owner's data
			 */
			JSONArray customersArray = 
					orgDAO.loadCustomer(Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId")));
			JSONObject customer = customersArray.getJSONObject(0);
			AEDescriptor customerDescr = Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId"));
			customerDescr.setName(customer.optString("name"));
			
			/**
			 * determine customer's VAT account
			 */
			JSONArray accJSONArray = accDAO.loadSubAccountsByOwner(aeRequest.getArguments().getLong("ownerId"));
			Map<Long, JSONObject> accMapById = AccAccount.indexById(accJSONArray);
			Map<String, JSONObject> accMapByCode = AccAccount.indexByCode(accJSONArray);
			JSONObject vatAccount = null;
			if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoice.getSystemID()) {
//				for (int i = 0; i < accJSONArray.length(); i++) {
//					JSONObject accJSON = accJSONArray.getJSONObject(i);
//
//					// don't use else if because one acc can be multiple
//					// choosen as default account
//					if(accJSON.optString("code").trim()
//							.equalsIgnoreCase(customer.optString("defaultTVAInAccCode").trim())) {
//
//						// this is TVA account
//						vatAccount = accJSON;
//					} 
//				}
				vatAccount = accMapByCode.get(customer.optString("defaultTVAInAccCode").trim());
			} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
				vatAccount = accMapByCode.get(customer.optString("defaultSuppplierFNPVatCode").trim());
			} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AESaleInvoice.getSystemID()) {
//				for (int i = 0; i < accJSONArray.length(); i++) {
//					JSONObject accJSON = accJSONArray.getJSONObject(i);
//
//					// don't use else if because one acc can be multiple
//					// choosen as default account
//					if(accJSON.optString("code").trim()
//							.equalsIgnoreCase(customer.optString("defaultTVAOutAccCode").trim())) {
//
//						// this is TVA account
//						vatAccount = accJSON;
//					} 
//				}
				vatAccount = accMapByCode.get(customer.optString("defaultTVAOutAccCode").trim());
			}
			if(vatAccount == null) {
				throw new AEException("S'il vous plaît vérifier l'installation par défaut du compte.");
			}
			
			/**
			 * Determine supplierFNPAcc
			 */
			JSONObject supplierFNPAccJSON = null;
			AccAccount supplierFNPAcc = null;
			for (int i = 0; i < accJSONArray.length(); i++) {
				JSONObject accJSON = accJSONArray.getJSONObject(i);

				// don't use else if because one acc can be multiple
				// choosen as default account
				if(accJSON.optString("code").trim()
						.equalsIgnoreCase(customer.optString("defaultSuppplierFNPAccCode").trim())) {

					// this is TVA account
					supplierFNPAccJSON = accJSON;
				} 
			}
			if(supplierFNPAccJSON != null) {
				supplierFNPAcc = accDAO.loadById(AccAccount.lazyDescriptor(supplierFNPAccJSON.optLong("id")));
			}
			
			/**
			 * Create storno entries for Achats FNP for the previous period
			 * 
			 * 05.02.2012
			 * Menu "FNP": the back writing contains all the entries made 
			 * in the previous periods. As we have a solution to do 
			 * that automatically in PGI, could you suppress this feature?
			 */
//			if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
//				AccPeriod accLastClosedPeriod = getLastClosedPeriod(
//						aeRequest.getArguments().getLong("ownerId"),
//						AEApp.PURCHASE_FNP_MODULE_ID,
//						localConnection);
//				
//				if(accLastClosedPeriod != null) {
//					ExportRequest expRequest = journalDAO.loadForExport(aeRequest.getArguments().optLong("ownerId"), accLastClosedPeriod.getID());
//					if(expRequest != null && !AECollectionUtil.isEmpty(expRequest.getAccJournalItemsList())) {
//						AccJournalEntry stornoEntry = new AccJournalEntry();
//						AccJournalItemsList stornoAccJournalItems = expRequest.getAccJournalItemsList();
//						for (AccJournalItem accJournalItem : stornoAccJournalItems) {
//							// reset as new
//							accJournalItem.setPersistentState(State.NEW);
//							accJournalItem.setID(AEPersistentUtil.NEW_ID);
//							
//							// init with new period
//							accJournalItem.setDate(accPeriod.getStartDate());
//							accJournalItem.setAccPeriod(accPeriod.getDescriptor());
//							accJournalItem.setBatchId(AEPersistentUtil.NEW_ID);
//							accJournalItem.setEntryId(AEPersistentUtil.NEW_ID);
//							
//							// reverse dt and ct
//							Double tmpCtAmount = accJournalItem.getCtAmount();
//							accJournalItem.setCtAmount(accJournalItem.getDtAmount());
//							accJournalItem.setDtAmount(tmpCtAmount);
//							
//							// update description
//							accJournalItem.setDescription("Ext " + accJournalItem.getDescription());
//							
//							// add item
//							stornoEntry.addItem(accJournalItem);
//						}
//						
//						// insert storno entry
//						journalDAO.insertEntry(stornoEntry);
//					}
//				}
//			}
			
			/**
			 * Load purchase invoices in given period. 
			 * For every invoice create account entry and insert it
			 */
			AEDescriptive ownerDescr = 
				Organization.lazyDescriptor(aeRequest.getArguments().optLong("ownerId"));
			
			AEDescriptorsList docDescrList = tradeDocDAO.loadDescriptors(
					aeRequest.getArguments().getLong("ownerId"), 
					startDate, 
					endDate, 
					aeRequest.getArguments().optLong("docType"));
			for (AEDescriptor docDescr : docDescrList) {
				AETradeDocument doc = (AETradeDocument) load(docDescr, localConnection);
				
				// create doc entry for this document
				AccJournalEntry docEntry = new AccJournalEntry();
				
				// journal 
				AEDescriptive journal = doc.getJournal();
				AEDescriptor journalDescr = null;
				if(journal != null) {
					journalDescr = journal.getDescriptor();
				}
				
				// Concrete VAT Accout 
				// Should be detect only in case of not FNP document.
				JSONObject vatAccountDoc = vatAccount;
				if(doc.getVatAccount() != null 
						&& aeRequest.getArguments().optLong("docType") != AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
					
					// 
					vatAccountDoc = accMapById.get(doc.getVatAccount().getDescriptor().getID());
				}
				
				// start accounting of the document
				if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoice.getSystemID()
						|| aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
		
					// party account
					AccAccount partyAcc = null;
					AEDescriptive partyDescr = null;
					
					if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoice.getSystemID()) {
						if(doc.getIssuer() != null) {
							partyDescr = orgDAO.loadDescriptive(doc.getIssuer().getDescriptor().getID());
							if(partyDescr != null) {
								partyAcc = orgDAO.loadAccAccount(partyDescr.getDescriptor());
							}
						} else if(doc.getIssuerAcc()  != null) {
							partyAcc = accDAO.loadById(doc.getIssuerAcc().getDescriptor());
						}
					} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
						if(doc.getIssuer() != null) {
							partyDescr = orgDAO.loadDescriptive(doc.getIssuer().getDescriptor().getID());
						}
						partyAcc = supplierFNPAcc;
					}
					if(partyAcc == null) {
						String errMsg = 
							"Clôture impossible, le compte de la facture \""
							+  doc.getDescriptor().getDescription()
							+ "\" n’est pas renseigné.";
						throw new AEException((int) AEError.System.INVOICE_THERE_IS_NO_PARTY_ACCOUNT.getSystemID(), errMsg);
					}
					
					// entry description
//					REQ_INV_20120903_1					
//					•	If no description is seized, 
//  					then use suppliers (customers) name for all the lines from writing.
//					•	If a description is seized in the field “Libellé”, 
//  					then that description is used for all the lines, except the line with 
//  					third party where the supplier/customer name is kept.
//					•	If the user modifies the description in the “Montant HT” frame, 
//  					that description appears in the export file only for corresponding line, 
//  					whereas for the others both previous rules remain.
					String itemsDescr = AEStringUtil.EMPTY_STRING;
					if(AEStringUtil.isEmpty(doc.getDescription())) {
						if(partyDescr != null) {
							itemsDescr = partyDescr.getDescriptor().getName();
						} else if(partyAcc != null) {
							itemsDescr = partyAcc.getName();
						}
					} else {
						itemsDescr = doc.getDescription();
					}
					
					// additional rule for AEPurchaseInvoiceFNP
					if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
						if(!AEStringUtil.isEmpty(itemsDescr)) {
							itemsDescr = "FAR -" + itemsDescr;
						} else {
							itemsDescr = "FAR";
						}
					}
					
					// supplier item
					if(Math.abs(doc.getAmount()) >= 0.0001) {
						AccJournalItem supplierItem = new AccJournalItem(accPeriod);

						supplierItem.setCompany(ownerDescr);
						supplierItem.setJournal(journalDescr);
						supplierItem.setDate(doc.getDate());
						supplierItem.setAccount(partyAcc.getDescriptor());
						supplierItem.setAuxiliary(partyAcc.getAuxiliareDescr());
						supplierItem.setDescription(partyDescr != null ? partyDescr.getDescriptor().getName() : AEStringUtil.EMPTY_STRING);
						supplierItem.setReference(doc.getDescriptorShort());
						if(doc.getAmount() > 0.0) {
							supplierItem.setCtAmount(doc.getAmount());
						} else {
							supplierItem.setDtAmount(Math.abs(doc.getAmount()));
						}
						
						docEntry.addItem(supplierItem);
					}
					
					// VAT item
					double vatAmount = doc.getVatAmount();
					if(Math.abs(vatAmount) >= 0.0001) {
						AccJournalItem vatItem = new AccJournalItem(accPeriod);
						
						vatItem.setCompany(ownerDescr);
						vatItem.setJournal(journalDescr);
						vatItem.setDate(doc.getDate());
						vatItem.setAccount(AccAccount.lazyDescriptor(vatAccountDoc.getLong("id")));
						vatItem.setDescription(itemsDescr);
						vatItem.setReference(doc.getDescriptorShort());
						if(vatAmount > 0.0) {
							vatItem.setDtAmount(vatAmount);
						} else {
							vatItem.setCtAmount(Math.abs(vatAmount));
						}
						
						docEntry.addItem(vatItem);
					}
					
					// goods items		
					AEDocumentItemsList docItems = doc.getItems();
					for (AEDocumentItem aeDocumentItem : docItems) {
						Double dtAmount = aeDocumentItem.getTaxableAmount();
						if(Math.abs(dtAmount) >= 0.0001) {
							AccJournalItem goodDtItem = new AccJournalItem(accPeriod);
							
							goodDtItem.setCompany(ownerDescr);
							goodDtItem.setJournal(journalDescr);
							goodDtItem.setDate(doc.getDate());
							goodDtItem.setAccount(
									aeDocumentItem.getAccount() != null ?
									aeDocumentItem.getAccount().getDescriptor() : 
									null);
							// set description
							// in the "name" field we have 
							// or empty string 
							// or the account name 
							goodDtItem.setDescription(itemsDescr); // by default
							if(!AEStringUtil.isEmpty(aeDocumentItem.getName())) {
								if(aeDocumentItem.getAccount() != null) {
									JSONObject aeDocumentItemAcc = accMapById.get(aeDocumentItem.getAccount().getDescriptor().getID());
									if(aeDocumentItemAcc != null 
											&& !aeDocumentItem.getName().equalsIgnoreCase(aeDocumentItemAcc.optString("name"))) {

										goodDtItem.setDescription(aeDocumentItem.getName());
									}
								}
							}
							goodDtItem.setReference(doc.getDescriptorShort());
							if(dtAmount > 0.0) {
								goodDtItem.setDtAmount(dtAmount);
							} else {
								goodDtItem.setCtAmount(Math.abs(dtAmount));
							}
							
							// add item to the entry
							docEntry.addItem(goodDtItem);
						}
					}
				} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AESaleInvoice.getSystemID()) {
					// party account
					AccAccount partyAcc = null;
					AEDescriptive partyDescr = null;
					if(doc.getRecipient() != null) {
						partyDescr = orgDAO.loadDescriptive(doc.getRecipient().getDescriptor().getID());
						if(partyDescr != null) {
							partyAcc = orgDAO.loadAccAccount(partyDescr.getDescriptor());
						}
					} else if(doc.getRecipientAcc()  != null) {
						partyAcc = accDAO.loadById(doc.getRecipientAcc().getDescriptor());
					}
					if(partyAcc == null) {
						throw new AEException(
							(int) AEError.System.INVOICE_THERE_IS_NO_PARTY_ACCOUNT.getSystemID(),
							doc.getDescriptor().getDescription() 
							+ " - Code d'erreur: " 
							+ AEError.System.INVOICE_THERE_IS_NO_PARTY_ACCOUNT.getSystemID());
					}
					
					// entry description
//					REQ_INV_20120903_1					
//					•	If no description is seized, 
//  					then use suppliers (customers) name for all the lines from writing.
//					•	If a description is seized in the field “Libellé”, 
//  					then that description is used for all the lines, except the line with 
//  					third party where the supplier/customer name is kept.
//					•	If the user modifies the description in the “Montant HT” frame, 
//  					that description appears in the export file only for corresponding line, 
//  					whereas for the others both previous rules remain.
					String itemsDescr = AEStringUtil.EMPTY_STRING;
					if(AEStringUtil.isEmpty(doc.getDescription())) {
						if(partyDescr != null) {
							itemsDescr = partyDescr.getDescriptor().getName();
						} else if(partyAcc != null) {
							itemsDescr = partyAcc.getName();
						}
					} else {
						itemsDescr = doc.getDescription();
					}
					
					// client item
					if(Math.abs(doc.getAmount()) >= 0.0001) {
						AccJournalItem clientItem = new AccJournalItem(accPeriod);

						clientItem.setCompany(ownerDescr);
						clientItem.setJournal(journalDescr);
						clientItem.setDate(doc.getDate());
						clientItem.setAccount(partyAcc.getDescriptor());
						clientItem.setAuxiliary(partyAcc.getAuxiliareDescr());
						clientItem.setDescription(partyDescr != null ? partyDescr.getDescriptor().getName() : AEStringUtil.EMPTY_STRING);
						clientItem.setReference(doc.getDescriptor());
						if(doc.getAmount() > 0.0) {
							clientItem.setDtAmount(doc.getAmount());
						} else {
							clientItem.setCtAmount(Math.abs(doc.getAmount()));
						}
						
						docEntry.addItem(clientItem);
					}
					
					// VAT item
					double vatAmount = doc.getVatAmount();
					if(Math.abs(vatAmount) > 0.0001) {
						AccJournalItem vatItem = new AccJournalItem(accPeriod);
						
						vatItem.setCompany(ownerDescr);
						vatItem.setJournal(journalDescr);
						vatItem.setDate(doc.getDate());
						vatItem.setAccount(AccAccount.lazyDescriptor(vatAccountDoc.getLong("id")));
						vatItem.setDescription(itemsDescr);
						vatItem.setReference(doc.getDescriptor());
						if(vatAmount > 0.0) {
							vatItem.setCtAmount(vatAmount);
						} else {
							vatItem.setDtAmount(Math.abs(vatAmount));
						}
						
						docEntry.addItem(vatItem);
					}
					
					// goods items		
					AEDocumentItemsList docItems = doc.getItems();
					for (AEDocumentItem aeDocumentItem : docItems) {
						Double ctAmount = aeDocumentItem.getTaxableAmount();
						if(Math.abs(ctAmount) >= 0.0001) {
							AccJournalItem goodCtItem = new AccJournalItem(accPeriod);
							
							goodCtItem.setCompany(ownerDescr);
							goodCtItem.setJournal(journalDescr);
							goodCtItem.setDate(doc.getDate());
							goodCtItem.setAccount(
									aeDocumentItem.getAccount() != null ?
									aeDocumentItem.getAccount().getDescriptor() : 
									null);
							// set description
							// in the "name" field we have 
							// or empty string 
							// or the account name 
							goodCtItem.setDescription(itemsDescr); // by default
							if(!AEStringUtil.isEmpty(aeDocumentItem.getName())) {
								if(aeDocumentItem.getAccount() != null) {
									JSONObject aeDocumentItemAcc = accMapById.get(aeDocumentItem.getAccount().getDescriptor().getID());
									if(aeDocumentItemAcc != null 
											&& !aeDocumentItem.getName().equalsIgnoreCase(aeDocumentItemAcc.optString("name"))) {

										goodCtItem.setDescription(aeDocumentItem.getName());
									}
								}
							}
							goodCtItem.setReference(doc.getDescriptor());
							if(ctAmount > 0.0) {
								goodCtItem.setCtAmount(ctAmount);
							} else {
								goodCtItem.setDtAmount(Math.abs(ctAmount));
							}
							
							// add item to the entry
							docEntry.addItem(goodCtItem);
						}
					}
				} 

				// insert docEntry
				if(docEntry.getAccJournalItems() != null && !docEntry.getAccJournalItems().isEmpty()) {
					journalDAO.insertEntry(docEntry);
				}
			}

			/**
			 * close the period
			 */
			// close acc period
			accPeriodDAO.closePeriod(accPeriod.getID());
			
			/**
			 * commit transaction and return response 
			 */
			localConnection.commit();
			
			/**
			 * Process after close.
			 * Must be after transaction close.
			 * If after close fails, this should not affect the whole close task.
			 */
			try {
				// send e-mail
				Emailer emailer = new Emailer();
				if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoice.getSystemID()) {
					emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.PURCHASE_MODULE_ID);
				} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
					emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.PURCHASE_FNP_MODULE_ID);
				} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AESaleInvoice.getSystemID()) {
					emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.SALE_MODULE_ID);
				}
				
				// auto export
				JSONObject exportRequestArguments = new JSONObject();
				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				if("secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "CEGID");
				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "SAGE");
				}
				exportRequestArguments.put("ownerId", aeRequest.getArguments().getLong("ownerId"));
				AERequest exportRequest = new AERequest(exportRequestArguments);
				accLocal.export(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			
			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}

	}

	@Override
	public AEResponse closeSalePeriod(AERequest aeRequest) throws AEException {
		// TODO Auto-generated method stub
		return null;
	}
	
//	@Override
//	protected AccPeriod getFirstOpenPeriod(long ownerId, long moduleId, AEConnection aeConnection) throws AEException {
//		AEConnection localConnection = null;
//		try {
//			AccPeriod accPeriod = null;
//			
//			DAOFactory daoFactory = DAOFactory.getInstance();
//			localConnection = daoFactory.getConnection(aeConnection);
//			
//			// detect the first open period
//			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
//			accPeriod = accPeriodDAO.getFirstOpenPeriod(ownerId, moduleId);
//			if(accPeriod == null) {
//				// there is no open period, so we need to create one
//				Date startDate = null;
//				accPeriod = accPeriodDAO.getLastClosedPeriod(ownerId, moduleId);
//				if(accPeriod == null) {	
//					// there is no closed period, so start from absolute start date
//					startDate = getAbsoluteStartDate(ownerId, localConnection);
//				} else {
//					// there is closed period, start the period from next day
//					startDate = AEDateUtil.addDaysToDate(accPeriod.getEndDate(), 1);
//				}
//				
//				accPeriod = new AccPeriod();
//				accPeriod.setCompany(Organization.lazyDescriptor(ownerId));
//				accPeriod.setModuleId(moduleId);
//				accPeriod.setStartDate(startDate);
//				accPeriod.setEndDate(getPeriodEndDate(accPeriod.getStartDate()));
//				accPeriod.setClosed(false);
//				accPeriodDAO.insert(accPeriod);
//			}
//			
//			assert(accPeriod.isPersistent());
//			return accPeriod;
//		} catch (Throwable t) {
//			throw new AEException(t);
//		} finally {
//			AEConnection.close(localConnection);
//		}
//	}
	
//	private Date getAbsoluteStartDate(long ownerId, AEConnection aeConnection) throws AEException {
//		AEConnection localConnection = null;
//		try {
//			DAOFactory daoFactory = DAOFactory.getInstance();
//			localConnection = daoFactory.getConnection(aeConnection);
//			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(aeConnection);
//			Date startDate = orgDAO.getStartDate(ownerId);
//			if(startDate == null) {
//				// there is no start date
//				// so start from the first date of current month
//				startDate = AEDateUtil.dayOfMonth(AEDateUtil.getClearDate(new Date()), -1, 1);
//			}
//			return startDate;
//		} catch (Throwable t) {
//			throw new AEException(t);
//		} finally {
//			AEConnection.close(localConnection);
//		}
//	}
	
//	/**
//	 * The period is one month.
//	 * 
//	 * @param startDate
//	 * @return
//	 * @throws AEException
//	 */
//	private Date getPeriodEndDate(Date startDate) throws AEException {
//		Date endDate = AEDateUtil.getLastDate(
//				AEDateUtil.getMonthInYear(startDate) - 1, AEDateUtil.getYear(startDate));
//		return endDate;
//	}
//
	@Override
	public AEDocument load(AEDescriptor docDescr, AEConnection aeConnection) throws AEException {
		assert(docDescr instanceof AEDocumentDescriptor);
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection
			localConnection = daoFactory.getConnection(aeConnection);
			
			// load document
			AEDocumentDescriptor documentDescr = (AEDocumentDescriptor) docDescr;
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(documentDescr.getDocumentType(), localConnection);
			AEDocument aeDocument = docDAO.load(docDescr);
			
			// load items
			AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
			aeDocument.setItems(itemDAO.load(docDescr));

			return aeDocument;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse openPurchasePeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			/**
			 * determine period
			 */
			AccPeriod accPeriod = null;
			if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoice.getSystemID()) {
				accPeriod = getLastClosedPeriod(
						aeRequest.getArguments().getLong("ownerId"),
						AEApp.PURCHASE_MODULE_ID,
						localConnection);
			} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
				accPeriod = getLastClosedPeriod(
						aeRequest.getArguments().getLong("ownerId"),
						AEApp.PURCHASE_FNP_MODULE_ID,
						localConnection);
			} else if(aeRequest.getArguments().optLong("docType") == AEDocumentType.System.AESaleInvoice.getSystemID()) {
				accPeriod = getLastClosedPeriod(
						aeRequest.getArguments().getLong("ownerId"),
						AEApp.SALE_MODULE_ID,
						localConnection);
			}
			
			if(accPeriod != null) {
				localConnection.beginTransaction();
				
				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());
				
				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());
				
				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());
			}
			
			/**
			 * commit transaction and return response
			 */
			localConnection.commit();
			
			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse openSalePeriod(AERequest aeRequest) throws AEException {
		// TODO Auto-generated method stub
		return null;
	}
	
//	private AccPeriod getLastClosedPeriod(long ownerId, long moduleId, AEConnection aeConnection) throws AEException {
//		AEConnection localConnection = null;
//		try {
//			AccPeriod accPeriod = null;
//			
//			DAOFactory daoFactory = DAOFactory.getInstance();
//			localConnection = daoFactory.getConnection(aeConnection);
//			
//			// detect the last closed period
//			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
//			accPeriod = accPeriodDAO.getLastClosedPeriod(ownerId, moduleId);
//
//			return accPeriod;
//		} catch (Throwable t) {
//			t.printStackTrace();
//			throw new AEException(t.getMessage(), t);
//		} finally {
//			AEConnection.close(localConnection);
//		}
//	}

	@Override
	public AEResponse delete(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			long sOwnerId = aeRequest.getArguments().optLong("sOwnerId");
			JSONObject jsonDocument = aeRequest.getArguments().getJSONObject("document");
			AEDocumentFactory docFactory = AEDocumentFactory.getInstance(
					AEDocumentType.valueOf(jsonDocument.getLong("xType")));
			AEDocument aeDocument = 
					docFactory.createDocument(aeRequest.getArguments().getJSONObject("document"));
			
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			/**
			 * Validation
			 */
			
			// delete document is allowed for administrator or poweruser or accounting's role 
			if(!(ap.hasAdministratorRights() 
					|| ap.hasPowerUserRights() 
					|| ap.isMemberOf(AuthRole.System.accountant)
					|| ap.isMemberOf(AuthRole.System.operative_accountant))) {
				
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException(); 
			}
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);
			
			// whether this document is ownered by this customer
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(aeDocument.getType(), localConnection);
			docDAO.ownershipValidation(jsonDocument.optLong("id"), sOwnerId);
			
			// validate in closed period if the document is not template
			if(!aeDocument.isTemplate()) {
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				boolean bRet = false;
				if(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoice).equals(aeDocument.getType())) {
					bRet = accPeriodDAO.isInClosedPeriod(
							aeDocument.getDate(), 
							aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
									AEApp.PURCHASE_MODULE_ID);
				} else if(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoiceFNP).equals(aeDocument.getType())) {
					bRet = accPeriodDAO.isInClosedPeriod(
							aeDocument.getDate(), 
							aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
									AEApp.PURCHASE_FNP_MODULE_ID);
				}  else if(AEDocumentType.valueOf(AEDocumentType.System.AESaleInvoice).equals(aeDocument.getType())) {
					bRet = accPeriodDAO.isInClosedPeriod(
							aeDocument.getDate(), 
							aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
									AEApp.SALE_MODULE_ID);
				}
				if(bRet) {
					throw new AEException(
							(int) AEError.System.CANNOT_DELETE_FROM_CLOSED_PERIOD.getSystemID(), 
					"Un document d\'une période clôturée ne peut pas être supprimer!");
				}
			}
			
			localConnection.beginTransaction();
			
			// delete document (tradeDocument and items will be auto deleted)
			docDAO.delete(jsonDocument.optLong("id"));
			
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
	public AEDocument loadHeader(AEDescriptor docDescr,
			AEInvocationContext invContext, AEConnection aeConnection)
			throws AEException {

		assert(docDescr instanceof AEDocumentDescriptor);
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection
			localConnection = daoFactory.getConnection(aeConnection);
			
			// load document
			AEDocumentDescriptor documentDescr = (AEDocumentDescriptor) docDescr;
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(documentDescr.getDocumentType(), localConnection);
			AEDocument aeDocument = docDAO.load(docDescr);
			
			return aeDocument;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse importInvoiceGuides(File impFile, AEInvocationContext invContext) throws AEException {
		FileInputStream fis = null; 
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * get connection to the DB
			 */	
			
			/**
			 * prepare dao's
			 */
			
			/**
			 * local attributes
			 */
			// prepare source in stream
			fis = new FileInputStream(impFile); 
			BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "Windows-1252"), 2048);
			JSONArray jsonArray = new JSONArray();
			JSONObject jsonDoc = null;
			String line = null;
			while((line = reader.readLine()) != null) {
				line = AEStringUtil.trim(line);
				JSONObject json = createInvoiceGuideItem(line);
				if(json != null) {
					switch(json.getInt("lineCode")) {
						case 10: {
							jsonDoc = json;
							jsonArray.put(jsonDoc);
							break;
						} case 20: {
							if(jsonDoc != null) {
								jsonDoc.getJSONArray("items").put(json);
							}
							break;
						}
					}
				}
			}
			
			// create and rerturn response
			JSONObject payload = new JSONObject();
			payload.put("items", jsonArray);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	private JSONObject createInvoiceGuideItem(String line) {
		if(AEStringUtil.isEmpty(line)) {
			return null;
		}
		
		JSONObject json = new JSONObject();
		
		LightStringTokenizer tokenizer = new LightStringTokenizer(line, "\t");
		int lineCode = 0;
		String token = null;
		
		//lineCode	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				lineCode = Integer.parseInt(token);
				json.put("lineCode", lineCode);
			} catch (Exception e) {
				return null;
			};
		} else {
			return null;
		}
		
		switch(lineCode) {
			case 10: {
				// document

				// items
				try {
					json.put("items", new JSONArray());
				} catch (JSONException e1) {}
				
				// partyName
				if(tokenizer.hasMoreTokens()) {
					token = AEStringUtil.trim(tokenizer.nextToken());
					try {
						json.put("partyName", token);
					} catch (Exception e) {};
				}
				
				// name
				if(tokenizer.hasMoreTokens()) {
					token = AEStringUtil.trim(tokenizer.nextToken());
					try {
						json.put("name", token);
					} catch (Exception e) {};
				}
				
				// journalCode
				if(tokenizer.hasMoreTokens()) {
					token = AEStringUtil.trim(tokenizer.nextToken());
					try {
						json.put("journalCode", token);
					} catch (Exception e) {};
				}
				
				// accCode
				if(tokenizer.hasMoreTokens()) {
					token = AEStringUtil.trim(tokenizer.nextToken());
					try {
						json.put("accCode", token);
					} catch (Exception e) {};
				}
				break;
			} case 20: {
				// accCode
				if(tokenizer.hasMoreTokens()) {
					token = AEStringUtil.trim(tokenizer.nextToken());
					try {
						json.put("accCode", token);
					} catch (Exception e) {};
				}
				
				// description
				if(tokenizer.hasMoreTokens()) {
					token = AEStringUtil.trim(tokenizer.nextToken());
					try {
						json.put("description", token);
					} catch (Exception e) {};
				}
				break;
			} default:
				return null;
		}
		
		return json;
	}

	@Override
	public AEResponse saveDocuments(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// factory and transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			long docType = aeRequest.getArguments().optLong("docType");

			// prepare accounts
			AccountDAO accDAO = DAOFactory.getInstance().getAccountDAO(localConnection);
			JSONArray accounts = null;
			if(docType == AEDocumentType.System.AEPurchaseInvoice.getSystemID()
					|| docType == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
				accounts = accDAO.loadSupplyAccounts(ownerId);
			} else if(docType == AEDocumentType.System.AESaleInvoice.getSystemID()) {
				accounts = accDAO.loadSaleAccounts(ownerId);
			}
			Map<String, JSONObject> accountsMap = new HashMap<String, JSONObject>();
			if(accounts != null) {
				for (int i = 0; i < accounts.length(); i++) {
					JSONObject acc = accounts.getJSONObject(i);
					accountsMap.put(acc.getString("code"), acc);
				}
			}
			
			// prepare parties
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			JSONArray parties = null;
			if(docType == AEDocumentType.System.AEPurchaseInvoice.getSystemID()
					|| docType == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
				parties = orgDAO.loadCompanies(ownerId, 30);
			} else if(docType == AEDocumentType.System.AESaleInvoice.getSystemID()) {
				parties = orgDAO.loadCompanies(ownerId, 10);
			}
			Map<String, JSONObject> partiesMap = new HashMap<String, JSONObject>();
			for (int i = 0; i < parties.length(); i++) {
				JSONObject party = parties.getJSONObject(i);
				partiesMap.put(party.getString("name"), party);
			}
			
			// get collection and save documents
			localConnection.beginTransaction();
			JSONArray jsonDocumentsArray = aeRequest.getArguments().getJSONArray("items");
			for (int i = 0; i < jsonDocumentsArray.length(); i++) {
				JSONObject jsonDocument = jsonDocumentsArray.getJSONObject(i);

				/**
				 * Init accounts
				 */
				JSONObject accJSON = (JSONObject) accountsMap.get(jsonDocument.optString("accCode"));
				if(accJSON != null && (docType == AEDocumentType.System.AEPurchaseInvoice.getSystemID()
						|| docType == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID())) {
					jsonDocument.put("issuerAccId", accJSON.optLong("id"));
				} else if(accJSON != null && docType == AEDocumentType.System.AESaleInvoice.getSystemID()) {
					jsonDocument.put("recipientAccId", accJSON.optLong("id"));
				}
				JSONArray itemsJSON = jsonDocument.optJSONArray("items");
				if(itemsJSON != null) {
					for (int j = 0; j < itemsJSON.length(); j++) {
						JSONObject jsonItem = itemsJSON.getJSONObject(j);
						JSONObject jsonAcc = accountsMap.get(jsonItem.optString("accCode"));
						if(jsonAcc != null) {
							jsonItem.put("accId", jsonAcc.optLong("id"));
						}
					}
				}
				
				/**
				 * Init party
				 */
				JSONObject partyJSON = (JSONObject) partiesMap.get(jsonDocument.optString("partyName"));
				if(partyJSON != null && (docType == AEDocumentType.System.AEPurchaseInvoice.getSystemID()
						|| docType == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID())) {
					jsonDocument.put("issuerId", partyJSON.optLong("id"));
				} else if(partyJSON != null && docType == AEDocumentType.System.AESaleInvoice.getSystemID()) {
					jsonDocument.put("recipientId", partyJSON.optLong("id"));
				}
				
				AEDocumentFactory docFactory = AEDocumentFactory.getInstance(
						AEDocumentType.valueOf(jsonDocument.getLong("xType")));

				/**
				 * Create document
				 */
				AEDocument aeDocument = docFactory.createDocument(jsonDocument);

				/**
				 * PreSave preparation
				 */
				if(aeDocument instanceof AETradeDocument) {
					vatCalculation((AETradeDocument) aeDocument);
				}

				/**
				 * Validate document save operation
				 */
				aeDocument.validateWith(docFactory.getSaveValidator());

//				// validate in opened period
//				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
//				boolean bRet = false;
//				if(!aeDocument.isTemplate()) {
//					// check closed period
//					if(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoice).equals(aeDocument.getType())) {
//						bRet = accPeriodDAO.isInClosedPeriod(
//								aeDocument.getDate(), 
//								aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
//										AEApp.PURCHASE_MODULE_ID);
//					} else if(AEDocumentType.valueOf(AEDocumentType.System.AESaleInvoice).equals(aeDocument.getType())) {
//						bRet = accPeriodDAO.isInClosedPeriod(
//								aeDocument.getDate(), 
//								aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
//										AEApp.SALE_MODULE_ID);
//					} else if(
//							AEDocumentType.valueOf(AEDocumentType.System.AECheque).equals(aeDocument.getType())
//							|| AEDocumentType.valueOf(AEDocumentType.System.AEVirement).equals(aeDocument.getType())
//							|| AEDocumentType.valueOf(AEDocumentType.System.AEPrelevement).equals(aeDocument.getType())
//							|| AEDocumentType.valueOf(AEDocumentType.System.AELCR).equals(aeDocument.getType())) {
//
//						bRet = accPeriodDAO.isInClosedPeriod(
//								aeDocument.getDate(), 
//								aeDocument.getCompany() != null ? aeDocument.getCompany().getDescriptor().getID() : AEPersistentUtil.NEW_ID,
//										AEApp.PAYMENT_MODULE_ID);
//					}
//				}
//				if(bRet) {
//					throw new AEException(
//							(int) AEError.System.CANNOT_INSERT_UPDATE_CLOSED_PERIOD.getSystemID(), 
//					"Période close, veuillez saisir la date de facture en reference!");
//				}

				// save in transaction
				switch(aeDocument.getPersistentState()) {
				case NEW:
					insert(aeDocument, invContext, localConnection);
					break;
				case UPDATED:
					update(aeDocument, invContext, localConnection);
					break;
				default:
					// internal error
					assert(false);
					break;
				}

				/**
				 *  process items
				 */
				processItems(aeDocument, invContext, localConnection);

			}

			localConnection.commit();

			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (AEException e) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			e.printStackTrace();
			throw e;
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			t.printStackTrace();
			throw new AEException((int) AEError.System.NA.getSystemID(), t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadSOT(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate caler
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			DAOFactory daoFactory = DAOFactory.getInstance();
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("ownerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);

			int docType = arguments.optInt("docType");
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			long moduleId = AEPersistentUtil.NEW_ID;
			if(AEDocumentType.valueOf(AEDocumentType.System.PERIMES).equals(aeDocType)) {
				moduleId = AEApp.PERIMES_MODULE_ID;
			} else if(AEDocumentType.valueOf(AEDocumentType.System.STOCKS).equals(aeDocType)) {
				moduleId = AEApp.STOCKS_MODULE_ID;
			} else {
				throw new AEException("System Error: Unknnown document type!");
			}
			boolean isTemplate = arguments.optBoolean("isTemplate");
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// load accounts
			JSONArray accounts = null;
			if(isTemplate) {
				AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
				accounts = accDAO.loadSubAccountsByOwner(ownerId);
			}
			
			/**
			 * determine AccPeriod
			 */
			boolean editable = !(aeRequest.getArguments().optBoolean("history"));
			AccPeriod accPeriod = null;
			if(!isTemplate) {
				if(editable) {
					// not history
					accPeriod = getFirstOpenPeriod(ownerId, moduleId, localConnection);
				} else {
					// history
					int month = aeRequest.getArguments().getInt("month");
					int year = aeRequest.getArguments().getInt("year");

					Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
					Date endDate = AEDateUtil.getLastDate(month - 1, year);
					accPeriod = daoFactory.getAccPeriodDAO(localConnection).loadAccPeriod(
							startDate, 
							endDate, 
							moduleId,
							ownerId);
				}
				if(accPeriod == null) {
					throw new AEException(
							AEError.System.ACC_PERIOD_WAS_NOT_FOUD.getSystemID(), 
					"La période n'a pas été trouvé.");
				}
			}
			
			// load or create document
			AEDocumentsList docsList = null;
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			if(isTemplate) {
				AEDocumentFilter templateFilter = new AEDocumentFilter();
				templateFilter.setTemplate(Boolean.TRUE);
				templateFilter.setDocType(aeDocType);
				templateFilter.setOwner(ownerDescr);
				docsList = loadDocumentsFull(templateFilter, invContext);
			} else if(accPeriod != null){
				AEDocumentFilter templateFilter = new AEDocumentFilter();
				templateFilter.setTemplate(Boolean.FALSE);
				templateFilter.setDocType(aeDocType);
				templateFilter.setOwner(ownerDescr);
				templateFilter.setDateFrom(accPeriod.getStartDate());
				templateFilter.setDateTo(accPeriod.getEndDate());
				docsList = loadDocumentsFull(templateFilter, invContext);
			}
			AETradeDocument aeDocument = null;
			if(!AECollectionUtil.isEmpty(docsList)) {
				aeDocument = (AETradeDocument) docsList.get(0);
			}
			
			if(aeDocument == null && !isTemplate && accPeriod != null) {
				// there is no saved document for specified period, so create it from template 
				aeDocument = createSOTDocument(aeDocType, ownerDescr, accPeriod, invContext);
			}
			
			if(aeDocument == null && isTemplate) {
				// there is no template, create it
				AEDocumentFactory docFactory = AEDocumentFactory.getInstance(aeDocType);
				if(docFactory != null) {
					aeDocument = (AETradeDocument) docFactory.createDocument(aeDocType);
					aeDocument.setCompany(ownerDescr);
					aeDocument.setIsTemplate(isTemplate);
				}
			}
			if(aeDocument != null) {
				aeDocument.sortItems(new AEDocumentItemSIndexComparator());
			}
			
			JSONObject payload = new JSONObject();
			if(accounts != null) {
				payload.put("accounts", accounts);
			}
			if(aeDocument != null) {
				payload.put("document", aeDocument.toJSONObject());
			}
			if(accPeriod != null) {
				payload.put("accPeriod", accPeriod.toJSONObject());
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEDocumentsList loadDocumentsFull(AEDocumentFilter docFilter, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// prepare DAO's and get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			AEDocumentDAO docDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			
			FileAttachmentDAO fileAttDAO = null;
			if(docFilter.isLoadAttachments()) {
				fileAttDAO = daoFactory.getFileAttachmentDAO(localConnection);
			}
			
			AEDocumentsList docItemsList = new AEDocumentsList();
			AEDescriptorsList docDescrList = docDAO.loadDescriptors(docFilter);
			if(!AECollectionUtil.isEmpty(docDescrList)) {
				for (AEDescriptor docDescr : docDescrList) {
					// load document
					AEDocument aeDocument = docDAO.load(docDescr);
					if(aeDocument != null) {
						// load items
						AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
						aeDocument.setItems(itemDAO.load(docDescr));
						
						//attachments
						if(docFilter.isLoadAttachments() && fileAttDAO != null) {
							aeDocument.setFileAttachment(fileAttDAO.loadTo(aeDocument.getDescriptor()));
						}
						
						docItemsList.add(aeDocument);
					}
				}
			}
			
			return docItemsList;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private AETradeDocument createSOTDocument(
			AEDocumentType aeDocType, AEDescriptor ownerDescr, AccPeriod accPeriod, AEInvocationContext invContext) throws AEException {

		// the SOT document
		AETradeDocument sotDocument = null;

		// load template for specified owner and doc type
		AEDocumentFilter templateFilter = new AEDocumentFilter();
		templateFilter.setTemplate(Boolean.TRUE);
		templateFilter.setDocType(aeDocType);
		templateFilter.setOwner(ownerDescr);
		AEDocumentsList docsList = loadDocumentsFull(templateFilter, invContext);
		if(!AECollectionUtil.isEmpty(docsList)) {
			AETradeDocument sotTemplate = (AETradeDocument) docsList.get(0);

			// validate template
			boolean validTemplate = true;
			// skip template validation
			// If you take a closer look at the menu, 
			// you'll find some lines with no account numbers indication. 
			// This is correct, as those amounts are only an indication, 
			// not to be entered directly in PGI via the export file.
//			AEDocumentItemsList templateItems = sotTemplate.getItems();
//			for (AEDocumentItem templateItem : templateItems) {
//				if(templateItem.getAccount() == null || templateItem.getAccountSecondary() == null) {
//					validTemplate = false;
//					break;
//				}
//			}

			if(validTemplate) {
				// create SOT document from template
				sotDocument = sotTemplate;
				
				// create from template
				sotDocument.setTemplate(sotTemplate.getDescriptor());
				sotDocument.setIsTemplate(false);
				sotDocument.setDate(accPeriod.getEndDate());
				
				// reset as new 
				sotDocument.setID(AEPersistentUtil.getTmpID());
				sotDocument.setPersistentState(State.NEW);
				
				// reset items as new
				AEDocumentItemsList docItems = sotDocument.getItems();
				for (AEDocumentItem docItem : docItems) {
					docItem.setID(AEPersistentUtil.getTmpID());
					docItem.setPersistentState(State.NEW);
				}
			}
		}
		
		return sotDocument;
	}

	@Override
	public AEResponse closeSOTPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("ownerId");

			int docType = arguments.optInt("docType");
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			AEDocumentDescriptor sotDocDescr = new AEDocumentDescriptorImp();
			sotDocDescr.create(arguments.optJSONObject("objDescr"));
			sotDocDescr.setClazz(DomainClass.AeDocument);
			
			long moduleId = AEPersistentUtil.NEW_ID;
			if(AEDocumentType.valueOf(AEDocumentType.System.PERIMES).equals(aeDocType)) {
				moduleId = AEApp.PERIMES_MODULE_ID;
			} else if(AEDocumentType.valueOf(AEDocumentType.System.STOCKS).equals(aeDocType)) {
				moduleId = AEApp.STOCKS_MODULE_ID;
			} else {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			/**
			 * Owner descriptor
			 */
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			AEDescriptive ownerDescriptive = orgDAO.loadDescriptive(ownerId);
			AEDescriptor ownerDescr = null;
			if(ownerDescriptive != null) {
				ownerDescr = ownerDescriptive.getDescriptor();
			} else {
				ownerDescr = Organization.lazyDescriptor(ownerId);
			}
			
			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					ownerId,
					moduleId,
					localConnection);
			if(accPeriod == null) {
				throw new AEException("Internal Error: Cannot detect accounting period!");
			}
			Date endDate = AEDateUtil.getClearDate(accPeriod.getEndDate());
			
			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(endDate)) {
				throw new AEException(
						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
						"La période n'est pas terminée.");
			}
			
			/**
			 * Load the SOT document
			 */
			AEDocumentDAO docDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			AEDocument sotDocument = docDAO.load(sotDocDescr.getDescriptor());
			if(sotDocument != null) {
				// load items
				AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
				sotDocument.setItems(itemDAO.load(sotDocDescr));
			} else {
				throw new AEException("System Error: The document cannot be loaded.");
			}
			
			/**
			 * Create and save the journal
			 */
			// create journal entry for this document
			AccJournalEntry docEntry = new AccJournalEntry();
			
			// Currency
			AEDescriptor currDescr = new AEDescriptorImp();
			currDescr.setCode("EUR");
			
			// journal 
			AEDescriptive journal = sotDocument.getJournal();
			AEDescriptor journalDescr = null;
			if(journal != null) {
				journalDescr = journal.getDescriptor();
			}
			
			// journal items
			AEDocumentItemsList sotItems = sotDocument.getItems();
			for (AEDocumentItem sotItem : sotItems) {
				// process item if it is suitable processing
				if(Math.abs(sotItem.getAmount()) >= 0.005
						&& sotItem.getAccount() != null
						&& sotItem.getAccountSecondary() != null) {
					// debit journal item
					AccJournalItem dtJournalItem = new AccJournalItem(accPeriod);

					dtJournalItem.setCompany(ownerDescr);
					dtJournalItem.setJournal(journalDescr);
					dtJournalItem.setDate(sotDocument.getDate());
					dtJournalItem.setAccount(sotItem.getAccount().getDescriptor());
					dtJournalItem.setAuxiliary(null);
					dtJournalItem.setDescription(sotItem.getDescription());
					dtJournalItem.setReference(sotDocDescr);
					dtJournalItem.setAccPeriod(accPeriod.getDescriptor());
					dtJournalItem.setCurrency(currDescr);
					dtJournalItem.setDtAmount(sotItem.getAmount());

					docEntry.addItem(dtJournalItem);
					
					// credit hournal item
					AccJournalItem ctJournalItem = new AccJournalItem(accPeriod);

					ctJournalItem.setCompany(ownerDescr);
					ctJournalItem.setJournal(journalDescr);
					ctJournalItem.setDate(sotDocument.getDate());
					ctJournalItem.setAccount(sotItem.getAccountSecondary().getDescriptor());
					ctJournalItem.setAuxiliary(null);
					ctJournalItem.setDescription(sotItem.getDescription());
					ctJournalItem.setReference(sotDocDescr);
					ctJournalItem.setAccPeriod(accPeriod.getDescriptor());
					ctJournalItem.setCurrency(currDescr);
					ctJournalItem.setCtAmount(sotItem.getAmount());

					docEntry.addItem(ctJournalItem);
				}
			}
			
			/**
			 * Begin Transaction
			 */
			localConnection.beginTransaction();
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
			
			// insert docEntry
			if(docEntry.getAccJournalItems() != null && !docEntry.getAccJournalItems().isEmpty()) {
				journalDAO.insertEntry(docEntry);
			}

			/**
			 * close the period
			 */
			accPeriodDAO.closePeriod(accPeriod.getID());
			
			/**
			 * commit transaction and return response 
			 */
			localConnection.commit();
			
			/**
			 * Process after close.
			 * Must be after transaction close.
			 * If after close fails, this should not affect the whole close task.
			 */
			try {
				// send e-mail
				Emailer emailer = new Emailer();
				emailer.onPeriodClosed(ownerDescr, invContext, accPeriod, moduleId);
				
				// auto export
				JSONObject exportRequestArguments = new JSONObject();
				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				if("secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "CEGID");
				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "SAGE");
				}
				exportRequestArguments.put("ownerId", ownerId);
				AERequest exportRequest = new AERequest(exportRequestArguments);
				accLocal.export(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			
//			return loadStatementOfTransactions(aeRequest, invContext);
			/**
			 * return empty response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse openSOTPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("ownerId");

			int docType = arguments.optInt("docType");
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			long moduleId = AEPersistentUtil.NEW_ID;
			if(AEDocumentType.valueOf(AEDocumentType.System.PERIMES).equals(aeDocType)) {
				moduleId = AEApp.PERIMES_MODULE_ID;
			} else if(AEDocumentType.valueOf(AEDocumentType.System.STOCKS).equals(aeDocType)) {
				moduleId = AEApp.STOCKS_MODULE_ID;
			} else {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
						ownerId,
						moduleId,
						localConnection);
			
			if(accPeriod != null) {
				localConnection.beginTransaction();
				
				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());
				
				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());
				
				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());
			}
			
			/**
			 * commit transaction and return response
			 */
			localConnection.commit();
			
			/**
			 * return empty response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadSOTExc(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate caler
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			DAOFactory daoFactory = DAOFactory.getInstance();
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("ownerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);

			int docType = arguments.optInt("docType");
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			long moduleId = AEPersistentUtil.NEW_ID;
			if(AEDocumentType.valueOf(AEDocumentType.System.SUIVI_GPL).equals(aeDocType)) {
				moduleId = AEApp.SUIVI_GPL_MODULE_ID;
			} else if(AEDocumentType.valueOf(AEDocumentType.System.DONNEES).equals(aeDocType)) {
				moduleId = AEApp.DONNEES_MODULE_ID;
			} else {
				throw new AEException("System Error: Unknnown document type!");
			}
			boolean isTemplate = arguments.optBoolean("isTemplate");
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// load accounts
			JSONArray accounts = null;
			JSONArray allCashAcounts = null;
			if(isTemplate) {
				AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
				accounts = accDAO.loadSubAccountsByOwner(ownerId);
				
				// load accounts
				allCashAcounts = new JSONArray();
				AEResponse aeResponseGOA = accLocal.loadGOA(aeRequest);
				JSONArray assetsArray = aeResponseGOA.getPayload().optJSONObject("assetsGOA").optJSONArray("accounts");
				if(assetsArray != null) {
					for(int i = 0; i < assetsArray.length(); i++) {
						allCashAcounts.put(assetsArray.getJSONObject(i));
					}
				}
				JSONArray expensesArray = aeResponseGOA.getPayload().optJSONObject("expensesGOA").optJSONArray("accounts");
				if(expensesArray != null) {
					for(int i = 0; i < expensesArray.length(); i++) {
						allCashAcounts.put(expensesArray.getJSONObject(i));
					}
				}
			}
			
			/**
			 * determine AccPeriod
			 */
			boolean editable = !(aeRequest.getArguments().optBoolean("history"));
			AccPeriod accPeriod = null;
			if(!isTemplate) {
				if(editable) {
					// not history
					accPeriod = getFirstOpenPeriod(ownerId, moduleId, localConnection);
				} else {
					// history
					int month = aeRequest.getArguments().getInt("month");
					int year = aeRequest.getArguments().getInt("year");

					Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
					Date endDate = AEDateUtil.getLastDate(month - 1, year);
					accPeriod = daoFactory.getAccPeriodDAO(localConnection).loadAccPeriod(
							startDate, 
							endDate, 
							moduleId,
							ownerId);
				}
				if(accPeriod == null) {
					throw new AEException(
							AEError.System.ACC_PERIOD_WAS_NOT_FOUD.getSystemID(), 
					"La période n'a pas été trouvé.");
				}
			}
			
			// load or create document
			AEDocumentsList docsList = null;
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			if(isTemplate) {
				AEDocumentFilter templateFilter = new AEDocumentFilter();
				templateFilter.setTemplate(Boolean.TRUE);
				templateFilter.setDocType(aeDocType);
				templateFilter.setOwner(ownerDescr);
				docsList = loadDocumentsFull(templateFilter, invContext);
			} else if(accPeriod != null){
				AEDocumentFilter templateFilter = new AEDocumentFilter();
				templateFilter.setTemplate(Boolean.FALSE);
				templateFilter.setDocType(aeDocType);
				templateFilter.setOwner(ownerDescr);
				templateFilter.setDateFrom(accPeriod.getStartDate());
				templateFilter.setDateTo(accPeriod.getEndDate());
				docsList = loadDocumentsFull(templateFilter, invContext);
			}
			AETradeDocument aeDocument = null;
			if(!AECollectionUtil.isEmpty(docsList)) {
				aeDocument = (AETradeDocument) docsList.get(0);
			}
			
			if(aeDocument == null && !isTemplate && accPeriod != null) {
				// there is no saved document for specified period, so create it from template 
				aeDocument = createSOTDocument(aeDocType, ownerDescr, accPeriod, invContext);
			}
			
			if(aeDocument == null && isTemplate) {
				// there is no template, create it
				AEDocumentFactory docFactory = AEDocumentFactory.getInstance(aeDocType);
				if(docFactory != null) {
					aeDocument = (AETradeDocument) docFactory.createDocument(aeDocType);
					aeDocument.setCompany(ownerDescr);
					aeDocument.setIsTemplate(isTemplate);
				}
			}
			
			if(aeDocument != null && !isTemplate && accPeriod != null) {
				invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
				synchronizeSOTExc(aeDocument, accPeriod, invContext);
			}
			if(aeDocument != null) {
				aeDocument.sortItems(new AEDocumentItemSIndexComparator());
			}
			
			JSONObject payload = new JSONObject();
			if(allCashAcounts != null) {
				payload.put("allCashAcounts", allCashAcounts);
			}
			if(accounts != null) {
				payload.put("accounts", accounts);
			}
			if(aeDocument != null) {
				payload.put("document", aeDocument.toJSONObject());
			}
			if(accPeriod != null) {
				payload.put("accPeriod", accPeriod.toJSONObject());
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closeSOTExcPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("ownerId");

			int docType = arguments.optInt("docType");
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			AEDocumentDescriptor sotDocDescr = new AEDocumentDescriptorImp();
			sotDocDescr.create(arguments.optJSONObject("objDescr"));
			sotDocDescr.setClazz(DomainClass.AeDocument);
			
			long moduleId = AEPersistentUtil.NEW_ID;
			if(AEDocumentType.valueOf(AEDocumentType.System.SUIVI_GPL).equals(aeDocType)) {
				moduleId = AEApp.SUIVI_GPL_MODULE_ID;
			} else if(AEDocumentType.valueOf(AEDocumentType.System.DONNEES).equals(aeDocType)) {
				moduleId = AEApp.DONNEES_MODULE_ID;
			} else {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			/**
			 * Owner descriptor
			 */
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			AEDescriptive ownerDescriptive = orgDAO.loadDescriptive(ownerId);
			AEDescriptor ownerDescr = null;
			if(ownerDescriptive != null) {
				ownerDescr = ownerDescriptive.getDescriptor();
			} else {
				ownerDescr = Organization.lazyDescriptor(ownerId);
			}
			
			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					ownerId,
					moduleId,
					localConnection);
			if(accPeriod == null) {
				throw new AEException("Internal Error: Cannot detect accounting period!");
			}
			Date endDate = AEDateUtil.getClearDate(accPeriod.getEndDate());
			
			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(endDate)) {
				throw new AEException(
						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
						"La période n'est pas terminée.");
			}
			
			/**
			 * Load the SOT document
			 */
			AEDocumentDAO docDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			AEDocument sotDocument = docDAO.load(sotDocDescr.getDescriptor());
			if(sotDocument != null) {
				// load items
				AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
				sotDocument.setItems(itemDAO.load(sotDocDescr));
			} else {
				throw new AEException("System Error: The document cannot be loaded.");
			}
			
			/**
			 * Create and save the journal
			 */
			// create journal entry for this document
			AccJournalEntry docEntry = new AccJournalEntry();
			
			// Currency
			AEDescriptor currDescr = new AEDescriptorImp();
			currDescr.setCode("EUR");
			
			// journal 
			AEDescriptive journal = sotDocument.getJournal();
			AEDescriptor journalDescr = null;
			if(journal != null) {
				journalDescr = journal.getDescriptor();
			}
			
			// journal items
			AEDocumentItemsList sotItems = sotDocument.getItems();
			for (AEDocumentItem sotItem : sotItems) {
				// process item if it is suitable processing
				if(Math.abs(sotItem.getAmount()) >= 0.005
						&& sotItem.getAccount() != null
						&& sotItem.getAccountSecondary() != null) {
					
					// debit journal item
					AccJournalItem dtJournalItem = new AccJournalItem(accPeriod);

					dtJournalItem.setCompany(ownerDescr);
					dtJournalItem.setJournal(journalDescr);
					dtJournalItem.setDate(sotDocument.getDate());
					dtJournalItem.setAccount(sotItem.getAccount().getDescriptor());
					dtJournalItem.setAuxiliary(null);
					dtJournalItem.setDescription(sotItem.getDescription());
					dtJournalItem.setReference(sotDocDescr);
					dtJournalItem.setAccPeriod(accPeriod.getDescriptor());
					dtJournalItem.setCurrency(currDescr);
					dtJournalItem.setDtAmount(sotItem.getAmount());

					docEntry.addItem(dtJournalItem);
					
					// credit hournal item
					AccJournalItem ctJournalItem = new AccJournalItem(accPeriod);

					ctJournalItem.setCompany(ownerDescr);
					ctJournalItem.setJournal(journalDescr);
					ctJournalItem.setDate(sotDocument.getDate());
					ctJournalItem.setAccount(sotItem.getAccountSecondary().getDescriptor());
					ctJournalItem.setAuxiliary(null);
					ctJournalItem.setDescription(sotItem.getDescription());
					ctJournalItem.setReference(sotDocDescr);
					ctJournalItem.setAccPeriod(accPeriod.getDescriptor());
					ctJournalItem.setCurrency(currDescr);
					ctJournalItem.setCtAmount(sotItem.getAmount());

					docEntry.addItem(ctJournalItem);
				}
			}
			
			/**
			 * Begin Transaction
			 */
			localConnection.beginTransaction();
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
			
			// insert docEntry
			if(docEntry.getAccJournalItems() != null && !docEntry.getAccJournalItems().isEmpty()) {
				journalDAO.insertEntry(docEntry);
			}

			/**
			 * close the period
			 */
			accPeriodDAO.closePeriod(accPeriod.getID());
			
			/**
			 * commit transaction and return response 
			 */
			localConnection.commit();
			
			/**
			 * Process after close.
			 * Must be after transaction close.
			 * If after close fails, this should not affect the whole close task.
			 */
			try {
				// send e-mail
				Emailer emailer = new Emailer();
				emailer.onPeriodClosed(ownerDescr, invContext, accPeriod, moduleId);
				
				// auto export
				JSONObject exportRequestArguments = new JSONObject();
				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				if("secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "CEGID");
				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "SAGE");
				}
				exportRequestArguments.put("ownerId", ownerId);
				AERequest exportRequest = new AERequest(exportRequestArguments);
				accLocal.export(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			
			/**
			 * return empty response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse openSOTExcPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.optLong("ownerId");

			int docType = arguments.optInt("docType");
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			long moduleId = AEPersistentUtil.NEW_ID;
			if(AEDocumentType.valueOf(AEDocumentType.System.SUIVI_GPL).equals(aeDocType)) {
				moduleId = AEApp.SUIVI_GPL_MODULE_ID;
			} else if(AEDocumentType.valueOf(AEDocumentType.System.DONNEES).equals(aeDocType)) {
				moduleId = AEApp.DONNEES_MODULE_ID;
			} else {
				throw new AEException("System Error: Unknnown document type!");
			}
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
						ownerId,
						moduleId,
						localConnection);
			
			if(accPeriod != null) {
				localConnection.beginTransaction();
				
				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());
				
				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());
				
				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());
			}
			
			/**
			 * commit transaction and return response
			 */
			localConnection.commit();
			
			/**
			 * return empty response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
	 * Refreshes data from DB, evaluate formulas and save the document.
	 * 
	 * @param sotExcDoc
	 * @param accPeriod
	 * @param invContext
	 * @return
	 * @throws AEException
	 */
	private AETradeDocument synchronizeSOTExc(AETradeDocument sotExcDoc, AccPeriod accPeriod, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			if(sotExcDoc == null || sotExcDoc.isTemplate() || sotExcDoc.getCompany() == null) {
				return sotExcDoc;
			}
			
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// prepare DAO's and get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			/**
			 * Sort doc items
			 */
			sotExcDoc.sortItems(new AEDocumentItemSIndexComparator());
			AEDocumentItemsList docItems = sotExcDoc.getItems();

			/**
			 * Load cash journal
			 */
			CashModuleDAO cashModuleDAO = daoFactory.getCashModuleDAO(localConnection);
			CashJournalEntriesList dbCashEntriesList =
				cashModuleDAO.loadJournalEntries(
						sotExcDoc.getCompany().getDescriptor().getID(), 
						accPeriod.getStartDate(), 
						accPeriod.getEndDate());

			/**
			 * Inject DB values
			 */
			for (AEDocumentItem docItem : docItems) {
				// process meta data
				if(AEValue.isNull(docItem.getMDValue())) {
					continue;
				}
				if(AEDocumentItem.MDNType.ACCOUNT.equals(docItem.getMDNType())) {
					// get the value from cashJournal for specified in the mdoel account
					double amount = AEMath.doubleValue(dbCashEntriesList.getGOASum(docItem.getMDValue().optLong()));
					docItem.setTaxableAmount(amount);
					docItem.setAmount(amount);
					docItem.setUpdated();
				} else if(AEDocumentItem.MDNType.ATTRIBUTE.equals(docItem.getMDNType())) {
					// get the value from cashJournal for specified in the mdoel account 
					double amount = AEMath.doubleValue(dbCashEntriesList.getAttributeSum(docItem.getMDValue().optLong()));
					docItem.setTaxableAmount(amount);
					docItem.setAmount(amount);
					docItem.setUpdated();
				}
			}

			/**
			 * Evaluate formulas.
			 */
			for (AEDocumentItem docItem : docItems) {
				// process meta data
				if(AEValue.isNull(docItem.getMDValue())) {
					continue;
				}
				if(AEDocumentItem.MDNType.EXPRESSION.equals(docItem.getMDNType())) {
					// extract the expression
					String expression = docItem.getMDValue().getString();

					// configure parser to evaluate expression
					JEP parser = new JEP();
					parser.setAllowUndeclared(true);
					parser.parseExpression(expression);

					// iterate, detect and set parameters
					SymbolTable paramsTable = parser.getSymbolTable();
					@SuppressWarnings("unchecked")
					Set<String> params = paramsTable.keySet();
					for (String paramName : params) {
						double paramValue = 0.0;
						// param is regexpr R1, R2, ....
						if(paramName.startsWith("R")) {
							try {
								int paramRowIndex = Integer.parseInt(paramName.substring(1)) - 1;
								for (AEDocumentItem docItem1 : docItems) {
									if(docItem1.getSequenceNumber() == paramRowIndex) {
										paramValue = docItem1.getAmount();
										paramValue = AEMath.round(paramValue, 2);
										break;
									}
								}
								parser.addVariable(paramName, paramValue);
							} catch (Exception e) {
								// error in the expression
								throw new AEException(e);
							}
						}
					}
					// variables are set, so evaluate
					double amount = parser.getValue();
					if(Double.isNaN(amount) || Double.isInfinite(amount)) {
						amount = 0.0;
					}
					amount = AEMath.round(amount, 2);
					docItem.setTaxableAmount(amount);
					docItem.setAmount(amount);
					docItem.setUpdated();
				}
			}

			/**
			 * Calculate amounts
			 */
			sotExcDoc.calculate();

			/**
			 * Save calculated document
			 */
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			save(sotExcDoc, invContext);
			return sotExcDoc;
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse recreateSOTExc(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate caler
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			localConnection.beginTransaction();
			
			/**
			 * delete old document
			 */
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			delete(aeRequest, invContext);
			
			/**
			 * Load a new one
			 */
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			AEResponse aeResponse = loadSOTExc(aeRequest, invContext);
			
			/**
			 * Commit and return
			 */
			localConnection.commit();
			return aeResponse;
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEResponse recreateSOT(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate caler
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			localConnection.beginTransaction();
			
			/**
			 * delete old document
			 */
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			delete(aeRequest, invContext);
			
			/**
			 * Load a new one
			 */
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			AEResponse aeResponse = loadSOT(aeRequest, invContext);
			
			/**
			 * Commit and return
			 */
			localConnection.commit();
			return aeResponse;
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse onValidated(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long docId = arguments.optLong("id");
			boolean validated = arguments.optBoolean("validated");

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			
			/**
			 * Get connection and begin transaction
			 */
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			//
			AEDocumentDAO docDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			if(validated) {
				docDAO.onValidated(docId, validated, invContext.getAuthPrincipal().getName(), new Date());
			} else {
				docDAO.onValidated(docId, validated, null, null);
			}
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			/**
			 * return empty response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse updateToLocked(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			AEDocumentDescriptor docDescr = new AEDocumentDescriptorImp();
			docDescr.create(arguments.getJSONObject("document"));

			// get connection and dao
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();
			
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(docDescr.getDocumentType(), localConnection);
			if(docDAO == null) {
				throw new AEException("System error: AEDocumentDAO cannot be created. May be invalid document type.");
			}
			
			docDAO.updateToLocked(docDescr);
			
			/**
			 * commit
			 */
			localConnection.commit();
			
			/**
			 * return empty response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse updateToNotLocked(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			AuthPrincipal ap = invContext.getAuthPrincipal();
			if(AuthRole.isOperative(ap)) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			AEDocumentDescriptor docDescr = new AEDocumentDescriptorImp();
			docDescr.create(arguments.getJSONObject("document"));

			// get connection and dao
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(docDescr.getDocumentType(), localConnection);
			if(docDAO == null) {
				throw new AEException("System error: AEDocumentDAO cannot be created. May be invalid document type.");
			}
			
			// update in transaction
			localConnection.beginTransaction();
			
			docDAO.updateToNotLocked(docDescr);
			
			/**
			 * commit
			 */
			localConnection.commit();
			
			/**
			 * return empty response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse onValidatedArray(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.toString()); // should be get
			JSONArray docIdsArray = arguments.getJSONArray(AEDocument.JSONKey.ids.toString()); // should be get
			boolean validated = arguments.getBoolean(AEDocument.JSONKey.validated.toString()); // should be get

			/**
			 * Whether this user is ownered by this customer
			 */
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			
			/**
			 * Get connection and begin transaction
			 */
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Process request
			 */
			List<Long> docIds = new ArrayList<Long>();
			for (int i = 0; i < docIdsArray.length(); i++) {
				docIds.add(docIdsArray.getLong(i));
			}
			
			AEDocumentDAO docDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			List<Long> retDocIds = null;
			if(validated) {
				retDocIds = docDAO.onValidated(docIds, ownerId, validated, invContext.getAuthPrincipal().getName(), new Date());
			} else {
				retDocIds = docDAO.onValidated(docIds, ownerId, validated, null, null);
			}
			
			JSONArray retDocIdsArray = new JSONArray();
			for (Long retDocId : retDocIds) {
				retDocIdsArray.put(retDocId);
			}
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEDocument.JSONKey.ids.toString(), retDocIdsArray);
			payload.put(AEDocument.JSONKey.validated.toString(), validated);
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}
