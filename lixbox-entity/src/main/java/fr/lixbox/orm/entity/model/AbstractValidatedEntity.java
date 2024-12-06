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

import fr.lixbox.common.exceptions.BusinessException;
import fr.lixbox.common.model.ConteneurEvenement;
import fr.lixbox.common.model.Contexte;
import fr.lixbox.common.model.enumeration.NiveauEvenement;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
/**
 * Cette classe represente l'entite generique.
 * 
 * @author ludovic.terral
 */
public abstract class AbstractValidatedEntity implements Dao, ValidatedDao
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
	public ConteneurEvenement validate() throws BusinessException
	{
		return validate(this.getClass().getSimpleName().toLowerCase() + ".");
	}
	
	
	
	@Override
	public ConteneurEvenement validate(final String parent, final Contexte contexte) throws BusinessException
	{
		Set<ConstraintViolation<AbstractValidatedEntity>> constraintViolations = VALIDATOR.validate(this);
		ConteneurEvenement conteneur = new ConteneurEvenement();
		Iterator<ConstraintViolation<AbstractValidatedEntity>> iterator = constraintViolations.iterator();
		while (iterator.hasNext()) {
			ConstraintViolation<AbstractValidatedEntity> constraint = iterator.next();
			conteneur.add(NiveauEvenement.ERROR, parent + constraint.getPropertyPath() + " : " + constraint.getMessage(), Calendar.getInstance(), contexte, "classe");
		}
		return conteneur;
	}
	
	
	
	@Override
	public ConteneurEvenement validate(final String parent) throws BusinessException
    {   
        final Contexte contexte = new Contexte();    
        return validate(parent, contexte);
    }
	
	
	
	@Override
	public String toString()
	{
		return String.format("%s[oid=%s]", getClass().getSimpleName(), getOid());
	}
	
	
	
	@Override
    public int hashCode()
	{
        return (getOid() != null) ? (getClass().hashCode() + getOid().hashCode()) : super.hashCode();
    }

	
	
    @Override
    public boolean equals(Object other)
    {
        return (other != null && getClass() == other.getClass() && getOid() != null) ? getOid().equals(((AbstractValidatedEntity) other).getOid()) : (other == this);
    }
}