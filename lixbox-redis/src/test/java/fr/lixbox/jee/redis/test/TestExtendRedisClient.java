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
package fr.lixbox.jee.redis.test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.lixbox.common.util.CollectionUtil;
import fr.lixbox.common.util.DateUtil;
import fr.lixbox.jee.redis.model.JNO;
import fr.lixbox.orm.redis.client.ExtendRedisClient;


/**
 * Cette classe interface l'univers redis avec l'univers POJO.
 * 
 * @author ludovic.terral
 */
public class TestExtendRedisClient implements Serializable
{    
    private static final long serialVersionUID = -3968936170594429132L;
    private static final Log LOG = LogFactory.getLog(TestExtendRedisClient.class);
    
    private ExtendRedisClient client;
    
    
    @Before
    public void prepare()
    {
        client = new ExtendRedisClient("main.host", 6480);
    }

    
    
    @After
    public void finish() throws IOException
    {
      client.clear();
    }
    
    
    
    @Test
    public void test_merge() 
    {
        JNO anniversaire = new JNO();
        anniversaire.setDateEvent(DateUtil.parseCalendar("22/09/1982 10:18", "dd/MM/yyyy HH:mm"));
        anniversaire.setLibelle("anniversaire Ludo");
        anniversaire = client.merge(anniversaire);
        Assert.assertTrue("Impossible d'insérer l'index", anniversaire.getOid()!=null);
    }
    
    
    
    @Test
    public void test_merge_consecutif() 
    {
        JNO anniversaire = new JNO();
        anniversaire.setDateEvent(DateUtil.parseCalendar("22/09/1982 10:18", "dd/MM/yyyy HH:mm"));
        anniversaire.setLibelle("anniversaire Ludo");
        anniversaire = client.merge(anniversaire);
        Assert.assertTrue("Impossible d'insérer l'index", anniversaire.getOid()!=null);
        
        anniversaire = new JNO();
        anniversaire.setOid("230219821820");
        anniversaire.setDateEvent(DateUtil.parseCalendar("23/02/1982 18:18", "dd/MM/yyyy HH:mm"));
        anniversaire.setLibelle("anniversaire Steph");
        anniversaire = client.merge(anniversaire);
        
        try {
            List<JNO> jours = client.findByExpression(JNO.class, "anniversa*");
            Assert.assertTrue("Nombre incorrect d'elements remontes", CollectionUtil.isNotEmpty(jours)&&jours.size()==2);
        }
        catch (Exception e)
        {
            LOG.fatal(e,e);
            Assert.fail("Aucun element remonte");
        }
    }
    
    
    
    @Test
    public void test_merge_multiple() 
    {
        JNO anniversaire = new JNO();
        anniversaire.setDateEvent(DateUtil.parseCalendar("22/09/1982 10:18", "dd/MM/yyyy HH:mm"));
        anniversaire.setLibelle("anniversaire Ludo");
        
        JNO anniversaire2 = new JNO();
        anniversaire2 = new JNO();
        anniversaire2.setOid("230219821820");
        anniversaire2.setDateEvent(DateUtil.parseCalendar("23/02/1982 18:18", "dd/MM/yyyy HH:mm"));
        anniversaire2.setLibelle("anniversaire Steph");
        
        List<JNO> merged = client.merge(Arrays.asList(anniversaire, anniversaire2));
        Assert.assertTrue("Nombre incorrect d'elements merges", merged.size()==2);
        
        try 
        {
            List<JNO> jours = client.findByExpression(JNO.class, "anniversa*");
            Assert.assertTrue("Nombre incorrect d'elements remontes", CollectionUtil.isNotEmpty(jours)&&jours.size()==2);
        }
        catch (Exception e)
        {
            LOG.fatal(e,e);
            Assert.fail("Aucun element remonte");
        }
    }
    
    
    
    @Test
    public void test_findByExpression() 
    {
        JNO anniversaire = new JNO();
        anniversaire.setDateEvent(DateUtil.parseCalendar("22/09/1982 10:18", "dd/MM/yyyy HH:mm"));
        anniversaire.setLibelle("anniversaire Ludo");
        
        JNO anniversaire2 = new JNO();
        anniversaire2 = new JNO();
        anniversaire2.setOid("230219821820");
        anniversaire2.setDateEvent(DateUtil.parseCalendar("23/02/1982 18:18", "dd/MM/yyyy HH:mm"));
        anniversaire2.setLibelle("anniversaire Steph");
        
        List<JNO> merged = client.merge(Arrays.asList(anniversaire, anniversaire2));
        Assert.assertTrue("Nombre incorrect d'elements merges", merged.size()==2);
        
        try 
        {
            List<JNO> jours = client.findByExpression(JNO.class, "Lud*");
            Assert.assertTrue("Nombre incorrect d'elements remontes", CollectionUtil.isNotEmpty(jours)&&jours.size()==1);
        }
        catch (Exception e)
        {
            LOG.fatal(e,e);
            Assert.fail("Aucun element remonte");
        }
    }
}
