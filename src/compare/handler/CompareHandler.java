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
 *  along with Copare.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2015
 */

package compare.handler;
import calliope.core.URLEncoder;
import calliope.core.Utils;
import calliope.core.constants.Database;
import calliope.core.constants.Formats;
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connection;
import calliope.core.exception.CalliopeException;
import calliope.core.database.Connector;
import calliope.core.exception.DbException;
import calliope.core.handler.GetHandler;
import compare.exception.CompareException;
import compare.handler.get.CorCode;
import calliope.core.handler.EcdosisVersion;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Abstract super-class for all handlers: PUT, POST, DELETE, GET
 * @author ddos
 */
abstract public class CompareHandler extends GetHandler
{
    protected String encoding;
    protected String version1;
    protected String docid;
    public CompareHandler()
    {
        this.encoding = Charset.defaultCharset().name();
    }
    public abstract void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws CompareException;
    /**
     * Get an array of CorCodes, and their styles and formats too
     * @param docID the docID for the resource
     * @param version1 the group-path+version name
     * @param userCC an array of specified CorCode names for this docID
     * @param diffCC the CorCode of the diffs
     * @param styleNames an array of predefined style-names
     * @param styles an empty arraylist of style names to be filled
     * @return a simple array of CorCode texts in their corresponding formats
     */
    protected String[] getCorCodes( String docID, String version1, 
        String[] userCC, CorCode diffCC, String[] styleNames, 
        ArrayList<String> styles ) 
        throws CompareException, CalliopeException
    {
        String[] ccTexts = new String[userCC.length+1];
        // add diffCC entries to corcodes and formats but not styles
        ccTexts[0] = diffCC.toString();
        // load user-defined styles
        if ( styleNames.length>0 )
        {
            String[] styleTexts = fetchStyles( styleNames );
            for ( int i=0;i<styleTexts.length;i++ )
                styles.add( styleTexts[i] );
        }
        HashSet<String> found = new HashSet<String>();
        for ( int i=0;i<userCC.length;i++ )
        {
            String ccResource = userCC[i];
            EcdosisVersion ev = doGetResourceVersion( Database.CORCODE, 
                ccResource, version1 );
            try
            {
                char[] versionText = ev.getVersion();
                if ( versionText == null )
                    throw new CompareException("version not found");
                ccTexts[i+1] = new String(versionText);
                String style = ev.getStyle();
                if ( !found.contains(style) )
                {
                    styles.add( fetchStyle(style) );
                    found.add(style);
                }
            }
            catch ( Exception e )
            {
                throw new CompareException( e );
            }
        }
        return ccTexts;
    }
    /**
     * Get the document body of the given urn or null
     * @param db the database where it is
     * @param docID the docID of the resource
     * @return the document body or null if not present
     */
    private static String getDocumentBody( String db, String docID ) 
        throws CompareException
    {
        try
        {
            String jStr = Connector.getConnection().getFromDb(db,docID);
            if ( jStr != null )
            {
                JSONObject jDoc = (JSONObject)JSONValue.parse( jStr );
                if ( jDoc != null )
                {
                    String body = (String)jDoc.get(JSONKeys.BODY);
                    if ( body != null )
                        return body;
                }
            }
            throw new CompareException("document "+db+"/"+docID+" not found");
        }
        catch ( Exception e )
        {
            throw new CompareException( e );
        }
    }
    /**
     * Fetch a single style text
     * @param style the path to the style in the corform database
     * @return the text of the style
     * @throws CompareException if the style was not in the database
     */
    protected String fetchStyle( String style ) throws CompareException
    {
        // 1. try to get each literal style name
        String actual = getDocumentBody(Database.CORFORM,style);
        while ( actual == null )
        {
            // 2. add "default" to the end
            actual = getDocumentBody( Database.CORFORM,
                URLEncoder.append(style,Formats.DEFAULT) );
            if ( actual == null )
            {
                // 3. pop off last path component and try again
                if ( style.length()>0 )
                    style = Utils.chomp(style);
                else
                    throw new CompareException("no suitable format");
            }
        }
        return actual;
    }
    /**
     * Get the actual styles from the database. Make sure we fetch something
     * @param styles an array of style ids
     * @return an array of database contents for those ids
     * @throws CompareException only if the database is not set up
     */
    protected String[] fetchStyles( String[] styles ) throws CompareException
    {
        String[] actual = new String[styles.length];
        for ( int i=0;i<styles.length;i++ )
        {
            actual[i] = fetchStyle( styles[i] );
        }
        return actual;
    }
    /**
     * Get the best style or the default if none available
     * @param docID the document id
     * @return a style docid
     */
    String findStyleFor( String docID ) throws DbException
    {
        Connection conn = Connector.getConnection();
        String original = new String(docID);
        String jStr = null;
        do
        {
            jStr = conn.getFromDb(Database.CORFORM,docID);
            if ( jStr == null )
            {
                String last = Utils.last(docID);
                if ( last.equals("default") )
                {
                    docID = Utils.chomp(docID);
                    if ( docID.length()>0 )
                        docID = Utils.chomp(docID)+"/"+last;
                    else if ( !docID.equals("TEI") )
                    {
                        docID = "TEI/default";
                    }
                    else
                        break;
                }
                else
                    docID += "/"+"default";
            }
        }
        while ( jStr == null );
        if ( jStr == null )
            throw new DbException("Failed to find "+original);
        else
            return docID;
    }
    /**
     * Try to retrieve the CorTex/CorCode version specified by the path
     * @param db the database to fetch from
     * @param docID the document ID
     * @param vPath the groups/version path to get
     * @return the CorTex/CorCode version contents or null if not found
     * @throws CompareException if the resource couldn't be found
    protected EcdosisVersion doGetResourceVersion( String db, String docID, 
        String vPath ) throws CompareException
    {
        EcdosisVersion version = new EcdosisVersion();
        JSONObject doc = null;
        char[] data = null;
        String res = null;
        //System.out.println("fetching version "+vPath );
        try
        {
            res = Connector.getConnection().getFromDb(db,docID);
            if ( res != null )
                doc = (JSONObject)JSONValue.parse( res );
            if ( doc != null )
            {
                String format = (String)doc.get(JSONKeys.FORMAT);
                if ( format == null )
                    throw new CompareException("doc missing format");
                version.setFormat( format );
                if ( version.getFormat().equals(Formats.MVD) )
                {
                    MVD mvd = MVDFile.internalise( (String)doc.get(
                        JSONKeys.BODY) );
                    if ( vPath == null )
                        vPath = (String)doc.get( JSONKeys.VERSION1 );
                    version.setStyle(findStyleFor(docID));
                    String sName = Utils.getShortName(vPath);
                    String gName = Utils.getGroupName(vPath);
                    int vId = mvd.getVersionByNameAndGroup(sName, gName );
                    version.setMVD(mvd);
                    if ( vId != 0 )
                    {
                        data = mvd.getVersion( vId );
                        if ( data != null )
                            version.setVersion( data );
                        else
                            throw new CompareException("Version "+vPath+" not found");
                    }
                    else
                        throw new CompareException("Version "+vPath+" not found");
                }
                else
                {
                    String body = (String)doc.get( JSONKeys.BODY );
                    version.setStyle((String)doc.get(JSONKeys.STYLE));
                    if ( body == null )
                        throw new CompareException("empty body");
                    data = new char[body.length()];
                    body.getChars(0,data.length,data,0);
                    version.setVersion( data );
                }
            }
            return version;
        }
        catch ( Exception e )
        {
            throw new CompareException( e );
        }
    }
     */
}
