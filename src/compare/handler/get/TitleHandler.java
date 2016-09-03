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
import org.json.simple.JSONObject;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import calliope.core.constants.JSONKeys;

/**
 * Get the title attribute of a CORTEX
 * @author desmond
 */
public class TitleHandler extends MetadataHandler
{
    public TitleHandler()
    {
        super( JSONKeys.TITLE );
    }
    /**
     * Get the title metadata item from the MVD itself
     * @param jObj the BSON document from CORTEX
     * @throws CompareException if MVDFIle read failed
     */
    protected void getMetadataFromObject( JSONObject jObj ) 
        throws CompareException
    {
        try
        {
            String body = (String)jObj.get(metadataKey);
            if ( body != null )
            {
                MVD mvd = MVDFile.internalise(body);
                metadataValue = mvd.getDescription();
            }
            else    // give up
                metadataValue = "";
        }
        catch ( Exception e )
        {
            throw new CompareException(e);
        }
    }
}
