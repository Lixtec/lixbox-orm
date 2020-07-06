/*******************************************************************************
 *    
 *                           FRAMEWORK Lixbox
 *                          ==================
 *      
 * This file is part of lixbox-orm.
 *
 *    lixbox-supervision is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    lixbox-supervision is distributed in the hope that it will be useful,
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
package fr.lixbox.orm.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;

/**
 * Cette classe est le clone de la classe initiale.
 * 
 * @author ludovic.terral
 */
public class NullExpression implements Criterion
{
    // ----------- Attribut -----------   
    private static final long serialVersionUID = -1201207271514L;
    
	private final String propertyName;
	private static final TypedValue[] NO_VALUES = new TypedValue[0];
    
    
    
    // ----------- Methode -----------
    protected NullExpression(String propertyName)
    {
        this.propertyName = propertyName;
    }



    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
    {
        String[] columns = criteriaQuery.findColumns(propertyName, criteria);
        String result = String.join(" and ", StringHelper.suffix(columns, " is null"));
        if (columns.length > 1) result = '(' + result + ')';
        return result;
    }



    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
    {
        return NO_VALUES;
    }



    public String toString()
    {
        return propertyName + " is null";
    }
}
