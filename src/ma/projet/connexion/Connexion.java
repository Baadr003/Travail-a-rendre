
package ma.projet.connexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Connexion {
    
    private static String url = "jdbc:mysql://localhost:3306/travailarendre";
    private static String login = "root";
    private static String password = "";
    private static Connection cn = null;

    public static Connection getCn() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            cn = DriverManager.getConnection(url, login, password);
            
            
        } catch (ClassNotFoundException ex) {   
            System.out.println("Driver introuvable");
        } catch (SQLException ex) {
            Logger.getLogger(Connexion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return cn;
    }
    
}
