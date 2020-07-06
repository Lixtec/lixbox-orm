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

import java.sql.Types;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.Type;

/**
 * Cette classe est le clone de la classe initiale.
 * 
 * @author ludovic.terral
 */
public class SimpleExpression implements Criterion
{
    // ----------- Attribut -----------
    private static final long serialVersionUID = -1201207271516L;
    private final String propertyName;
    private transient Object value;
    private boolean ignoreCase;
    private final String op;



    // ----------- Methode -----------
    protected SimpleExpression(String propertyName, Object value, String op)
    {
        this.propertyName = propertyName;
        this.value = value;
        this.op = op;
    }
    protected SimpleExpression(String propertyName, Object value, String op, boolean ignoreCase)
    {
        this.propertyName = propertyName;
        this.value = value;
        this.ignoreCase = ignoreCase;
        this.op = op;
    }
    public SimpleExpression ignoreCase()
    {
        ignoreCase = true;
        return this;
    }



    @SuppressWarnings("deprecation")
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
    {
        String[] columns = criteriaQuery.findColumns(propertyName, criteria);
        Type type = criteriaQuery.getTypeUsingProjection(criteria, propertyName);
        StringBuilder fragment = new StringBuilder();
        if (columns.length > 1) fragment.append('(');
        SessionFactoryImplementor factory = criteriaQuery.getFactory();
        int[] sqlTypes = type.sqlTypes(factory);
        for (int i = 0; i < columns.length; i++)
        {
            boolean lower = ignoreCase && (sqlTypes[i] == Types.VARCHAR || sqlTypes[i] == Types.CHAR);
            if (lower)
            {
                fragment.append(factory.getDialect().getLowercaseFunction()).append('(');
            }
            fragment.append(columns[i]);
            if (lower) fragment.append(')');
            fragment.append(getOp()).append("?");
            if (i < columns.length - 1) fragment.append(" and ");
        }
        if (columns.length > 1) fragment.append(')');
        return fragment.toString();
    }



    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
    {
        Object icvalue = ignoreCase ? value.toString().toLowerCase() : value;
        return new TypedValue[] { criteriaQuery.getTypedValue(criteria, propertyName, icvalue) };
    }



    public String toString()
    {
        return propertyName + getOp() + value;
    }



    protected final String getOp()
    {
        return op;
    }
}
