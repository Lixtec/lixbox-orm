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
package fr.lixbox.orm.redis.client;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import fr.lixbox.common.exceptions.BusinessException;
import fr.lixbox.common.guid.GuidGenerator;
import fr.lixbox.common.util.CollectionUtil;
import fr.lixbox.common.util.ExceptionUtil;
import fr.lixbox.common.util.StringUtil;
import fr.lixbox.io.json.JsonUtil;
import fr.lixbox.orm.entity.model.Dao;
import fr.lixbox.orm.entity.model.OptimisticDao;
import fr.lixbox.orm.redis.model.RedisSearchDao;
import io.redisearch.Client;
import io.redisearch.Document;
import io.redisearch.Query;
import io.redisearch.SearchResult;
import io.redisearch.client.Client.IndexOptions;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Cette classe interface l'univers redis avec l'univers POJO.
 * 
 * @author ludovic.terral
 */
public class ExtendRedisClient implements Serializable
{    
    // ----------- Attibuts -----------
    private static final long serialVersionUID = -3968936170594429132L;
    private static final Log LOG = LogFactory.getLog(ExtendRedisClient.class);
    
    private transient JedisPool pool;
    private transient Map<String, Client> searchClients;



    //----------- Methodes -----------
    public ExtendRedisClient(String host, int port) 
    {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(1);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(10);
        poolConfig.setTimeBetweenEvictionRunsMillis(60000);
        poolConfig.setBlockWhenExhausted(false);
        poolConfig.setTestWhileIdle(true);
        this.pool = new JedisPool(poolConfig, host, port);
        this.searchClients = new HashMap<>();
    }
    public ExtendRedisClient(JedisPool pool) 
    {
        this.pool = pool;
        this.searchClients = new HashMap<>();
    }
    
    
    
    public Jedis getPoolResource()
    {
        return pool.getResource();
    }
    
    
    
    public boolean isOpen()
    {
        boolean result = false;
        
        try
        {
            if (pool!=null)
            {
                result &= !pool.isClosed();
            }
        }
        catch (Exception e)
        {
            result = true;
        }
        return result;
    }
    
    
    
    public void dispose()
    {
        if (searchClients.size()>0)
        {
            for (Client searchClient : searchClients.values())
            {
                try 
                {
                    searchClient.close();
                }
                catch (Exception e)
                {
                    //pas actif
                }
            }
            searchClients.clear();
        }
        if (pool!=null)
        {
            pool.close();
        }
    }
    
    
    
    public <T extends RedisSearchDao> boolean createSchema(T objet)
    {
        return getSearchClient(objet)!=null;
    }
    
    
    
    public List<String> getKeys(String pattern)
    {
        List<String> result = new ArrayList<>(); 
        try (Jedis redisClient = pool.getResource())
        {
            String internamPattern = StringUtil.isEmpty(pattern)?"*":pattern;
            result.addAll(redisClient.keys(internamPattern));
        }
        return result;
    }
    
    
    
    /**
     * Cette methode renvoie la valeur associée à une clé
     * @param key
     * 
     * @return null si pas de valeur.
     */
    public String get(String key)
    {
        String result = "";
        if (key!=null)
        {
            try (Jedis redisClient = pool.getResource())
            {
                switch (redisClient.type(key))
                {
                    case "string":
                        result = redisClient.get(key);
                        break;
                    default:
                        LOG.error("UNSUPPORTED FORMAT "+redisClient.type(key));
                }
            }
        }
        return result;
    }



    public List<String> mget(String[] arrays)
    {
        List<String> result = new ArrayList<>();
        try (Jedis redisClient = pool.getResource())
        {
            if (arrays!=null && arrays.length>0)
            {
                result.addAll(redisClient.mget(arrays));
            }
        }
        return result;
    }

    

