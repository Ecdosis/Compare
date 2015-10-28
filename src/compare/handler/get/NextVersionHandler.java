/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compare.handler.get;

import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import compare.constants.Params;
import compare.exception.CompareException;
import calliope.core.handler.EcdosisMVD;
import calliope.core.Utils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Get the next version given one as input
 * @author desmond
 */
public class NextVersionHandler extends CompareGetHandler 
{
    public NextVersionHandler()
    {
        super();
    }
    /**
     * Instantiate this class when *not* handling a request
     * @param docid the already read docid
     * @param version1 the already read version1
     */
    public NextVersionHandler( String docid, String version1 )
    {
        super();
        this.docid = docid;
        this.version1 = version1;
    }
    /**
     * Get version2 based on version1 and docid
     * @return version2 corresponding to version1
     * @throws CompareException 
     */
    String getVersion2() throws CompareException
    {
        String fullName = "";
        String shortName = Utils.getShortName( version1 );
        String groups = Utils.getGroupName( version1 );
        EcdosisMVD mvd = loadMVD( Database.CORTEX, docid );
        int v1 = mvd.mvd.getVersionByNameAndGroup( shortName, groups );
        if ( v1 > 0 )
        {
            int v2 = mvd.mvd.getNextVersionId( (short)v1 );
            String groups2 = mvd.mvd.getGroupPath( (short)v2 );
            String shortName2 = mvd.mvd.getVersionShortName( (short)v2 );
            fullName = groups2+"/"+shortName2;
        }
        else
            throw new CompareException( shortName+groups+" not found" );
        return fullName;
    }
    /**
     * Write the next version to the output stream
     * @param request the request
     * @param response the response
     * @param urn the urn of the CorTex
     * @throws AeseException 
     */
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws CompareException
    {
        version1 = request.getParameter( Params.VERSION1 );
        docid = request.getParameter( Params.DOCID );
        try
        {
            String fullName = "";
            if ( version1 != null && docid != null )
                fullName = getVersion2();
            else
                throw new CompareException( "version1 or docid was unspecidied");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println( fullName );
        }
        catch ( Exception e )
        {
            throw new CompareException( e );
        }
    }
}
