/*
 * This file is part of Compare.
 *
 *  Compare is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Compare is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Compare.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2015
 */
package compare.handler.get;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import compare.exception.CompareException;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import compare.constants.Params;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;

/**
 * Get the version1 attribute of a CORTEX
 * @author desmond
 */
public abstract class MetadataHandler extends CompareGetHandler
{
    boolean saved;
    String metadataKey;
    String metadataValue;
    public MetadataHandler( String metadataKey )
    {
        this.metadataKey = metadataKey;
        this.saved = false;
    }
    /**
     * Save the version1 metadata item to the METADATA database
     * @param jObj1 the object retrieved from the metadata database or null
     * @param conn the database connection
     * @throws CompareException 
     */
    protected void saveToMetadata( JSONObject jObj1, Connection conn )
        throws CompareException
    {
        try
        {
            if ( !saved && metadataValue != null && metadataValue.length()>0 )
            {
                if ( jObj1 == null )
                    jObj1 = new JSONObject();
                // update metadata for next time
                jObj1.put(metadataKey, metadataValue);
                if ( jObj1.containsKey( JSONKeys._ID ) )
                    jObj1.remove(JSONKeys._ID);
                conn.putToDb(Database.METADATA,docid,jObj1.toJSONString());
            }
        }
        catch ( Exception e )
        {
            throw new CompareException( e );
        }
    }
    /**
     * Get the version1 metadata item from the MVD itself
     * @param jObj2 the BSON document from CORTEX
     * @throws CompareException 
     */
    protected abstract void getMetadataFromObject( JSONObject jObj2 ) 
        throws CompareException;
    /**
     * Get the version1 metadata item from the CORTEX BSON
     * @param conn the database connection
     * @throws CompareException 
     */
    private void getMetadataFromCortex( Connection conn ) throws CompareException
    {
        try
        {
            String res = conn.getFromDb(Database.CORTEX,docid);
            JSONObject jObj2 = (JSONObject)JSONValue.parse(res);
            if ( jObj2.containsKey(metadataKey) )
                metadataValue = (String) jObj2.get(metadataKey);
            else
                getMetadataFromObject( jObj2 );
        }
        catch ( Exception e )
        {
            throw new CompareException(e);
        }
    }
    /**
     * Handle a request to get some metadata for a document
     * @param request the http request
     * @param response the response
     * @param urn the current urn (ignored)
     * @throws CompareException 
     */
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws CompareException 
    {
        try 
        {
            Connection conn = Connector.getConnection();
            docid = request.getParameter(Params.DOCID);
            String res = conn.getFromDb(Database.METADATA,docid);
            JSONObject  jObj1 = null;
            if ( res != null )
            {
                jObj1 = (JSONObject)JSONValue.parse(res);
                if ( jObj1.containsKey(metadataKey) )
                {
                    metadataValue = (String) jObj1.get(metadataKey);
                    saved = true;
                }
            }
            if ( metadataValue == null )
                getMetadataFromCortex( conn );
            saveToMetadata( jObj1, conn );
            response.setContentType("text/plain");
            response.getWriter().write(metadataValue);
        }
        catch ( Exception e )
        {
            throw new CompareException(e);
        }
    }
}
