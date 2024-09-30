/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ma.projet.services;

import java.sql.Statement;
import ma.projet.connexion.Connexion;

/**
 *
 * @author DELL
 */
public class DevDataserv {
    
     public static void createTable() {
        try {
            // Créer une requête SQL pour créer la table
            String req = "CREATE TABLE DevData ("
                    + "Developpeurs VARCHAR(32), "
                    + "Jour CHAR(11), "
                    + "NbScripts INTEGER)";
            
            // Créer un statement
            Statement st = Connexion.getCn().createStatement();
            
            // Exécuter la requête
            
            st.executeUpdate(req);
            System.out.println("Création de la table avec sucèes");
            
        } catch (Exception e) {
            System.out.println("Erreur  " + e.getMessage());
        }
    }
     
    
    public static void insertData() {
        try {
            Statement st = Connexion.getCn().createStatement();
            
            // Insértion des données dans la table
            
            String req1 = "INSERT INTO DevData VALUES ('ALAMI', 'Lundi', 1)";
            String req2 = "INSERT INTO DevData VALUES ('WAFI', 'Lundi', 2)";
            String req3 = "INSERT INTO DevData VALUES ('SALAMI', 'Mardi', 9)";
            String req4 = "INSERT INTO DevData VALUES ('SAFI', 'Mardi', 2)";
            String req5 = "INSERT INTO DevData VALUES ('ALAMI', 'Mardi', 2)";
            String req6 = "INSERT INTO DevData VALUES ('SEBIHI', 'Mercredi',2)";
            String req7 = "INSERT INTO DevData VALUES ('WAFI', 'Jeudi',3)";
            String req8 = "INSERT INTO DevData VALUES ('ALAOUI', 'Vendredi',9)";
            String req9 = "INSERT INTO DevData VALUES ('WAFI', 'VENDREDI',3)";
            String req10 = "INSERT INTO DevData VALUES ('SEBIHI', 'VENDREDI',4)";
            
            // Exécution des requêtes d'insertion
            
            st.executeUpdate(req1);
            st.executeUpdate(req2);
            st.executeUpdate(req3);
            st.executeUpdate(req4);
            st.executeUpdate(req5);
            st.executeUpdate(req6);
            st.executeUpdate(req7);
            st.executeUpdate(req8);
            st.executeUpdate(req9);
            st.executeUpdate(req10);
            
            System.out.println("Données insérées");
        } catch (Exception e) {
            System.out.println("Erreur lors de l'insertion des données : " + e.getMessage());
        }
    }
    
}