    /**
     * Cette methode supprime une clé et sa valeur dans le cache.
     * @param key
     * 
     * @return true si la suppression est effective.
     */
    public boolean remove(String key)
    {
        boolean result = false;
        if (key!=null)
        {
            try (Jedis redisClient = pool.getResource())
            {
                switch (redisClient.type(key))
                {
                    case "string":
                        if (redisClient.del(key)>0)
                        {
                            result = true;
                        } 
                        break;
                    default:
                        LOG.error("UNSUPPORTED FORMAT "+redisClient.type(key));
                }
            }
        }
        return result;
    }

    

    /**
     * Cette methode supprime les clés et leurs valeurs dans le cache.
     * @param keys
     * 
     * @return true si la suppression est effective.
     */
    public boolean remove(String... keys)
    {
        boolean result = false;
        if (keys!=null)
        {
            try (Jedis redisClient = pool.getResource())
            {
                if (redisClient.del(keys)>0)
                {
                    result = true;
                }
            }
        }
        return result;
    }
      
    
    
    /**
     * Cette methode renvoie le nombre de clés qui correspondent à une pattern.
     * Si la pattern n'est pas renseigné le wildcar est utilisé.
     * @param pattern
     * 
     * return le nombre de clés.
     */
    public int size(String pattern)
    {
        String internamPattern = StringUtil.isEmpty(pattern)?"*":pattern;
        List<String> result  = new ArrayList<>();
        try (Jedis redisClient = pool.getResource())
        {
            result.addAll(redisClient.keys(internamPattern));
        }
        return result.size();
    }
    
    

    /**
     * Cette methode verifie la présence d'une clé.     
     * @param pattern
     * 
     * return true si la clé est présente.
     */
    public boolean containsKey(String pattern)
    {  
        boolean result;
        List<String> tmp  = getKeys(pattern);
        result = !tmp.isEmpty();
        return result;
    }
        
    
    
    /**
     * Cette methode insère une clé et sa valeur dans le cache.
     * @param key
     * @param value
     * 
     * @return true si l'enregistrement est effectif.
     */
    public boolean put(String key, String value)
    {
        boolean result=false;
        if (!StringUtil.isEmpty(key))
        {
            try (Jedis redisClient = pool.getResource())
            {
                result = !StringUtil.isEmpty(redisClient.set(key,value));
            }
        }
        return result;
    }
    public boolean put(String key, String value, long ttl)
    {
        boolean result=put(key,value);
        try (Jedis redisClient = pool.getResource())
        {
            redisClient.pexpire(key, ttl);
        }
        result &= true;
        return result;
    }

    
    /**
     * Cette methode efface l'ensemble des données du cache.
     * 
     * @return true si le nettoyage est ok.
     */
    public boolean clearDb()
    {
        boolean result;
        try (Jedis redisClient = pool.getResource())
        {
            result = redisClient.flushAll().contains("OK");
        }
        if (searchClients.size()>0)
        {
            for (Client searchClient : searchClients.values())
            {
                try 
                {
                    result &= searchClient.dropIndex();
                    searchClient.close();
                }
                catch (Exception e)
                {
                    //pas actif
                }
            }
            searchClients.clear();
        }
        return result;
    }
    

    
    
    /**
     * Cette methode enregistre les associations clé valeur dans le cache.
     * 
     * @param entries
     * 
     * @return true si l'écriture est ok
     */
    public boolean put(Map<String,String> entries)
    {
        boolean result;
        List<String> tmp = new ArrayList<>();        
        for (Entry<String, String> entry : entries.entrySet())
        {
            tmp.add(entry.getKey());
            tmp.add(entry.getValue());
        }                
        try (Jedis redisClient = pool.getResource())
        {
            result = redisClient.mset(tmp.toArray(new String[0])).contains("OK");
        }
        return result;
    }
    public boolean put(Map<String,String> entries, long ttl)
    {
        boolean result = put(entries);
        for (String key : entries.keySet())
        {
            try (Jedis redisClient = pool.getResource())
            {
                redisClient.pexpire(key, ttl);
            }
        }  
        return result;
    }
    
    
    /**
     * Cette methode récupère les valeurs associées à la liste des clés
     * fournie en paramètres
     * 
     * @param keys
     * 
     * @return la liste des valeurs
     */
    public Map<String, String> get(String... keys)
    {
        Map<String,String> result = new HashMap<>();
        try (Jedis redisClient = pool.getResource())
        {
            List<String> values = redisClient.mget(keys);
            for (int ix=0; ix<keys.length; ix++)
            {
                result.put(keys[ix], values.get(ix));
            }
        }
        return result;
    }
    
    
    
