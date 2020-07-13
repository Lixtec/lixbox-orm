/*******************************************************************************
 *    
 *                           FRAMEWORK Lixbox
 *                          ==================
 *      
 *    This file is part of lixbox-orm.
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
package fr.lixbox.orm.hibernate.id;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.UUID;

import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.jboss.logging.Logger;

import fr.lixbox.common.util.StringUtil;

/**
 * Cette classe est le clone de la classe initiale mais ne remplace pas l'id s'il est saisi.
 * 
 * @author ludovic.terral
 */
public class UUIDGenerator implements IdentifierGenerator, Configurable
{
    // ----------- Attribut -----------
    public static final String UUID_GEN_STRATEGY = "uuid_gen_strategy";
    public static final String UUID_GEN_STRATEGY_CLASS = "uuid_gen_strategy_class";

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, UUIDGenerator.class.getName());

    private UUIDGenerationStrategy strategy;
    private UUIDTypeDescriptor.ValueTransformer valueTransformer;



    // ----------- Methode -----------
    public static UUIDGenerator buildSessionFactoryUniqueIdentifierGenerator()
    {
        final UUIDGenerator generator = new UUIDGenerator();
        generator.strategy = StandardRandomStrategy.INSTANCE;
        generator.valueTransformer = UUIDTypeDescriptor.ToStringTransformer.INSTANCE;
        return generator;
    }

        
    
    public void configure(Type type, Properties params, Dialect d)
    {
        // check first for the strategy instance
        strategy = (UUIDGenerationStrategy) params.get(UUID_GEN_STRATEGY);
        if (strategy == null)
        {
            // next check for the strategy class
            final String strategyClassName = params.getProperty(UUID_GEN_STRATEGY_CLASS);
            if (strategyClassName != null)
            {
                try
                {
                    this.getClass();
                    final Class<?> strategyClass = Class.forName(strategyClassName);
                    try
                    {
                        strategy = (UUIDGenerationStrategy) strategyClass.getConstructor().newInstance();
                    }
                    catch (Exception ignore)
                    {
                        LOG.unableToInstantiateUuidGenerationStrategy(ignore);
                    }
                }
                catch (ClassNotFoundException ignore)
                {
                    LOG.unableToLocateUuidGenerationStrategy(strategyClassName);
                }
            }
        }
        if (strategy == null)
        {
            strategy = StandardRandomStrategy.INSTANCE;
        }
        if (UUID.class.isAssignableFrom(type.getReturnedClass()))
        {
            valueTransformer = UUIDTypeDescriptor.PassThroughTransformer.INSTANCE;
        }
        else if (String.class.isAssignableFrom(type.getReturnedClass()))
        {
            valueTransformer = UUIDTypeDescriptor.ToStringTransformer.INSTANCE;
        }
        else if (byte[].class.isAssignableFrom(type.getReturnedClass()))
        {
            valueTransformer = UUIDTypeDescriptor.ToBytesTransformer.INSTANCE;
        }
        else
        {
            throw new HibernateException("Unanticipated return type [" + type.getReturnedClass().getName()
                    + "] for UUID conversion");
        }
    }
        


    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry)
    {
        // check first for the strategy instance
        strategy = (UUIDGenerationStrategy) params.get(UUID_GEN_STRATEGY);
        if (strategy == null)
        {
            // next check for the strategy class
            final String strategyClassName = params.getProperty(UUID_GEN_STRATEGY_CLASS);
            if (strategyClassName != null)
            {
                try
                {
                    final ClassLoaderService cls = serviceRegistry
                            .getService(ClassLoaderService.class);
                    final Class<?> strategyClass = cls.classForName(strategyClassName);
                    try
                    {
                        strategy = (UUIDGenerationStrategy) strategyClass.getConstructor().newInstance();
                    }
                    catch (Exception ignore)
                    {
                        LOG.unableToInstantiateUuidGenerationStrategy(ignore);
                    }
                }
                catch (ClassLoadingException ignore)
                {
                    LOG.unableToLocateUuidGenerationStrategy(strategyClassName);
                }
            }
        }
        if (strategy == null)
        {
            // lastly use the standard random generator
            strategy = StandardRandomStrategy.INSTANCE;
        }
        if (UUID.class.isAssignableFrom(type.getReturnedClass()))
        {
            valueTransformer = UUIDTypeDescriptor.PassThroughTransformer.INSTANCE;
        }
        else if (String.class.isAssignableFrom(type.getReturnedClass()))
        {
            valueTransformer = UUIDTypeDescriptor.ToStringTransformer.INSTANCE;
        }
        else if (byte[].class.isAssignableFrom(type.getReturnedClass()))
        {
            valueTransformer = UUIDTypeDescriptor.ToBytesTransformer.INSTANCE;
        }
        else
        {
            throw new HibernateException("Unanticipated return type ["
                    + type.getReturnedClass().getName() + "] for UUID conversion");
        }
    }

    

    public Serializable generate(SessionImplementor session, Object object)
    {
        Serializable result = extractExistentOid(object);
        if (result == null || (result instanceof String && StringUtil.isEmpty((String) result)))
        {
            result = valueTransformer.transform(strategy.generateUUID(session));
        }        
        return result;
    }



    public Serializable generate(SharedSessionContractImplementor session, Object object)
    {
        Serializable result = extractExistentOid(object);
        if (result == null || (result instanceof String && StringUtil.isEmpty((String) result)))
        {
            result = valueTransformer.transform(strategy.generateUUID(session));
        }        
        return result;
    }
    
    
    private Serializable extractExistentOid(Object object)
    {
        Serializable result = null;
        for (Field field: object.getClass().getFields())
        {
            Id id = field.getAnnotation(Id.class);
            if (id!=null)
            {
                try
                {
                    result = (Serializable) field.get("");
                }
                catch (Exception e)
                {
                    LOG.debug(e);
                }
            }            
        }        
        
        for (Method field: object.getClass().getMethods())
        {
            Id id = field.getAnnotation(Id.class);
            if (id!=null)
            {
                try
                {
                    result = (Serializable) field.invoke(object);
                }
                catch (Exception e)
                {
                    LOG.debug(e);
                }
            }            
        }
        return result;
    }
}
