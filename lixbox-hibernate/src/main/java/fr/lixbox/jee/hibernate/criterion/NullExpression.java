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
