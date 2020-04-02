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
package fr.lixbox.orm.hibernate.criterion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import fr.lixbox.common.util.StringUtil;

/**
 * Cette classe corrige le bug sur les exclusions de proprietess de type chaine.
 * Si la chaine est vide ou "", on exclut les proprietes.
 * 
 * Support for query by example.
 * <pre>
 * List results = session.createCriteria(Parent.class)
 *     .add( Example.create(parent).ignoreCase() )
 *     .createCriteria("child")
 *         .add( Example.create( parent.getChild() ) )
 *     .list();
 * </pre>
 * "Examples" may be mixed and matched with "Expressions" in the same <tt>Criteria</tt>.
 * @see org.hibernate.Criteria
 * 
 * @author ludovic.terral
 */

public class Example implements Criterion 
{
    // ----------- Attribut -----------   
    private static final long serialVersionUID = -1201207271357L;
    private static final PropertySelector NOT_NULL = new NotNullPropertySelector();
    private static final PropertySelector ALL = new AllPropertySelector();
    private static final PropertySelector NOT_NULL_OR_ZERO = new NotNullOrZeroPropertySelector();
    private static final TypedValue[] TYPED_VALUES = new TypedValue[0];

	private transient Object entity;
	private final Set<String> excludedProperties = new HashSet<>();
	private PropertySelector selector;
	private boolean isLikeEnabled;
	private Character escapeCharacter;
	private boolean isIgnoreCaseEnabled;
	private MatchMode matchMode;

	
	
    // ----------- Methode -----------
    public static Example create(Object entity)
    {
        if (entity == null) throw new NullPointerException("null example");
        return new Example(entity, NOT_NULL);
    }
    
    
    
    protected Example(Object entity, PropertySelector selector)
    {
        this.entity = entity;
        this.selector = selector;
    }
	   
	   

    public Example setEscapeCharacter(Character escapeCharacter)
    {
        this.escapeCharacter = escapeCharacter;
        return this;
    }



    public Example setPropertySelector(PropertySelector selector)
    {
        this.selector = selector;
        return this;
    }

	
	
    public Example excludeZeroes()
    {
        setPropertySelector(NOT_NULL_OR_ZERO);
        return this;
    }



    public Example excludeNone()
    {
        setPropertySelector(ALL);
        return this;
    }


	
    public Example enableLike(MatchMode matchMode)
    {
        isLikeEnabled = true;
        this.matchMode = matchMode;
        return this;
    }


	
    public Example enableLike()
    {
        return enableLike(MatchMode.EXACT);
    }
    


    public Example ignoreCase()
    {
        isIgnoreCaseEnabled = true;
        return this;
    }

    

	public Example excludeProperty(String name) 
	{
		excludedProperties.add(name);
		return this;
	}


	
    public String toString()
    {
        return "example (" + entity + ')';
    }



    private boolean isPropertyIncluded(Object value, String name, Type type)
    {
        return !excludedProperties.contains(name) && !type.isAssociationType() && selector.include(value, name, type);
    }



    @SuppressWarnings("deprecation")
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
    {
        StringBuilder buf = new StringBuilder().append('(');
        EntityPersister meta = criteriaQuery.getFactory().getEntityPersister(criteriaQuery.getEntityName(criteria));
        String[] propertyNames = meta.getPropertyNames();
        Type[] propertyTypes = meta.getPropertyTypes();

        Object[] propertyValues = meta.getPropertyValues(entity);
        for (int i = 0; i < propertyNames.length; i++)
        {
            Object propertyValue = propertyValues[i];
            String propertyName = propertyNames[i];
            boolean isPropertyIncluded = i != meta.getVersionProperty() && isPropertyIncluded(propertyValue, propertyName, propertyTypes[i]);
            if (isPropertyIncluded)
            {
                if (propertyTypes[i].isComponentType())
                {
                    appendComponentCondition(propertyName, propertyValue, (CompositeType) propertyTypes[i], criteria, criteriaQuery, buf);
                }
                else
                {
                    appendPropertyCondition(propertyName, propertyValue, criteria, criteriaQuery, buf);
                }
            }
        }
        if (buf.length() == 1) buf.append("1=1"); // yuck!
        return buf.append(')').toString();
    }



    @SuppressWarnings("deprecation")
    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
    {
        EntityPersister meta = criteriaQuery.getFactory().getEntityPersister(criteriaQuery.getEntityName(criteria));
        String[] propertyNames = meta.getPropertyNames();
        Type[] propertyTypes = meta.getPropertyTypes();
        Object[] values = meta.getPropertyValues(entity);
        List<TypedValue> list = new ArrayList<>();
        for (int i = 0; i < propertyNames.length; i++)
        {
            Object value = values[i];
            Type type = propertyTypes[i];
            String name = propertyNames[i];
            boolean isPropertyIncluded = i != meta.getVersionProperty() && isPropertyIncluded(value, name, type);
            if (isPropertyIncluded)
            {
                if (propertyTypes[i].isComponentType())
                {
                    addComponentTypedValues(name, value, (CompositeType) type, list, criteria, criteriaQuery);
                }
                else
                {
                    addPropertyTypedValue(value, type, list);
                }
            }
        }
        return list.toArray(TYPED_VALUES);
    }
	
	

