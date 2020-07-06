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
package fr.lixbox.orm.hibernate.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;

import fr.lixbox.orm.entity.model.Dao;

/**
 * Cette classe assure le détachement des entités. Il traite les problèmes de LazyLoading.
 * 
 * @author ludovic.terral
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class HibernateDetachUtil implements Serializable
{
    // ----------- Attributs -----------
    private static final Log log = LogFactory.getLog(HibernateDetachUtil.class);
    private static final long serialVersionUID = 201710031501L;  
    
    private static final int depthAllowed = 20;
    private static final boolean throwExceptionOnDepthLimit = true;
    private static final boolean dumpStackOnThresholdLimit = true;
    private static final long millisThresholdLimit = 5000;
    private static final int sizeThresholdLimit = 10000;
    
    private static final HashCodeGenerator hashCodeGenerator = new SystemHashCodeGenerator();
            

    public enum SerializationType
    {
        SERIALIZATION, JAXB
    }
    
        
    static interface HashCodeGenerator
    {
        Integer getHashCode(Object value);
    }
    

    static class SystemHashCodeGenerator implements HashCodeGenerator
    {
        @Override
        public Integer getHashCode(Object value)
        {
            return System.identityHashCode(value);
        }
    }



    // ----------- Methodes -----------
    public static void nullOutUninitializedFields(Object value, SerializationType serializationType) throws IllegalAccessException, ClassNotFoundException, InstantiationException, InvocationTargetException, IntrospectionException
    {
        long start = System.currentTimeMillis();
        Map<Integer, Object> checkedObjectMap = new HashMap<>();
        Map<Integer, List<Object>> checkedObjectCollisionMap = new HashMap<>();
        nullOutUninitializedFields(value, checkedObjectMap, checkedObjectCollisionMap, 0, serializationType);
        long duration = System.currentTimeMillis() - start;
        if (dumpStackOnThresholdLimit)
        {
            int numObjectsProcessed = checkedObjectMap.size();
            if (duration > millisThresholdLimit || numObjectsProcessed > sizeThresholdLimit)
            {
                String rootObjectString = (value != null) ? value.getClass().toString() : "null";
                log.warn(
                        "Detached [" + numObjectsProcessed + "] objects in [" + duration
                                + "]ms from root object [" + rootObjectString + "]",
                        new Throwable("HIBERNATE DETACH UTILITY STACK TRACE"));
            }
        }
        else
        {
            // 10s is really long, log SOMETHING
            if (duration > 10000L && log.isDebugEnabled())
            {
                log.debug("Detached [" + checkedObjectMap.size() + "] objects in [" + duration
                        + "]ms");
            }
        }
        // help the garbage collector be clearing these before we leave
        checkedObjectMap.clear();
        checkedObjectCollisionMap.clear();
    }



    /**
     * @param value
     *            the object needing to be detached/scrubbed.
     * @param checkedObjectMap
     *            This maps identityHashCodes to Objects we've already detached.
     *            In that way we can quickly determine if we've already done the
     *            work for the incoming value and avoid taversing it again. This
     *            works well almost all of the time, but it is possible that two
     *            different objects can have the same identity hash (conflicts
     *            are always possible with a hash). In that case we utilize the
     *            checkedObjectCollisionMap (see below).
     * @param checkedObjectCollisionMap
     *            checkedObjectMap maps the identityhash to the *first* object
     *            with that hash. In most cases there will only be mapping for
     *            one hash, but it is possible to encounter the same hash for
     *            multiple objects, especially on 32bit or IBM JVMs. It is
     *            important to know if an object has already been detached
     *            because if it is somehow self-referencing, we have to stop the
     *            recursion. This map holds the 2nd..Nth mapping for a single
     *            hash and is used to ensure we never try to detach an object
     *            already processed.
     * @param depth
     *            used to stop infinite recursion, defaults to a depth we don't
     *            expectto see, but it is configurable.
     * @param serializationType
     * @throws InvocationTargetException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     * @throws IntrospectionException 
     * @throws Exception
     *             if a problem occurs
     * @throws IllegalStateException
     *             if the recursion depth limit is reached
     */
    private static void nullOutUninitializedFields(Object value,
            Map<Integer, Object> checkedObjectMap,
            Map<Integer, List<Object>> checkedObjectCollisionMap, int depth,
            SerializationType serializationType) throws IllegalAccessException, ClassNotFoundException, InstantiationException, InvocationTargetException, IntrospectionException
    {
        if (depth > depthAllowed)
        {
            String warningMessage = "Recursed too deep [" + depth + " > " + depthAllowed
                    + "], will not attempt to detach object of type ["
                    + ((value != null) ? value.getClass().getName() : "N/A")
                    + "]. This may cause serialization errors later. ";
            log.warn(warningMessage);
            if (throwExceptionOnDepthLimit)
            {
                throw new IllegalStateException(warningMessage);
            }
            return;
        }
        if (null == value)
        {
            return;
        }
        Integer valueIdentity = hashCodeGenerator.getHashCode(value);
        Object checkedObject = checkedObjectMap.get(valueIdentity);
        if (null == checkedObject)
        {
            checkedObjectMap.put(valueIdentity, value);
        }
        else if (value == checkedObject)
        {
            return;
        }
        else
        {
            boolean alreadyDetached = false;
            List<Object> collisionObjects = checkedObjectCollisionMap.get(valueIdentity);
            if (null == collisionObjects)
            {
                collisionObjects = new ArrayList<>(1);
                checkedObjectCollisionMap.put(valueIdentity, collisionObjects);
            }
            else
            {
                for (Object collisionObject : collisionObjects)
                {
                    if (value == collisionObject)
                    {
                        alreadyDetached = true;
                        break;
                    }
                }
            }
            if (log.isDebugEnabled())
            {
                StringBuilder message = new StringBuilder("\n\tIDENTITY HASHCODE COLLISION [hash=");
                message.append(valueIdentity);
                message.append(", alreadyDetached=");
                message.append(alreadyDetached);
                message.append("]");
                message.append("\n\tCurrent  : ");
                message.append(value.getClass().getName());
                message.append("\n\t    ");
                message.append(value);
                message.append("\n\tPrevious : ");
                message.append(checkedObject.getClass().getName());
                message.append("\n\t    ");
                message.append(checkedObject);
                for (Object collisionObject : collisionObjects)
                {
                    message.append("\n\tPrevious : ");
                    message.append(collisionObject.getClass().getName());
                    message.append("\n\t    ");
                    message.append(collisionObject);
                }
                log.debug(message.toString());
            }
            if (alreadyDetached)
            {
                return;
            }
            collisionObjects.add(value);
        }
        
        // Perform the detaching
        if (value instanceof Object[])
        {
            Object[] objArray = (Object[]) value;
            for (int i = 0; i < objArray.length; i++)
            {
                Object listEntry = objArray[i];
                Object replaceEntry = replaceObject(listEntry);
                if (replaceEntry != null)
                {
                    objArray[i] = replaceEntry;
                }
                nullOutUninitializedFields(objArray[i], checkedObjectMap, checkedObjectCollisionMap,
                        depth + 1, serializationType);
            }
        }
        else if (value instanceof List)
        {
            ListIterator i = ((List)((List) value).stream().distinct().collect(Collectors.toList())).listIterator();            
            while (i.hasNext())
            {
                Object val = i.next();
                Object replace = replaceObject(val);
                if (replace != null)
                {
                    val = replace;
                    i.set(replace);
                }
                nullOutUninitializedFields(val, checkedObjectMap, checkedObjectCollisionMap,
                        depth + 1, serializationType);
            }
        }
        else if (value instanceof Collection)
        {
            Collection collection = ((Collection)((Collection) value).stream().distinct().collect(Collectors.toList()));
            Collection itemsToBeReplaced = new ArrayList();
            Collection replacementItems = new ArrayList();
            for (Object item : collection)
            {
                Object replacementItem = replaceObject(item);
                if (replacementItem != null)
                {
                    itemsToBeReplaced.add(item);
                    replacementItems.add(replacementItem);
                    item = replacementItem;
                }
                nullOutUninitializedFields(item, checkedObjectMap, checkedObjectCollisionMap, depth + 1, serializationType);
            }
            collection.removeAll(itemsToBeReplaced);
            collection.addAll(replacementItems);
        }
        else if (value instanceof Map)
        {
            Map<Object, Object> originalMap = (Map) value;
            HashMap<Object, Object> replaceMap = new HashMap<>();
            for (Iterator i = originalMap.keySet().iterator(); i.hasNext();)
            {
                Object originalKey = i.next();
                Object originalKeyValue = originalMap.get(originalKey);
                Object replaceKey = replaceObject(originalKey);
                Object replaceValue = replaceObject(originalKeyValue);
                if (replaceKey != null || replaceValue != null)
                {
                    Object newKey = (replaceKey != null) ? replaceKey : originalKey;
                    Object newValue = (replaceValue != null) ? replaceValue : originalKeyValue;
                    replaceMap.put(newKey, newValue);
                    i.remove();
                }
            }
            originalMap.putAll(replaceMap);
            for (Entry<Object, Object> entry : originalMap.entrySet())
            {
                nullOutUninitializedFields(entry.getValue(), checkedObjectMap, checkedObjectCollisionMap, depth + 1, serializationType);
                nullOutUninitializedFields(entry.getKey(), checkedObjectMap, checkedObjectCollisionMap, depth + 1, serializationType);
            }
        }
        else if (value instanceof Enum)
        {
            return;
        }

        if (serializationType == SerializationType.JAXB)
        {
            XmlAccessorType at = value.getClass().getAnnotation(XmlAccessorType.class);
            if (at != null && at.value() == XmlAccessType.FIELD)
            {
                nullOutFieldsByFieldAccess(value, checkedObjectMap, checkedObjectCollisionMap, depth, serializationType);
            }
            else
            {
                nullOutFieldsByAccessors(value, checkedObjectMap, checkedObjectCollisionMap, depth, serializationType);
            }
        }        
        else if (serializationType == SerializationType.SERIALIZATION)
        {
            nullOutFieldsByFieldAccess(value, checkedObjectMap, checkedObjectCollisionMap, depth, serializationType);
        }
    }



    private static void nullOutFieldsByFieldAccess(Object object,
            Map<Integer, Object> checkedObjects,
            Map<Integer, List<Object>> checkedObjectCollisionMap, int depth,
            SerializationType serializationType) throws IllegalAccessException, ClassNotFoundException, InstantiationException, InvocationTargetException, IntrospectionException
    {
        Class tmpClass = object.getClass();
        List<Field> fieldsToClean = new ArrayList<>();
        while (tmpClass != null && tmpClass != Object.class)
        {
            Field[] declaredFields = tmpClass.getDeclaredFields();
            for (Field declaredField : declaredFields)
            {
                int modifiers = declaredField.getModifiers();
                if (!((Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))
                        || Modifier.isTransient(modifiers)))
                {
                    fieldsToClean.add(declaredField);
                }
            }
            tmpClass = tmpClass.getSuperclass();
        }
        nullOutFieldsByFieldAccess(object, fieldsToClean, checkedObjects, checkedObjectCollisionMap,
                depth, serializationType);
    }



    private static void nullOutFieldsByFieldAccess(Object object, List<Field> classFields,
            Map<Integer, Object> checkedObjects,
            Map<Integer, List<Object>> checkedObjectCollisionMap, int depth,
            SerializationType serializationType) throws IllegalAccessException, ClassNotFoundException, InstantiationException, InvocationTargetException, IntrospectionException
    {
        boolean accessModifierFlag = false;
        for (Field field : classFields)
        {
            accessModifierFlag = false;
            if (!field.isAccessible())
            {
                field.setAccessible(true);
                accessModifierFlag = true;
            }
            Object fieldValue = field.get(object);
            if (fieldValue instanceof HibernateProxy)
            {
                Object replacement = null;
                String assistClassName = fieldValue.getClass().getName();
                if (assistClassName.contains("javassist")
                        || assistClassName.contains("EnhancerByCGLIB"))
                {
                    Class assistClass = fieldValue.getClass();
                    try
                    {
                        Method m = assistClass.getMethod("writeReplace");
                        replacement = m.invoke(fieldValue);
                        String assistNameDelimiter = assistClassName.contains("javassist") ? "_$$_" : "$$";
                        assistClassName = assistClassName.substring(0, assistClassName.indexOf(assistNameDelimiter));
                        if (!replacement.getClass().getName().contains("hibernate"))
                        {
                            nullOutUninitializedFields(replacement, checkedObjects, checkedObjectCollisionMap, depth + 1, serializationType);
                            field.set(object, replacement);
                        }
                        else
                        {
                            replacement = null;
                        }
                    }
                    catch (Exception e)
                    {
                        log.error("Unable to write replace object " + fieldValue.getClass(), e);
                    }
                }
                if (replacement == null)
                {
                    String className = ((HibernateProxy) fieldValue).getHibernateLazyInitializer()
                            .getEntityName();
                    // see if there is a context classloader we should use
                    // instead of the current one.
                    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                    Class clazz = contextClassLoader == null ? Class.forName(className)
                            : Class.forName(className, true, contextClassLoader);
                    Class[] constArgs = { Integer.class };
                    Constructor construct = null;
                    try
                    {
                        construct = clazz.getConstructor(constArgs);
                        replacement = construct.newInstance((Integer) ((HibernateProxy) fieldValue)
                                .getHibernateLazyInitializer().getIdentifier());
                        field.set(object, replacement);
                    }
                    catch (NoSuchMethodException nsme)
                    {
                        Field idField = null;
                        try
                        {
                            idField = clazz.getDeclaredField("oid");
                        }
                        catch (Exception e)
                        {
                            //no code
                        }
                        if (idField==null)
                        {                            
                            try
                            {
                                idField = clazz.getDeclaredField("id");
                            }
                            catch (Exception e)
                            {
                                //no code
                            }
                        }
                        
                        try
                        {
                            Constructor ct = clazz.getDeclaredConstructor();
                            ct.setAccessible(true);
                            replacement = ct.newInstance();
                            if (!field.isAccessible())
                            {
                                idField.setAccessible(true);
                            }
                            idField.set(replacement, ((HibernateProxy) fieldValue)
                                    .getHibernateLazyInitializer().getIdentifier());
                        }
                        catch (Exception e)
                        {
                            log.error("No id constructor and unable to set field id for base bean " + className, e);
                            log.debug(e,e);                            
                        }
                        field.set(object, replacement);
                    }
                }
            }
            else
            {
                if (fieldValue instanceof PersistentCollection)
                {
                    // Replace hibernate specific collection types
                    if (!((PersistentCollection) fieldValue).wasInitialized())
                    {
                        field.set(object, null);
                    }
                    else
                    {
                        Object replacement = null;
                        boolean needToNullOutFields = true;
                        if (fieldValue instanceof Map)
                        {
                            replacement = new HashMap((Map) fieldValue);
                        }
                        else if (fieldValue instanceof List)
                        {
                            replacement = new ArrayList((List)((List) fieldValue).stream().distinct().collect(Collectors.toList()));
                        }
                        else if (fieldValue instanceof Set)
                        {
                            ArrayList l = new ArrayList((Set)((Set) fieldValue).stream().distinct().collect(Collectors.toList()));
                            nullOutUninitializedFields(l, checkedObjects, checkedObjectCollisionMap,
                                    depth + 1, serializationType);
                            replacement = new HashSet(l);
                            needToNullOutFields = false;
                        }
                        else if (fieldValue instanceof Collection)
                        {
                            replacement = new ArrayList((Collection)((Collection) fieldValue).stream().distinct().collect(Collectors.toList()));
                        }
                        field.set(object, replacement);
                        if (needToNullOutFields)
                        {
                            nullOutUninitializedFields(replacement, checkedObjects,
                                    checkedObjectCollisionMap, depth + 1, serializationType);
                        }
                    }
                }
                else
                {
                    if (fieldValue != null
                            && (fieldValue instanceof Dao || fieldValue instanceof Collection
                                    || fieldValue instanceof Object[] || fieldValue instanceof Map))
                        nullOutUninitializedFields((fieldValue), checkedObjects,
                                checkedObjectCollisionMap, depth + 1, serializationType);
                }
            }
            if (accessModifierFlag)
            {
                field.setAccessible(false);
            }
        }
    }



    private static Object replaceObject(Object object)
    {
        Object replacement = null;
        if (object instanceof HibernateProxy && object.getClass().getName().contains("javassist"))
        {
            Class assistClass = object.getClass();
            try
            {
                Method m = assistClass.getMethod("writeReplace");
                replacement = m.invoke(object);
            }
            catch (Exception e)
            {
                log.error("Unable to write replace object " + object.getClass(), e);
            }
        }
        return replacement;
    }



    private static void nullOutFieldsByAccessors(Object value, Map<Integer, Object> checkedObjects,
            Map<Integer, List<Object>> checkedObjectCollisionMap, int depth,
            SerializationType serializationType) throws IntrospectionException, IllegalAccessException, ClassNotFoundException, InstantiationException, InvocationTargetException
    {
        // Null out any collections that aren't loaded
        BeanInfo bi = Introspector.getBeanInfo(value.getClass(), Object.class);
        PropertyDescriptor[] pds = bi.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds)
        {
            Object propertyValue = null;
            try
            {
                propertyValue = pd.getReadMethod().invoke(value);
            }
            catch (Exception lie)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Couldn't load: " + pd.getName() + " off of " + value.getClass().getSimpleName(), lie);
                }
            }
            if (!Hibernate.isInitialized(propertyValue))
            {
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Nulling out: " + pd.getName() + " off of "
                                + value.getClass().getSimpleName());
                    }
                    Method writeMethod = pd.getWriteMethod();
                    if ((writeMethod != null)
                            && (writeMethod.getAnnotation(XmlTransient.class) == null))
                    {
                        pd.getWriteMethod().invoke(value, new Object[] { null });
                    }
                    else
                    {
                        nullOutField(value, pd.getName());
                    }
                }
                catch (Exception lie)
                {
                    log.debug(
                            "Couldn't null out: " + pd.getName() + " off of "
                                    + value.getClass().getSimpleName() + " trying field access",
                            lie);
                    nullOutField(value, pd.getName());
                }
            }
            else
            {
                if (propertyValue instanceof Collection || propertyValue instanceof Dao)
                {
                    nullOutUninitializedFields(propertyValue, checkedObjects,
                            checkedObjectCollisionMap, depth + 1, serializationType);
                }
            }
        }
    }



    private static void nullOutField(Object value, String fieldName)
    {
        try
        {
            Field f = value.getClass().getDeclaredField(fieldName);
            if (f != null)
            {
                // try to set the field this way
                f.setAccessible(true);
                f.set(value, null);
            }
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            // ignore this
        }
    }



    public static void nullOutUninitializedFields(EntityManager em, Object entity,
            SerializationType serialization) throws IllegalAccessException, ClassNotFoundException, InstantiationException, InvocationTargetException, IntrospectionException
    {
        em.detach(entity);
        nullOutUninitializedFields(entity, serialization);
    }
}