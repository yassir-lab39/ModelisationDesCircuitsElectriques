package layout.validation;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import layout.*;
import layout.util.LayoutSwitch;

/**
 * Validateur pour les modèles Layout.
 * Vérifie les contraintes de cohérence et de nommage pour les PCB.
 * 
 * @author L12 - 5
 * @version 1.0
 */
public class LayoutValidator extends LayoutSwitch<Boolean> {
    
    /** Expression régulière pour un identifiant valide */
    private static final String IDENT_REGEX = "^[A-Za-z_][A-Za-z0-9_]*$";
    
    /** Résultat de la validation */
    private ValidationResult result = null;
    
    /**
     * Constructeur
     */
    public LayoutValidator() {}
    
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
     * C1 : Validation d'un Layout.
     */
    @Override
    public Boolean caseLayout(Layout object) {
        // C1.1 : Nom non vide
        if (object.getNom() != null) {
            this.result.recordIfFailed(
                !object.getNom().isEmpty(),
                object,
                "Le nom du layout ne doit pas être vide s'il est défini"
            );
            
            // C1.2 : Nom valide
            this.result.recordIfFailed(
                object.getNom().matches(IDENT_REGEX),
                object,
                "Le nom du layout ne respecte pas les conventions (identifiant valide)"
            );
        }
        
        // C1.3 : Netlist référencée existe
        this.result.recordIfFailed(
            object.getNetlistRef() != null,
            object,
            "Le layout doit référencer une netlist"
        );
        
        // C1.4 : Au moins un board
        this.result.recordIfFailed(
            object.getBoards() != null && object.getBoards().size() > 0,
            object,
            "Le layout doit contenir au moins un board"
        );
        
        // C1.5 : Version non vide si présente
        if (object.getVersion() != null) {
            this.result.recordIfFailed(
                !object.getVersion().isEmpty(),
                object,
                "La version ne doit pas être vide si elle est définie"
            );
        }
        
        // C1.6 : Auteur non vide si présent
        if (object.getAuteur() != null) {
            this.result.recordIfFailed(
                !object.getAuteur().isEmpty(),
                object,
                "L'auteur ne doit pas être vide s'il est défini"
            );
        }
        
        // Visiter les boards
        if (object.getBoards() != null) {
            for (Board board : object.getBoards()) {
                this.doSwitch(board);
            }
        }
        
        return null;
    }
    
    /**
     * C2 : Validation d'un Board.
     */
    @Override
    public Boolean caseBoard(Board object) {
        // C2.1 : Nom non vide
        if (object.getNom() != null) {
            this.result.recordIfFailed(
                !object.getNom().isEmpty(),
                object,
                "Le nom du board ne doit pas être vide s'il est défini"
            );
        }
        
        // C2.2 : Largeur strictement positive
        this.result.recordIfFailed(
            object.getLargeur() > 0,
            object,
            "La largeur du board doit être strictement positive"
        );
        
        // C2.3 : Hauteur strictement positive
        this.result.recordIfFailed(
            object.getHauteur() > 0,
            object,
            "La hauteur du board doit être strictement positive"
        );
        
        // C2.4 : Au moins 2 couches
        this.result.recordIfFailed(
            object.getCouches() != null && object.getCouches().size() >= 2,
            object,
            "Le board doit avoir au moins 2 couches"
        );
        
        // C2.5 : Noms de couches uniques dans le board
        if (object.getCouches() != null && object.getCouches().size() > 1) {
            for (int i = 0; i < object.getCouches().size(); i++) {
                Couche c1 = object.getCouches().get(i);
                if (c1.getNom() == null || c1.getNom().isEmpty()) {
                    continue;
                }
                
                for (int j = i + 1; j < object.getCouches().size(); j++) {
                    Couche c2 = object.getCouches().get(j);
                    if (c1.getNom().equals(c2.getNom())) {
                        this.result.recordError(
                            object,
                            "Le nom de couche '" + c1.getNom() + "' est en double dans le board"
                        );
                    }
                }
            }
        }
        
        // C2.6 : OrdreZ uniques dans le board
        if (object.getCouches() != null && object.getCouches().size() > 1) {
            for (int i = 0; i < object.getCouches().size(); i++) {
                Couche c1 = object.getCouches().get(i);
                
                for (int j = i + 1; j < object.getCouches().size(); j++) {
                    Couche c2 = object.getCouches().get(j);
                    if (c1.getOrdreZ() == c2.getOrdreZ()) {
                        this.result.recordError(
                            object,
                            "L'ordreZ " + c1.getOrdreZ() + " est en double dans le board"
                        );
                    }
                }
            }
        }
        
        // Visiter les couches
        if (object.getCouches() != null) {
            for (Couche couche : object.getCouches()) {
                this.doSwitch(couche);
            }
        }
        
        return null;
    }
    
