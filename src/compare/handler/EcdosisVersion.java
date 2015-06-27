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
package compare.handler;
import edu.luc.nmerge.mvd.MVD;

/**
 * Represent a version returned from an MVD
 * @author desmond
 */
public class EcdosisVersion 
{
    /** format of the MVD if any */
    String format;
    /** format the text is in */
    String content;
    /** actual text of the version */
    char[] version;
    /** if CorCode the default style */
    String defaultStyle;
    /** the original mvd data or null */
    MVD mvd;
    /**
     * Set the format of this version
     * @param format the format to set
     */
    public void setFormat( String format )
    {
        int slashPos = format.indexOf("/");
        if ( slashPos != -1 )
        {
            content = format.substring( slashPos+1 );
            this.format = format.substring(0,slashPos);
        }
        else
            this.content = this.format = format;
    }
    /**
     * Set the default style of this version
     * @param style the style to set
     */
    public void setStyle( String style )
    {
        this.defaultStyle = style;
    }
    /**
     * Set the version data of this version
     * @param format the version contents to set
     */
    public void setVersion( char[] version )
    {
        this.version = version;
    }
    /**
     * Remember the MVD used to get the version
     * @param mvd 
     */
    public void setMVD( MVD mvd )
    {
        this.mvd = mvd;
    }
     /**
     * Set the format of this version
     * @return the name of the format 
     */
    public String getFormat()
    {
        return format;
    }
    /**
     * Set the format of this version's content
     * @return the name of the format 
     */
    public String getContentFormat()
    {
        return content;
    }
     /**
     * Set the mvd used to fetch this version
     * @return the original MVD
     */
    public MVD getMVD()
    {
        return mvd;
    }
    /**
     * Get the version data of this version
     * @return the version contents 
     */
    public char[] getVersion()
    {
        return version;
    }
    /**
     * Get the MVD version as a String
     * @return the string or an "invalid encoding" message
     */
    public String getVersionString()
    {
        return new String( version );
    }
    /**
     * Get the default style of this version
     * @return the default style if any
     */
    public String getStyle()
    {
        return this.defaultStyle;
    }
    /**
     * Get the length of the data version
     * @return the version's length
     */
    public int getVersionLength()
    {
        return version.length;
    }
}
