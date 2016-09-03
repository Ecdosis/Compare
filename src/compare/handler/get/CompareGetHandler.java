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
import calliope.core.constants.Formats;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import calliope.core.exception.CalliopeException;
import compare.constants.Params;
import compare.constants.Service;
import calliope.core.Utils;
import calliope.json.JSONResponse;
import calliope.core.json.corcode.Range;
import calliope.core.json.corcode.STILDocument;
import compare.handler.CompareHandler;
import calliope.core.handler.EcdosisMVD;
import edu.luc.nmerge.mvd.MVD;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import calliope.core.handler.EcdosisVersion;
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
            String first = Utils.first(urn);
            if ( first.length()==0 )
                new HTMLComparisonHandler().handle(request,response,Utils.pop(urn));
            else if ( first.equals(Service.LIST) )
                new ListHandler().handle(request,response,Utils.pop(urn) );
            else if ( first.equals(Service.LAYERS) )
                new LayerHandler().handle(request,response, Utils.pop(urn) );
            else if ( first.equals(Service.VERSION2) )
                new NextVersionHandler().handle(request,response,Utils.pop(urn));
            else if ( first.equals(Service.VERSION1) )
                new Version1Handler().handle(request,response,Utils.pop(urn));
            else if ( first.equals(Service.TITLE) )
                new TitleHandler().handle(request,response,Utils.pop(urn));
            else if ( first.equals(Service.TABLE) )
                new TableHandler().handle(request,response, Utils.pop(urn) );
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
     * Fetch and load an MVD
     * @param db the database 
     * @param docID the document identifier to fetch
     * @return the loaded MVD
     * @throws CompareException if not found
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
     * @param response the response to write to
     * @param urn the original request urn
     * @throws CompareException if the response could not be written
     */
    protected void handleGetVersion( HttpServletRequest request, 
        HttpServletResponse response, String urn )
        throws CompareException, CalliopeException
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
    protected String getVersionTable( String urn ) throws CompareException
    {
        try
        {
            JSONObject doc = loadJSONObject( Database.CORTEX, urn );
            String fmt = (String)doc.get(JSONKeys.FORMAT);
            if ( fmt != null && fmt.startsWith(Formats.MVD) )
            {
                EcdosisMVD mvd = loadMVD( Database.CORTEX, urn );
                return mvd.getVersionTable();
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
