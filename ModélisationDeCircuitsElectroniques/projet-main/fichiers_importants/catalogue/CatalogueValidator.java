package catalogue.validation;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import catalogue.*;
import catalogue.util.CatalogueSwitch;

/**
 * Validateur pour les modèles Catalogue.
 * Vérifie les contraintes de cohérence et de nommage.
 * 
 * @author L12 - 5 
 * @version 1.0
 */
public class CatalogueValidator extends CatalogueSwitch<Boolean> {
    
    /** Expression régulière pour un identifiant valide */
    private static final String IDENT_REGEX = "^[A-Za-z_][A-Za-z0-9_]*$";
    
    /** Résultat de la validation */
    private ValidationResult result = null;
    
    /**
     * Constructeur
     */
    public CatalogueValidator() {}
    
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
     * C1 : Validation d'un Catalogue.
     */
    @Override
    public Boolean caseCatalogue(Catalogue object) {
        // Visiter les composants
        for (Composant c : object.getComposants()) {
            this.doSwitch(c);
        }
        
        return null;
    }
    
    /**
     * C2 : Validation d'un Composant.
     */
    @Override
    public Boolean caseComposant(Composant object) {
        // C2.1 : Nom non vide
        this.result.recordIfFailed(
            object.getNom() != null && !object.getNom().isEmpty(),
            object,
            "Le nom du composant ne doit pas être vide"
        );
        
        // C2.2 : Fabricant non vide (si présent)
        if (object.getFabricant() != null) {
            this.result.recordIfFailed(
                !object.getFabricant().isEmpty(),
                object,
                "Le fabricant du composant ne doit pas être vide s'il est défini"
            );
        }
        
        // C2.3 : Si empreinte existe, elle doit être valide
        if (object.getEmpreinte() != null) {
            this.doSwitch(object.getEmpreinte());
        }
        
        // C2.4 : Au moins un port
        this.result.recordIfFailed(
            object.getPorts() != null && object.getPorts().size() > 0,
            object,
            "Le composant doit avoir au moins un port"
        );
        
        // Visiter les ports
        if (object.getPorts() != null) {
            for (Port p : object.getPorts()) {
                this.doSwitch(p);
            }
        }
        
        // Visiter les contraintes
        if (object.getContraintes() != null) {
            for (Contrainte contrainte : object.getContraintes()) {
                this.doSwitch(contrainte);
            }
        }
        
        return null;
    }
    
    /**
     * C3 : Validation d'une Empreinte.
     */
    @Override
    public Boolean caseEmpreinte(Empreinte object) {
        // C3.1 : Largeur strictement positive
        this.result.recordIfFailed(
            object.getLargeur() > 0,
            object,
            "La largeur de l'empreinte doit être strictement positive"
        );
        
        // C3.2 : Hauteur strictement positive
        this.result.recordIfFailed(
            object.getHauteur() > 0,
            object,
            "La hauteur de l'empreinte doit être strictement positive"
        );
        
        // Visiter les positions de port
        if (object.getPositionsPort() != null) {
            for (PositionPort pp : object.getPositionsPort()) {
                this.doSwitch(pp);
            }
        }
        
        return null;
    }
    
    /**
     * C4 : Validation d'un Port.
     */
    @Override
    public Boolean casePort(Port object) {
        // C4.1 : Nom non vide
        this.result.recordIfFailed(
            object.getNom() != null && !object.getNom().isEmpty(),
            object,
            "Le nom du port ne doit pas être vide"
        );
        
        // C4.2 : Nom unique dans le composant
        Composant composant = (Composant) object.eContainer();
        if (composant != null && composant.getPorts() != null) {
            long count = composant.getPorts().stream()
                .filter(p -> p.getNom() != null && p.getNom().equals(object.getNom()))
                .count();
            
            this.result.recordIfFailed(
                count == 1,
                object,
                "Le nom du port '" + object.getNom() + "' n'est pas unique dans le composant"
            );
        }
        
        // C4.3 : Type défini
        this.result.recordIfFailed(
            object.getType() != null,
            object,
            "Le type du port doit être défini"
        );
        
        return null;
    }
    
