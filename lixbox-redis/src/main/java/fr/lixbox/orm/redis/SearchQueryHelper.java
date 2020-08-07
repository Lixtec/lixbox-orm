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
package fr.lixbox.orm.redis;

import java.util.Collection;

import fr.lixbox.common.util.CollectionUtil;
import fr.lixbox.common.util.StringUtil;
import fr.lixbox.orm.redis.model.RedisSearchDao;
import io.redisearch.Schema.Field;

public class SearchQueryHelper
{
    private SearchQueryHelper()
    {
        //singleton
    }
    
    
    
    public static String toAndMultipurposeAttribute(String attribute, Collection<?> values)
    {
        char andChar = ' ';
        StringBuilder sbf = new StringBuilder("@"+attribute+":(");
        for (Object value:values)
        {
            sbf.append(value!=null?value.toString():"");
            sbf.append(andChar);
        }
        if (sbf.charAt(sbf.length()-1)==andChar) 
        {
            sbf.delete(sbf.length()-1, sbf.length());
        }
        sbf.append(")");
        return clearQuery(sbf);
    }
    
    
    
    public static String toOrMultipurposeAttribute(String attribute, Collection<?> values)
    {
        char andChar = '|';
        StringBuilder sbf = new StringBuilder("@"+attribute+":(");
        for (Object value:values)
        {
            sbf.append(value!=null?value.toString():"");
            sbf.append(andChar);
        }
        if (sbf.charAt(sbf.length()-1)==andChar) 
        {
            sbf.delete(sbf.length()-1, sbf.length());
        }
        sbf.append(")");
        return clearQuery(sbf);
    }



    public static String toQueryByCriteria(RedisSearchDao criteria)
    {
        StringBuilder query = new StringBuilder("");
        for (Field index : criteria.getIndexSchema().fields)
        {
            if (criteria.getIndexFieldValues().containsKey(index.name))
            {
                Object value = criteria.getIndexFieldValues().get(index.name);
                if (value instanceof String && StringUtil.isNotEmpty((String) value))
                {
                    query.append(toStringAttribute(index.name, (String) value));
                    query.append(' ');
                }
                if (value instanceof Collection<?> && CollectionUtil.isNotEmpty((Collection<?>) value))
                {
                    query.append(toAndMultipurposeAttribute(index.name, (Collection<?>) value));
                    query.append(' ');
                }
                if (value instanceof Boolean)
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
        return clearQuery(query);
    }



    public static String toStringAttribute(String name, String value)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append(value);
        return clearQuery(query);
    }
    
    
    private static String clearQuery(StringBuilder sbf)
    {
        String query = sbf.toString();
        query.replace('-', '*');
        return query;
    }
}