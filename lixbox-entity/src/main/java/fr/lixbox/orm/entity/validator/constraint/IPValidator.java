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
package fr.lixbox.orm.entity.validator.constraint;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.lixbox.common.util.ValidatorUtil;

/**
 * Ce validateur permet de valider une adresse IPv4 de la forme 0.0.0.0
 * 
 * @author virgile.de-lacerda
 *
 */
public class IPValidator implements ConstraintValidator<IP, Object> {
	@Override
	public void initialize(IP arg0)
    {
        //rien de particulier
    }

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext ctx) {
		try {
			return ValidatorUtil.isIP((String) value);
		}
		catch (ClassCastException e) {
			return false;
		}
	}
}