/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ma.projet.exojdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import ma.projet.connexion.Connexion;


public class ExoJDBC {
    
    public static void MaxScriptQuot() {
    try {
        Statement stmt = Connexion.getCn().createStatement();
        String req = "SELECT Developpeurs, jour, max(NbScripts)"
                      +"from devdata"
                      +" group by jour" ;
        
        ResultSet rs = stmt.executeQuery(req);
        
        while (rs.next()) {
            System.out.println( rs.getString("Developpeurs") + " a réalisé le maximum de scripts le " + rs.getString("Jour") + " avec " + rs.getInt("max(NbScripts)") + " scripts.");
        }
        
    } catch (Exception e) {
        System.out.println("Erreur lors de recherche " + e.getMessage());
    }
}
    
    public static void Triage() {
    
        try {
            Statement stmt = Connexion.getCn().createStatement();
            String req = "SELECT Developpeurs, SUM(NbScripts) AS c  "
                         + "FROM DevData "
                         + "GROUP BY Developpeurs "
                         + "ORDER BY c DESC";
            
            ResultSet rs = stmt.executeQuery(req);
            
            while(rs.next()) {
                
                System.out.println(rs.getString("Developpeurs") + " a réalisé " + rs.getInt("c") + " scripts au total");
            
            }
        } catch (Exception e) {
            System.out.println("Erreur lors du tri: " + e.getMessage());
        }
        
}
    public static void totalScriptsSemaine() {
    try {
        // Créer une connexion et une requête SQL pour obtenir le total des scripts réalisés en une semaine
        Statement st = Connexion.getCn().createStatement();
        String req = "SELECT SUM(NbScripts) AS ts "
                   + "FROM devdata";

        ResultSet rs = st.executeQuery(req);
        
        if (rs.next()) {
            
            System.out.println("Le nombre total de scripts réalisés en une semaine est : " + rs.getInt("ts"));
        }
    
    } catch (Exception e) {
        System.out.println("Erreur: " + e.getMessage());
    }
    }
    
    public static void totalScriptDeveloppeur () {
        try {
            Statement st = Connexion.getCn().createStatement();
            String req = "SELECT Developpeurs, SUM(NbScripts) AS ts "
                         + "FROM DevData "
                         + "GROUP BY Developpeurs "
                         + "ORDER BY ts DESC";
            
           ResultSet rs = st.executeQuery(req);
            
            while (rs.next()) {
                System.out.println(rs.getString("Developpeurs") + " a réalisé " + rs.getInt("ts") + " scripts au total.");
            }
        } catch (Exception e) {
            System.out.println("Erreur lors du tri: " + e.getMessage());
        }
    }
    
    public static void afficherMetaData() {
        try {
    

            // Création du Statement
            Statement st = Connexion.getCn().createStatement();
           String req="SELECT * FROM DevData";
            // Exécution de la requête
            ResultSet rs = st.executeQuery(req);

            // Obtention des métadonnées
            ResultSetMetaData rsmd = rs.getMetaData();

            // Affichage des informations des colonnes
            int nombreColonnes = rsmd.getColumnCount();
            for (int i = 1; i <= nombreColonnes; i++) {
                System.out.println("Colonne " + i + ": " + rsmd.getColumnName(i) + " (" + rsmd.getColumnTypeName(i) + ")");
            }
                
            // Affichage des données ligne par ligne
            while (rs.next()) {
                for (int i = 1; i <= nombreColonnes; i++) {
                    System.out.print(rs.getString(i) + " ");
                    
                }
                System.out.println();
                
            }
            System.out.println(" Le nombre de colonne est : "+nombreColonnes);
        } catch (Exception e) {
           System.out.println("Erreur " + e.getMessage());
        }
    }
}