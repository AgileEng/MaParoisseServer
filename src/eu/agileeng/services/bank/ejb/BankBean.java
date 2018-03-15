package eu.agileeng.services.bank.ejb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.ejb.Stateless;

import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEExceptionsList;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.business.bank.BankAccountBalance;
import eu.agileeng.domain.business.bank.BankAccountsList;
import eu.agileeng.domain.business.bank.BankRecognitionRule;
import eu.agileeng.domain.business.bank.BankRecognitionRulesList;
import eu.agileeng.domain.business.bank.BankTransaction;
import eu.agileeng.domain.business.bank.BankTransactionsList;
import eu.agileeng.domain.business.bank.EbicsBankAccountTransactions;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.acc.AccountDAO;
import eu.agileeng.persistent.dao.cash.BankAccountDAO;
import eu.agileeng.persistent.dao.cash.BankJournalDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.persistent.dao.oracle.PartyDAO;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.bank.EbicsFilesComparator;
import eu.agileeng.util.AEFileUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.mail.Emailer;

/**
 * Agile Engineering Ltd
 * 
 * @author Vesko Vatov
 * @date 15.11.2013 12:00:43
 */
@Stateless
public class BankBean extends AEBean implements BankLocal, BankRemote {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4733340987749491268L;
	
	@Override
	public BankRecognitionRulesList saveRecognitionRules(BankRecognitionRulesList rules, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			
			localConnection.beginTransaction();
			
			BankJournalDAO bankJDAO = daoFactory.getBankJournalDAO(localConnection);
			for (BankRecognitionRule rule : rules) {
				//validate rule
				try {
					Pattern patt = Pattern.compile("^[A-Za-z0-9]{2}$");
					Matcher matcher = patt.matcher(rule.getCodeAFB());
					if (!matcher.find()) throw new AEException("Le code AFB '"+rule.getCodeAFB()+"' n’est pas valide. Pour toutes les règles, un code AFB en deux caractères doit être renseigné.");
					
					switch(rule.getRuleCondition()) {
					case BEGINS_WITH:
						Pattern.compile("^"+rule.getTextCondition());
						break;
					case CONTAINS:
						Pattern.compile(rule.getTextCondition());
						break;
					case ENDS_WITH:
						Pattern.compile(rule.getTextCondition()+"$");
						break;
					case EQUALS:
						Pattern.compile("^"+rule.getTextCondition()+"$");
						break;
					case NA:
						break;
					case REG_EXP:
						Pattern.compile(rule.getTextCondition());
						break;
					default:
						break;
					
					}
					
				} catch (PatternSyntaxException e) {
					throw new AEException("Invalid rule text matching condition!",e);
				}
				
				switch(rule.getPersistentState()) {
				case NEW: 
					preparePartyId(rule, localConnection);
					bankJDAO.insert(rule); 
					break;
				case UPDATED: 
					preparePartyId(rule, localConnection);
					bankJDAO.update(rule); 
					break;
				case DELETED: 
					bankJDAO.delete(rule); 
					break;
				case VIEW: 
					// to preparePartyId for old saved rules
					preparePartyId(rule, localConnection);
					bankJDAO.update(rule); 
					break;
				}
			}
			
			localConnection.commit();
			
			return rules;
		} catch(Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private void preparePartyId(BankRecognitionRule rule, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			// don't remove this check
			if(rule.getAuxiliary() == null) {
				return;
			}

			// manage auxiliaryId (partyId) by auxiliaryCode
			String auxiliaryCode = rule.getAuxiliary().getCode();
			if(!AEStringUtil.isEmpty(auxiliaryCode)) {
				// find the party by accId and auxiliaryCode
				if(rule.getAccount() != null && rule.getAccount().getID() > 0) {
					OrganizationDAO orgDADao = daoFactory.getOrganizationDAO(localConnection);
					Long partyId = orgDADao.loadIdByAccInfo(rule.getAccount().getID(), auxiliaryCode);
					if(partyId != null) {
						rule.getAuxiliary().setID(partyId);
					}
				}
			} else {
				rule.setAuxiliary(null);
			}
		} catch(Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public BankRecognitionRulesList saveRecognitionRules(AERequest aeRequest) throws AEException {
		BankRecognitionRulesList rules = null;
		AEConnection localConnection = null;
		
		try {
			//create java brr object
			BankRecognitionRule rule = new BankRecognitionRule();
			rule.create(aeRequest.getArguments().getJSONObject("recognitionRule"));
			//prepare with ownerId and bnakAccId
			rule.setCompany(Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId")));
			AEDescriptor bankAccDescr = BankAccount.lazyDescriptor(aeRequest.getArguments().getLong("bankAccId"));
			rule.setBankAccount(bankAccDescr);
			
			rules = new BankRecognitionRulesList();
			rules.add(rule);
			
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			saveRecognitionRules(rules, localConnection);
			
			return rules;
		} catch(Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public JSONObject loadRecognitionRules(AERequest aeRequest) throws AEException {
		JSONObject payload = null;
		AEConnection localConnection = null;
		
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			BankJournalDAO bankJDAO = daoFactory.getBankJournalDAO(localConnection);
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			
			//load rules
			BankRecognitionRulesList rules = bankJDAO.load(aeRequest.getArguments().getLong("bankAccountId"));
			
			JSONArray jsonRules = rules.toJSONArray();
			
			//load bank accounts
			JSONArray jsonAccounts = accDAO.loadBankAccounts(aeRequest.getArguments().optLong("ownerId"));
			
			
			payload = new JSONObject();
			payload.put("recognitionRules", jsonRules);
			payload.put("accAccounts", jsonAccounts);
			
			return payload;
		} catch (JSONException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEResponse importETEBAC(File fileETEBAC, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		FileInputStream fis = null; 
		BufferedReader reader = null;
		AEExceptionsList exceptionsList = new AEExceptionsList();
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			/**
			 * prepare dao's
			 */
			BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(localConnection);
			PartyDAO partyDAO = daoFactory.getOrganizationDAO(localConnection);

			/**
			 * local attributes
			 */
			BankTransactionsList btList = null;
			
			/**
			 * Represents one EBICS file where for one bank account
			 * we may have multiple sets of bank transactions (periods)
			 * 
			 * The bank account is defined with String ribWithoutCle
			 */
			Map<String, EbicsBankAccountTransactions> ebicsFile = new HashMap<String, EbicsBankAccountTransactions>();
			Map<String, BankAccount> bankAccounts = new HashMap<String, BankAccount>();
			Set<String> notUniqueSet = new HashSet<String>();
			
			// prepare source in stream
			fis = new FileInputStream(fileETEBAC); 
			reader = new BufferedReader(new InputStreamReader(fis, "Windows-1252"), 2048);
			int length = 120;
			char[] buffered = new char[length];
			int len = length;
			
			// start file processing record by record
			while(reader.read(buffered, 0, len) > -1) {
				if (len == length) {
					len = 1;
					reader.mark(length);
				} else {
					if (buffered[0] == '\r' || buffered[0] == '\n') {
						reader.mark(length);
					} else {
						reader.reset();
						len = length;
					}
					continue;
				}

				// create record with reference to file and entry
				String record = new String(buffered);
				BankTransaction bt = createBankTransactionETEBAC(record);
				if(bt == null) {
					// this record is not interested for us, so continue with next line
					continue;
				}
				
				AEDescriptor statementRef = new AEDescriptorImp()
					.withDescriptor(Integer.toString(record.hashCode()))
					.withName(fileETEBAC.getName());
				bt.setStatementRef(statementRef);

				// process record
				AEDescriptor bankAccountDescr = bt.getBankAccount(); // here bankAccountDescr has no id, only code contains ribWithoutCle
				String ribWithoutCle = bankAccountDescr.getCode(); // this is the unique key for bank account
				
				// ensure the room for this new record
				EbicsBankAccountTransactions ebicsBankAccountTransactions = ebicsFile.get(ribWithoutCle);
				if(ebicsBankAccountTransactions == null) {
					// not processed bank account in this file
					
					// check in notUnique set
					if(notUniqueSet.contains(ribWithoutCle)) {
						continue;
					}
					
					// search bank account in database
					BankAccountsList bankAccountsList = bankAccountDAO.loadByRibWithoutCle(ribWithoutCle);

					long bankAccountsCount = bankAccountsList.size();
					if(bankAccountsCount == 1) {
						// bank account has been unique identified so init instance
						BankAccount bankAccount = bankAccountsList.get(0); 
						
						if(bankAccount.getEntryType() != BankAccount.ENTRY_TYPE_ETEBAC) {
							String errMsg = String.format(
									"WARN [%s] [%s] The bank account is not configured for Ebics processing ",
									fileETEBAC.getName(), bankAccount.getRib());
							exceptionsList.add(new AEException(errMsg));

							// set as notUnique
							notUniqueSet.add(ribWithoutCle);

							continue;
						}
						
						// company
						AEDescriptor companyDescr = partyDAO.loadDescriptor(bankAccount.getCompany().getDescriptor().getID());
						bankAccount.setCompany(companyDescr);
						
						bankAccountDescr.setID(bankAccount.getID());
						bankAccountDescr.setName(bankAccount.getName());
						bankAccountDescr.setDescription(bankAccount.getDescription());
						
						bankAccounts.put(ribWithoutCle, bankAccount);
					} else {
						// process exception
						if(bankAccountsCount == 0) {
							String errMsg = String.format(
									"Attention: [%s] [%s] Des écritures sont disponibles pour ce RIB qui n'est pas enregistré sur Monentreprise Vérifiez à quel dossier il appartient.",
									fileETEBAC.getName(), ribWithoutCle);
							exceptionsList.add(new AEException(errMsg));
							
							// set as notUnique
							notUniqueSet.add(ribWithoutCle);
							
							continue;
						} else {
							// case when bankAccountsCount > 1, this is configuration error
							String errMsg = String.format(
									"Attention: [%s] [%s] Ce RIB a été enregistré plusieurs fois sur Monentreprise Vérifiez à quel dossier il appartient.",
									fileETEBAC.getName(), ribWithoutCle);
							exceptionsList.add(new AEException(errMsg));
							
							// set as notUnique
							notUniqueSet.add(ribWithoutCle);
							
							continue;
						}
					}

					// we have valid bank account, create room for it
					ebicsBankAccountTransactions = new EbicsBankAccountTransactions();
					ebicsFile.put(ribWithoutCle, ebicsBankAccountTransactions);
				}

				// detect bank transaction list where to put transaction
				if("01".equalsIgnoreCase(bt.getCode()) && bt.getDtAmount() != null) {
					// precondition
					assert(bt == null);

					// start new bank transactions list
					btList = new BankTransactionsList();

					btList.setOpeningBalance(bt.getDtAmount());
					btList.setOpeningBalanceDate(bt.getDate());
					btList.add(bt);
				} else if("07".equalsIgnoreCase(bt.getCode()) && bt.getDtAmount() != null) {
					// precondition
					assert(bt != null);

					btList.setFinalBalance(bt.getDtAmount());
					btList.setFinalBalanceDate(bt.getDate());
					btList.add(bt);

					// finish current bank transactions list 
					btList.extractEbicsDates();
					for (BankTransaction bankTransaction : btList) {
						bankTransaction.setPeriodFinalDate(bt.getDate());
					}

					ebicsBankAccountTransactions.add(btList);

					// Now there is no current bank transaction list, so set it to null
					btList = null;
				} else if("04".equalsIgnoreCase(bt.getCode())) {
					// precondition
					assert(bt != null);

					// add transaction to the current bank transactions list
					btList.add(bt);
				}
			}

			/**
			 * Close the file
			 */
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
			
			/**
			 * Import parsed file
			 */
			AEExceptionsList importExceptionsList = importEbicsFile(ebicsFile, bankAccounts, fileETEBAC.getName(), localConnection);
			exceptionsList.addAll(importExceptionsList);
			
			/**
			 * Process exceptions
			 */
			if(exceptionsList != null && !exceptionsList.isEmpty()) {
				// send e-mail
				Emailer emailer = new Emailer();
				emailer.onEtebacsReport(exceptionsList, invContext);
				
				// log
				for (AEException aeException : exceptionsList) {
					AEApp.logger().error(aeException.getMessage());
				}
			}
			
			/**
			 * Move from inbox
			 */
			Properties props = AEApp.getInstance().getProperties();
			if(exceptionsList == null || exceptionsList.isEmpty()) {
				try {
					File folderImported = new File(props.getProperty("jedeclare.mail.local.folder.imported"));
					folderImported.mkdirs();
					FileUtils.moveFile(fileETEBAC, AEFileUtil.createNextVersionFile(fileETEBAC, folderImported));
				} catch(Throwable t) {
					AEApp.logger().error("Cannot move processed Ebics file to imported folder: " + t.getMessage());
				}
			} else if(!checkForDelayingExceptions(exceptionsList)) {
				// move to failed
				try {
					File folderFailed = new File(props.getProperty("jedeclare.mail.local.folder.failed"));
					folderFailed.mkdirs();
					FileUtils.moveFile(fileETEBAC, AEFileUtil.createNextVersionFile(fileETEBAC, folderFailed));
				} catch(Throwable t) {
					AEApp.logger().error("Cannot move processed Ebics file to failed folder " + t.getMessage());
				}
			}
			
//			/////////////////////////////////////////
//			// Remove
//			BankTransactionsList tmpBankTransactionsList = new BankTransactionsList();
//			Set<String> ribWithoutCleSet = ebicsFile.keySet();
//			for (String ribWithoutCle : ribWithoutCleSet) {
//				EbicsBankAccountTransactions ebicsBankAccountTransactions = ebicsFile.get(ribWithoutCle);
//				for (BankTransactionsList ebicsBankTransactionsList : ebicsBankAccountTransactions) {
//					tmpBankTransactionsList = ebicsBankTransactionsList;
//				}
//			}
			
			// create and rerturn response
			JSONObject payload = new JSONObject();
//			try {
//				payload.put("items", tmpBankTransactionsList.toJSONArray());
//				payload.put("openingBalance", tmpBankTransactionsList.getOpeningBalance());
//				payload.put(
//						"openingBalanceDate", 
//						AEDateUtil.convertToString(tmpBankTransactionsList.getOpeningBalanceDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
//				payload.put("finalBalance", tmpBankTransactionsList.getFinalBalance());
//				payload.put(
//						"finalBalanceDate", 
//						AEDateUtil.convertToString(tmpBankTransactionsList.getFinalBalanceDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
//				
//				return new AEResponse(payload);
//			} catch (Exception e) {
//				return new AEResponse(new JSONObject());
//			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
			AEConnection.close(localConnection);
		}
	}
	
	private boolean checkForDelayingExceptions(AEExceptionsList aeExceptions) {
		boolean bRet = false;
		
		for (AEException aeException : aeExceptions) {
			if(aeException.getCode() == AEError.System.BANK_ETEBAC_BUNDLE_OPENING_BALANCE_DOESNT_MATCH.getSystemID()) {
				bRet = true;
			}
		}
		
		return bRet;
	}
	
	/**
	 * Creates an BankTransaction instance from specified <code>etebacLine</code>
	 * for lines with codes "01", "04" and "07".
	 * Another codes are not processed and this method will return <code>null</code> for them.
	 * 
	 * @param etebacLine
	 * @return
	 * @throws AEException
	 */
	private BankTransaction createBankTransactionETEBAC(String etebacLine) throws AEException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyy");
		BankTransaction bt = null;

		// the type of this transaction
		String trCode = etebacLine.substring(0, 2);
		
		// bbbbbgggggcccccccccccxx
		// b = National bank code
		// g = Branch code (fr:code guichet)
		// c = Account number
		// x = National check digits (fr:clé RIB)
		String bankCode = etebacLine.substring(2, 7);        // 5 digits (N 5)
		String branchCode = etebacLine.substring(11, 16);    // 5 digits (N 5)
		String accountNumber = etebacLine.substring(21, 32); // 11 character/digit (AN 11)
		String ribWithoutCle = bankCode + branchCode + accountNumber; 
		
		AEDescriptor bankAccount = BankAccount.lazyDescriptor(-1);
		bankAccount.setCode(ribWithoutCle);
		
		if("04".equals(trCode)) {
			// movement
			bt = new BankTransaction();
			bt.setCode(trCode);

			// the code of this line
			bt.setCodeOperation(etebacLine.substring(32, 34));

			// description
			bt.setDescription(etebacLine.substring(48, 79));

			// document reference
			String rawRef = etebacLine.substring(81, 88);
			if("0000000".equals(rawRef) == false) {
				AEDocumentDescriptor reference = new AEDocumentDescriptorImp();
				reference.setDescription(rawRef);
				bt.setReference(reference);
			}

			// transaction date
			try {
				bt.setDate(dateFormat.parse(etebacLine.substring(34, 40)));
			} catch (ParseException e) {
				throw new AEException(e);
			}

			// transaction amount
			String strAmount = etebacLine.substring(90, 104);
			double amount = decodeETEBACAmount(strAmount);
			if(amount > 0) {
				bt.setDtAmount(new Double(Math.abs(amount)));
			} else {
				bt.setCtAmount(new Double(Math.abs(amount)));
			}
		} else if("01".equals(trCode)) {
			// opening balance
			bt = new BankTransaction();
			bt.setCode(trCode);

			// the code of this line
			bt.setCodeOperation("99");
			
			// description
			bt.setDescription("ANCIEN SOLDE");
			
			// transaction date
			try {
				bt.setDate(dateFormat.parse(etebacLine.substring(34, 40)));
			} catch (ParseException e) {
				throw new AEException(e);
			}

			// amount
			String strAmount = etebacLine.substring(90, 104);
			bt.setDtAmount(decodeETEBACAmount(strAmount));
		} else if("07".equals(trCode)) {
			// final balance
			bt = new BankTransaction();
			bt.setCode(trCode);

			// the code of this line
			bt.setCodeOperation("99");
			
			// description
			bt.setDescription("NOUVEAU SOLDE");

			// transaction date
			try {
				bt.setDate(dateFormat.parse(etebacLine.substring(34, 40)));
			} catch (ParseException e) {
				throw new AEException(e);
			}

			// amount
			String strAmount = etebacLine.substring(90, 104);
			bt.setDtAmount(decodeETEBACAmount(strAmount));
		}
		
		// set common fields
		if(bt != null) {
			bt.setCode(trCode);
			bt.setBankAccount(bankAccount);
		}

		return bt;
	}
	
	private double decodeETEBACAmount(String strAmount) throws AEException {
		// paranoic check
		if(AEStringUtil.isEmpty(strAmount)) {
			return 0.0;
		}

		// real decode
		try {
			String retString = AEStringUtil.EMPTY_STRING;
			int sign = 1;
			char lastChar = strAmount.charAt(strAmount.length() - 1);
			switch (lastChar) {
			case 'A':
				retString = strAmount.replace('A', '1');
				break;
			case 'B':
				retString = strAmount.replace('B', '2');
				break;
			case 'C':
				retString = strAmount.replace('C', '3');
				break;
			case 'D':
				retString = strAmount.replace('D', '4');
				break;
			case 'E':
				retString = strAmount.replace('E', '5');
				break;
			case 'F':
				retString = strAmount.replace('F', '6');
				break;
			case 'G':
				retString = strAmount.replace('G', '7');
				break;
			case 'H':
				retString = strAmount.replace('H', '8');
				break;
			case 'I':
				retString = strAmount.replace('I', '9');
				break;
			case '{':
				retString = strAmount.replace('{', '0');
				break;
			case '}':
				retString = strAmount.replace('}', '0');
				sign = -1;
				break;
			case 'J':
				retString = strAmount.replace('J', '1');
				sign = -1;
				break;
			case 'K':
				retString = strAmount.replace('K', '2');
				sign = -1;
				break;
			case 'L':
				retString = strAmount.replace('L', '3');
				sign = -1;
				break;
			case 'M':
				retString = strAmount.replace('M', '4');
				sign = -1;
				break;
			case 'N':
				retString = strAmount.replace('N', '5');
				sign = -1;
				break;
			case 'O':
				retString = strAmount.replace('O', '6');
				sign = -1;
				break;
			case 'P':
				retString = strAmount.replace('P', '7');
				sign = -1;
				break;
			case 'Q':
				retString = strAmount.replace('Q', '8');
				sign = -1;
				break;
			case 'R':
				retString = strAmount.replace('R', '9');
				sign = -1;
				break;
			}

			return AEMath.round(AEMath.parseDouble(retString, true) * sign / 100, 2);
		} catch(Exception e) {
			throw new AEException(e);
		}
	}
	
	/**
	 * Import into database.
	 * 
	 * @param ebicsFile Parsed EBICS file
	 * @param bankAccounts Heavy initialized bank account instances found in EBICS file  
	 * @param aeConnection The DB connection for use
	 * @return <code>true</code> if and only if the every bank transactions list is imported into DB 
	 * 		(ie the whole <code>ebicsFile</code> is imported); false otherwise.
	 * @throws AEException
	 */
	private AEExceptionsList importEbicsFile(Map<String, EbicsBankAccountTransactions> ebicsFile, Map<String, BankAccount> bankAccounts, String ebicsFileName, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			AEExceptionsList exceptionsList = new AEExceptionsList();
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * prepare dao's
			 */
			BankJournalDAO bankJDAO = daoFactory.getBankJournalDAO(localConnection);
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			BankAccountDAO bankAccDAO = daoFactory.getBankAccountDAO(localConnection);
			
			/**
			 * Process parsed Ebics file
			 */
			Set<String> ribWithoutCleSet = ebicsFile.keySet();
			for (String ribWithoutCle : ribWithoutCleSet) {
				// go to processing bank account
				
				// get bank account
				BankAccount bankAccount = bankAccounts.get(ribWithoutCle);
				
				// Do it for every bank account 
				AccPeriod accPeriod = getFirstOpenPeriod(bankAccount.getCompany().getDescriptor().getID(), AEApp.BANK_MODULE_ID, localConnection);
				
				// process bank account by Ebics bundles
				EbicsBankAccountTransactions ebicsBankAccountTransactions = ebicsFile.get(ribWithoutCle);
				for (BankTransactionsList ebicsBankTransactionsList : ebicsBankAccountTransactions) {
					// ebicsBankTransactionsList is an atomic bank transactions list (bundle) from EBICS file
					
					/**
					 * check whether ebicsBankTransactionsList can be imported into DB
					 */
					
					// 20140220 found exception: ebicsBankTransactionsList without movements
					if(!ebicsBankTransactionsList.haveEbicsMovements()) {
						// there are no movements
						
						// paranoic check for internal error
						if(ebicsBankTransactionsList.getOpeningBalance() != ebicsBankTransactionsList.getFinalBalance()) {
							throw new AEException("Internal error: Bundle without movements and OP != FB");
						}
						
						// don't process this bundle
						// and continue to next bundle from Etebics file
						continue;
					}
					
					// The first ebics transaction SHOULD NOT BE in closed period
					// We are checking the first transaction date because opening balance date
					// can be in previous month
					if(ebicsBankTransactionsList.getFirstTransactionDate().before(accPeriod.getStartDate())) {
						String errMsg = String.format(
								AEError.System.BANK_ETEBAC_BUNDLE_IN_CLOSED_PERIOD.getMessage(),
								ebicsFileName, ribWithoutCle, bankAccount.getCompany().getDescriptor().getName());
						exceptionsList.add(new AEException(AEError.System.BANK_ETEBAC_BUNDLE_IN_CLOSED_PERIOD.getSystemID(), errMsg));
						continue; // to next bundle from Etebics file
					}
					
					// Ebics opening balance should match previous DB final balance
					// do it for every ebicsBankTransactionsList to get the correct DB balance
					BankAccountBalance bab = bankAccDAO.load(bankAccount.getDescriptor(), accPeriod.getDescriptor());
					BankAccountBalance finalBalance = bankJDAO.calculateFinalBalance(
							bankAccount.getDescriptor(), 
							bab != null ? AEMath.doubleValue(bab.getFinalBalance()) : 0.0, 
							accPeriod.getEndDate());
					double ebicsOpeningBalance = AEMath.doubleValue(ebicsBankTransactionsList.getOpeningBalance());
					double dbFinalBalance = AEMath.doubleValue(finalBalance.getFinalBalance());
					

					// To me if there is a missing file, i.e. initial balance from the file that 
					// I try to import is different from final calculated balance, 
					// then we stop to import bank writing into this file until 
					// there is a fix to this situation 
					// (missing writings are seized, initial balance is modified, or we recover the missing file.

					// validate the next bundle in the chain (only amount) 
					if(Math.abs(dbFinalBalance - ebicsOpeningBalance) >= 0.005) {
						// opening balance cannot match in cases below:
						// - already imported bundle;
						// - missing bundle;
						// - error in initial configuration
						
						// check for already imported bundle
						AEDescriptor statementRef = new AEDescriptorImp().withName(ebicsFileName);
						boolean alreadyImported = bankJDAO.ebicsImportExists(bankAccount.getCompany().getDescriptor(), bankAccount.getDescriptor(), statementRef);
						if(alreadyImported) {
							continue; // to next bundle from Etebics file without error
						}
						
						// missing bundle or error in initial configuration
						String errMsg = String.format(
								AEError.System.BANK_ETEBAC_BUNDLE_OPENING_BALANCE_DOESNT_MATCH.getMessage(),
								ebicsFileName, ribWithoutCle, bankAccount.getCompany().getDescriptor().getName(), ebicsOpeningBalance, dbFinalBalance);
						exceptionsList.add(new AEException(AEError.System.BANK_ETEBAC_BUNDLE_OPENING_BALANCE_DOESNT_MATCH.getSystemID(), errMsg));
						continue; // to next bundle from Etebics file
					}

					
					// ebicsBankTransactionsList can be imported
					
					// init with default account
					AEDescriptor diversAccAccountDescr = accDAO.loadDescrByOwnerAndDiversAccCode(bankAccount.getCompany().getDescriptor().getID());
					for (BankTransaction ebicsBankTransaction : ebicsBankTransactionsList) {
						ebicsBankTransaction.setAccount(diversAccAccountDescr);
						ebicsBankTransaction.setAuxiliary(null);
					}
					
					// recognize
					BankRecognitionRulesList rules = bankJDAO.load(bankAccount.getID());
					recognizeBankTransactionsList(ebicsBankTransactionsList, rules);
					
					// prepare ebicsBankTransactionsList for save
					for (BankTransaction ebicsBankTransaction : ebicsBankTransactionsList) {
						ebicsBankTransaction.setCompany(bankAccount.getCompany());
						ebicsBankTransaction.setBankAccount(bankAccount.getDescriptor());
						ebicsBankTransaction.setJournal(bankAccount.getAccJournal().getDescriptor());
						ebicsBankTransaction.setAccPeriod(accPeriod.getDescriptor());
						ebicsBankTransaction.setPeriodFinalDate(ebicsBankTransactionsList.getFinalBalanceDate());
					}
					
					/**
					 * Save in transaction (this is one bundle from Ebics file)
					 */
					localConnection.beginTransaction();
					
					// transactions
					saveBankTransactions(ebicsBankTransactionsList, bankAccount, accPeriod, localConnection);
					
					// balance
					// don't check for balance
					// it should throw exception if balance is null
					BankTransactionsList dbBankTransactionsList = loadBankTransactions(bankAccount, accPeriod, localConnection);
					bab.calculate(dbBankTransactionsList);
					bankAccDAO.update(bab);
					
					localConnection.commit();
				}
			}

			return exceptionsList;
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private BankTransactionsList loadBankTransactions(BankAccount bankAccount, AccPeriod accPeriod, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * prepare dao's
			 */
			BankJournalDAO bankJournalDAO = daoFactory.getBankJournalDAO(localConnection);
			// BankAccountDAO bankAccDAO = daoFactory.getBankAccountDAO(localConnection); 

			/**
			 * local attributes
			 */
			AEDescriptor ownerDescr = bankAccount.getCompany().getDescriptor();
			AEDescriptor bankAccDescr = bankAccount.getDescriptor();

			/**
			 * determine AccPeriod
			 */
			if(accPeriod == null) {
				accPeriod = getFirstOpenPeriod(ownerDescr.getID(), AEApp.BANK_MODULE_ID, localConnection);
			}
			if(accPeriod == null) {
				throw new AEException(AEError.System.ACC_PERIOD_WAS_NOT_FOUD.getSystemID(), "La période n'a pas été trouvé.");
			}

			// load transactions
			BankTransactionsList bankTransactions = bankJournalDAO.load(
					ownerDescr, 
					bankAccDescr, 
					accPeriod.getStartDate(), 
					accPeriod.getEndDate());

//			// load balance
//			BankAccountBalance bab = bankAccDAO.load(
//					bankAccDescr, 
//					accPeriod.getDescriptor());
//			
//			if(bab == null) {
//				bab = new BankAccountBalance();
//				bab.setBankAccount(bankAccDescr);
//				bab.setAccPeriod(accPeriod);
//
//				// try to find last closed balance
//				AccPeriod lastClosedPeriod = getLastClosedPeriod(ownerDescr.getID(), AEApp.BANK_MODULE_ID, localConnection);
//				if(lastClosedPeriod != null) {
//					BankAccountBalance closedBB = bankAccDAO.load(
//							bankAccDescr, 
//							lastClosedPeriod.getDescriptor());
//					if(closedBB != null) {
//						bab.setOpeningBalance(closedBB.getFinalBalance());
//					}
//				}
//
//				bab.calculate(bankTransactions);
//				bankAccDAO.insert(bab);
//			} else {
//				bankTransactions.setOpeningBalance(bab.getOpeningBalance());
//				bankTransactions.setFinalBalance(bab.getFinalBalance());
//				
//				bankTransactions.extractDates();
//			}

			return bankTransactions;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
//	private AccPeriod getFirstOpenPeriod(long ownerId, long moduleId, AEConnection aeConnection) throws AEException {
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
//			throw new AEException(t);
//		} finally {
//			AEConnection.close(localConnection);
//		}
//	}
	
	/**
	 * Saves specified transactions without update balances!!!
	 * 
	 * @param bankTransactionsList
	 * @param bankAccount
	 * @param accPeriod
	 * @param aeConnection
	 * @return
	 * @throws AEException
	 */
	private BankTransactionsList saveBankTransactions(BankTransactionsList bankTransactionsList, BankAccount bankAccount, AccPeriod accPeriod, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * DAOs
			 */
			BankJournalDAO bankJournalDAO = daoFactory.getBankJournalDAO(localConnection);
//			BankAccountDAO bankAccDAO = daoFactory.getBankAccountDAO(localConnection);

			// save 
			for (Iterator<BankTransaction> iterator = bankTransactionsList.iterator(); iterator.hasNext();) {
				BankTransaction bankTransaction = (BankTransaction) iterator.next();
				switch(bankTransaction.getPersistentState()) {
					case NEW:
						bankJournalDAO.insert(bankTransaction);
						break;
					case UPDATED:
						bankJournalDAO.update(bankTransaction);
						break;
					case DELETED:
						bankJournalDAO.delete(bankTransaction);
						iterator.remove();
						break;
					default:
						break;
				}
			}

//			// update balance
//			BankAccountBalance bab = bankAccDAO.load(bankAccount.getDescriptor(), accPeriod.getDescriptor());
//			bab.calculate(bankTransactionsList);
//			
//			BankAccountDAO babDAO = daoFactory.getBankAccountDAO(localConnection);
//			babDAO.update(bab);

			return bankTransactionsList;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public void recognizeBankTransactionsList(BankTransactionsList bankTransList, BankRecognitionRulesList rulesList) throws AEException {
		
		for(BankTransaction t : bankTransList) {
			BankRecognitionRulesList matchingRules = new BankRecognitionRulesList();
			
			// skip balance transactions
			if("01".equals(t.getCode()) || "07".equals(t.getCode())) {
				continue;
			}
			
			//find matching rules
			for(BankRecognitionRule rule: rulesList) {
				if (t.getCodeOperation().equals(rule.getCodeAFB())) {
					Pattern patt = null;
					
					try {
						//construct the text condition if available
						switch(rule.getRuleCondition()) {
						case BEGINS_WITH:
							patt = Pattern.compile("^"+rule.getTextCondition());
							break;
						case CONTAINS:
							patt = Pattern.compile(rule.getTextCondition());
							break;
						case ENDS_WITH:
							patt = Pattern.compile(rule.getTextCondition()+"$");
							break;
						case EQUALS:
							patt = Pattern.compile("^"+rule.getTextCondition()+"$");
							break;
						case REG_EXP:
							patt = Pattern.compile(rule.getTextCondition());
							break;
						case NA:
						default:
							break;
						
						}
					} catch (PatternSyntaxException e) {
						throw new AEException("The rule "+rule.getRuleCondition().name()+" '"+rule.getTextCondition()+"' failed compilation!", e);
					}
					
					if (patt != null) {
						Matcher matcher = patt.matcher(t.getDescription());
						
						if(matcher.find()) matchingRules.add(rule);
					} else {
						matchingRules.add(rule);
					}
					
				}
			}
			
			//compare matching rules
			AEDescriptor account = AccAccount.lazyDescriptor(0);
			AEDescriptor aux = new AEDescriptorImp();
			aux.setCode(null);
			
			//match every rule against the first one
			for(BankRecognitionRule matchingRule: matchingRules) {
				if(matchingRule.getAccount() != null) {
					if (account.getID() == 0) {
						//if the variables haven't been set - set them
						//considering that would happen on the first element
						account.setID(matchingRule.getAccount().getID());
						aux = matchingRule.getAuxiliary();
					} else if (account.getID() != matchingRule.getAccount().getID() || (aux != matchingRule.getAuxiliary() && 
							((aux == null || matchingRule.getAuxiliary() == null) || !AEStringUtil.equals(aux.getCode(), matchingRule.getAuxiliary().getCode())))) {
						//the rules differ
						account.setID(0);
						aux.setCode(null);
						break;
					}
				}
			}
			
			//make decision about the transaction
			if (account.getID() > 0) {
				//recognized
				t.setAccount(account);
				t.setAuxiliary(aux);
				t.setRecognised(true);
			} else {
				//unrecognized
				t.setRecognised(false);
			}
			
			t.setUpdated();
		}
	}

	@Override
	public void importEbicsFiles(AEInvocationContext invContext) throws AEException {
		Properties aeAppProps = AEApp.getInstance().getProperties();
		String folder = aeAppProps.getProperty("jedeclare.mail.local.folder.inbox");

		File importFolder = new File(folder);
		File[] files = importFolder.listFiles();
		if(files != null) {
			// sort
			Arrays.sort(files, EbicsFilesComparator.getInstance());
			for (int i = 0; i < files.length; i++) {
				try {
					importETEBAC(files[i], invContext);
				} catch (Exception e) {
					AEApp.logger().error("Import Ebics files failed ", e);
				}
			}
		}
	}
}
