/*******************************************************************************
 *    
 *                           FRAMEWORK Lixbox
 *                          ==================
 *      
 * This file is part of lixbox-orm.
 *
 *    lixbox-supervision is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    lixbox-supervision is distributed in the hope that it will be useful,
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
package fr.lixbox.jee.redis.model;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.lixbox.io.json.JsonUtil;
import fr.lixbox.orm.entity.model.Dao;
import fr.lixbox.orm.redis.model.RedisSearchDao;
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
    
    
    
    @Override
    public long getTTL()
    {
        return 0;
    }
}