    public Object getTypedFromKey(String key)
    {
        Object result = null;        
        if (!StringUtil.isEmpty(key))
        {            
            String value = get(key);
            result = JsonUtil.transformJsonToObject(value, getTypeReferenceFromKey(key));
        } 
        return result;
    }
    
    

    @SuppressWarnings("unchecked")
    public <T extends Dao> List<T> getTypedFromKeys(List<String> keys)
    {
        List<T> result = new ArrayList<>();
        if (keys!=null && !keys.isEmpty())
        {            
            Map<String, String> convertMap = get(keys.toArray(new String[0]));
            for (Entry<String, String> entry : convertMap.entrySet())
            {
                if (StringUtil.isNotEmpty(entry.getValue()))
                {
                    result.add((T) JsonUtil.transformJsonToObject(entry.getValue(), getTypeReferenceFromKey(entry.getKey())));
                }
            }
        } 
        return result;
    }
    
    
    
    public <T extends RedisSearchDao> T merge(T object)
    {
        mergeNoManaged(object);
        return object;
    }
    
    
    
    public <T extends RedisSearchDao> List<T> merge(List<T> objects)
    {
        if (CollectionUtil.isEmpty(objects))
        {
            return objects;
        }
        for (T object : objects)
        {
            mergeNoManaged(object);
        }
        return objects;
    }
    
    
    
    public <T extends RedisSearchDao> void remove(Class<T> entityClass, String id) throws BusinessException
    {
        try (Jedis redisClient = pool.getResource())
        {
            T tmp = entityClass.getDeclaredConstructor().newInstance();
            tmp.setOid(id);
            redisClient.del(tmp.getKey());
            getSearchClientByClass(entityClass).deleteDocument(id);
        }
        catch(Exception e) 
        {
            ExceptionUtil.traiterException(e, "Impossible de supprimer l'objet", false);
        }
    }  
    
    
    
    public <T extends RedisSearchDao> T findById(Class<T> entityClass, String id) 
        throws BusinessException
    {
        T result = null;
        try (Jedis redisClient = pool.getResource())
        {
            T tmp = entityClass.getDeclaredConstructor().newInstance();
            tmp.setOid(id);
            String json = redisClient.get(tmp.getKey());
            result = JsonUtil.transformJsonToObject(json, getTypeReferenceFromClass(entityClass));
        }
        catch (Exception e)
        {
            ExceptionUtil.traiterException(e, "Impossible de charger la classe", true);
        }
        if (result == null)
        {
            LOG.error("No entity find with id "+id);
            throw new BusinessException("No entity find with id "+id);
        }
        return result;
    }



    public <T extends RedisSearchDao> List<T> findByExpression(Class<T> entityClass, String expression) 
        throws BusinessException
    {
        List<T> result = new ArrayList<>();
        
        Client searchClient = getSearchClientByClass(entityClass);
        Query q = new Query(expression);
        q.limit(0, 500);
        SearchResult res = searchClient.search(q);
        
        if (res.totalResults>0)
        {
            List<String> keys = new ArrayList<>();
            for (Document doc : res.docs) 
            {
                keys.add((String) doc.get("key"));
            }
            result = getTypedFromKeys(keys);
        }
        if (CollectionUtil.isEmpty(result))
        {
            LOG.error("No entity find with expression "+expression);
            throw new BusinessException("No entity find with expression "+expression);
        }
        return result;
    }



