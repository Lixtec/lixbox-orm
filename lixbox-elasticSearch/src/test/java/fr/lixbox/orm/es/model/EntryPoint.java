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

import fr.lixbox.orm.es.model.enumeration.EntryPointType;

/**
 * Cette entite represente un EntryPoint.
 * 
 * @author ludovic.terral
 */
public class EntryPoint 
{
    // ----------- Attribut(s) -----------
    private EntryPointType type;
    private String ressourceUri;

    
    
    // ----------- Methode(s) -----------
    public EntryPointType getType()
    {
        return type;
    }
    public void setType(EntryPointType type)
    {
        this.type = type;
    }
    
    
    
    public String getRessourceUri()
    {
        return ressourceUri;
    }
    public void setRessourceUri(String ressourceUri)
    {
        this.ressourceUri = ressourceUri;
    }
}
