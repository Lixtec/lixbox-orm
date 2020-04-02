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

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.TypedValue;

/**
 * Cette classe est le clone de la classe initiale.
 * 
 * @author ludovic.terral
 */
public class LikeExpression implements Criterion
{
    // ----------- Attribut -----------   
    private static final long serialVersionUID = -1201207271507L;
    
    private final String propertyName;
    private transient Object value;
    private final Character escapeChar;
    private final boolean ignoreCase;
    
    
    
    // ----------- Methode -----------
    protected LikeExpression(String propertyName, String value, Character escapeChar,
            boolean ignoreCase)
    {
        this.propertyName = propertyName;
        this.value = value;
        this.escapeChar = escapeChar;
        this.ignoreCase = ignoreCase;
    }
    protected LikeExpression(String propertyName, String value)
    {
        this(propertyName, value, null, false);
    }
    protected LikeExpression(String propertyName, String value, MatchMode matchMode)
    {
        this(propertyName, matchMode.toMatchString(value));
    }
    protected LikeExpression(String propertyName, String value, MatchMode matchMode,
            Character escapeChar, boolean ignoreCase)
    {
        this(propertyName, matchMode.toMatchString(value), escapeChar, ignoreCase);
    }



    @SuppressWarnings("deprecation")
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
    {
        Dialect dialect = criteriaQuery.getFactory().getDialect();
        String[] columns = criteriaQuery.findColumns(propertyName, criteria);
        if (columns.length != 1)
        {
            throw new HibernateException("Like may only be used with single-column properties");
        }
        String escape = escapeChar == null ? "" : " escape \'" + escapeChar + "\'";
        String column = columns[0];
        if (ignoreCase)
        {
            if (dialect.supportsCaseInsensitiveLike())
            {
                return column + " " + dialect.getCaseInsensitiveLike() + " ?" + escape;
            }
            else
            {
                return dialect.getLowercaseFunction() + '(' + column + ')' + " like ?" + escape;
            }
        }
        else
        {
            return column + " like ?" + escape;
        }
    }



    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
    {
        return new TypedValue[] { criteriaQuery.getTypedValue(criteria, propertyName, ignoreCase ? value.toString().toLowerCase() : value.toString()) };
    }
}