    @SuppressWarnings("deprecation")
    private EntityMode getEntityMode(Criteria criteria, CriteriaQuery criteriaQuery)
    {
        EntityPersister meta = criteriaQuery.getFactory().getEntityPersister(criteriaQuery.getEntityName(criteria));
        EntityMode result = meta.getEntityMode();
        if (!meta.getEntityMetamodel().getTuplizer().isInstance(entity))
        {
            throw new ClassCastException(entity.getClass().getName());
        }
        return result;
    }

	
	
    protected void addPropertyTypedValue(Object value, Type type, List<TypedValue> list)
    {
        if (value != null)
        {
            if (value instanceof String)
            {
                String string = (String) value;
                if (isIgnoreCaseEnabled) string = string.toLowerCase();
                if (isLikeEnabled) string = matchMode.toMatchString(string);
                value = string;
            }
            list.add(new TypedValue(type, value));
        }
    }



    protected void addComponentTypedValues(String path, Object component, CompositeType type,
            List<TypedValue> list, Criteria criteria, CriteriaQuery criteriaQuery)
    {
        if (component != null)
        {
            String[] propertyNames = type.getPropertyNames();
            Type[] subtypes = type.getSubtypes();
            Object[] values = type.getPropertyValues(component, getEntityMode(criteria, criteriaQuery));
            for (int i = 0; i < propertyNames.length; i++)
            {
                Object value = values[i];
                Type subtype = subtypes[i];
                String subpath = StringHelper.qualify(path, propertyNames[i]);
                if (isPropertyIncluded(value, subpath, subtype))
                {
                    if (subtype.isComponentType())
                    {
                        addComponentTypedValues(subpath, value, (CompositeType) subtype, list, criteria, criteriaQuery);
                    }
                    else
                    {
                        addPropertyTypedValue(value, subtype, list);
                    }
                }
            }
        }
    }



    protected void appendPropertyCondition(String propertyName, Object propertyValue,
            Criteria criteria, CriteriaQuery cq, StringBuilder buf)
    {
        Criterion crit;
        if (propertyValue != null)
        {
            boolean isString = propertyValue instanceof String;
            if (isLikeEnabled && isString)
            {
                crit = new LikeExpression(propertyName, (String) propertyValue, matchMode, escapeCharacter, isIgnoreCaseEnabled);
            }
            else
            {
                crit = new SimpleExpression(propertyName, propertyValue, "=", isIgnoreCaseEnabled && isString);
            }
        }
        else
        {
            crit = new NullExpression(propertyName);
        }
        String critCondition = crit.toSqlString(criteria, cq);
        if (buf.length() > 1 && critCondition.trim().length() > 0) buf.append(" and ");
        buf.append(critCondition);
    }



    protected void appendComponentCondition(String path, Object component, CompositeType type,
            Criteria criteria, CriteriaQuery criteriaQuery, StringBuilder buf)
    {
        if (component != null)
        {
            String[] propertyNames = type.getPropertyNames();
            Object[] values = type.getPropertyValues(component, getEntityMode(criteria, criteriaQuery));
            Type[] subtypes = type.getSubtypes();
            for (int i = 0; i < propertyNames.length; i++)
            {
                String subpath = StringHelper.qualify(path, propertyNames[i]);
                Object value = values[i];
                if (isPropertyIncluded(value, subpath, subtypes[i]))
                {
                    Type subtype = subtypes[i];
                    if (subtype.isComponentType())
                    {
                        appendComponentCondition(subpath, value, (CompositeType) subtype, criteria, criteriaQuery, buf);
                    }
                    else
                    {
                        appendPropertyCondition(subpath, value, criteria, criteriaQuery, buf);
                    }
                }
            }
        }
    }
	

    
    // ----------- inner class -----------
    public static interface PropertySelector extends Serializable 
    {
        public boolean include(Object propertyValue, String propertyName, Type type);
    }


    
    static final class AllPropertySelector implements PropertySelector 
    {
        private static final long serialVersionUID = 3609330848926999905L;
        public boolean include(Object object, String propertyName, Type type) 
        {
            return true;
        }
        
        
        private Object readResolve()
        {
            return ALL;
        }
    }
    
    
    
    static final class NotNullPropertySelector implements PropertySelector 
    {
        private static final long serialVersionUID = -7698520962073100665L;
        public boolean include(Object object, String propertyName, Type type) 
        {
            return object!=null;
        }
        
        
        private Object readResolve() 
        {
            return NOT_NULL;
        }
    }
    
    

    static final class NotNullOrZeroPropertySelector implements PropertySelector
    {
        private static final long serialVersionUID = -1418416547255760578L;
        public boolean include(Object object, String propertyName, Type type)
        {
            return object!=null && (
                    ((object instanceof String) && !StringUtil.isEmpty((String)object)) ||
                    ((object instanceof Number) && (((Number) object).longValue() != 0)) ||
                    (!(object instanceof String)&& !(object instanceof Number)));
        }



        private Object readResolve()
        {
            return NOT_NULL_OR_ZERO;
        }
    }
}
