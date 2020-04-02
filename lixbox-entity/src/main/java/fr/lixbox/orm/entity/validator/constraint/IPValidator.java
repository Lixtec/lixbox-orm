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