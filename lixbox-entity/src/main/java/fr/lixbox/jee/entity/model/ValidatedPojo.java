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

import java.io.Serializable;

import fr.lixbox.common.exceptions.BusinessException;
import fr.lixbox.common.model.ConteneurEvenement;
import fr.lixbox.common.model.Contexte;


/**
 * Cette interface represente les attributs necessaire a une entite pour
 * pouvoir etre utiliser par le manager de DAO.
 * 
 * @author ludovic.terral
 */
public interface ValidatedPojo extends Serializable
{    
    // ----------- Methode(s) -----------
    boolean equals(Object other);
    int hashCode();
    String toString();  
    ConteneurEvenement validate() throws BusinessException;
    ConteneurEvenement validate(final String parent) throws BusinessException;
    ConteneurEvenement validate(final String parent, final Contexte contexte) throws BusinessException;
}