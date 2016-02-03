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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import compare.exception.CompareException;
import calliope.core.exception.NativeException;
import calliope.AeseFormatter;
import compare.constants.Params;
import calliope.core.handler.GetHandler;
import calliope.json.JSONResponse;
import java.util.Map;
/**
 * Read the versions of the specified CorTex. Format them into:
 * a) plain text copy of the short and long names
 * b) a layer of STIL markup describing the text to say which are the long 
 * names, and which are the short ones. If the user supplies a CorForm, 
 * then format the resulting combination into HTML and return it. The user 
 * can then animate the HTML in any way using php+javascript or whatever.
 * @author desmond
 */
public class ListHandler extends CompareGetHandler
{
    private String unescape( String str)
    {
        if ( str.contains("%2f") )
            str = str.replace("%2f","/");
        else if (str.contains("%2F") )
            str = str.replace("%2F","/");
        return str;
    }
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws CompareException
    {
        Map map = request.getParameterMap();
        String[] styles = (String[])map.get( Params.STYLE );
        if ( styles == null )
        {
            styles = new String[1];
            styles[0] = "list/default";
        }
        version1 = request.getParameter( Params.VERSION1 );
        docid = request.getParameter(Params.DOCID);
        try
        {
            if ( docid != null )
            {
                String table = getVersionTable( docid );
                table = unescape(table);
                String listName = request.getParameter( Params.NAME );
                if ( listName == null )
                    listName = "versions";
                String longNameId = request.getParameter( Params.LONG_NAME_ID );
                String markup = GetHandler.markupVersionTable( table, listName, 
                    longNameId, version1 );
                String[] corcodes = new String[1];
                corcodes[0] = markup;
                String[] css = fetchStyles( styles );
                JSONResponse html = new JSONResponse(JSONResponse.HTML );
                //System.out.println("about to format list");
                // String text, String[] markup, String[] css,JSONResponse output 
                int res = new AeseFormatter().format( table, corcodes, css, html );
                if ( res == 0 )
                    throw new NativeException("formatting failed");
                else
                {
                    response.setContentType("text/html;charset=UTF-8");
                    response.getWriter().println( html.getBody() );
                }
            }
            else
            {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println( "document identifier missing" );
            }
            
        }
        catch ( Exception e )
        {
            throw new CompareException( e );
        }
    }
}
