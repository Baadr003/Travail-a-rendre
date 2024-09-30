# Projet JDBC - Scripts des développeurs

Ce projet démontre l'utilisation de JDBC pour interagir avec une base de données MySQL, en mettant l'accent sur la gestion des scripts de développeurs.

## Structure du Projet

- `Connexion.java` : Gère la connexion à la base de données.
- `DevDataService.java` : Contient les méthodes pour créer et remplir la table.
- `ExoJDBC.java` : Contient les méthodes d'analyse des données.
- `Test.java` : Classe principale pour exécuter les différentes fonctionnalités.

## Fonctionnalités

1. **Connexion à la base de données**
   - Établissement d'une connexion unique et réutilisable.

2. **Création et remplissage de la table**
   - Méthode `createTable()` : Crée la table DevData.
   - Méthode `insertData()` : Insère des données d'exemple dans la table.

3. **Analyse des données**
   - `maxScriptQuot()` : Trouve la personne ayant réalisé le nombre maximum de scripts en une journée.
   - `Triage()` : Liste les personnes triées dans l'ordre décroissant selon leur nombre de scripts.
   - `totalScriptsSemaine()` : Calcule le nombre total de scripts réalisés en une semaine.
   - `totalScriptDeveloppeur()` : Calcule le nombre total de scripts réalisés par chaque développeur.

4. **Métadonnées et requête libre**
   - `afficherMetaData()` : Affiche les métadonnées des colonnes et les données de la table DevData.

## Comment utiliser

1. Assurez-vous d'avoir MySQL installé et configuré.
2. Modifiez les informations de connexion dans la classe `Connexion` si nécessaire.
3. Exécutez la classe `Test` pour voir toutes les fonctionnalités en action.

## Exemples de code

```java
// Création de la table
DevDataService.createTable();

// Insertion des données
DevDataService.insertData();

// Analyse des données
ExoJDBC.maxScriptQuot();
ExoJDBC.Triage();
ExoJDBC.totalScriptsSemaine();
ExoJDBC.totalScriptDeveloppeur();

// Affichage des métadonnées
ExoJDBC.afficherMetaData();
