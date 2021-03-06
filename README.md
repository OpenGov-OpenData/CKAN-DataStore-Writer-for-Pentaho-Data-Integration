# CKAN-DataStore-Writer-for-Pentaho-Data-Integration
CKAN DataStore Writer for Pentaho Data Integration (Kettle)

### Installation

Copy the files below to their respective folders under the Pentaho Data Integration installation directory:
- ../data-integration/plugins/steps/ckan-datastore-plugin/ckan\_datastore.jar
- ../data-integration/plugins/steps/ckan-datastore-plugin/ckan\_logo.png
- ../data-integration/plugins/steps/ckan-datastore-plugin/plugin.xml
- ../data-integration/plugins/steps/ckan-datastore-plugin/lib/commons-logging-1.2.jar
- ../data-integration/plugins/steps/ckan-datastore-plugin/lib/gson-2.2.jar
- ../data-integration/plugins/steps/ckan-datastore-plugin/lib/httpclient-4.2.jar
- ../data-integration/plugins/steps/ckan-datastore-plugin/lib/httpcore-4.2.jar
- ../data-integration/plugins/steps/ckan-datastore-plugin/lib/httpmime-4.2.jar
- ../data-integration/plugins/steps/ckan-datastore-plugin/lib/jsoup-1.8.1.jar


### User Guide

The CKAN DataStore writer plugin allows users to upload tabular data to a CKAN data portal.
Drag and drop the CKAN DataStore Upload step from the output category to the workspace.
Link another step to the CKAN DataStore Upload step to upload the output of the previous step.

### Options
To create an new Datastore resource provide a valid Package ID and omit the Resource ID

| Option         | Description                                                                                             |
| -------------- | ------------------------------------------------------------------------------------------------------- |
| Step name	     |Name of the step. This name should be unique in a single transformation.                                 |
| Domain         |The domain of the CKAN data portal to which data will be uploaded. (eg: http://demo.ckan.org)            |
| API Key        |Users can find their API key in their user profile on the CKAN data portal.                              |
| Package ID     |The ID of an existing package where data will be uploaded in a DataStore resource. (eg: test-dataset)    |
| Resource Title |The title of the resource.                                                                               |
| Description    |A short description about the resource. (optional)                                                       |
| Resource ID    |The ID of an existing DataStore resource to update. If left empty a new DataStore resource will be made. |
| Batch Size     |The writer will upload rows of data in batches of this amount. (default: 5000)                           |
| Primary Key    |If a Primary Key is specified then data will be upserted instead of inserted. Multiple fields can be specified as the Primary Key, use double semicolons to delineate the fields (eg: field1;;field2) |

### PDI Sample Transformation
http://blog.professorcoruja.com/2017/06/unlock-your-data-using-pdi-and-ckan-pdi.html

### Install the plugin using your own PDI Marketplace

The way to override the metadata url is to edit system/karaf/etc/pentaho.marketplace.di.cfg <BR>
And change the value of the marketplace.site property.<BR>

Change to: <BR>
marketplace.site = https://raw.githubusercontent.com/caiomsouza/marketplace-metadata/master/marketplace.xml<BR>

and then start PDI.<BR>

### Publish Plugin to Official PDI Markeplace

I created an issue asking the authors to publish it to Official PDI Markeplace<BR>
https://github.com/OpenGov-OpenData/CKAN-DataStore-Writer-for-Pentaho-Data-Integration/issues/1