    /**
     * C5 : Validation d'une PositionPort.
     */
    @Override
    public Boolean casePositionPort(PositionPort object) {
        // C5.1 : Port référencé
        this.result.recordIfFailed(
            object.getPort() != null,
            object,
            "Une position de port doit référencer un port existant"
        );
        
        // C5.2 : Coordonnées valides par rapport à l'empreinte
        Empreinte empreinte = (Empreinte) object.eContainer();
        if (empreinte != null) {
            this.result.recordIfFailed(
                object.getX() >= 0 && object.getX() <= empreinte.getLargeur(),
                object,
                "La coordonnée X doit être entre 0 et " + empreinte.getLargeur()
            );
            
            this.result.recordIfFailed(
                object.getY() >= 0 && object.getY() <= empreinte.getHauteur(),
                object,
                "La coordonnée Y doit être entre 0 et " + empreinte.getHauteur()
            );
        }
        
        // C5.3 : Port référencé appartient au même composant
        if (object.getPort() != null && empreinte != null) {
            Composant composant = (Composant) empreinte.eContainer();
            if (composant != null && composant.getPorts() != null) {
                this.result.recordIfFailed(
                    composant.getPorts().contains(object.getPort()),
                    object,
                    "Le port référencé doit appartenir au même composant que l'empreinte"
                );
            }
        }
        
        return null;
    }
    
    /**
     * C6 : Validation de base d'une Contrainte.
     */
    @Override
    public Boolean caseContrainte(Contrainte object) {
        // C6.1 : Description non vide
        if (object.getDescription() != null) {
            this.result.recordIfFailed(
                !object.getDescription().isEmpty(),
                object,
                "La description de la contrainte ne doit pas être vide si elle est définie"
            );
        }
        
        return null;
    }
    
    /**
     * C7 : Validation de ContrainteComposite.
     */
    @Override
    public Boolean caseContrainteComposite(ContrainteComposite object) {
        // D'abord valider en tant que Contrainte
        caseContrainte(object);
        
        // C7.1 : Opérateur défini
        this.result.recordIfFailed(
            object.getOperateur() != null,
            object,
            "L'opérateur logique doit être défini (ET, OU, ou NON)"
        );
        
        // C7.2 : Au moins une sous-contrainte
        this.result.recordIfFailed(
            object.getSousContraintes() != null && object.getSousContraintes().size() > 0,
            object,
            "Une contrainte composite doit avoir au moins une sous-contrainte"
        );
        
        if (object.getSousContraintes() != null && object.getOperateur() != null) {
            // C7.3 : Pour l'opérateur NON, exactement 1 sous-contrainte
            if (object.getOperateur() == OperateurLogique.NON) {
                this.result.recordIfFailed(
                    object.getSousContraintes().size() == 1,
                    object,
                    "L'opérateur NON doit avoir exactement 1 sous-contrainte (actuellement: "
                    + object.getSousContraintes().size() + ")"
                );
            }
            
            // C7.4 : Pour ET/OU, au moins 2 sous-contraintes
            if (object.getOperateur() == OperateurLogique.ET
             || object.getOperateur() == OperateurLogique.OU) {
                this.result.recordIfFailed(
                    object.getSousContraintes().size() >= 2,
                    object,
                    "L'opérateur " + object.getOperateur()
                    + " doit avoir au moins 2 sous-contraintes (actuellement: "
                    + object.getSousContraintes().size() + ")"
                );
            }
            
            // C7.5 : Visiter les sous-contraintes pour les valider
            for (Contrainte sousContrainte : object.getSousContraintes()) {
                this.doSwitch(sousContrainte);
            }
        }
        
        // C7.6 : Détecter les cycles
        verifierPasDeCycle(object, new java.util.HashSet<>());
        
        return null;
    }
    
