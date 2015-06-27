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
import compare.exception.CompareException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;

/**
 * Abstract super-class for all handlers: PUT, POST, DELETE, GET
 * @author ddos
 */
abstract public class CompareHandler 
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
}
