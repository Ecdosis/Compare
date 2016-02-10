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
 *  (c) copyright Desmond Schmidt 2016
 */

package compare.handler.get;


import calliope.core.constants.JSONKeys;
import compare.constants.Params;
import java.util.Map;
import compare.exception.CompareException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;

/**
 * Return information about a potential table but don't generate it
 * @author desmond
 */
import calliope.core.handler.EcdosisMVD;
import compare.constants.Database;
public class TableInfoHandler extends TableHandler 
{
    static final int CHUNK = 50;
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
        Map map = request.getParameterMap();
        docid = request.getParameter(JSONKeys.DOCID);
        version1 = getStringOption(map,Params.VERSION1,"");
        String json = "{}";
        try
        {
            if ( docid != null && docid.length()>0 )
            {
                EcdosisMVD mvd = loadMVD( Database.CORTEX, docid );
                String selected = getStringOption(map,Params.SELECTED,ALL);
                String baseVersion = selectVersion1(mvd,selected);
                short base = getBaseVersion(mvd.mvd,baseVersion);
                int baseLen = getBaseVersionLen(mvd.mvd,base);
                int[] offsets = mvd.mvd.measureTable( base, selected );
                int numSegs = Math.round((float)offsets.length/(float)CHUNK);
                if ( numSegs == 0 )
                    numSegs = 1;
                int[] segs = new int[numSegs+1];
                if ( numSegs == 2 )
                {
                    segs[0] = 0;
                    segs[1] = baseLen;
                }
                else
                {
                    for ( int i=0;i<numSegs;i++ )
                        segs[i] = offsets[i*CHUNK];
                }
                segs[segs.length-1] = baseLen;
                JSONArray arr = new JSONArray();
                for ( int i=0;i<segs.length;i++ )
                    arr.add(segs[i]);
                json = arr.toJSONString();
            }
            else
                System.out.println("CORTEX mvd "+docid+" not found");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().println( json );
        }
        catch ( Exception e )
        {
            throw new CompareException(e);
        }
    }
}
