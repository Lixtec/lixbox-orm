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
package fr.lixbox.orm.entity.validator.constraint;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.lixbox.common.exceptions.BusinessException;
import fr.lixbox.common.model.ConteneurEvenement;
import fr.lixbox.common.model.Evenement;
import fr.lixbox.orm.entity.model.ValidatedPojo;

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
                conteneur.addAll(validateObject((ValidatedPojo) item));
            }
        }
        else
        {
            conteneur = validateObject((ValidatedPojo) value);
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
    
    
    
    protected ConteneurEvenement validateObject(ValidatedPojo value)
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