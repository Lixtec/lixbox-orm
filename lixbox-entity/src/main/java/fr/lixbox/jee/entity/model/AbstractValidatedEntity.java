/*******************************************************************************
 *    
 *                           FRAMEWORK Lixbox
 *                          ==================
 *      
 *   Copyrigth - LIXTEC - Tous droits reserves.
 *   
 *   Le contenu de ce fichier est la propriete de la societe Lixtec.
 *   
 *   Toute utilisation de ce fichier et des informations, sous n'importe quelle
 *   forme necessite un accord ecrit explicite des auteurs
 *   
 *   @AUTHOR Ludovic TERRAL
 *
 ******************************************************************************/
package fr.lixbox.jee.entity.model;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;

import fr.lixbox.common.exceptions.BusinessException;
import fr.lixbox.common.model.ConteneurEvenement;
import fr.lixbox.common.model.Contexte;
import fr.lixbox.common.model.enumeration.NiveauEvenement;
/**
 * Cette classe represente l'entite generique.
 * 
 * @author ludovic.terral
 */
public abstract class AbstractValidatedEntity implements Dao
{
    // ----------- Attribut(s) -----------
    private static final long serialVersionUID = 5806736426434897771L;
	private static final Validator validator = Validation.byDefaultProvider().configure()
			.messageInterpolator(new ResourceBundleMessageInterpolator(new PlatformResourceBundleLocator("MessagesValidator")))
			.buildValidatorFactory().getValidator();
	
	
	
    // ----------- Methode(s) -----------
	@Override
	public ConteneurEvenement validate() throws BusinessException
	{
		return validate(this.getClass().getSimpleName().toLowerCase() + ".");
	}
	
	
	
	@Override
	public ConteneurEvenement validate(final String parent, final Contexte contexte) throws BusinessException
	{
		Set<ConstraintViolation<AbstractValidatedEntity>> constraintViolations = validator.validate(this);
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