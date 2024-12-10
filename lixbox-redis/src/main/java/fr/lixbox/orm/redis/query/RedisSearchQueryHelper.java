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

import fr.lixbox.common.helper.StringTokenizer;
import fr.lixbox.common.util.CollectionUtil;
import fr.lixbox.common.util.StringUtil;
import fr.lixbox.orm.redis.model.RedisSearchDao;
import redis.clients.jedis.search.Schema.Field;
import redis.clients.jedis.search.Schema.FieldType;

public class RedisSearchQueryHelper
{
    private RedisSearchQueryHelper()
    {
        //singleton
    }
    
    
    
    public static String toAndMultipurposeField(String attribute, Collection<?> values)
    {
        char andChar = ' ';
        StringBuilder sbf = new StringBuilder("(@"+attribute+":");
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
    
    
    
    public static String toOrMultipurposeField(String attribute, Collection<?> values)
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

    
    
	public static String toQueryByCriteria(RedisSearchDao criteria, boolean startWith) {
		StringBuilder query = new StringBuilder("");

		for (Field index : criteria.getIndexSchema().fields) {
			String fieldName = index.getName().getName(); // Utilisation du getter
			FieldType fieldType = index.getType(); // Getter pour le type du champ

			// Vérification de la présence de la valeur pour ce champ
			if (criteria.getIndexFieldValues().containsKey(fieldName)
					&& criteria.getIndexFieldValues().get(fieldName) != null) {

				Object value = criteria.getIndexFieldValues().get(fieldName);

				// Ajout des champs numériques
				if (FieldType.NUMERIC.equals(fieldType)) {
					query.append(addNumericField(fieldName, value));
				}
				// Ajout des champs texte
				else if (FieldType.TEXT.equals(fieldType)) {
					query.append(addTextField(fieldName, value, startWith));
				}
			}
		}

		// Si aucun champ n'a été ajouté, utiliser le wildcard
		if (query.length() == 0) {
			query.append('*');
		}

		return query.toString();
	}



    private static String addTextField(String indexName, Object value, boolean startWith)
    {
        StringBuilder query = new StringBuilder("");
        if (value instanceof String && ((String)value).startsWith("[") && ((String)value).endsWith("]"))
        {
            if (((String)value).length()>2)
            {
                String sValue = (String) value;
                sValue = sValue.replace("[ ","").replace(" ]", "");
                StringTokenizer tokenizer = new StringTokenizer(sValue, " ");
                query.append(toAndMultipurposeField(indexName, tokenizer.getTokens()));
                query.append(' ');
            }
        }
        else if (value instanceof String && !(((String)value).startsWith("[")  && ((String)value).endsWith("]")))
        {
            query.append(toTextField(indexName, (String) value, startWith));
            query.append(' ');
        }
        else if (value.getClass().isArray())
        {
            if (((Object[])value).length>0)
            {
                query.append(toAndMultipurposeField(indexName, CollectionUtil.convertArrayToList((Object[])value)));
                query.append(' ');
            }
        }
        else if (value instanceof Collection<?>)
        {
            if (CollectionUtil.isNotEmpty((Collection<?>) value))
            {
                query.append(toAndMultipurposeField(indexName, (Collection<?>) value));
                query.append(' ');
            }
        }
        else
        {
            query.append(toTextField(indexName, value.toString()));
            query.append(' ');
        }
        return query.toString();
    }



    private static String addNumericField(String indexName, Object value)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(indexName);
        query.append(':');
        query.append("[");
        query.append(value);
        query.append(" ");
        query.append(value);
        query.append("]");
        return query.toString();
    }



    public static String toTextField(String name, String value)
    {
        return toTextField(name, value, false);
    }



    public static String toNumericField(String name, String value)
    {
        return addNumericField(name, value);
    }


    public static String toTextField(String name, String value, boolean startWith)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append(RedisSearchValueSanitizer.sanitizeValue(value));
        if (StringUtil.isEmpty(value))
        {
            query = query.delete(0, query.length());
        }
        else if (startWith)
        {
            query.append('*');
        }
        return query.toString();
    }



    public static String toLowerEqualThanField(String name, long value)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append("[-inf ");
        query.append(value);
        query.append(']');
        return query.toString();
    }



    public static String toLowerThanField(String name, long value)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append("[-inf (");
        query.append(value);
        query.append(']');
        return query.toString();
    }



    public static String toGreaterEqualThanField(String name, long value)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append("[");
        query.append(value);
        query.append(" +inf]");
        return query.toString();
    }



    public static String toGreaterThanField(String name, long value)
    {
        StringBuilder query = new StringBuilder("@");
        query.append(name);
        query.append(':');
        query.append("[(");
        query.append(value);
        query.append(" +inf]");
        return query.toString();
    }



    public static String toBetweenField(String name, long valueMin, long valueMax)
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
        query.deleteCharAt(query.length()-1);
        return query.toString();
    }



    public static String toNotCriteria(String criteria)
    {
        StringBuilder query = new StringBuilder("-");
        query.append(criteria);
        return query.toString();
    }
}