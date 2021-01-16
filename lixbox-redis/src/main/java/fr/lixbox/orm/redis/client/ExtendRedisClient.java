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
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Cette classe interface l'univers redis avec l'univers POJO.
 * 
 * @author ludovic.terral
 */
public class ExtendRedisClient implements Serializable, AutoCloseable
{    
    private static final long serialVersionUID = -3968936170594429132L;
    private static final Log LOG = LogFactory.getLog(ExtendRedisClient.class);
    
    private transient JedisPool pool;
    private transient Jedis redisClient;
    private transient Map<String, Client> searchClients;
    private String host;
    private int port=0;

    
    /**
     * Ce constructeur sert à l'initialisation de l'acces a la base.
     */
    public ExtendRedisClient(String host, int port) 
    {        
        this.host = host;
        this.port = port;
        this.searchClients = new HashMap<>();
    }
    public ExtendRedisClient(JedisPool pool) 
    {
        this.pool = pool;
        this.redisClient = pool.getResource();
        this.searchClients = new HashMap<>();
    }
    
    
    
    public Jedis getRedisClient()
    {
        if (needToOpen())
        {
            open();
        }
        return redisClient;
    }
    
    
    
    private boolean needToOpen()
    {
        boolean result = false;
        
        try
        {
            if (redisClient==null || !"pong".equalsIgnoreCase(redisClient.ping()))
            {
                result = true;
            }
        }
        catch (Exception e)
        {
            result = true;
        }
        return result;
    }
    
    
    public boolean open()
    {
        //ouverture du client redis
        boolean isOpen = false;
        try
        {
            if (redisClient==null|| ("pong".equalsIgnoreCase(redisClient.ping())))
            {
                if (pool!=null)
                {
                    redisClient = pool.getResource();
                }
                else
                {
                    redisClient = new Jedis(host, port);
                }
            }
            isOpen = true;
        }
        catch (Exception e)
        {
            redisClient = null;
            isOpen = open();
        }
        return isOpen;
    }
    
    
    
    @Override
    public void close()
    {
        redisClient.close();
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
    }
    
    
    
    public <T extends RedisSearchDao> boolean createSchema(T objet)
    {
        return getSearchClient(objet)!=null;
    }
    
    
    
    public List<String> getKeys(String pattern)
    {
        String internamPattern = StringUtil.isEmpty(pattern)?"*":pattern;
        List<String> result  = new ArrayList<>(getRedisClient().keys(internamPattern));
        close();
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
            switch (getRedisClient().type(key))
            {
                case "string":
                    result = getRedisClient().get(key);
                    break;
                default:
                    LOG.error("UNSUPPORTED FORMAT "+getRedisClient().type(key));
            }
            close();
        }
        return result;
    }



    public List<String> mget(String[] arrays)
    {
        List<String> result = new ArrayList<>();
        if (arrays!=null && arrays.length>0)
        {
            result.addAll(getRedisClient().mget(arrays));
        }
        close();
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
            switch (getRedisClient().type(key))
            {
                case "string":
                    if (getRedisClient().del(key)>0)
                    {
                        result = true;
                    } 
                    break;
                default:
                    LOG.error("UNSUPPORTED FORMAT "+getRedisClient().type(key));
            }
            close();
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
            if (getRedisClient().del(keys)>0)
            {
                result = true;
            } 
            close();
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
        List<String> result  = new ArrayList<>(getRedisClient().keys(internamPattern));
        close();
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
            result = !StringUtil.isEmpty(getRedisClient().set(key,value));
            close();  
        }
        return result;
    }
    public boolean put(String key, String value, long ttl)
    {
        boolean result=put(key,value);
        getRedisClient().pexpire(key, ttl);
        close();
        result &= true;
        return result;
    }

    
    /**
     * Cette methode efface l'ensemble des données du cache.
     * 
     * @return true si le nettoyage est ok.
     */
    public boolean clear()
    {
        boolean result;
        result = getRedisClient().flushAll().contains("OK");
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
            close();
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
        result = getRedisClient().mset(tmp.toArray(new String[0])).contains("OK");
        close();   
        return result;
    }
    public boolean put(Map<String,String> entries, long ttl)
    {
        boolean result = put(entries);
        for (String key : entries.keySet())
        {
            getRedisClient().pexpire(key, ttl);
        }  
        close();
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
        List<String> values = getRedisClient().mget(keys);
        for (int ix=0; ix<keys.length; ix++)
        {
            result.put(keys[ix], values.get(ix));
        }
        close();
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
        close();
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
        close();
        return objects;
    }
    
    
    
    public <T extends RedisSearchDao> void remove(Class<T> entityClass, String id) throws BusinessException
    {
        try
        {
            T tmp = entityClass.getDeclaredConstructor().newInstance();
            tmp.setOid(id);
            getRedisClient().del(tmp.getKey());
            getSearchClientByClass(entityClass).deleteDocument(id);
        }
        catch(Exception e) 
        {
            ExceptionUtil.traiterException(e, "Impossible de supprimer l'objet", false);
        }
        close();
    }  
    
    
    
    public <T extends RedisSearchDao> T findById(Class<T> entityClass, String id) 
        throws BusinessException
    {
        T result = null;
        try 
        {
            T tmp = entityClass.getDeclaredConstructor().newInstance();
            tmp.setOid(id);
            String json = getRedisClient().get(tmp.getKey());
            result = JsonUtil.transformJsonToObject(json, getTypeReferenceFromClass(entityClass));
        }
        catch (Exception e)
        {
            ExceptionUtil.traiterException(e, "Impossible de charger la classe", true);
        }
        close();
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
        close();
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
        close();
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
        if (!searchClients.containsKey(entityClass.getSimpleName()))
        {
            if (pool!=null)
            {
                searchClients.put(entityClass.getSimpleName() , new io.redisearch.client.Client(entityClass.getSimpleName(), pool));
            }
            else
            {
                searchClients.put(entityClass.getSimpleName() , new io.redisearch.client.Client(entityClass.getSimpleName(), host, port));
            }
            
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
                    searchClients.get(entityClass.getSimpleName()).getInfo();
                }
                catch (JedisDataException e)
                {
                    LOG.debug(e);
                    searchClients.get(entityClass.getSimpleName()).createIndex(instance.getIndexSchema(), defaultOptions);
                }
            }
            catch (Exception e)
            {
                //existe peut être
            }
        }
        return searchClients.get(entityClass.getSimpleName());
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
        getRedisClient().set(object.getKey(), json);
        if (object.getTTL()>0)
        {
            getRedisClient().pexpire(object.getKey(), object.getTTL());
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
