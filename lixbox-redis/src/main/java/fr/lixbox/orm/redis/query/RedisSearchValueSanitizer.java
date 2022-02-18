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
package fr.lixbox.orm.redis.query;

import fr.lixbox.common.util.StringUtil;
import fr.lixbox.io.json.JsonUtil;

public class RedisSearchValueSanitizer
{
    private RedisSearchValueSanitizer()
    {
        //singleton
    }
    
    
    
    public static String sanitizeValue(Object value)
    {
        String result = "";
        if (value==null)
        {
            result="";
        }
        else if (value instanceof String && StringUtil.isNotEmpty((String)value))
        {
            result = (String) value;
        }
        else
        {
            result = JsonUtil.transformObjectToJson(value, false);
        }
        result = result.replace('-', '_').replace('@', '_').replace('.', '_');
        return result;
    }
}