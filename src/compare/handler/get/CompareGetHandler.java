/*
 * This file is part of Project.
 *
 *  Project is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Project is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Project.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2014
 */

package compare.handler.get;

import calliope.AeseFormatter;
import calliope.core.database.*;
import compare.exception.CompareException;
import calliope.core.URLEncoder;
import calliope.core.constants.Formats;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import compare.constants.Params;
import compare.constants.Service;
import calliope.core.Utils;
import calliope.json.JSONResponse;
import calliope.json.corcode.Range;
import calliope.json.corcode.STILDocument;
import compare.handler.CompareHandler;
import compare.handler.EcdosisMVD;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import compare.handler.EcdosisVersion;
import edu.luc.nmerge.mvd.Pair;
import html.Comment;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * Get a project document from the database
 * @author desmond
 */
public class CompareGetHandler extends CompareHandler
{
    public void handle(HttpServletRequest request,
            HttpServletResponse response, String urn) throws CompareException 
    {
        try 
        {
            if ( urn.length()==0 )
                new HTMLComparisonHandler().handle(request,response,Utils.pop(urn));
            else if ( urn.equals(Service.LIST) )
                new ListHandler().handle(request,response,Utils.pop(urn) );
            else if ( urn.equals(Service.VERSION2) )
                new NextVersionHandler().handle(request,response,Utils.pop(urn));
            else if ( urn.equals(Service.VERSION1) )
                new Version1Handler().handle(request,response,Utils.pop(urn));
            else if ( urn.equals(Service.TITLE) )
                new TitleHandler().handle(request,response,Utils.pop(urn));
            else
                throw new CompareException("Unknown service "+urn);
        } 
        catch (Exception e) 
        {
            try
            {
                response.setCharacterEncoding("UTF-8");
                response.getWriter().println(e.getMessage());
            }
            catch ( Exception ex )
            {
                throw new CompareException(ex);
            }
        }
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
     * @throws a CompareException only if the database is not set up
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
     * Try to retrieve the CorTex/CorCode version specified by the path
     * @param db the database to fetch from
     * @param docID the document ID
     * @param vPath the groups/version path to get
     * @return the CorTex/CorCode version contents or null if not found
     * @throws CompareException if the resource couldn't be found
     */
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
        }
        catch ( Exception e )
        {
            throw new CompareException( e );
        }
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
                version.setStyle((String)doc.get(JSONKeys.STYLE));
                String sName = Utils.getShortName(vPath);
                String gName = Utils.getGroupName(vPath);
                int vId = mvd.getVersionByNameAndGroup(sName, gName );
                version.setMVD(mvd);
                if ( vId != 0 )
                {
                    data = mvd.getVersion( vId );
                    String desc = mvd.getDescription();
                    //System.out.println("description="+desc);
                    //int nversions = mvd.numVersions();
                    //System.out.println("nversions="+nversions);
                    //System.out.println("length of version "+vId+"="+data.length);
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
    /**
     * Fetch and load an MVD
     * @param db the database 
     * @param docID
     * @return the loaded MVD
     * @throws an CompareException if not found
     */
    protected EcdosisMVD loadMVD( String db, String docID ) throws CompareException
    {
        try
        {
            String data = Connector.getConnection().getFromDb(db,docID);
            if ( data != null && data.length() > 0 )
            {
                JSONObject doc = (JSONObject)JSONValue.parse(data);
                if ( doc != null )
                    return new EcdosisMVD( doc );
            }
            throw new CompareException( "MVD not found "+docID );
        }
        catch ( Exception e )
        {
            throw new CompareException( e );
        }
    }
    /**
     * Does one set of versions entirely contain another
     * @param container the putative container
     * @param contained the containee
     * @return true if all the bits of contained are in container
     */
    boolean containsVersions( BitSet container, BitSet contained )
    {
        for (int i=contained.nextSetBit(0);i>=0;i=contained.nextSetBit(i+1)) 
        {
            if ( container.nextSetBit(i)!= i )
                return false;
        }
        return true;
    }
    /**
     * Compute the IDs of spans of text in a set of versions
     * @param corCodes the existing corCodes array
     * @param mvd the MVD to use
     * @param version1 versionID of the base version
     * @param spec a comma-separated list of versionIDs 
     * @return an updated corCodes array
     */
    String[] addMergeIds( String[] corCodes, MVD mvd, String version1, 
        String spec )
    {
        STILDocument doc = new STILDocument();
        int base = mvd.getVersionByNameAndGroup(
            Utils.getShortName(version1),
            Utils.getGroupName(version1));
        ArrayList<Pair> pairs = mvd.getPairs();
        BitSet merged = mvd.convertVersions( spec );
        int start = -1;
        int len = 0;
        int pos = 0;
        int id = 1;
        for ( int i=0;i<pairs.size();i++ )
        {
            Pair p = pairs.get( i );
            if ( p.versions.nextSetBit(base)==base )
            {
                if ( containsVersions(p.versions,merged) )
                {
                    if ( start == -1 )
                        start = pos;
                    len += p.length();
                }
                else if ( start != -1 )
                {
                    // add range with annotation to doc
                    try
                    {
                        // see diffs/default corform
                        Range r = new Range("merged", start, len );
                        r.addAnnotation( "mergeid", "v"+id );
                        id++;
                        doc.add( r );
                        start = -1;
                        len = 0;
                    }
                    catch ( Exception e )
                    {
                        // ignore it: we just failed to add that range
                        start = -1;
                        len = 0;
                    }
                }
                // the position within base
                pos += p.length();
            }
        }
        // coda: in case we have a part-fulfilled range
        if ( start != -1 )
        {
            try
            {
                Range r = new Range("merged", start, len );
                r.addAnnotation( "mergeid", "v"+id );
                id++;
                doc.add( r );
            }
            catch ( Exception e )
            {
                // ignore it: we just failed to add that range
            }
        }
        // add new CorCode to the set
        String[] newCCs = new String[corCodes.length+1];
        newCCs[newCCs.length-1] = doc.toString();
        for ( int i=0;i<corCodes.length;i++ )
            newCCs[i] = corCodes[i];
        return newCCs;
    }
    /**
     * Format the requested URN version as HTML
     * @param request the original http request
     * @param urn the original request urn
     * @return the converted HTML
     * @throws AeseException 
     */
    protected void handleGetVersion( HttpServletRequest request, 
        HttpServletResponse response, String urn )
        throws CompareException
    {
        String version1 = request.getParameter( Params.VERSION1 );
        if ( version1 == null )
        {
            try
            {
                response.getWriter().println(
                    "<p>version1 parameter required</p>");
            }
            catch ( Exception e )
            {
                throw new CompareException( e );
            }
        }
        else
        {
            String selectedVersions = request.getParameter( 
                Params.SELECTED_VERSIONS );
            //System.out.println("version1="+version1);
            EcdosisVersion corTex = doGetResourceVersion( Database.CORTEX, urn, version1 );
            // 1. get corcodes and styles
            String[] corCodes = request.getParameterValues( Params.CORCODE );
            String[] styles = request.getParameterValues( Params.STYLE );
            HashSet<String> styleSet = new HashSet<String>();
            for ( int i=0;i<styles.length;i++ )
                styleSet.add( styles[i] );
            try
            {
                for ( int i=0;i<corCodes.length;i++ )
                {
                    String ccResource = Utils.canonisePath(urn,corCodes[i]);
                    EcdosisVersion ev = doGetResourceVersion( Database.CORCODE, 
                        ccResource, version1 );
                    Comment comment = new Comment();
                    comment.addText( "version-length: "+ev.getVersionLength() );
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().println( comment.toString() );
                    styleSet.add( ev.getStyle() );
                    corCodes[i] = ev.getVersionString();
                }
            }
            catch ( Exception e )
            {
                // this won't ever happen because UTF-8 is always supported
                throw new CompareException( e );
            }
            // 2. add mergeids if needed
            if ( selectedVersions != null && selectedVersions.length()>0 )
            {
                corCodes = addMergeIds( corCodes, corTex.getMVD(), version1, 
                    selectedVersions );
                styleSet.add( "diffs/default" );
            }
            // 3. recompute styles array (docids)
            styles = new String[styleSet.size()];
            styleSet.toArray( styles );
            // 4. convert style names to actual corforms
            styles = fetchStyles( styles );
            // 5. call the native library to format it
            JSONResponse html = new JSONResponse(JSONResponse.HTML);
            String text = corTex.getVersionString();
    //        // debug
//            try{
//                String textString = new String(text,"UTF-8");
//                System.out.println(textString);
//            }catch(Exception e){}
            // end
//            if ( text.length==30712 )
//            {
//                try
//                {
//                    String textStr = new String( text, "UTF-8");
//                    System.out.println(textStr );
//                }
//                catch ( Exception e )
//                {
//                }
//            }
            int res = new AeseFormatter().format( 
                text, corCodes, styles, html );
            if ( res == 0 )
                throw new CompareException("formatting failed");
            else
            {
                response.setContentType("text/html;charset=UTF-8");
                try
                {
                    Comment comment = new Comment();
                    comment.addText( "styles: ");
                    for ( int i=0;i<styles.length;i++ )
                        comment.addText( styles[i] );
                    response.getWriter().println( comment.toString() );
                    response.getWriter().println(html.getBody());   
                }
                catch ( Exception e )
                {
                    throw new CompareException( e );
                }
            }
        }
    }
    /**
     * Use this method to retrieve the doc just to see its format
     * @param db the database to fetch from
     * @param docID the doc's ID
     * @return a JSON doc as returned by Mongo
     * @throws AeseException 
     */
    JSONObject loadJSONObject( String db, String docID ) 
        throws CompareException
    {
        try
        {
            String data = Connector.getConnection().getFromDb(db,docID);
            if ( data.length() > 0 )
            {
                JSONObject doc = (JSONObject)JSONValue.parse(data);
                if ( doc != null )
                    return doc;
            }
            throw new CompareException( "Doc not found "+docID );
        }
        catch ( Exception e )
        {
            throw new CompareException( e );
        }
    }     
    protected String getVersionTableForUrn( String urn ) throws CompareException
    {
        try
        {
            JSONObject doc = loadJSONObject( Database.CORTEX, urn );
            String fmt = (String)doc.get(JSONKeys.FORMAT);
            if ( fmt != null && fmt.startsWith(Formats.MVD) )
            {
                EcdosisMVD mvd = loadMVD( Database.CORTEX, urn );
                return mvd.mvd.getVersionTable();
            }
            else if ( fmt !=null && fmt.equals(Formats.TEXT) )
            {
                // concoct a version list of length 1
                StringBuilder sb = new StringBuilder();
                String version1 = (String)doc.get(JSONKeys.VERSION1);
                if ( version1 == null )
                    throw new CompareException("Lacks version1 default");
                sb.append("Single version\n");
                String[] parts = version1.split("/");
                for ( int i=0;i<parts.length;i++ )
                {
                    sb.append(parts[i]);
                    sb.append("\t");
                }
                sb.append(parts[parts.length-1]+" version");
                sb.append("\n");
                return sb.toString();
            }
            else
                throw new CompareException("Unknown of null Format");
        }
        catch ( Exception e )
        {
            throw new CompareException(e);
        }   
    }
}
