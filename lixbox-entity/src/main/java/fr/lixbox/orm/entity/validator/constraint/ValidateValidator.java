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

import fr.lixbox.common.exceptions.BusinessException;
import fr.lixbox.common.model.ConteneurEvenement;
import fr.lixbox.common.model.Evenement;
import fr.lixbox.orm.entity.model.ValidatedDao;

/**
 * Ce validateur invoque la validation de l'entite
 * 
 * @author virgile.de-lacerda
 *
 */
public class ValidateValidator implements ConstraintValidator<Validate, Object>
{
    // ----------- Attribut(s) -----------
    @Override
    public void initialize(Validate arg0)
    {
        //rien de particulier
    }



    // ----------- Methode(s) -----------
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext ctx)
    {
        ctx.disableDefaultConstraintViolation();
        ConteneurEvenement conteneur;
        
        if (value instanceof Iterable<?>)
        {
            conteneur = new ConteneurEvenement();
            for (Object item : (Iterable<?>)value)
            {
                conteneur.addAll(validateObject((ValidatedDao) item));
            }
        }
        else
        {
            conteneur = validateObject((ValidatedDao) value);
        }

        if (conteneur.getSize()>0)
        {
            for (Evenement event : conteneur.getEvenements())
            {
                ctx.buildConstraintViolationWithTemplate(event.getLibelle()).addConstraintViolation();
            }
        }
        return (conteneur.getSize() == 0);
    }
    
    
    
    protected ConteneurEvenement validateObject(ValidatedDao value)
    {
        ConteneurEvenement conteneur;
        try
        {
            if (value != null)
            {
                conteneur = (value).validate();
            }
            else
            {
                conteneur = new ConteneurEvenement();
            }
        }
        catch (BusinessException be)
        {
            conteneur = be.getConteneur();
        }
        return conteneur;
    }
}