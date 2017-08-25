package org.ckan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.CKANclient.CKANException;
import org.CKANclient.Client;
import org.CKANclient.Connection;
import org.CKANclient.Dataset;
import org.CKANclient.DataStore;
import org.CKANclient.Field;
import org.CKANclient.Resource;


public class ckan extends BaseStep implements StepInterface {
	private ckanData data;
	private ckanMeta meta;
	
	private String ckanDomain = "";
	private String ckanApiKey = "";
	private String ckanPackageId = "";
	private String ckanResourceTitle = "";
	private String ckanResourceDescription = "";
	private String ckanResourceId = "";
	private int ckanBatchSize = 5000;
	private List<String> ckanPrimaryKeyList = new ArrayList<String>();
	private String outputResourceId = "";
	
	private boolean primaryKeyExists = false;
	private int fieldLength = 0;
	private int rowCount = 0;
	
	private List<String> primaryKeyField = new ArrayList<String>();
	private List<Field> fields = new ArrayList<Field>();
	private LinkedHashMap<String, Object> dataRow = new LinkedHashMap<String, Object>();
	private List<LinkedHashMap<String, Object>> records = new ArrayList<LinkedHashMap<String, Object>>();
	
	
	public ckan(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
		super(s,stepDataInterface,c,t,dis);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (ckanMeta)smi;
		data = (ckanData)sdi;
		
		String fieldName = "";
		String fieldType = "";
		int indexOfValue = 0;
		String fieldValue = "";
		
		// Get row, blocks when needed!
		Object[] r = getRow();
		
		// No more input to be expected...
		if (r==null) {
			if (rowCount % ckanBatchSize != 0) {
				uploadDataStore(records);
			}
			setOutputDone();
			return false;
		}
		
		if (first) {
			first = false;
			
			// Determine output field structure
			data.inputRowMeta = getInputRowMeta();
			data.outputRowMeta = (RowMetaInterface)getInputRowMeta().clone();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);

			fieldLength = data.outputRowMeta.getFieldNames().length;
			for(int i=0; i < fieldLength; i++) {
				fieldName = data.outputRowMeta.getFieldNames()[i];
				fieldType = data.outputRowMeta.getFieldNamesAndTypes(0)[i].trim();
				
				if (fieldType.equals("(Integer)") || fieldType.equals("(Number)")) {
					fields.add(new Field(fieldName, "numeric"));
				}
				else if (fieldType.equals("(Date)")) {
					fields.add(new Field(fieldName, "timestamp"));
				}
				else {
					fields.add(new Field(fieldName, "text"));
				}
			}
			
			List<String> fieldList = Arrays.asList(data.outputRowMeta.getFieldNames());
			if ( fieldList.containsAll(ckanPrimaryKeyList) && !(ckanPrimaryKeyList.isEmpty()) ) {
				primaryKeyExists = true;
				if (log.isBasic()) logBasic("Primary Key found, upserting data.");
			}
			else {
				if (log.isBasic()) logBasic("Primary Key not found, inserting data.");
			}
			
			// Create or update a DataStore resource
			newDataStoreResorce(fields);
			
			if (outputResourceId.length() == 0) {
				// Unable to upload data, so end early
				if (log.isBasic()) logError("No valid resource ID to upload data to.");
				setOutputDone();
				return false;
			}
		}
		
		rowCount++;
		dataRow = new LinkedHashMap<String, Object>();
		for(int i=0; i < fieldLength; i++) {
			fieldName = data.outputRowMeta.getFieldNames()[i];
			
			indexOfValue = data.inputRowMeta.indexOfValue(environmentSubstitute(fieldName));
			fieldValue = data.inputRowMeta.getString(r, indexOfValue);
			
			dataRow.put(fieldName, fieldValue);
		}
		
		records.add(dataRow);
		
		// Upload data in batches every 5000 rows
		if (rowCount % ckanBatchSize == 0) {
			uploadDataStore(records);
			records = new ArrayList<LinkedHashMap<String, Object>>();
		}
		
		// Copy row to possible alternate rowset(s)
		putRow(data.outputRowMeta, r);
		
