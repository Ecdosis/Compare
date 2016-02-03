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

import calliope.core.handler.EcdosisMVD;
import compare.constants.Database;
import compare.constants.Params;
import compare.exception.CompareException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Handle requests for a JSON formatted version of the MVD as a table
 * @author desmond
 */
public class JSONTableHandler extends TableHandler
{
    int offset;
    int length;
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws CompareException
    {
        Map map = request.getParameterMap();
        docid = getStringOption(map,Params.DOCID,"");
        version1 = getStringOption(map,Params.VERSION1,"");
        offset = getIntOption(map,Params.OFFSET,0);
        length = getIntOption(map,Params.LENGTH,Integer.MAX_VALUE);
        String selected = getStringOption(map,Params.SELECTED,ALL);
        try
        {
            String baseVersion=null;
            String json = "{}";
            if ( docid != null && docid.length()>0 )
            {
                EcdosisMVD mvd = loadMVD( Database.CORTEX, docid );
                if ( mvd != null )
                {
                    baseVersion = selectVersion1(mvd,selected);
                    short base = getBaseVersion(mvd.mvd,baseVersion);
                    json = mvd.mvd.getTable( base, offset, length, selected );
                }
            }
            else
                System.out.println("empty docid");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().println( json );
        }
        catch ( Exception e )
        {
            throw new CompareException(e);
        }
    }
}
