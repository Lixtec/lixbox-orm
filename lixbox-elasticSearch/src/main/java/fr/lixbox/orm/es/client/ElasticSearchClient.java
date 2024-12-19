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
package fr.lixbox.orm.es.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import fr.lixbox.common.exceptions.BusinessException;
import fr.lixbox.common.exceptions.ProcessusException;
import fr.lixbox.common.util.CollectionUtil;
import fr.lixbox.common.util.ExceptionUtil;
import fr.lixbox.orm.entity.model.NoSqlSearchDao;

/**
 * Cette classe interface l'univers ElasticSearch avec l'univers POJO.
 * 
 * @author ludovic.terral
 */
public class ElasticSearchClient implements Serializable
{    
    // ----------- Attibuts -----------
    private static final long serialVersionUID = 202410181620L;
    private static final Log LOG = LogFactory.getLog(ElasticSearchClient.class);
    static final String SERVICE_CODE = "ESCLI";
    
    String elasticSearchHost;
    int elasticSearchPort;
    String elasticSearchProtocol;


    
    //----------- Methodes -----------
    public ElasticSearchClient(String elasticSearchHost, int elasticSearchPort, 
            String elasticSearchProtocol)
    {
        this.elasticSearchHost = elasticSearchHost;
        this.elasticSearchPort = elasticSearchPort;
        this.elasticSearchProtocol = elasticSearchProtocol;
    }
    
    
    
    /**
     * Cette methode index un objet
     * 
     * @param object
     * 
     * @return l'objet mergé
     * 
     * @throws BusinessException
     * @throws IOException
     */
    public <T extends NoSqlSearchDao> T merge(T object)
            throws BusinessException, IOException 
    {
        if (object==null) 
        {
            return object;
        }
        
        ElasticsearchClient client = getElasticSearchClient();
        try ( ByteArrayInputStream inputStream = 
                new ByteArrayInputStream(object.toString().getBytes(StandardCharsets.UTF_8));)
        {
            IndexRequest<Object> esRequest = IndexRequest.of(esIdx -> esIdx
                .index(filterIndexName(object.getClass().getCanonicalName()))
                .withJson(inputStream)
            );
            IndexResponse response = client.index(esRequest);
            LOG.debug("Indexation sous : "+response.id()+" de l'objet "+object.toString());
            object.setOid(response.id());
        }
        catch (Exception e)
        {
            ExceptionUtil.traiterException(e, SERVICE_CODE, true);
        }
        finally
        {
            LOG.info("Fermeture du client en cours : "+client.toString());
            client._transport().close();
        }
        return object;
    }
    
    
    
    /**
     * Cette methode index une liste d'objets
     * 
     * @param objects
     * 
     * @return la liste avec les objets mergés
     * 
     * @throws BusinessException
     * @throws IOException
     */
    public <T extends NoSqlSearchDao> List<T> merge(List<T> objects) 
		throws BusinessException, IOException
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
    
    
    
    public <T extends NoSqlSearchDao> void remove(Class<T> entityClass, String id) 
		throws BusinessException
    {
    	ElasticsearchClient client = getElasticSearchClient();
		try 
		{
			DeleteRequest deleteRequest = DeleteRequest.of(req -> req
	                .index(filterIndexName(entityClass.getCanonicalName()))
	                .id(id));

			DeleteResponse deleteResponse = client.delete(deleteRequest);

			// Retourner si la suppression est réussie
			if (!deleteResponse.result().jsonValue().equals("deleted"))
			{
				throw new ProcessusException("Failed to remove document "+id);
			};
		} 
		catch (Exception e) 
		{
            ExceptionUtil.traiterException(e, ElasticSearchClient.SERVICE_CODE, true);
		}
    }
    

    
    /**
     * Cette methode renvoie l'objet DAO enregistré avec l'id en param
     * 
     * @param entityClass
     * @param id
     * 
     * @return l'objet DAO enregistré
     * 
     * @throws IOException
     * @throws BusinessException
     */
    @SuppressWarnings("unchecked")
	public <T extends NoSqlSearchDao> T findById(Class<T> entityClass, String id) 
        throws BusinessException, IOException
    {
    	ElasticsearchClient client = getElasticSearchClient();
        T dao = null;
        try
        {
            GetRequest getRequest = new GetRequest.Builder()
                .index(filterIndexName(entityClass.getCanonicalName()))
                .id(id)
                .build();
    
            GetResponse<NoSqlSearchDao> getResponse = (GetResponse<NoSqlSearchDao>) client.get(getRequest, entityClass);
            if (getResponse.found()) 
            {
                dao = (T) getResponse.source();
            } 
        }
        catch (Exception e)
        {
            ExceptionUtil.traiterException(e, ElasticSearchClient.SERVICE_CODE, true);
        }
        finally
        {
            LOG.info("Fermeture du client en cours : "+client.toString());
            client._transport().close();
        }
        return dao;
    }

    
    
