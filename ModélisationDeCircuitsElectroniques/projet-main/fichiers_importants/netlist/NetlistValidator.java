package netlist.validation;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import netlist.*;
import netlist.util.NetlistSwitch;

/**
 * Validateur pour les modèles Netlist.
 * Vérifie les contraintes de cohérence et de nommage.
 * 
 * @author L12-5
 * @version 1.0
 */
public class NetlistValidator extends NetlistSwitch<Boolean> {
    
    /** Expression régulière pour un identifiant valide */
    private static final String IDENT_REGEX = "^[A-Za-z_][A-Za-z0-9_]*$";
    
    /** Résultat de la validation */
    private ValidationResult result = null;
    
    /**
     * Constructeur
     */
    public NetlistValidator() {}
    
    /**
     * Lance la validation d'une ressource.
     * @param resource ressource à valider
     * @return résultat de validation
     */
    public ValidationResult validate(Resource resource) {
        this.result = new ValidationResult();
        
        for (EObject object : resource.getContents()) {
            this.doSwitch(object);
        }
        
        return this.result;
    }
    
    /**
     * C1 : Validation d'un Netlist.
     */
    @Override
    public Boolean caseNetlist(Netlist object) {
        // C1.1 : Nom non vide (si présent)
        if (object.getNom() != null) {
            this.result.recordIfFailed(
                !object.getNom().isEmpty(),
                object,
                "Le nom du netlist ne doit pas être vide s'il est défini"
            );
            
            // C1.2 : Nom valide (identifiant)
            this.result.recordIfFailed(
                object.getNom().matches(IDENT_REGEX),
                object,
                "Le nom du netlist ne respecte pas les conventions (identifiant valide)"
            );
        }
        
        // C1.3 : Vérifier qu'il y a au moins une instance
        if (object.getInstances() != null && object.getInstances().isEmpty()) {
            this.result.recordError(
                object,
                "WARNING : Le netlist ne contient aucune instance de composant"
            );
        }
        
        // Visiter les instances
        if (object.getInstances() != null) {
            for (InstanceComposant inst : object.getInstances()) {
                this.doSwitch(inst);
            }
        }
        
        // Visiter les connexions
        if (object.getConnexions() != null) {
            for (Connexion conn : object.getConnexions()) {
                this.doSwitch(conn);
            }
        }
        
        // Visiter les commentaires
        if (object.getCommentaires() != null) {
            for (Commentaire comm : object.getCommentaires()) {
                this.doSwitch(comm);
            }
        }
        
        return null;
    }
    
    /**
     * C2 : Validation d'une InstanceComposant.
     */
    @Override
    public Boolean caseInstanceComposant(InstanceComposant object) {
        // C2.1 : ID non vide
        this.result.recordIfFailed(
            object.getId() != null && !object.getId().isEmpty(),
            object,
            "L'ID de l'instance ne doit pas être vide"
        );
        
        // C2.2 : ID valide 
        if (object.getId() != null) {
            this.result.recordIfFailed(
                object.getId().matches(IDENT_REGEX),
                object,
                "L'ID de l'instance ne respecte pas les conventions (identifiant valide)"
            );
        }
        
        // C2.3 : ID unique dans le netlist
        Netlist netlist = (Netlist) object.eContainer();
        if (netlist != null && netlist.getInstances() != null && object.getId() != null) {
            long count = netlist.getInstances().stream()
                .filter(inst -> inst.getId() != null && inst.getId().equals(object.getId()))
                .count();
            
            this.result.recordIfFailed(
                count == 1,
                object,
                "L'ID de l'instance '" + object.getId() + "' n'est pas unique dans le netlist"
            );
        }
        
        // C2.4 : Composant référencé existe
        this.result.recordIfFailed(
            object.getComposantRef() != null,
            object,
            "L'instance doit référencer un composant du catalogue"
        );
        
        // C2.5 : Si valeur est présente, elle ne doit pas être vide
        if (object.getValeur() != null) {
            this.result.recordIfFailed(
                !object.getValeur().isEmpty(),
                object,
                "La valeur de l'instance ne doit pas être vide si elle est définie"
            );
        }
        
        // Visiter les commentaires de l'instance
        if (object.getCommentaires() != null) {
            for (Commentaire comm : object.getCommentaires()) {
                this.doSwitch(comm);
            }
        }
        
        return null;
    }
    
