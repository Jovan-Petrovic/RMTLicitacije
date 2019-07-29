/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.util.LinkedList;
import proizvodServer.StavkaProizvodaClass;

/**
 *
 * @author Luka
 */
public class LicitacijaClass {
    public static StavkaProizvodaClass trenutnoLicitiraniProizvod = null;
    public static double trenutnaCena = 0;
    public static  LinkedList<String> korisniciULicitaciji = new LinkedList<String>();
    public static int indeksPoslednjegOdgovora = -1;
    public static LinkedList<String> lideriULicitaciji = new LinkedList<String>();
    public static boolean pocelaLicitacija = false;
    public static boolean imaJosProizvoda = true;

   
    public LicitacijaClass() {
    }
    // ovo pozivivam kada mi se prvi klijent konektuje u konstruktoru
    public LicitacijaClass(StavkaProizvodaClass trenutni){
        trenutnoLicitiraniProizvod = trenutni;
        this.trenutnaCena = trenutni.getCena();
        korisniciULicitaciji = new LinkedList<String>();
        indeksPoslednjegOdgovora = -1;
        lideriULicitaciji = new LinkedList<String>();
    }
 
    public static String licitacijaPrvog(String username) {
        LicitacijaClass.korisniciULicitaciji.addLast(username);
        return "Molimo sacekate dok se ne pojavi jos neko u licitaciji!";
    }
    
    public static void Licitiranje(ServerNitClass[] klijenti,String username){
        pocelaLicitacija = true;
        korisniciULicitaciji.addLast(username);
        for (int i = 0; i < klijenti.length; i++) {
            if (klijenti[i] != null) {
                if (indeksPoslednjegOdgovora < i && korisniciULicitaciji.contains(klijenti[i].username)) {
                    klijenti[i].izlazniTokKaKlijentu.println(trenutnoLicitiraniProizvod.toString());
                    klijenti[i].izlazniTokKaKlijentu.println("Pocetna cena proizvoda iznosi: " + trenutnoLicitiraniProizvod.getCena());
                    indeksPoslednjegOdgovora++;
                }
            }
           

        }
        
        indeksPoslednjegOdgovora = -1;
        for (int j = 0; j < klijenti.length; j++) {
            if (klijenti[j] != null && indeksPoslednjegOdgovora < j && korisniciULicitaciji.contains(klijenti[j].username)) {
                System.out.println("Zelite li da prihvatite cenu od " + trenutnaCena);
                indeksPoslednjegOdgovora++;
            }
        }
        indeksPoslednjegOdgovora++;
    }
    

    public static StavkaProizvodaClass getTrenutnoLicitiraniProizvod() {
        return trenutnoLicitiraniProizvod;
    }

    public static double getTrenutnaCena() {
        return trenutnaCena;
    }

    public static LinkedList<String> getKorisniciULicitaciji() {
        return korisniciULicitaciji;
    }

    public static int getIndeksPoslednjegOdgovora() {
        return indeksPoslednjegOdgovora;
    }

    public static LinkedList<String> getLideriULicitaciji() {
        return lideriULicitaciji;
    }

    public static void setTrenutnoLicitiraniProizvod(StavkaProizvodaClass trenutnoLicitiraniProizvod) {
        LicitacijaClass.trenutnoLicitiraniProizvod = trenutnoLicitiraniProizvod;
    }

    public static void setTrenutnaCena(double trenutnaCena) {
        LicitacijaClass.trenutnaCena = trenutnaCena;
    }

    public static void setKorisniciULicitaciji(LinkedList<String> korisniciULicitaciji) {
        LicitacijaClass.korisniciULicitaciji = korisniciULicitaciji;
    }

    public static void setIndeksPoslednjegOdgovora(int indeksPoslednjegOdgovora) {
        LicitacijaClass.indeksPoslednjegOdgovora = indeksPoslednjegOdgovora;
    }

    public static void setLideriULicitaciji(LinkedList<String> lideriULicitaciji) {
        LicitacijaClass.lideriULicitaciji = lideriULicitaciji;
    }
    
    
}
