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

import compare.exception.CompareException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.Utils;
import calliope.core.handler.EcdosisMVD;
import compare.constants.Service;
import edu.luc.nmerge.mvd.MVD;
import java.util.Map;

/**
 * Return a JSON or HTML table of an MVD
 * @author desmond
 */
public class TableHandler extends CompareGetHandler
{
    protected static String ALL = "all";
    /**
     * String options are just literals
     * @param map the map of passed in params
     * @param key the parameter key
     * @param defaultValue its default value
     * @return a String
     */
    protected String getStringOption( Map map, String key, String defaultValue )
    {
        String[] values = (String[])map.get( key );
        if ( values == null || values[0].length()==0 )
            return defaultValue;
        else
            return values[0];
    }
    /**
     * String options are just literals
     * @param map the map of passed in params
     * @param key the parameter key
     * @param defaultValue its default value
     * @return an int
     */
    protected int getIntOption( Map map, String key, int defaultValue )
    {
        String[] values = (String[])map.get( key );
        if ( values == null || values[0].length()==0 )
            return defaultValue;
        else
            return Integer.parseInt(values[0]);
    }
    /**
     * Get the base version number (1-#versions)
     * @param baseVersion the vid or full version name
     * @return the base version without fail (1 by default)
     */
    protected short getBaseVersion( MVD mvd, String baseVersion )
    {
        String shortName="";
        String groups="";
        if ( baseVersion != null )
        {
            int pos = baseVersion.lastIndexOf("/");
            if ( pos != -1 )
            {
                shortName = baseVersion.substring(pos+1);
                groups = baseVersion.substring(0,pos);
            }
            else
                shortName = baseVersion;
        }
        short base = (short)mvd.getVersionByNameAndGroup( shortName, 
            groups );
        if ( base == 0 )
        {
            System.out.println("version "+shortName+" in group "
                +groups+" not found. Substituting 1");
            base = 1;
        }
        return base;
    }
    /**
     * Choose which version to regard as version1 in case it is unspecified
     * @param mvd the loaded MVD to choose version1 from
     * @param selected a selection of version in mvd
     * @return the full version name of the selected version1 or null
     */
    protected String selectVersion1( EcdosisMVD mvd, String selected )
    {
        String baseVersion = null;
        if ( version1 != null && version1.length()>0 )
            baseVersion = version1;
        else if ( selected.equals(ALL) )
            baseVersion = mvd.version1;  
        else
        {
            String[] parts = selected.split(",");
            if ( parts.length>=1 )
                baseVersion = parts[0];
        }
        return baseVersion;
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
        if ( urn.equals(Service.HTML) )
            new HTMLTableHandler().handle(request,response,Utils.pop(urn) );
        else if ( urn.equals(Service.JSON) )
            new JSONTableHandler().handle(request,response,Utils.pop(urn) );
        else if ( urn.equals(Service.INFO) )
            new TableInfoHandler().handle(request,response,Utils.pop(urn) );
        else
            throw new CompareException("Unknown service "+urn);
    }
}
