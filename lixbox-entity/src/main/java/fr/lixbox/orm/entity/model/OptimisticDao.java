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
package fr.lixbox.orm.entity.model;

import java.io.Serializable;
import java.util.Calendar;


/**
 * Cette interface represente les attributs necessaire a une entite pour
 * pouvoir etre utiliser par le manager de DAO.
 * 
 * @author ludovic.terral
 */
public interface OptimisticDao extends Serializable, Dao
{    
    // ----------- Methode -----------
    Calendar getVersion();
    void setVersion(Calendar version);
}