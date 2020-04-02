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

import fr.lixbox.common.model.ConteneurEvenement;
import fr.lixbox.common.model.Contexte;
import fr.lixbox.common.model.enumeration.NiveauEvenement;
/**
 * Cette classe represente un objet embarcable generique.
 * 
 * @author ludovic.terral
 */
public abstract class AbstractValidatedEmbeddable implements ExtendEmbeddable
{
    // ----------- Attribut(s) -----------
    private static final long serialVersionUID = 5806736426434897771L;
	private static final Validator validator = Validation.byDefaultProvider().configure()
			.messageInterpolator(new ResourceBundleMessageInterpolator(new PlatformResourceBundleLocator("MessagesValidator")))
			.buildValidatorFactory().getValidator();



    // ----------- Methode(s) -----------
	@Override
	public ConteneurEvenement validate()
	{
		return validate(this.getClass().getName().toLowerCase() + ".");
	}



	@Override
	public ConteneurEvenement validate(final String parent, final Contexte contexte)
	{
		Set<ConstraintViolation<AbstractValidatedEmbeddable>> constraintViolations = validator.validate(this);
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