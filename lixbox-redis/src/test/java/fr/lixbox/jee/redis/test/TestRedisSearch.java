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
 ********************************************************************************/
package fr.lixbox.jee.redis.test;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.lixbox.common.util.DateUtil;
import fr.lixbox.jee.redis.model.JNO;
import io.redisearch.Client;
import io.redisearch.Query;
import io.redisearch.SearchResult;
import io.redisearch.client.Client.IndexOptions;

/**
 * Cette interface est le contrat de base pour pouvoir utiliser
 * la recherche avancee de Redis
 * 
 * @author ludovic.terral
 */
public class TestRedisSearch
{
    private static Client redisSearchClient;
    
    
    
    @Before
    public void prepare()
    {
        redisSearchClient = new io.redisearch.client.Client(JNO.class.getSimpleName(), "localhost", 6380);
        JNO jno = new JNO();
        Assert.assertTrue("Impossible de créer le schéma dans redissearch", redisSearchClient.createIndex(jno.getIndexSchema(), IndexOptions.defaultOptions()));
    }

    
    
    @After
    public void finish() throws IOException
    {
        redisSearchClient.dropIndex();
        redisSearchClient.close();
    }
    
    
    
    @Test
    public void test_insertIndex() 
    {
        JNO anniversaire = new JNO();
        anniversaire.setOid("198209221018");
        anniversaire.setDateEvent(DateUtil.parseCalendar("22/09/1982 10:18", "dd/MM/yyyy HH:mm"));
        anniversaire.setLibelle("anniversaire Ludo");
        Assert.assertTrue("Impossible d'insérer l'index", redisSearchClient.addDocument(anniversaire.getOid(), anniversaire.getIndexFieldValues()));
    }
    
    
    @Test
    public void test_insertMultiple() 
    {
        JNO anniversaire = new JNO();
        anniversaire.setOid("198209221018");
        anniversaire.setDateEvent(DateUtil.parseCalendar("22/09/1982 10:18", "dd/MM/yyyy HH:mm"));
        anniversaire.setLibelle("anniversaire Ludo");
        Assert.assertTrue("Impossible d'insérer l'index", redisSearchClient.addDocument(anniversaire.getOid(), anniversaire.getIndexFieldValues()));
        
        anniversaire = new JNO();
        anniversaire.setOid("230219821820");
        anniversaire.setDateEvent(DateUtil.parseCalendar("23/02/1982 18:18", "dd/MM/yyyy HH:mm"));
        anniversaire.setLibelle("anniversaire Steph");
        Assert.assertTrue("Impossible d'insérer l'index", redisSearchClient.addDocument(anniversaire.getOid(), anniversaire.getIndexFieldValues()));
    }
    
    
    
    @Test
    public void test_searchFromIndex() 
    {
        //prepare
        test_insertMultiple();
        
        //test
        Query q = new Query("ludo").limit(0,5);
        SearchResult res = redisSearchClient.search(q);
        Query q2 = new Query("anniversair*").limit(0,5);
        SearchResult res2 = redisSearchClient.search(q2);
        
        //verify
        Assert.assertTrue("Mauvais nombre renvoyé", res.totalResults==1);
        Assert.assertTrue("Mauvais document renvoyé", "198209221018".equals(res.docs.get(0).get("oid")));
        Assert.assertTrue("Mauvais nombre renvoyé", res2.totalResults==2);
    }
}