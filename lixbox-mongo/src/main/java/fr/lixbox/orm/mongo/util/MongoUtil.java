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
 */
package fr.lixbox.orm.mongo.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import fr.lixbox.common.exceptions.BusinessException;
import fr.lixbox.common.guid.GuidGenerator;
import fr.lixbox.common.util.CollectionUtil;
import fr.lixbox.common.util.ExceptionUtil;
import fr.lixbox.common.util.StringUtil;
import fr.lixbox.io.json.JsonUtil;
import fr.lixbox.orm.entity.model.Dao;
import fr.lixbox.orm.entity.model.OptimisticDao;

/**
 * Cette classe interface l'univers redis avec l'univers POJO.
 * 
 * @author ludovic.terral
 */
public class MongoUtil implements Serializable
{    
    private static final long serialVersionUID = -3968936170594429132L;
    private static final Log LOG = LogFactory.getLog(MongoUtil.class);
    
    private transient MongoClient mongoClient;
    private String uri;
    private String dbName;
    private transient MongoDatabase db;
    
    
    /**
     * Ce constructeur sert à l'initialisation de l'acces a la base.
     * L'uri est de la forme "mongodb://user1:pwd1@host1/?authSource=db1&ssl=true"
     * 
     * @param uri
     */
    public MongoUtil(String uri, String dbName) 
    {        
        this.uri = uri;
        this.dbName = dbName;
    }
    
    
    
    public MongoDatabase createConnection()
    {
        if (db==null)
        {
            mongoClient = MongoClients.create(new ConnectionString(uri));
            db = mongoClient.getDatabase(dbName);
        }
        return db;
    }
    
    
    
    public void closeConnection()
    {
        if (mongoClient!=null)
        {
            mongoClient.close();
        }
    }
    
    
    
    public void createCollection(String name)
    {
        MongoDatabase database = createConnection();
        database.createCollection(name);   
    }
    
    
    
    public void removeCollection(String name)
    {
        MongoDatabase database = createConnection();
        MongoCollection<Document> collection = database.getCollection(name);
        if (collection!=null)
        {
            collection.drop();
        }
    }
    
    
    
    public <T extends Dao> T merge(T object)
    {
        MongoDatabase database = createConnection();
        MongoCollection<Document> collection = database.getCollection(object.getClass().getSimpleName());
        if (object instanceof OptimisticDao)
        {
            ((OptimisticDao)object).setVersion(Calendar.getInstance());
        }
        if (StringUtil.isEmpty(object.getOid()))
        {
            object.setOid(GuidGenerator.getGUID(object));
            String json = JsonUtil.transformObjectToJson(object, false);
            Document doc = Document.parse(json);
            doc.put("_id", object.getOid());
            collection.insertOne(doc);
        }
        else
        {
            String json = JsonUtil.transformObjectToJson(object, false);
            Document doc = Document.parse(json);
            doc.put("_id", object.getOid());
            collection.findOneAndReplace(Filters.eq("oid", object.getOid()), doc);
        }
        return object;
    }    
    
    
    
    public <T extends Dao> List<T> mergeMany(List<T> objects)
    {
        MongoDatabase database = createConnection();
        if (CollectionUtil.isEmpty(objects))
        {
            return objects;
        }
        MongoCollection<Document> collection = database.getCollection(objects.get(0).getClass().getSimpleName());
        
        List<Document> insertable = new ArrayList<>();
        List<Document> updatable = new ArrayList<>();
        for (T object : objects)
        {
            if (object instanceof OptimisticDao)
            {
                ((OptimisticDao)object).setVersion(Calendar.getInstance());
            }
            if (StringUtil.isEmpty(object.getOid()))
            {
                object.setOid(GuidGenerator.getGUID(object));
                String json = JsonUtil.transformObjectToJson(object, false);
                Document doc = Document.parse(json);
                doc.put("_id", object.getOid());
                insertable.add(doc);
            }
            else
            {
                String json = JsonUtil.transformObjectToJson(object, false);
                Document doc = Document.parse(json);
                doc.put("_id", object.getOid());
                updatable.add(doc);
            }
        }
        if (CollectionUtil.isNotEmpty(insertable))
        {
            collection.insertMany(insertable);
        }
        
        for (Document doc : updatable)
        {
            collection.findOneAndReplace(Filters.eq("oid", doc.get("oid")), doc);
        }
        return objects;
    }    
    
    
    
