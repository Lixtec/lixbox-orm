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

import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import fr.lixbox.common.model.ConteneurEvenement;
import fr.lixbox.common.model.Contexte;
import fr.lixbox.common.model.enumeration.NiveauEvenement;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
/**
 * Cette classe represente un objet embarcable generique.
 * 
 * @author ludovic.terral
 */
public abstract class AbstractValidatedEmbeddable implements ExtendEmbeddable
{
    // ----------- Attribut(s) -----------
    private static final long serialVersionUID = 5806736426434897771L;
    private static final Validator VALIDATOR =
            Validation.byDefaultProvider()
              .configure()
              .messageInterpolator(new ParameterMessageInterpolator())
              .buildValidatorFactory()
              .getValidator();



    // ----------- Methode(s) -----------
	@Override
	public ConteneurEvenement validate()
	{
		return validate(this.getClass().getName().toLowerCase() + ".");
	}



	@Override
	public ConteneurEvenement validate(final String parent, final Contexte contexte)
	{
		Set<ConstraintViolation<AbstractValidatedEmbeddable>> constraintViolations = VALIDATOR.validate(this);
		ConteneurEvenement conteneur = new ConteneurEvenement();
		Iterator<ConstraintViolation<AbstractValidatedEmbeddable>> iterator = constraintViolations.iterator();
		while (iterator.hasNext()) {
			ConstraintViolation<AbstractValidatedEmbeddable> constraint = iterator.next();
			conteneur.add(NiveauEvenement.ERROR, parent + constraint.getPropertyPath() + " : " + constraint.getMessage(), Calendar.getInstance(), contexte, "classe");
		}

		return conteneur;
	}
	
	
	
	@Override
	public ConteneurEvenement validate(final String parent)
    {   
        final Contexte contexte = new Contexte();    
        return validate(parent, contexte);
    }	
}