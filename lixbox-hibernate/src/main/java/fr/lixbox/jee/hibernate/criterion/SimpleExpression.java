/*******************************************************************************
 *    
 *                           FRAMEWORK Lixbox
 *                          ==================
 *      
 *   Copyrigth - LIXTEC - Tous droits reserves.
 *   
 *   Le contenu de ce fichier est la propriete de la societe Lixtec.
 *   
 *   Toute utilisation de ce fichier et des informations, sous n'importe quelle
 *   forme necessite un accord ecrit explicite des auteurs
 *   
 *   @AUTHOR Ludovic TERRAL
 *
 ******************************************************************************/
package fr.lixbox.jee.hibernate.criterion;

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