    public <T extends Dao> void remove(Class<T> entityClass, String id)
    {
        MongoDatabase database = createConnection();        
        MongoCollection<Document> collection = database.getCollection(entityClass.getSimpleName());
        Document doc = new Document();
        doc.put("oid", id);
        collection.deleteOne(doc);
    }  



    public <T extends Dao> void remove(Class<T> entityClass, Bson expression)
    {
        MongoDatabase database = createConnection();        
        MongoCollection<Document> collection = database.getCollection(entityClass.getSimpleName());
        collection.deleteMany(expression);        
    }
    
    
    
    public <T extends Dao> List<T> find(Class<T> entityClass) 
        throws BusinessException
    {
        List<T> result = new ArrayList<>();
        MongoDatabase database = createConnection();        
        MongoCollection<Document> collection = database.getCollection(entityClass.getSimpleName());
        FindIterable<Document> tmp = collection.find();
        for (Document bsonResult : tmp)
        {
            JsonWriterSettings settings = JsonWriterSettings.builder()
                    .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
                    .build();
            String json = bsonResult.toJson(settings);
            try
            {
                ObjectMapper mapper = new ObjectMapper();
                result.add(mapper.readValue(json, entityClass));
            }
            catch (IOException e)
            {
                ExceptionUtil.traiterException(e, "", true);
            }
        }
        if (CollectionUtil.isEmpty(result))
        {
            LOG.error("No entity find on "+entityClass.getSimpleName());
            throw new BusinessException("No entity find on "+entityClass.getSimpleName());
        }
        return result;
    }
    
    
    
    public <T extends Dao> T findById(Class<T> entityClass, String id) 
        throws BusinessException
    {
        T result = null;
        MongoDatabase database = createConnection();        
        MongoCollection<Document> collection = database.getCollection(entityClass.getSimpleName());
        Document doc = new Document();
        doc.put("oid", id);
        Document bsonResult = collection.find(doc).first();        
        if (bsonResult!=null)
        {
            JsonWriterSettings settings = JsonWriterSettings.builder()
                    .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
                    .build();
            String json = bsonResult.toJson(settings);
            try
            {
                ObjectMapper mapper = new ObjectMapper();
                result = mapper.readValue(json, entityClass);
            }
            catch (IOException e)
            {
                ExceptionUtil.traiterException(e, "", true);
            }
        }
        if (result == null)
        {
            LOG.error("No entity find with id "+id);
            throw new BusinessException("No entity find with id "+id);
        }
        return result;
    }



    public <T extends Dao> List<T> findByExpression(Class<T> entityClass, Bson expression) 
        throws BusinessException
    {
        List<T> result = new ArrayList<>();
        MongoDatabase database = createConnection();        
        MongoCollection<Document> collection = database.getCollection(entityClass.getSimpleName());
        FindIterable<Document> tmp = collection.find(expression);
        for (Document bsonResult : tmp)
        {
            JsonWriterSettings settings = JsonWriterSettings.builder()
                    .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
                    .build();
            String json = bsonResult.toJson(settings);
            try
            {
                ObjectMapper mapper = new ObjectMapper();
                result.add(mapper.readValue(json, entityClass));
            }
            catch (IOException e)
            {
                ExceptionUtil.traiterException(e, "", true);
            }
        }
        if (CollectionUtil.isEmpty(result))
        {
            LOG.error("No entity find with expression "+expression);
            throw new BusinessException("No entity find with expression "+expression);
        }
        return result;
    }
}