    /**
     * C3 : Validation d'une Couche (abstraite).
     */
    @Override
    public Boolean caseCouche(Couche object) {
        // C3.1 : Nom non vide (si présent)
        if (object.getNom() != null) {
            this.result.recordIfFailed(
                !object.getNom().isEmpty(),
                object,
                "Le nom de la couche ne doit pas être vide s'il est défini"
            );
        }
        
        // C3.2 : OrdreZ >= 0
        this.result.recordIfFailed(
            object.getOrdreZ() >= 0,
            object,
            "L'ordreZ de la couche doit être positif ou nul"
        );
        
        // Visiter les pistes
        if (object.getPistes() != null) {
            for (Piste piste : object.getPistes()) {
                this.doSwitch(piste);
            }
        }
        
        return null;
    }
    
    /**
     * C4 : Validation d'une CoucheExterne.
     */
    @Override
    public Boolean caseCoucheExterne(CoucheExterne object) {
        // D'abord valider en tant que Couche
        caseCouche(object);
        
        // C4.1 : Face définie
        this.result.recordIfFailed(
            object.getFace() != null,
            object,
            "La face de la couche externe doit être définie (DESSUS ou DESSOUS)"
        );
        
        // C4.2 : Vérifier que les placements sont dans les limites du board
        if (object.getPlacements() != null) {
            Board board = (Board) object.eContainer();
            if (board != null) {
                for (PlacementComposant placement : object.getPlacements()) {
                    // Vérification des limites
                    if (placement.getX() < 0 || placement.getX() > board.getLargeur()) {
                        this.result.recordError(
                            object,
                            "Placement hors limites : X=" + placement.getX()
                            + " (largeur board=" + board.getLargeur() + ")"
                        );
                    }
                    
                    if (placement.getY() < 0 || placement.getY() > board.getHauteur()) {
                        this.result.recordError(
                            object,
                            "Placement hors limites : Y=" + placement.getY() 
                            + " (hauteur board=" + board.getHauteur() + ")"
                        );
                    }
                }
            }
        }
        
        // Visiter les placements
        if (object.getPlacements() != null) {
            for (PlacementComposant placement : object.getPlacements()) {
                this.doSwitch(placement);
            }
        }
        
        return null;
    }
    
    /**
     * C5 : Validation d'une CoucheInterne.
     */
    @Override
    public Boolean caseCoucheInterne(CoucheInterne object) {
        // Valider en tant que Couche
        caseCouche(object);

        if (object.getPistes() == null || object.getPistes().isEmpty()) {
            this.result.recordError(
                object,
                "WARNING : La couche interne ne contient aucune piste"
            );
        }
        
        return null;
    }
    
