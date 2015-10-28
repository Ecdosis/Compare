/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compare.handler.get;

import compare.exception.CompareException;
import org.json.simple.JSONObject;
import calliope.core.constants.JSONKeys;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
/**
 * Get the version1 metadata field for a document
 * @author desmond
 */
public class Version1Handler extends MetadataHandler
{
    public Version1Handler()
    {
        super( JSONKeys.VERSION1 );
    }
    public Version1Handler( String docid )
    {
        super( JSONKeys.VERSION1 );
        this.docid = docid;
    }
    /**
     * When all else fails, get version1 from the MVD
     * @param jObj the BSON object from the CORTEX
     * @throws CompareException 
     */
    protected void getMetadataFromObject( JSONObject jObj ) 
        throws CompareException
    {
        try
        {
            String body = (String)jObj.get(JSONKeys.BODY);
            if ( body != null )
            {
                MVD mvd = MVDFile.internalise(body);
                String groupPath = mvd.getGroupPath((short)1);
                String shortName = mvd.getVersionShortName((short)1);
                metadataValue = groupPath+"/"+shortName;
            }
            else    // give up
                metadataValue = "";
        }
        catch ( Exception e )
        {
            throw new CompareException(e);
        }
    }
}