    public <T extends RedisSearchDao> List<T> findByExpression(Class<T> entityClass, Query query) 
        throws BusinessException
    {
        List<T> result = new ArrayList<>();
        Client searchClient = getSearchClientByClass(entityClass);
        SearchResult res = searchClient.search(query);
        
        if (res.totalResults>0)
        {
            List<String> keys = new ArrayList<>();
            for (Document doc : res.docs) 
            {
                keys.add((String) doc.get("key"));
            }
            result = getTypedFromKeys(keys);
        }
        if (CollectionUtil.isEmpty(result))
        {
            LOG.error("No entity find with expression "+query.toString());
            throw new BusinessException("No entity find with expression "+query.toString());
        }
        return result;
    }
    
    
    
    public <T extends RedisSearchDao> Client getSearchClient(T object)
    {
        Client result = null;
        if (object!=null)
        {
            result = getSearchClientByClass(object.getClass());
        }
        return result;
    }
    

    
    public <T extends RedisSearchDao> Client getSearchClientByClass(Class<T> entityClass)
    {
      //ouverture du client redis
        if (!searchClients.containsKey(entityClass.getCanonicalName()))
        {
            searchClients.put(entityClass.getCanonicalName() , new io.redisearch.client.Client(entityClass.getCanonicalName(), pool));
            
            try
            {
                T instance = entityClass.getDeclaredConstructor().newInstance();
                IndexOptions defaultOptions = IndexOptions.defaultOptions();
                if (instance.getTTL()>0)
                {
                    defaultOptions.setTemporary(instance.getTTL()/1000);
                }
                try
                {
                    searchClients.get(entityClass.getCanonicalName()).getInfo();
                }
                catch (JedisDataException e)
                {
                    LOG.debug(e);
                    searchClients.get(entityClass.getCanonicalName()).createIndex(instance.getIndexSchema(), defaultOptions);
                }
            }
            catch (Exception e)
            {
                //existe peut être
            }
        }
        return searchClients.get(entityClass.getCanonicalName());
    }



    private <T extends RedisSearchDao> TypeReference<T> getTypeReferenceFromClass(Class<T> classz)
    {
        return new TypeReference<T>(){
            @Override
            public Type getType() {
                return classz;
            }
        };
    }



    private <T> TypeReference<?> getTypeReferenceFromKey(String key)
    {
        String cleanKey = key.substring(key.indexOf(':', key.indexOf(':')+1)+1,key.indexOf(':', key.indexOf(':', key.indexOf(':')+1)+1));
        return new TypeReference<T>(){
            @Override
            public Type getType() {
                Type type = null;
                try
                {
                    type = Class.forName(cleanKey);
                }
                catch (ClassNotFoundException e)
                {
                    LOG.debug(e);
                }
                if (type==null)
                {
                    try
                    {
                        type = Thread.currentThread().getContextClassLoader().loadClass(cleanKey);
                    }
                    catch (ClassNotFoundException e)
                    {
                        LOG.fatal(e);
                    }
                }
                return type;
            }
        };
    }
    
    
    
    private <T extends RedisSearchDao> T mergeNoManaged(T object)
    {
        if (object==null) {
            return object;
        }
        Client searchClient = getSearchClient(object);
        if (object instanceof OptimisticDao)
        {
            ((OptimisticDao)object).setVersion(Calendar.getInstance());
        }
        if (StringUtil.isEmpty(object.getOid()))
        {
            object.setOid(GuidGenerator.getGUID(object));
        }
         
        String json = JsonUtil.transformObjectToJson(object, false);
        try (Jedis redisClient = pool.getResource())
        {
            redisClient.set(object.getKey(), json);
            if (object.getTTL()>0)
            {
                redisClient.pexpire(object.getKey(), object.getTTL());
                redisClient.pexpire(object.getOid(), object.getTTL());
            }
        }
        Map<String, Object> indexField = new HashMap<>(object.getIndexFieldValues());
        indexField.put("oid", object.getOid());
        indexField.put("key", object.getKey());
        
        if (StringUtil.isEmpty(object.getOid()))
        {
            searchClient.addDocument(object.getOid(), indexField);
        }
        else
        {
            searchClient.replaceDocument(object.getOid(), 1, indexField);
        }
        return object;
    }
}