    /**
     * C6 : Validation d'un PlacementComposant.
     */
    @Override
    public Boolean casePlacementComposant(PlacementComposant object) {
        // C6.1 : Instance composant référencée existe
        this.result.recordIfFailed(
            object.getInstanceComposant() != null,
            object,
            "Le placement doit référencer une instance de composant"
        );
        
        // C6.2 : Coordonnées X et Y positives
        this.result.recordIfFailed(
            object.getX() >= 0,
            object,
            "La coordonnée X du placement doit être positive ou nulle"
        );
        
        this.result.recordIfFailed(
            object.getY() >= 0,
            object,
            "La coordonnée Y du placement doit être positive ou nulle"
        );
        
        // C6.3 : Rotation entre 0 et 360
        this.result.recordIfFailed(
            object.getRotation() >= 0 && object.getRotation() < 360,
            object,
            "La rotation doit être entre 0 et 360 degrés"
        );

        // C6.5 : Une instance ne peut être placée qu'une seule fois par board
        if (object.getInstanceComposant() != null) {
            CoucheExterne couche = (CoucheExterne) object.eContainer();
            if (couche != null) {
                Board board = (Board) couche.eContainer();
                if (board != null && board.getCouches() != null) {
                    int count = 0;
                    for (Couche c : board.getCouches()) {
                        if (c instanceof CoucheExterne) {
                            CoucheExterne ce = (CoucheExterne) c;
                            if (ce.getPlacements() != null) {
                                count += ce.getPlacements().stream()
                                    .filter(p -> p.getInstanceComposant() != null 
                                              && p.getInstanceComposant().equals(object.getInstanceComposant()))
                                    .count();
                            }
                        }
                    }
                    
                    if (count > 1) {
                        this.result.recordError(
                            object,
                            "L'instance " + object.getInstanceComposant().getId() 
                            + " est placée plusieurs fois sur le même board"
                        );
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * C7 : Validation d'une Piste.
     */
    @Override
    public Boolean casePiste(Piste object) {
        // C7.1 : Largeur strictement positive
        this.result.recordIfFailed(
            object.getLargeur() > 0,
            object,
            "La largeur de la piste doit être strictement positive"
        );
        
        // C7.2 : Au moins 2 points
        this.result.recordIfFailed(
            object.getPoints() != null && object.getPoints().size() >= 2,
            object,
            "La piste doit avoir au moins 2 points"
        );
        
        // C7.3 : Les points doivent être dans les limites du board
        if (object.getPoints() != null) {
            Couche couche = (Couche) object.eContainer();
            if (couche != null) {
                Board board = (Board) couche.eContainer();
                if (board != null) {
                    for (Point point : object.getPoints()) {
                        if (point.getX() < 0 || point.getX() > board.getLargeur()) {
                            this.result.recordError(
                                object,
                                "Point de piste hors limites : X=" + point.getX()
                                + " (largeur board=" + board.getLargeur() + ")"
                            );
                        }
                        
                        if (point.getY() < 0 || point.getY() > board.getHauteur()) {
                            this.result.recordError(
                                object,
                                "Point de piste hors limites : Y=" + point.getY()
                                + " (hauteur board=" + board.getHauteur() + ")"
                            );
                        }
                    }
                }
            }
        }
     // C7.4 : Pas de points en double
        if (object.getPoints() != null && object.getPoints().size() > 1) {
            for (int i = 0; i < object.getPoints().size(); i++) {
                Point p1 = object.getPoints().get(i);
                
                for (int j = i + 1; j < object.getPoints().size(); j++) {
                    Point p2 = object.getPoints().get(j);
                    
                    // Vérifier si les deux points ont les mêmes coordonnées
                    if (Math.abs(p1.getX() - p2.getX()) < 0.001 
                        && Math.abs(p1.getY() - p2.getY()) < 0.001) {
                        this.result.recordError(
                            object,
                            "La piste contient des points en double aux coordonnées (" 
                            + p1.getX() + ", " + p1.getY() + ")"
                        );
                    }
                }
            }
        }
        
        // C7.5 : Les ports connectés doivent exister dans la netlist du layout
        if (object.getPortsConnectes() != null && !object.getPortsConnectes().isEmpty()) {
            Couche couche = (Couche) object.eContainer();
            if (couche != null) {
                Board board = (Board) couche.eContainer();
                if (board != null) {
                    Layout layout = (Layout) board.eContainer();
                    if (layout != null && layout.getNetlistRef() != null 
                        && layout.getNetlistRef().getConnexions() != null) {
                        
                        // Vérifier que chaque ReferencePort existe dans une connexion de la netlist
                        for (Object refPort : object.getPortsConnectes()) {
                            boolean trouve = false;
                            for (Object conn : layout.getNetlistRef().getConnexions()) {
                                if (refPort != null) {
                                    trouve = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Visiter les points
        if (object.getPoints() != null) {
            for (Point point : object.getPoints()) {
                this.doSwitch(point);
            }
        }
        
        return null;
    }
    
    /**
     * C8 : Validation d'un Point.
     */
    @Override
    public Boolean casePoint(Point object) {
        // C8.1 : Coordonnées positives ou nulles
        this.result.recordIfFailed(
            object.getX() >= 0,
            object,
            "La coordonnée X du point doit être positive ou nulle"
        );
        
        this.result.recordIfFailed(
            object.getY() >= 0,
            object,
            "La coordonnée Y du point doit être positive ou nulle"
        );
        
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
