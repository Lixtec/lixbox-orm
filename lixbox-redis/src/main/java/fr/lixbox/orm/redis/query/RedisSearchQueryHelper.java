/*******************************************************************************
 *    
 *                           FRAMEWORK Lixbox
 *                          ==================
 *      
 * This file is part of lixbox-orm.
 *
 *    lixbox-orm is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    lixbox-orm is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *    along with lixbox-orm.  If not, see <https://www.gnu.org/licenses/>
 *   
 *   @AUTHOR Lixbox-team
 *
 ******************************************************************************/
package fr.lixbox.orm.redis.query;

import java.util.Collection;

import com.fasterxml.jackson.core.type.TypeReference;

import fr.lixbox.common.util.CollectionUtil;
import fr.lixbox.common.util.StringUtil;
import fr.lixbox.io.json.JsonUtil;
import fr.lixbox.orm.redis.model.RedisSearchDao;
import io.redisearch.Schema.Field;

public class RedisSearchQueryHelper
{
    private RedisSearchQueryHelper()
    {
        //singleton
    }
    
    
    
    public static String toAndMultipurposeAttribute(String attribute, Collection<?> values)
    {
        char andChar = ' ';
        StringBuilder sbf = new StringBuilder("@"+attribute+":(");
        for (Object value:values)
        {
            sbf.append(value!=null?RedisSearchValueSanitizer.sanitizeValue(value.toString()):"");
            sbf.append(andChar);
        }
        if (sbf.charAt(sbf.length()-1)==andChar) 
        {
            sbf.delete(sbf.length()-1, sbf.length());
        }
        sbf.append(")");
        return sbf.toString();
    }
    
    
    
    public static String toOrMultipurposeAttribute(String attribute, Collection<?> values)
    {
        char andChar = '|';
        StringBuilder sbf = new StringBuilder("@"+attribute+":(");
        for (Object value:values)
        {
            sbf.append(value!=null?RedisSearchValueSanitizer.sanitizeValue(value.toString()):"");
            sbf.append(andChar);
        }
        if (sbf.charAt(sbf.length()-1)==andChar) 
        {
            sbf.delete(sbf.length()-1, sbf.length());
        }
        sbf.append(")");
        return sbf.toString();
    }

    

    public static String toQueryByCriteria(RedisSearchDao criteria)
    {
        return toQueryByCriteria(criteria, false);
    }

    
    
    public static String toQueryByCriteria(RedisSearchDao criteria, boolean startWith)
    {
        StringBuilder query = new StringBuilder("");
        for (Field index : criteria.getIndexSchema().fields)
        {
            if (criteria.getIndexFieldValues().containsKey(index.name))
            {
                Object value = criteria.getIndexFieldValues().get(index.name);
                if (value instanceof String && StringUtil.isNotEmpty((String) value) && ((String)value).startsWith("[")  && ((String)value).endsWith("]") && ((String)value).length()>2)
                {
                    query.append(toAndMultipurposeAttribute(index.name, CollectionUtil.convertArrayToList(JsonUtil.transformJsonToObject((String)value, new TypeReference<Object[]>(){}))));
                    query.append(' ');
                }
                else if (value instanceof String && StringUtil.isNotEmpty((String) value) && !(((String)value).startsWith("[")  && ((String)value).endsWith("]")))
                {
                    query.append(toStringAttribute(index.name, (String) value, startWith));
                    query.append(' ');
                }
                else if (value!=null && value.getClass().isArray())
                {
                    query.append(toAndMultipurposeAttribute(index.name, CollectionUtil.convertArrayToList((Object[])value)));
                    query.append(' ');
                }
                else if (value instanceof Collection<?> && CollectionUtil.isNotEmpty((Collection<?>) value))
                {
                    query.append(toAndMultipurposeAttribute(index.name, (Collection<?>) value));
                    query.append(' ');
                }
                else if (value instanceof Boolean || value instanceof Enum<?>)
                {
                    query.append(toStringAttribute(index.name, value.toString()));
                    query.append(' ');
                }
            }
        }
        if (query.length()==0)
        {
            query.append('*');
        }
        return query.toString();
    }



    public static String toStringAttribute(String name, String value)
    {
        return toStringAttribute(name, value, false);
    }


    public static String toStringAttribute(String name, String value, boolean startWith)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append(RedisSearchValueSanitizer.sanitizeValue(value));
        if (startWith)
        {
            query.append('*');
        }
        return query.toString();
    }



    public static String toLowerEqualThanAttribute(String name, long value)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append("[-inf ");
        query.append(value);
        query.append(']');
        return query.toString();
    }



    public static String toLowerThanAttribute(String name, long value)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append("[-inf (");
        query.append(value);
        query.append(']');
        return query.toString();
    }



    public static String toGreaterEqualThanAttribute(String name, long value)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append("[");
        query.append(value);
        query.append(" +inf]");
        return query.toString();
    }



    public static String toGreaterThanAttribute(String name, long value)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append("[(");
        query.append(value);
        query.append(" +inf]");
        return query.toString();
    }



    public static String toBetweenAttribute(String name, long valueMin, long valueMax)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append("[");
        query.append(valueMin);
        query.append(" ");
        query.append(valueMax);
        query.append("]");
        return query.toString();
    }



    public static String toOrCriterias(String... criterias)
    {
        StringBuilder query = new StringBuilder();
        for (String criteria : criterias)
        {
            query.append('(');
            query.append(criteria);
            query.append(')');
            query.append('|');
        }
        if (criterias!=null && criterias.length>1)
        {
            query.deleteCharAt(query.length()-1);
        }
        return query.toString();
    }



    public static String toNotCriteria(String criteria)
    {
        StringBuilder query = new StringBuilder("-");
        query.append(criteria);
        return query.toString();
    }
}