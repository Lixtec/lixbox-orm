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
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.fasterxml.jackson.core.type.TypeReference;

import fr.lixbox.common.exceptions.BusinessException;
import fr.lixbox.common.guid.GuidGenerator;
import fr.lixbox.common.util.CollectionUtil;
import fr.lixbox.common.util.ExceptionUtil;
import fr.lixbox.common.util.StringUtil;
import fr.lixbox.io.json.JsonUtil;
import fr.lixbox.orm.entity.model.Dao;
import fr.lixbox.orm.entity.model.OptimisticDao;
import fr.lixbox.orm.redis.model.EQuery;
import fr.lixbox.orm.redis.model.RedisSearchDao;
import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.SearchResult;

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
    
    private static final String NO_ENTITY_FIND_WITH_EXPRESSION_MSG = "No entity find with expression ";
    private static final String KEY_FIELD = "key";
    private static final String TYPE_FIELD = "type";
    
    private transient GenericObjectPoolConfig<Connection> poolConfig;
    private String host="";
    private int port=0;
    private String redisUri="";


    
    //----------- Methodes -----------
    public ExtendRedisClient(String host, int port) 
    {
        getConfigForPool(20);
        this.host = host;
        this.port = port;
    }
    public ExtendRedisClient(GenericObjectPoolConfig<Connection> poolConfig, String host, int port)
    {
        this.poolConfig = poolConfig;
        this.host = host;
        this.port = port;
    }
    public ExtendRedisClient(GenericObjectPoolConfig<Connection> poolConfig, String redisUri)
    {
        this.poolConfig = poolConfig;
        this.redisUri = redisUri;
    }
    public ExtendRedisClient(String redisUri) 
    {
        getConfigForPool(20);
        this.redisUri = redisUri;
    }
    
    
    
    public void getConfigForPool(int size)
    {
        poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(size);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setMaxIdle(size);
        poolConfig.setMinIdle(1);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(10);
        poolConfig.setBlockWhenExhausted(false);
        poolConfig.setTestWhileIdle(true);
    }
    
    
    
    public List<String> getKeys(String pattern)
    {
        List<String> result = new ArrayList<>(); 
        try (JedisPooled redisClient = getJedisPooled())
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
            try (JedisPooled redisClient = getJedisPooled())
            {
                result = redisClient.get(key);
            }
        }
        return result;
    }



    public List<String> mget(String[] arrays)
    {
        List<String> result = new ArrayList<>();
        try (JedisPooled redisClient = getJedisPooled())
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
            try (JedisPooled redisClient = getJedisPooled())
            {
                if (redisClient.del(key)>0)
                {
                    result = true;
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
            try (JedisPooled redisClient = getJedisPooled())
            {
                if (redisClient.del(keys)>0)
                {
                    result = true;
                }
            }
        }
        return result;
    }
    
    
    
    public boolean clearDb()
    {
        boolean result = false;
        List<String> keys = getKeys("*");
        if (CollectionUtil.isNotEmpty(keys))
        {
            try (JedisPooled redisClient = getJedisPooled())
            {
                if (redisClient.del(keys.toArray(new String[0]))>0)
                {
                    result = true;
                }
            }
        }
        return result;
    }
    
    
    
    public boolean ping()
    {
        boolean result = false;
        try (JedisPooled redisClient = getJedisPooled())
        {
            redisClient.keys("*");
            result = true;
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
        try (JedisPooled redisClient = getJedisPooled())
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
            try (JedisPooled redisClient = getJedisPooled())
            {
                result = !StringUtil.isEmpty(redisClient.set(key,value));
            }
        }
        return result;
    }
    public boolean put(String key, String value, long ttl)
    {
        boolean result=put(key,value);
        try (JedisPooled redisClient = getJedisPooled())
        {
            redisClient.pexpire(key, ttl);
        }
        result &= true;
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
        try (JedisPooled redisClient = getJedisPooled())
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
            try (JedisPooled redisClient = getJedisPooled())
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
        try (JedisPooled redisClient = getJedisPooled())
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
        if (object==null) {
            return object;
        }
        
        //creer ou raffraichir le schema
        try (JedisPooled redisClient = getJedisPooled())
        {
            try 
            {
                LOG.debug(redisClient.ftInfo(object.getClass().getName()));
            }
            catch(JedisDataException jde)
            {
                IndexOptions options = IndexOptions.defaultOptions();
                if (object.getTTL()>0)
                {
                    options.setTemporary(object.getTTL()/1000);
                }
                IndexDefinition rule = new IndexDefinition().setPrefixes(object.getClass().getName()+":");
                options.setDefinition(rule);
                redisClient.ftCreate(object.getClass().getName(),IndexOptions.defaultOptions().setDefinition(rule), object.getIndexSchema());
            }      
                        
            if (object instanceof OptimisticDao)
            {
                ((OptimisticDao)object).setVersion(Calendar.getInstance());
            }
            if (StringUtil.isEmpty(object.getOid()))
            {
                object.setOid(GuidGenerator.getGUID(object));
            }
            String json = JsonUtil.transformObjectToJson(object, false);

            redisClient.set(object.getKey(), json);
            if (object.getTTL()>0)
            {
                redisClient.pexpire(object.getKey(), object.getTTL());
                redisClient.pexpire(object.getOid(), object.getTTL());
            }
            Map<String, String> indexField = new HashMap<>(object.getIndexFieldValues());
            indexField.put("oid", object.getOid());
            indexField.put(KEY_FIELD, object.getKey());
            indexField.put(TYPE_FIELD, object.getClass().getName());
            redisClient.hset(object.getClass().getName()+":"+object.getOid(), indexField);
        }
        catch(Exception e)
        {
            LOG.fatal(e,e);
        }
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
            merge(object);
        }
        return objects;
    }
    
    
    
    public <T extends RedisSearchDao> void remove(Class<T> entityClass, String id) throws BusinessException
    {
        try (JedisPooled redisClient = getJedisPooled())
        {
            T tmp = entityClass.getDeclaredConstructor().newInstance();
            tmp.setOid(id);
            redisClient.del(tmp.getKey());
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
        try (JedisPooled redisClient = getJedisPooled())
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
        return findByExpression(entityClass, new EQuery(expression));
    }



    public <T extends RedisSearchDao> List<T> findByExpression(Class<T> entityClass, EQuery query) 
        throws BusinessException
    {
        List<T> result = new ArrayList<>();
        
        try (JedisPooled redisClient = getJedisPooled())
        {
            query.limit(0, 500);
            SearchResult res = redisClient.ftSearch(entityClass.getName(), query);
            if (res.getTotalResults()>0)
            {
                List<String> keys = new ArrayList<>();
                for (Document doc : res.getDocuments()) 
                {
                    if (doc!=null && 
                        StringUtil.isNotEmpty((String) doc.get(KEY_FIELD)) && 
                        doc.get(TYPE_FIELD).equals(entityClass.getName()))
                    {
                        keys.add((String) doc.get(KEY_FIELD));
                    }
                }
                if (CollectionUtil.isNotEmpty(keys))
                {
                    result = getTypedFromKeys(keys);
                }
            }
        }
        if (CollectionUtil.isEmpty(result))
        {
            LOG.error(NO_ENTITY_FIND_WITH_EXPRESSION_MSG+query.toString());
            throw new BusinessException(NO_ENTITY_FIND_WITH_EXPRESSION_MSG+query.toString());
        }
        return result;
    }
    


    private JedisPooled getJedisPooled()
    {
        JedisPooled jedis = null;
        if (StringUtil.isNotEmpty(redisUri))
        {
            jedis = new JedisPooled(poolConfig, redisUri);
        }
        else
        {
            jedis = new JedisPooled(poolConfig, host, port);
        }
        return jedis;
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
}