    /**
     * C8 : Validation de ContrainteGeometrique.
     */
    @Override
    public Boolean caseContrainteGeometrique(ContrainteGeometrique object) {
        // D'abord valider en tant que Contrainte
        caseContrainte(object);
        
        // C8.1 : Zone libre positive ou nulle
        this.result.recordIfFailed(
            object.getZoneLibre() >= 0,
            object,
            "La zone libre doit être positive ou nulle"
        );
        
        // C8.2 : Si nomComposantCible est défini, il doit être non vide
        if (object.getNomComposantCible() != null) {
            this.result.recordIfFailed(
                !object.getNomComposantCible().isEmpty(),
                object,
                "Le nom du composant cible ne doit pas être vide s'il est défini"
            );
            
            // C8.3 : Le composant cible doit exister dans le catalogue
            Composant composant = getComposantFromContrainte(object);
            if (composant != null) {
                Catalogue catalogue = (Catalogue) composant.eContainer();
                if (catalogue != null && catalogue.getComposants() != null) {
                    boolean existe = catalogue.getComposants().stream()
                        .anyMatch(c -> c.getNom() != null && 
                                      c.getNom().equals(object.getNomComposantCible()));
                    
                    this.result.recordIfFailed(
                        existe,
                        object,
                        "Le composant cible '" + object.getNomComposantCible() 
                        + "' n'existe pas dans le catalogue"
                    );
                }
            }
        }
        
        return null;
    }
    
    /**
     * C9 : Validation de ContrainteRedondance.
     */
    @Override
    public Boolean caseContrainteRedondance(ContrainteRedondance object) {
        // D'abord valider en tant que Contrainte
        caseContrainte(object);
        
        // C9.1 : Nombre strictement positif
        this.result.recordIfFailed(
            object.getNombre() > 0,
            object,
            "Le nombre de redondance doit être strictement positif"
        );
        
        return null;
    }
    
    /**
     * C10 : Validation de ContrainteBoard.
     */
    @Override
    public Boolean caseContrainteBoard(ContrainteBoard object) {
        // D'abord valider en tant que Contrainte
        caseContrainte(object);
        
        // C10.1 : Expression non vide
        this.result.recordIfFailed(
            object.getExpression() != null && !object.getExpression().isEmpty(),
            object,
            "L'expression de la contrainte board ne doit pas être vide"
        );
        
        return null;
    }
    
    /**
     * C11 : Validation de ContrainteAttribut.
     */
    @Override
    public Boolean caseContrainteAttribut(ContrainteAttribut object) {
        // D'abord valider en tant que Contrainte
        caseContrainte(object);
        
        // C11.1 : Nom d'attribut non vide
        this.result.recordIfFailed(
            object.getNomAttribut() != null && !object.getNomAttribut().isEmpty(),
            object,
            "Le nom de l'attribut ne doit pas être vide"
        );
        
        // C11.2 : Valeur attendue non vide
        this.result.recordIfFailed(
            object.getValeurAttendue() != null && !object.getValeurAttendue().isEmpty(),
            object,
            "La valeur attendue ne doit pas être vide"
        );
        
        return null;
    }
 
    /**
     * Vérifie qu'il n'y a pas de cycle dans les contraintes composites.
     */
    private void verifierPasDeCycle(ContrainteComposite contrainte, java.util.Set<Contrainte> visited) {
        if (visited.contains(contrainte)) {
            this.result.recordError(
                contrainte,
                "ERREUR : Cycle détecté dans les contraintes composites ! "
                + "La contrainte se référence elle-même."
            );
            return;
        }
        
        visited.add(contrainte);
        
        if (contrainte.getSousContraintes() != null) {
            for (Contrainte sc : contrainte.getSousContraintes()) {
                if (sc instanceof ContrainteComposite) {
                    verifierPasDeCycle((ContrainteComposite) sc, new java.util.HashSet<>(visited));
                }
            }
        }
    }
    
    /**
     * Récupère le Composant depuis une Contrainte.
     */
    private Composant getComposantFromContrainte(Contrainte contrainte) {
        EObject current = contrainte.eContainer();
        if (current instanceof Composant) {
            return (Composant) current;
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
