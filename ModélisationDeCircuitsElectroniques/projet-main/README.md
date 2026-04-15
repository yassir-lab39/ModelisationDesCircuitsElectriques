Ce fichier a pour objectif d’expliquer l’organisation des workspaces Eclipse fournis dans le rendu
du projet.

1. Workspace initial (workspace de développement)

Lors du démarrage du projet, un premier workspace Eclipse a été utilisé et localisé sur le Bureau.
Ce workspace a servi principalement à :
- la conception initiale des méta-modèles (catalogue, netlist, layout),
- la définition des premiers projets EMF,
- les premières expérimentations de modélisation.

Cependant, au cours du développement, plusieurs limitations techniques ont été identifiées:

- des problèmes de résolution de plugins,
- des difficultés liées aux références croisées entre méta-modèles,
- des incohérences de chargement entre les modèles catalogue, netlist et layout.

2. Passage à un workspace Eclipse de déploiement (runtime)

Afin de résoudre ces problèmes, le projet a ensuite été poursuivi dans un Eclipse de déploiement.
(à partie du dossier fr.n7.circuits.catalogue)

Ce workspace de déploiement contient :
- l’ensemble des plugins générés à partir des méta-modèles,
- les éditeurs Sirius,
- les modèles d’exemples (catalogue, netlist, layout),
- les références croisées correctement résolues entre les différents modèles,
- un environnement stable permettant l’exécution et la validation complète des fonctionnalités.

C’est dans ce workspace de déploiement que le projet a été finalisé et validé fonctionnellement.
