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
package fr.lixbox.jee.hibernate.id;

import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
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



    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry)
    {
        // check first for the strategy instance
        strategy = (UUIDGenerationStrategy) params.get(UUID_GEN_STRATEGY);
        if (strategy == null)
        {
            final String strategyClassName = params.getProperty(UUID_GEN_STRATEGY_CLASS);
            if (strategyClassName != null)
            {
                try
                {
                    final ClassLoaderService cls = serviceRegistry.getService(ClassLoaderService.class);
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
            throw new HibernateException("Unanticipated return type [" + type.getReturnedClass().getName() + "] for UUID conversion");
        }
    }



    public Serializable generate(SharedSessionContractImplementor session, Object object)
    {
        return valueTransformer.transform(strategy.generateUUID(session));
    }
}