		// Some basic logging every 5000 rows
		if (checkFeedback(getLinesRead())) {
			if (log.isBasic()) logBasic("Lines read: " + getLinesRead());
		}
		
		return true;
	}
		
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (ckanMeta)smi;
		data = (ckanData)sdi;
		
		// Get user parameters
		if (meta.getDomain() != null) {
			ckanDomain = meta.getDomain();
		}
		
		if (meta.getApiKey() != null) {
			ckanApiKey = meta.getApiKey();
		}
		
		if (meta.getPackageId() != null) {
			ckanPackageId = meta.getPackageId();
		}
		
		if (meta.getResourceTitle() != null) {
			ckanResourceTitle = meta.getResourceTitle();
		}
		
		if (meta.getResourceDescription() != null) {
			ckanResourceDescription = meta.getResourceDescription();
		}
		
		if (meta.getResourceId() != null) {
			ckanResourceId = meta.getResourceId();
		}
		
		try {
			ckanBatchSize = Integer.parseInt(meta.getBatchSize());
		} catch (NumberFormatException e) {
			if (log.isBasic()) logBasic("Batch Size [" + meta.getBatchSize() + "] was not valid, set to 5000");
		    ckanBatchSize = 5000;
		}
		
		if (meta.getPrimaryKey() != null) {
			ckanPrimaryKeyList = Arrays.asList(meta.getPrimaryKey().split("\\s*;;\\s*"));
		}
		
		return super.init(smi, sdi);
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (ckanMeta)smi;
		data = (ckanData)sdi;
		
		super.dispose(smi, sdi);
	}
	
	public boolean newDataStoreResorce(List<Field> fields) {
		int port;
		if (ckanDomain.toLowerCase().startsWith("https://")) {
			port = 443;
		}
		else {
			port= 80;
		}
		Client ckanClient = new Client( new Connection(ckanDomain, port), ckanApiKey);
		
		try {
			DataStore ds = new DataStore();
			
			if (ckanResourceId.length() != 0) {
				ds.setResource_id(ckanResourceId);
			}
			else {
				Resource rs = new Resource();
				rs.setPackage_id(ckanPackageId);
				rs.setName(ckanResourceTitle);
				rs.setDescription(ckanResourceDescription);
				rs.setFormat("CSV");
				ds.setResource(rs);
			}
			
			ds.setFields(fields);
			ds.setForce("True");
			
			if (primaryKeyExists) {
				for (int i=0; i<ckanPrimaryKeyList.size();i++){
					primaryKeyField.add(ckanPrimaryKeyList.get(i));
				}
				ds.setPrimary_key(primaryKeyField);
			}	
			
			DataStore result = ckanClient.createDataStore(ds);
			outputResourceId = result.getResource_id();
			if (log.isBasic()) logBasic("Uploading to Resource ID: " + outputResourceId);
			return true;
		} catch ( CKANException cke ) {
			logError(cke.toString());
			return false;
		}
		catch ( Exception e ) {
			logError(e.toString());
			return false;
		}
	}
	
	public boolean uploadDataStore(List<LinkedHashMap<String, Object>> records) {
		int port;
		if (ckanDomain.toLowerCase().startsWith("https://")) {
			port = 443;
		}
		else {
			port= 80;
		}
		Client ckanClient = new Client( new Connection(ckanDomain, port), ckanApiKey);
		
		try {
			DataStore ds = new DataStore();
			
			ds.setRecords(records);
			ds.setResource_id(outputResourceId);
			ds.setForce("True");
			if (primaryKeyExists) {
				ds.setMethod("upsert");
				if (log.isBasic()) logBasic("Upserting data...");
			}
			else {
				ds.setMethod("insert");
				if (log.isBasic()) logBasic("Inserting data...");
			}
			
			DataStore result = ckanClient.upsertDataStore(ds,3);
			return true;
		} catch ( CKANException cke ) {
			logError(cke.toString());
			return false;
		}
		catch ( Exception e ) {
			logError(e.toString());
			return false;
		}
	}
}
