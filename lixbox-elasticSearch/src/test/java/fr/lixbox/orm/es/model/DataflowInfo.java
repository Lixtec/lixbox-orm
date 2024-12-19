/*******************************************************************************
 *    
 *                           APPLICATION DATAFLOW
 *                          =======================
 * MIT License
 * <p>
 * Copyright (c) 2024 Ludovic TERRAL
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *   @AUTHOR Ludovic TERRAL
 *
 ******************************************************************************/
package fr.lixbox.orm.es.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.lixbox.common.exceptions.BusinessException;
import fr.lixbox.common.model.ConteneurEvenement;
import fr.lixbox.common.model.Contexte;
import fr.lixbox.common.util.StringUtil;
import fr.lixbox.io.json.JsonUtil;
import fr.lixbox.orm.entity.model.AbstractValidatedEntity;
import fr.lixbox.orm.entity.model.NoSqlSearchDao;
import fr.lixbox.orm.es.model.enumeration.DataFlowContainerType;
import fr.lixbox.orm.es.query.ElasticSearchValueSanitizer;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * Cette entite represente un dataflowInfo.
 * 
 * @author ludovic.terral
 */
public class DataflowInfo extends AbstractValidatedEntity implements NoSqlSearchDao
{
    // ----------- Attribut(s) -----------
    private static final long serialVersionUID = 20241015130512L;
    private static final String DEFAULT_DATAFLOW_CONTAINER_VERISON = "1.0";

    private String correlationId;  
    private String containerVersion;
    private DataFlowContainerType type;
    private long ttl;
    
    private String from;
    private String to;
    private Date timestamp;
    
    private String archiveId;
    private EntryPoint entryPoint;
    private String fileName;
    private String mediaType;
    private Integer replayCounter;
    
    private Map<String, String> metadatas;  
    
    
    
    // ----------- Methode(s) -----------
    public static long getDefaultTTL()
    {
        return 6*30*24*3600*1000;
    }
    
    
    
    @Override
    @XmlTransient
    public String getOid()
    {
        return this.correlationId;
    }
    @Override
    public void setOid(String oid)
    {
        this.correlationId = oid;        
    }
    
    

    @NotNull
    @NotEmpty
    public String getContainerVersion()
    {
        if (StringUtil.isEmpty(this.containerVersion))
        {
            this.containerVersion=DEFAULT_DATAFLOW_CONTAINER_VERISON;
        }
        return this.containerVersion;
    }
    public void setContainerVersion(String containerVersion)
    {
        this.containerVersion = containerVersion;
    }
    
    
    
    public DataFlowContainerType getType()
    {
        return type;
    }
    public void setType(DataFlowContainerType type)
    {
        this.type = type;
    }
    
    
    
    @NotNull
    @NotEmpty
    public String getFrom()
    {
        return from;
    }
    public void setFrom(String from)
    {
        this.from = from;
    }
    
    
    
    @NotNull
    @NotEmpty
    public String getTo()
    {
        return to;
    }
    public void setTo(String to)
    {
        this.to = to;
    }
    
    
    
    @NotNull
    public Date getTimestamp()
    {
        return timestamp;
    }
    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }
    
    
    
    @NotNull
    @NotEmpty
    public String getCorrelationId()
    {
        return correlationId;
    }
    public void setCorrelationId(String correlationId)
    {
        this.correlationId = correlationId;
    }
    
    
    
    public String getArchiveId()
    {
        return archiveId;
    }
    public void setArchiveId(String archiveId)
    {
        this.archiveId = archiveId;
    }
    
    
    
    public EntryPoint getEntryPoint()
    {
        return entryPoint;
    }
    public void setEntryPoint(EntryPoint entryPoint)
    {
        this.entryPoint = entryPoint;
    }
    
    
    
    public String getFileName()
    {
        return fileName;
    }
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
    
    
    
    public String getMediaType()
    {
        return mediaType;
    }
    public void setMediaType(String mediaType)
    {
        this.mediaType = mediaType;
    }
    
    
    
    public Integer getReplayCounter()
    {
        return replayCounter;
    }
    public void setReplayCounter(Integer replayCounter)
    {
        this.replayCounter = replayCounter;
    }
    
    
    
    @NotNull
    public Map<String, String> getMetadatas()
    {
        if (this.metadatas==null)
        {
            this.metadatas=new HashMap<>();
        }
        return metadatas;
    }
    public void setMetadatas(Map<String, String> metadatas)
    {
        this.metadatas = metadatas;
    }
    
    
    
    @Override
    /**
     * @FIXME : revoir les règles de vérification du conteneur
     */
    public ConteneurEvenement validate(final String parent, final Contexte contexte) throws BusinessException
    {
        ConteneurEvenement conteneur = super.validate(parent, contexte);
        
//        if (DataFlowContainerType.REJEU.equals(type) && replayCounter==null)
//        {
//            conteneur.add(new Evenement());
//        }
        return conteneur;
    }
    
    

    @Override
    public String toString()
    {
        return JsonUtil.transformObjectToJson(this, false);
    }

    
    
    @Override
    public long getTTL()
    {
        if (this.ttl==-1)
        {
            this.ttl = getDefaultTTL();
        }
        return this.ttl;
    }
    public void setTTL(long ttl)
    {
        this.ttl = ttl;
    }



    @JsonIgnore
    @XmlTransient
    @Override
    public String getKey()
    {
        return getIndex()+":"+getOid();
    }



    @JsonIgnore
    @XmlTransient
    @Override
    public String getIndex()
    {
        return "DATAFLOW:OBJECT:"+DataflowInfo.class.getName();
    }
    
    

    @Override
    public Map<String, Object> getIndexFieldValues()
    {
        Map<String, Object> indexFields = new HashMap<>();
        indexFields.put("type", ElasticSearchValueSanitizer.sanitizeValue(type));
        indexFields.put("from", ElasticSearchValueSanitizer.sanitizeValue(from));
        indexFields.put("timestamp", ElasticSearchValueSanitizer.sanitizeValue(timestamp));
        if (StringUtil.isNotEmpty(archiveId))
        {
            indexFields.put("archiveId", ElasticSearchValueSanitizer.sanitizeValue(archiveId));
        }
        if (StringUtil.isNotEmpty(fileName))
        {
            indexFields.put("fileName", ElasticSearchValueSanitizer.sanitizeValue(fileName));
        }
        return indexFields;
    }
    
    
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + Objects.hash(archiveId, containerVersion, correlationId, entryPoint, fileName,
                        from, mediaType, metadatas, replayCounter, timestamp, to, type);
        return result;
    }
    
    
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        DataflowInfo other = (DataflowInfo) obj;
        return Objects.equals(archiveId, other.archiveId)
                && Objects.equals(containerVersion, other.containerVersion)
                && Objects.equals(correlationId, other.correlationId)
                && Objects.equals(entryPoint, other.entryPoint)
                && Objects.equals(fileName, other.fileName) && Objects.equals(from, other.from)
                && Objects.equals(mediaType, other.mediaType)
                && Objects.equals(metadatas, other.metadatas)
                && Objects.equals(replayCounter, other.replayCounter)
                && Objects.equals(timestamp, other.timestamp) && Objects.equals(to, other.to)
                && type == other.type;
    }
}