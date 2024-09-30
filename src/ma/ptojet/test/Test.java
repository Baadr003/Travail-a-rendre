/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ma.ptojet.test;

import ma.projet.exojdbc.ExoJDBC;
import static ma.projet.exojdbc.ExoJDBC.totalScriptsSemaine;
import ma.projet.services.DevDataserv;
import static ma.projet.services.DevDataserv.createTable;
import static ma.projet.services.DevDataserv.insertData;


public class Test {
    public static void main(String[] args) {
        
       // Création de la table DevData
        //createTable();
       
       //Insertion des données
     // insertData();
       
      // Max des scripts par jour 
      //ExoJDBC.MaxScriptQuot();
        
      //Triage dans l’ordre décroissant selon leur nombre de scripts
    // ExoJDBC.Triage();
        
        //NB Totale des scripts en une semaine 
        //totalScriptsSemaine();
        
        //NB totale des scripts pour chaque developpeurs
      //  ExoJDBC.totalScriptDeveloppeur();
        ExoJDBC.afficherMetaData();
    }
}