    /**
     * C3 : Validation d'une Connexion.
     */
    @Override
    public Boolean caseConnexion(Connexion object) {
        // C3.1 : Nom non vide (si présent)
        if (object.getNom() != null) {
            this.result.recordIfFailed(
                !object.getNom().isEmpty(),
                object,
                "Le nom de la connexion ne doit pas être vide s'il est défini"
            );
        }
        
        // C3.2 : Au moins 2 ports connectés
        this.result.recordIfFailed(
            object.getPortsConnectes() != null && object.getPortsConnectes().size() >= 2,
            object,
            "Une connexion doit avoir au moins 2 ports connectés"
        );
        
        // C3.3 : Vérifier que les ports connectés ne sont pas en double
        if (object.getPortsConnectes() != null && object.getPortsConnectes().size() >= 2) {
            for (int i = 0; i < object.getPortsConnectes().size(); i++) {
                ReferencePort ref1 = object.getPortsConnectes().get(i);
                if (ref1.getInstance() == null || ref1.getPort() == null) {
                    continue; // Sera détecté par la validation de ReferencePort
                }
                
                for (int j = i + 1; j < object.getPortsConnectes().size(); j++) {
                    ReferencePort ref2 = object.getPortsConnectes().get(j);
                    if (ref2.getInstance() == null || ref2.getPort() == null) {
                        continue;
                    }
                    
                    // Vérifier si c'est le même port de la même instance
                    if (ref1.getInstance().equals(ref2.getInstance()) 
                        && ref1.getPort().equals(ref2.getPort())) {
                        this.result.recordError(
                            object,
                            "La connexion contient le même port en double : " 
                            + ref1.getInstance().getId() + "." + ref1.getPort().getNom()
                        );
                    }
                }
            }
        }
        
        // Visiter les références de port
        if (object.getPortsConnectes() != null) {
            for (ReferencePort ref : object.getPortsConnectes()) {
                this.doSwitch(ref);
            }
        }
        
        return null;
    }
    
    /**
     * C4 : Validation d'une ReferencePort.
     */
    @Override
    public Boolean caseReferencePort(ReferencePort object) {
        // C4.1 : Instance référencée existe
        this.result.recordIfFailed(
            object.getInstance() != null,
            object,
            "Une référence de port doit référencer une instance"
        );
        
        // C4.2 : Port référencé existe
        this.result.recordIfFailed(
            object.getPort() != null,
            object,
            "Une référence de port doit référencer un port"
        );
        
        // C4.3 : L'instance référencée appartient au même netlist
        Connexion connexion = (Connexion) object.eContainer();
        if (connexion != null && object.getInstance() != null) {
            Netlist netlist = (Netlist) connexion.eContainer();
            if (netlist != null && netlist.getInstances() != null) {
                this.result.recordIfFailed(
                    netlist.getInstances().contains(object.getInstance()),
                    object,
                    "L'instance référencée doit appartenir au même netlist"
                );
            }
        }
        
        
        return null;
    }
    
    /**
     * C5 : Validation d'un Commentaire.
     */
    @Override
    public Boolean caseCommentaire(Commentaire object) {
        // C5.1 : Texte non vide (si présent)
        if (object.getTexte() != null) {
            this.result.recordIfFailed(
                !object.getTexte().isEmpty(),
                object,
                "Le texte du commentaire ne doit pas être vide s'il est défini"
            );
        }
        
        // C5.2 : Si auteur est présent, il ne doit pas être vide
        if (object.getAuteur() != null) {
            this.result.recordIfFailed(
                !object.getAuteur().isEmpty(),
                object,
                "L'auteur du commentaire ne doit pas être vide s'il est défini"
            );
        }
        
        return null;
    }

    /**
     * Cas par défaut.
     */
    @Override
    public Boolean defaultCase(EObject object) {
        return null;
    }
}
