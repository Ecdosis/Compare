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
import compare.constants.Params;
import compare.constants.Database;
import calliope.core.handler.EcdosisMVD;
import compare.exception.CompareException;
import java.util.Map;

/**
 * Generate a variant table for a range in the base version
 * @author desmond
 */
public class HTMLTableHandler extends TableHandler
{
    private static String ALL = "all";
    /** offset into base version */
    int offset;
    /** length of range to compute table for */
    int length;
    /** Base version */
    short base;
    /** hide merged sections in all but base version */
    boolean hideMerged;
    /** compact versions where possible */
    boolean compact;
    /** expand diffs to whole words */
    boolean wholeWords;
    /** false if all versions are wanted */
    boolean someVersions;
    /** comma-separated string of all selected versions */
    String selectedVersions;
    /** first ID of aligned table cell (default 0 )*/
    int firstID;
    int getIntegerOption( Map map, String key, int defaultValue )
    {
        String[] values = (String[])map.get( key );
        if ( values == null )
        {
            values = new String[1];
            values[0] = Integer.toString(defaultValue);
        }
        return Integer.parseInt(values[0]);
    }
    /**
     * "boolean" options will be 1 or 0
     * @param map the map of passed in params
     * @param key the parameter key
     * @param defaultValue its default value
     * @return true or false
     */
    boolean getBooleanOption( Map map, String key, boolean defaultValue )
    {
        String[] values = (String[])map.get( key );
        if ( values == null )
        {
            int defValue;
            values = new String[1];
            if ( defaultValue )
                defValue = 1;
            else
                defValue = 0;
            values[0] = Integer.toString(defValue);
        }
        int value = Integer.parseInt(values[0]);
        return value==1;
    }
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws CompareException
    {
        Map map = request.getParameterMap();
        offset = getIntegerOption( map, Params.OFFSET, 0 );
        length = getIntegerOption( map, Params.LENGTH, 100 );
        wholeWords = getBooleanOption( map, Params.WHOLE_WORDS, false );
        compact = getBooleanOption( map, Params.COMPACT, false );
        hideMerged = getBooleanOption( map, Params.HIDE_MERGED, false );
        someVersions = getBooleanOption( map, Params.SOME_VERSIONS, false );
        firstID = getIntegerOption( map, Params.FIRSTID, 0 );
        if ( someVersions )
            selectedVersions = getStringOption(map, Params.SELECTED_VERSIONS,ALL );
        else
            selectedVersions = ALL;
        try
        {
            String shortName="";
            String groups = "";
            String baseVersion=null;
            String docid = request.getParameter(Params.DOCID);
            String table = "{}";
            if ( docid != null && docid.length()>0 )
            {
                EcdosisMVD mvd = loadMVD( Database.CORTEX, docid );
                if ( selectedVersions.equals(ALL) )
                    baseVersion = mvd.getDefaultVersion();  
                else
                {
                    String[] parts = selectedVersions.split(",");
                    if ( parts.length>=1 )
                        baseVersion = parts[0];
                }
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
                this.base = mvd.getVersionByNameAndGroup( shortName, groups );
                if ( base == 0 )
                {
                    System.out.println("version "+shortName+" in group "
                        +groups+" not found. Substituting 1");
                    base = 1;
                }
                table = mvd.getTableView( base,offset,length,
                    compact,hideMerged,wholeWords,selectedVersions,firstID,
                    "apparatus" );
            }
            else
            {
                System.out.println("Missing docid");
            }
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println( table );
        }
        catch ( Exception e )
        {
            throw new CompareException( e );
        }
    }
}
