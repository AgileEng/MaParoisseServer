package eu.agileeng.services.export.ejb;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.ejb.Stateless;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEFileUtil;

@Stateless
public class DocumentExportBean extends AEBean implements DocumentExportLocal {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1687190147828461907L;

	@Override
	public JSONObject exportGridAsCsv(JSONArray columns, JSONArray data, AEInvocationContext invContext) throws AEException {
		StringBuilder str = new StringBuilder();
		
		try {
			for (int i = 0; i < columns.length(); i++) {
				JSONObject col = columns.getJSONObject(i);
				str.append(col.optString("label")).append("\t");
			}
			
			str.deleteCharAt(str.length()-1);
			str.append("\r\n");
			
			for (int j = 0; j < data.length(); j++) {
				JSONObject row = data.getJSONObject(j);
				for (int i = 0; i < columns.length(); i++) {
					JSONObject col = columns.getJSONObject(i);
					str.append('"').append(row.optString(col.optString("name"))).append('"').append("\t");
				}
				
				str.deleteCharAt(str.length()-1);
				str.append("\r\n");
			}
			
			String tmpRepository = AEApp.getInstance().getProperties().getProperty("tmpRepository");
			
			// extract the data to the tmp folder
			File tmpFileRepository = new File(
					tmpRepository, 
					(String) invContext.getProperty(AEInvocationContext.HttpSessionId));
			if(!tmpFileRepository.exists()) {
				tmpFileRepository.mkdir();
			}
			StringBuilder fileName = new StringBuilder("exp_").append(AEDateUtil.convertToString(new Date(), "yyyyMMdd_HHmmss")).append(".csv");
			File tmpFile = new File(
					tmpFileRepository, 
					AEFileUtil.createTempFileName("exp_csv_", null, tmpFileRepository));
			
			PrintWriter writer = new PrintWriter(tmpFile, "UTF-8");
			writer.print(str.toString());
			writer.close();
			
			StringBuilder downloadUrl = new StringBuilder();
			downloadUrl.append("../../../FileDownloadServlet?");
			downloadUrl.append("file=").append(tmpFile.getAbsolutePath());
			downloadUrl.append("&fileName=").append(fileName.toString());
			downloadUrl.append("&deleteOnExit=").append(true);
			
			JSONObject payload = new JSONObject();
			payload.put("file", tmpFile.getAbsolutePath());
			payload.put("fileName", fileName);
			payload.put("downloadUrl", downloadUrl);
			
			return payload;
		} catch (IOException e) {
			throw new AEException("IOException", e);
		} catch (JSONException e) {
			throw new AEException("JSONException", e);
		}
	}
	
	@Override
	public JSONObject handleXls(String base64Data, AEInvocationContext invContext) throws AEException {
		try {
			String tmpRepository = AEApp.getInstance().getProperties().getProperty("tmpRepository");
			
			// save to the tmp folder
			File tmpFileRepository = new File(
					tmpRepository, 
					(String) invContext.getProperty(AEInvocationContext.HttpSessionId));
			if(!tmpFileRepository.exists()) {
				tmpFileRepository.mkdir();
			}
			StringBuilder fileName = new StringBuilder("exp_").append(AEDateUtil.convertToString(new Date(), "yyyyMMdd_HHmmss")).append(".xls");
			File tmpFile = new File(
					tmpFileRepository, 
					AEFileUtil.createTempFileName("exp_xls_", null, tmpFileRepository));
			
			PrintWriter writer = new PrintWriter(tmpFile, "UTF-8");
			writer.print(base64Data);
			writer.close();
			
			StringBuilder downloadUrl = new StringBuilder();
			downloadUrl.append("../../../FileDownloadServlet?");
			downloadUrl.append("file=").append(tmpFile.getAbsolutePath());
			downloadUrl.append("&fileName=").append(fileName.toString());
			downloadUrl.append("&deleteOnExit=").append(true);
			
			JSONObject payload = new JSONObject();
			payload.put("file", tmpFile.getAbsolutePath());
			payload.put("fileName", fileName);
			payload.put("downloadUrl", downloadUrl);
			
//			String path = AEApp.getInstance().getProperties().getProperty("tmpRepository");;
//			String file = UUID.nameUUIDFromBytes(this.getClass().getSimpleName().getBytes()).toString();
//			StringBuilder fileName = new StringBuilder("exp_").append(AEDateUtil.convertToString(new Date(), "yyyyMMdd_HHmmss")).append(".xls");
//			
//			PrintWriter writer = new PrintWriter(path+file, "UTF-8");
//			writer.print(base64Data);
//			
//			writer.close();
//			
//			StringBuilder downloadUrl = new StringBuilder();
//			downloadUrl.append("../../../FileDownloadServlet?");
//			downloadUrl.append("file=").append(path+file);
//			downloadUrl.append("&fileName=").append(fileName.toString());
//			downloadUrl.append("&deleteOnExit=").append(true);
//			
//			JSONObject payload = new JSONObject();
//			payload.put("file", path+file);
//			payload.put("fileName", file);
//			payload.put("downloadUrl", downloadUrl);
			
			return payload;
		} catch (IOException e) {
			throw new AEException("IOException", e);
		} catch (JSONException e) {
			throw new AEException("JSONException", e);
		}
		
	}
	
//	public JSONObject exportGridAsXls(JSONArray columns, JSONArray data) throws AEException {
//		PrintWriter writer = null;
//		
//		try {
//			String path = "C:\\AccBureau\\tmp";
//			String file = UUID.nameUUIDFromBytes(this.getClass().getSimpleName().getBytes()).toString();
//			StringBuilder fileName = new StringBuilder("exp_").append(AEDateUtil.convertToString(new Date(), "yyyyMMdd_HHmmss")).append(".csv");
//			
//			
//			
//			writer = new PrintWriter(path+file, "UTF-8");
//			
//			// open XML Workbook
//			StringBuilder buffer = new StringBuilder("<?xml version=\"1.0\"?><ss:Workbook xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");
//			//open Sheet tag
//			buffer.append("<ss:Worksheet ss:Name=\"Sheet1\">");
//			//open Table tag
//        	buffer.append("<ss:Table>");
//			
//			for (int i = 0; i < columns.length(); i++) {
//				JSONObject col = columns.getJSONObject(i);
//				str.append(col.optString("label")).append("\t");
//			}
//			
//			str.deleteCharAt(str.length()-1);
//			str.append("\r\n");
//			
//			for (int j = 0; j < data.length(); j++) {
//				JSONObject row = data.getJSONObject(j);
//				for (int i = 0; i < columns.length(); i++) {
//					JSONObject col = columns.getJSONObject(i);
//					str.append('"').append(row.optString(col.optString("name"))).append('"').append("\t");
//				}
//				
//				str.deleteCharAt(str.length()-1);
//				str.append("\r\n");
//			}
//			
//			
//			writer.print(str.toString());
//			
//			writer.close();
//			
//			StringBuilder downloadUrl = new StringBuilder();
//			downloadUrl.append("../../../FileDownloadServlet?");
//			downloadUrl.append("file=").append(path+file);
//			downloadUrl.append("&fileName=").append(fileName.toString());
//			downloadUrl.append("&deleteOnExit=").append(true);
//			
//			JSONObject payload = new JSONObject();
//			payload.put("file", path+file);
//			payload.put("fileName", file);
//			payload.put("downloadUrl", downloadUrl);
//			
//			return payload;
//		} catch (JSONException e) {
//			throw new AEException("Error reading document", e);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			throw new AEException("Error reading document", e);
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			throw new AEException("Error reading document", e);
//		} finally {
//			
//		}
//	}

}
