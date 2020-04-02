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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.lixbox.common.model.ConteneurEvenement;
import fr.lixbox.common.model.Contexte;
import fr.lixbox.io.json.JsonUtil;
import fr.lixbox.jee.entity.model.Dao;
import io.redisearch.Schema;

/**
 * Cette classe est l'entite qui stocke un jour non ouvré.
 * 
 * @author ludovic.terral
 */
public class JNO implements RedisSearchDao, Dao
{
    // ----------- Attribut -----------   
    protected static final Log log = LogFactory.getLog(JNO.class);
    private static final long serialVersionUID = -20120911092230L;
    
    private String libelle;
    private Calendar dateEvent;
    private boolean estActif = true;
        
    private String oid;
    
    
    
    // ----------- Methode -----------
    
    @Override
    public String getOid()
    {
        return this.oid;
    }
    @Override
    public void setOid(String oid)
    {
        this.oid = oid;
    }
    
    
    
    public String getLibelle()
    {
        return this.libelle;
    }
    public void setLibelle(String libelle)
    {
        this.libelle = libelle;
    }
    


    public Calendar getDateEvent()
    {
        return this.dateEvent;
    }
    public void setDateEvent(final Calendar dateEvent)
    {
        this.dateEvent = dateEvent;
    }
    
    

    public boolean getEstActif()
    {
        return this.estActif;
    }
    public void setEstActif(final boolean estActif)
    {
        this.estActif = estActif;
    }

    
    
    /**
     * Cette methode valide la coherence 
     * de l'entite
     *    
     * @return un ConteneurEvenement
     */
    @Override
    public ConteneurEvenement validate()
    {
        return validate("JourFerie.");
    }      
    
        
    
    /**
     * Cette methode valide la coherence 
     * de l'entite
     * 
     * @param parent element parent dans l'arbre des objets
     *     
     * @return un ConteneurEvenement
     */
    @Override
    public ConteneurEvenement validate(final String parent)
    {   
        final Contexte contexte = new Contexte();    
        return validate(parent, contexte);
    }
        
        
        
    /**
     * Cette methode valide la coherence 
     * de l'entite
     * 
     * @param parent element parent dans l'arbre des objets
     * @param contexte Contexte
     * 
     * @return un ConteneurEvenement
     */
    @Override
    public ConteneurEvenement validate(final String parent, final Contexte contexte)
    {  
        contexte.put("parent", parent);    
        contexte.put("classSignature", JNO.class.getName()); //$NON-NLS-1$
        return new ConteneurEvenement();
    }



    @Override
    public String toString()
    {
        return JsonUtil.transformObjectToJson(this, false);
    }
    
    
    
    @Override
    public Schema getIndexSchema()
    {
        return new Schema()
                .addTextField("oid", 1)
                .addTextField("libelle", 2)
                .addTextField("typeJour",1)
                .addNumericField("dateEvent");
    }
    
    
    
    @Override
    public Map<String, Object> getIndexFieldValues()
    {
        Map<String, Object> indexFields = new HashMap<>();
        indexFields.put("oid", oid);
        indexFields.put("libelle", libelle);
        if (dateEvent!=null)
        {
            indexFields.put("dateEvent",dateEvent.getTimeInMillis());
        }
        return indexFields;
    }

    
    

    @JsonIgnore
    @XmlTransient
    public String getKey()
    {
        return "LIXBOX:OBJECT:"+this.getClass().getName()+":"+oid;
    }
}