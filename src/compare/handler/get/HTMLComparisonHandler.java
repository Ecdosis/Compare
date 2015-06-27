/* This file is part of calliope.
 *
 *  calliope is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  calliope is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with calliope.  If not, see <http://www.gnu.org/licenses/>.
 */

package compare.handler.get;
import compare.exception.CompareException;
import compare.constants.Params;
import compare.constants.ChunkState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.AeseFormatter;
import calliope.exception.*;
import calliope.core.constants.Formats;
import calliope.core.constants.Database;
import calliope.core.exception.NativeException;
import calliope.core.Utils;
import calliope.json.JSONResponse;
import compare.handler.EcdosisVersion;
import compare.handler.EcdosisMVD;
import java.util.ArrayList;
import java.util.Map;
import html.Comment;
/**
 * Handle comparison between two versions of a document
 * @author desmond
 */
public class HTMLComparisonHandler extends CompareGetHandler
{
    /**
     * Get an array of CorCodes, and their styles and formats too
     * @param docID the docID for the resource
     * @param version1 the group-path+version name
     * @param userCC an array of specified CorCode names for this docID
     * @param diffCC the CorCode of the diffs
     * @param styleNamess an array of predefined style-names
     * @param styles an empty arraylist of style names to be filled
     * @param formats an empty array of CorCode formats to be filled
     * @return a simple array of CorCode texts in their corresponding formats
     */
    String[] getCorCodes( String docID, String version1, 
        String[] userCC, CorCode diffCC, String[] styleNames, 
        ArrayList<String> styles, ArrayList<String> formats ) 
        throws CompareException
    {
        String[] ccTexts = new String[userCC.length+1];
        // add diffCC entries to corcodes and formats but not styles
        ccTexts[0] = diffCC.toString();
        formats.add( Formats.STIL );
        // load user-defined styles
        if ( styleNames.length>0 )
        {
            String[] styleTexts = fetchStyles( styleNames );
            for ( int i=0;i<styleTexts.length;i++ )
                styles.add( styleTexts[i] );
        }
        for ( int i=0;i<userCC.length;i++ )
        {
            String ccResource = Utils.canonisePath(docID,userCC[i]);
            EcdosisVersion ev = doGetResourceVersion( Database.CORCODE, 
                ccResource, version1 );
            try
            {
                char[] versionText = ev.getVersion();
                if ( versionText == null )
                    throw new AeseException("version not found");
                ccTexts[i+1] = new String(versionText);
                styles.add( fetchStyle(ev.getStyle()) );
            }
            catch ( Exception e )
            {
                throw new CompareException( e );
            }
            formats.add( ev.getContentFormat() );
        }
        return ccTexts;
    }
    /**
     * Get the HTML of one version compared to another
     * @param request the request to read from
     * @param path the parsed URN
     * @return a formatted html String
     */
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws CompareException
    {
        docid = request.getParameter(Params.DOCID);
        // the version we will return, with added markup
        version1 = request.getParameter( Params.VERSION1 );
        // the version to compare WITH
        String version2 = request.getParameter( Params.VERSION2 );
        // the name for the differences with version2 that we generate
        String diffKind = request.getParameter( Params.DIFF_KIND );
        if ( version1 != null && version2 != null )
        {
            if ( diffKind == null )
            {
                diffKind = ChunkState.DELETED;
                System.out.println("Missing parameter "
                    +Params.DIFF_KIND+" assuming "+ChunkState.DELETED );
            }
            CorCode cc = new CorCode( diffKind );
            EcdosisMVD text = loadMVD( Database.CORTEX, docid );
            int v1 = text.mvd.getVersionByNameAndGroup(
                Utils.getShortName(version1),Utils.getGroupName(version1));
            if ( v1 == -1 )
                throw new CompareException(version1+" not found");
            int v2 = text.mvd.getVersionByNameAndGroup(
                Utils.getShortName(version2),
                Utils.getGroupName(version2) );
            if ( v2 == -1 )
                throw new CompareException(version2+" not found");
            int[] lengths = text.mvd.getVersionLengths();
            if ( lengths==null || lengths.length<v1 || lengths.length<v2  )
                throw new CompareException( "lengths array is empty" );
            Run[] runs = new Run[1];
            runs[0] = new Run( 0, lengths[v1-1] );
            cc.compareText( text.mvd, v1, v2, diffKind, runs );
            // get corCodes
            String[] corCodes = new String[0];
            String[] styles = new String[0];
            Map paramMap = request.getParameterMap();
            if ( paramMap != null )
            {
                String[] ccs = (String[])paramMap.get( Params.CORCODE );
                String[] sss = (String[])paramMap.get( Params.STYLE );
                if ( ccs != null && ccs.length > 0 )
                    corCodes = ccs;
                if ( sss != null && sss.length > 0 )
                    styles = sss;
            }
            ArrayList<String> styleNames = new ArrayList<String>();
            ArrayList<String> formats = new ArrayList<String>();
            // add diff styles
            String[] newStyles = new String[styles.length+1];
            System.arraycopy( styles, 0, newStyles, 0, styles.length );
            newStyles[styles.length] = "diffs/default";
            String[] ccTexts = getCorCodes( urn, 
                version1, corCodes, cc, newStyles, styleNames, formats );
            String[] styleTexts = new String[styleNames.size()];
            styleNames.toArray( styleTexts );
            String[] formatTexts = new String[formats.size()];
            formats.toArray( formatTexts );
            // call the native library
            JSONResponse html = new JSONResponse(JSONResponse.HTML);
            char[] mvdVersionText = text.mvd.getVersion(v1);
            try
            {
                int res = new AeseFormatter().format( 
                    new String(mvdVersionText), ccTexts, styleTexts, html );
                if ( res == 0 )
                    throw new NativeException("formatting failed");
                else
                {
                    response.setContentType("text/html;charset=UTF-8");
                    Comment comment = new Comment();
                    comment.addText( "styles: ");
                    for ( int i=0;i<styleTexts.length;i++ )
                        comment.addText( styleTexts[i] );
                    //System.out.println(html.getBody());
                    response.getWriter().println( comment.toString() );
                    response.getWriter().println(html.getBody());   
                }
            }
            catch ( Exception e )
            {
                throw new CompareException( e );
            }   
        }
        else if ( version1 != null )
        {
            // just get the version
            handleGetVersion( request, response, urn );
        }
        else
            throw new CompareException("versions unspecified");
    }
}
