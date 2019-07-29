/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 *
 * @author Luka
 */
public class PrviKlijentULicitacijiClass {
   // public static LicitacijaClass trenutnaLicitacija; 
    
    public static void licitacijaPrvog(String username){
      LicitacijaClass.korisniciULicitaciji.addLast(username);
      System.out.println("Molimo sacekate dok se ne pojavi jos neko u licitaciji!");
    }
}