    /**
     * Cette methode effectue une recherche à partir d'une requête forgée.
     * 
     * @param entityClass
     * @param query 
     * 
     * @return liste des objets correspondants
     * 
     * @throws BusinessException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
	public <T extends NoSqlSearchDao> List<T> findByQuery(Class<? extends NoSqlSearchDao> entityClass, Query query) 
        throws BusinessException, IOException 
    {
        List<T> daos = new ArrayList<>();
        ElasticsearchClient client = getElasticSearchClient();
        try
        {
            SearchRequest searchRequest = new SearchRequest.Builder()
                .index(filterIndexName(entityClass.getCanonicalName()))
                .query(query)
                .build();
            SearchResponse<T> searchResponse = (SearchResponse<T>) client.search(searchRequest, entityClass);
    
            List<Hit<T>> hits = searchResponse.hits().hits();
            LOG.debug("Réponse contient : "+searchResponse.hits().total());
            for (Hit<T> hit : hits) 
            {
                daos.add(hit.source());
            }
        }
        catch (Exception e)
        {
            ExceptionUtil.traiterException(e, SERVICE_CODE, true);
        }
        finally
        {
            LOG.info("Fermeture du client en cours : "+client.toString());
            client._transport().close();
        }
        return daos;
    }
    


    public <T extends NoSqlSearchDao> List<T> findByExpression(Class<T> entityClass, String expression) 
        throws BusinessException, IOException
    {
        List<T> daos = new ArrayList<>();
        ElasticsearchClient client = getElasticSearchClient();
        try
        {
            Query query = QueryBuilders.queryString(m -> m.query(expression)
            );
            SearchRequest searchRequest = new SearchRequest.Builder()
                .index(filterIndexName(entityClass.getCanonicalName()))
                .query(query)
                .build();
    
            SearchResponse<T> searchResponse = (SearchResponse<T>) client.search(searchRequest, entityClass);
    
            List<Hit<T>> hits = searchResponse.hits().hits();
            LOG.debug("Réponse contient : "+searchResponse.hits().total());
            for (Hit<T> hit : hits) 
            {
                daos.add(hit.source());
            }
        }
        catch (Exception e)
        {
            ExceptionUtil.traiterException(e, SERVICE_CODE, true);
        }
        finally
        {
            LOG.info("Fermeture du client en cours : "+client.toString());
            client._transport().close();
        }
        return daos;
    }


    
    private ElasticsearchClient getElasticSearchClient()
    {
        LOG.debug("URI du service ElasticSearch: "+elasticSearchProtocol+"://"+elasticSearchHost+":"+elasticSearchPort);
        RestClient restClient = RestClient.builder(
                new HttpHost(elasticSearchHost, elasticSearchPort, 
                        elasticSearchProtocol)).build();
        RestClientTransport transport = new RestClientTransport(
        restClient, new co.elastic.clients.json.jackson.JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
    
    
    
    private String filterIndexName(String indexName)
    {
        LOG.debug("Index du service ElasticSearch: "+indexName.toLowerCase());
        return indexName.toLowerCase();
    }
}
