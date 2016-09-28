package org.ckan;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

public class ckanData extends BaseStepData implements StepDataInterface
{
	public RowMetaInterface inputRowMeta;
	
	public RowMetaInterface outputRowMeta;

    public ckanData()
	{
		super();
	}
}