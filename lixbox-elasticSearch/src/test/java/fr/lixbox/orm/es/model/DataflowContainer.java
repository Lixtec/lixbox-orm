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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.lixbox.io.json.JsonUtil;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * Cette entite represente un DataFlowContainer qui embarque un msg.
 * 
 * @author ludovic.terral
 */
public class DataflowContainer extends DataflowInfo
{
    // ----------- Attribut(s) -----------
    private static final long serialVersionUID = 20241015124612L;

    private String b64Body;
    
    

    // ----------- Methode(s) -----------
    public String getB64Body()
    {
        return b64Body;
    }
    public void setB64Body(String b64Body)
    {
        this.b64Body = b64Body;
    }
    
    
    
    public String getBody()
    {
        return new String(Base64.getDecoder().decode(b64Body.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8) ;
    }
    public void setBody(String body)
    {
        this.b64Body = Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
    }
    
    

    @Override
    public String toString()
    {
        return JsonUtil.transformObjectToJson(this, false);
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
        return "DATAFLOW:OBJECT:"+DataflowContainer.class.getName();
    }

    
    
    @JsonIgnore
    @Override
    public Map<String, Object> getIndexFieldValues()
    {
        Map<String, Object> indexFields = super.getIndexFieldValues();
        return indexFields;
    }
}