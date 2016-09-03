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


import calliope.core.json.corcode.Annotation;
import calliope.core.json.corcode.Range;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.Pair;
import java.util.ArrayList;
import calliope.core.constants.JSONKeys;
import compare.constants.ChunkState;
import org.json.simple.JSONObject;
import calliope.core.json.corcode.RangeComplete;
import calliope.core.json.corcode.ProgressiveParser;
/**
 * Construct a CorCode programmatically
 * @author desmond
 */
public class CorCode extends JSONObject implements RangeComplete
{
    /** current range */
    Range current;
    /** ranges waiting to be completed */
    ArrayList<Range> pending;
    /** The ranges array */
    ArrayList<JSONObject> ranges;
    /** the last offset added to ranges */
    int lastOffset;
    /** the current offset */
    int offset;
    /** the transposition id-generator */
    IdGenerator idGen;
    static final int BOTH = 3;
    static final int NEITHER = 0;
    static final int V1 = 1;
    static final int V2 = 2;
    /**
     * Create a new CorCode
     * @param style the style name to use for this CorCode
     */
    public CorCode( String style )
    {
        ranges = new ArrayList<JSONObject>();
        pending = new ArrayList<Range>();
        put( JSONKeys.RANGES, ranges );
        put( JSONKeys.STYLE, style );
        idGen = new IdGenerator();
    }
    /**
     * A range in the CorCode has been completed
     * @param offset the absolute offset
     * @param len the range's length
     */
    @Override
    public void rangeComplete( int offset, int len )
    {
        this.offset = offset;
        // flush pending
        for ( int i=0;i<pending.size();i++ )
        {
            Range r = pending.get(i);
            r.len += len;
            if ( r.offset == Range.UNSET )
                r.offset = offset;
            if ( r.len > 0 )
                addRange( r );
        }
        pending.clear();
        // update current
        if ( current != null )
        {
            if ( current.offset == Range.UNSET )
                current.offset = offset;
            current.len += len;
        }
    }
    /**
     * Add a range to the CorCode
     * @param r the range to add
     * @return null to indicate that the passed-in range is now gone
     */
    private Range addRange( Range r ) 
    {
        try
        {
            int reloff = r.offset - lastOffset;
            if ( r.getHasText() )
            {
                JSONObject doc = new JSONObject();
                if ( reloff > 0 )
                    lastOffset = r.offset;
                doc.put( JSONKeys.NAME, r.name );
                doc.put( JSONKeys.RELOFF, reloff );
                doc.put( JSONKeys.LEN, r.len );
                if ( r.annotations != null && r.annotations.size() > 0 )
                {
                    ArrayList<Object> attrs = new ArrayList<Object>();
                    for ( int i=0;i<r.annotations.size();i++ )
                    {
                        Annotation a = r.annotations.get( i );
                        attrs.add( a.toJSONObject() );// throws JSONException
                    }
                    doc.put( JSONKeys.ANNOTATIONS, attrs );
                }
                ranges.add( doc );
            }
        }
        catch ( Exception e )
        {
        }
        return null;
    }
    /**
     * Generate ranges in corcode by comparing one version with another.
     * @param text the MVD to get the versions from
     * @param v1 the first version
     * @param v2 the second version
     * @param state the state to label pairs in v1 that are not in v2
     */
    public void compareCode( MVD text, int v1, int v2, String state )
    {
        try
        {
            ProgressiveParser pp = new ProgressiveParser( this );
            ArrayList<Pair> pairs = text.getPairs();
            for ( int i=0;i<pairs.size();i++ )
            {
                Pair p = pairs.get( i );
                if ( p.versions.nextSetBit(v1)==v1 )
                {
                    char[] chars = p.getChars();
                    if ( p.versions.nextSetBit(v2)!=v2 )
                    {
                        if ( current == null )
                            current = new Range( state );
                        boolean hasText = pp.parseData(chars);
                        if ( !current.hasText && hasText )
                            current.hasText = true;
                    }
                    else 
                    {
                        if ( current != null )
                        {
                            if ( current.hasText )
                                pending.add( current );
                            current = null;
                        }
                        pp.parseData( chars );
                    }
                }   
            }
        }
        catch ( Exception e )
        {
        }
    }
    /**
     * Compute the child id
     * @param id the numeric value
     * @return a string representation of it to use as a target
     */
    String childId( int id )
    {
        return "#c"+Integer.toString(id);
    }
    /**
     * Compute the parent id
     * @param id the numeric value
     * @return a string representation of it to use as a target
     */
    String parentId( int id )
    {
        return "#p"+Integer.toString(id);
    }
    /**
     * Get the name of the current range from the pair and the default name
     * @param p the pair to test
     * @param defaultName if not a transposition return this
     * @param hasv1 true if the current pair has v1
     * @param hasv2 true if the current pair has v2
     * @return a String being the name of the new range
     */
    String getStateName( Pair p, String defaultName, boolean hasv1, boolean hasv2 )
    {
        if ( !hasv1 && hasv2 )
            return "";
        else if ( hasv1 && hasv2 )
            return "merged";
        else if ( p.isParent() )
            return ChunkState.PARENT;
        else if ( p.isChild() )
            return ChunkState.CHILD;
        else
            return defaultName;
    }
    private boolean pairHasText( Pair p )
    {
        char[] data = p.getChars();
        for ( int i=0;i<data.length;i++ )
        {
            if ( !Character.isWhitespace(data[i]) )
            {
                return true;
            }
        }
        return false;
    }
    /**
     * Create a new Range
     * @param p the pair it was gleaned from
     * @param name its name
     * @return the new Range
     */
    Range newCurrent( Pair p, String name )
    {
        Range r = new Range( name );
        if ( p.isParent() )
        {
            int id = p.ensureId();
            r.addAnnotation(JSONKeys.CHILDID, childId(id) );
        }
        else if ( p.isChild() )
        {
            int id = p.getParent().ensureId();
            r.addAnnotation(JSONKeys.PARENTID, parentId(id) );
        }
        r.offset = offset;
        r.len = p.length();
        r.setHasText( pairHasText(p) );
        return r;
    }
    /**
     * Process a merged pair. Successive calls may work within the same run.
     * @param p a merged pair
     * @param prefix prefix this to all generated IDs
     * @param split split this merged pair from the previous if any
     */
    void doMergedPair( Pair p, char prefix, boolean split )
    {
        if ( split )
        {
            if ( current != null )
                current = addRange( current );
            current = new Range( ChunkState.MERGED, offset, p.length() );
            current.addAnnotation( "mergeid", prefix
                +new Integer(idGen.next()).toString() );
        }
        else
        {
            if ( current != null )
            {
                current.len += p.length();
            }
            else
            {
                current = new Range( ChunkState.MERGED, offset, p.length() );
                current.addAnnotation( "mergeid", prefix
                    +new Integer(idGen.next()).toString() );
            }
        }
    }
    int getSet(boolean v1, boolean v2 )
    {
        if ( v1 && v2 )
            return BOTH;
        else if ( !v1 && !v2 )
            return NEITHER;
        else if ( v1 )
            return V1;
        else
            return V2;
    }
    /**
     * Decide whether we need to split the current state from the new state
     * @param old the old match state of v1 and v2
     * @param newSet the new state 
     * @return true if we should break the current accumulating property
     */
    boolean getSplit( int old, int newSet, String oldName, String newName )
    {
        if ( !oldName.equals(newName) )
            return true;
        else
        {
            // may seem long-winded but it is clear
            switch ( old )
            {
                case BOTH:
                    switch (newSet)
                    {
                        case BOTH: 
                            return false;
                        case V1: case V2:
                            return true;
                    }
                    break;
                case V1:
                    switch (newSet)
                    {
                        case BOTH: case V2:
                            return true;
                        case V1: 
                            return false;
                    }
                    break;
                case V2:
                    switch (newSet)
                    {
                        case BOTH: case V1:
                            return true;
                        case V2: 
                            return false;
                    }
                    break;
                default: 
                    return false;
            }
            return false;
        }
    }
    /**
     * Compare two versions of an MVD and store the result in this corcode
     * @param text the MVD
     * @param v1 the first version to compare with v2
     * @param v2 the second version to compare with v1
     * @param state the name of the ranges to create
     */
    public void compareText( MVD text, int v1, int v2, String state )
    {
        try
        {
            ArrayList<Pair> pairs = text.getPairs();
            String name,currName;
            int oldSet = 0;
            int newSet = 0;
            boolean split = false;
            current = null;
            for ( int i=0;i<pairs.size();i++ )
            {
                Pair p = pairs.get( i );
                boolean hasv1 = p.versions.nextSetBit(v1)==v1;
                boolean hasv2 = p.versions.nextSetBit(v2)==v2;
                if ( hasv1 || hasv2 )
                {
                    name = getStateName(p,state,hasv1,hasv2);
                    oldSet = newSet;
                    newSet = getSet( hasv1,hasv2 );
                    currName = (current==null)?"":current.getName();
                    split = getSplit(oldSet,newSet,currName,name);
                    if ( split )
                    {
                        if ( current != null )
                            current = addRange( current );
                        if ( name.length()> 0 )
                        {
                            current = newCurrent( p, name );
                            if ( newSet == BOTH )
                            {
                                char prefix = state.charAt(0);
                                current.addAnnotation( "mergeid", prefix
                                    +new Integer(idGen.next()).toString() );
                            }
                        }
                    }
                    else if ( current != null )
                    {
                        current.len += p.length();
                        if ( !current.getHasText() && pairHasText(p) )
                            current.setHasText(true);
                    }
                    if ( hasv1 )
                        offset += p.length();
                }
            }
            if ( current != null )
                current = addRange( current );
//            for ( int i=0;i<ranges.size();i++ )
//                System.out.println(ranges.get(i).toJSONString());
//            System.out.println("Compared "+v1+" with "+v2+"; maxid="+idGen.getCurrent());
        }
        catch ( Exception e )
        {
            // shouldn't happen anyway
            e.printStackTrace(System.out);
        }
    }
}
