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
package fr.lixbox.jee.redis.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.lixbox.jee.entity.model.Dao;
import io.redisearch.Schema;

/**
 * Cette interface est le contrat de base pour pouvoir utiliser
 * la recherche avancee de Redis
 * 
 * @author ludovic.terral
 */
public interface RedisSearchDao extends Dao
{
    @JsonIgnore String getKey();
    @JsonIgnore Schema getIndexSchema();
    @JsonIgnore Map<String, Object> getIndexFieldValues();
}
