/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public static LinkedList<String> korisniciNaCekanju = new LinkedList<String>();
    public static boolean prodatProizvod = false;
    

   
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
       trenutnaCena = trenutnoLicitiraniProizvod.getCena();
        
       // pocelaLicitacija = true;
        if (!korisniciULicitaciji.contains(username)) {
            korisniciULicitaciji.add(username);
        }

        nudjenjeProizvoda(klijenti);
        String izbor = "";
        indeksPoslednjegOdgovora = -1;
        do {
            if(indeksPoslednjegOdgovora != -1){
                trenutnaCena = povecanaCena(trenutnaCena);
            }
            indeksPoslednjegOdgovora = -1;
            nadmetanje(klijenti, trenutnaCena);
        } while (lideriULicitaciji.size() > 1);
        indeksPoslednjegOdgovora = -1;
        
        
        if (lideriULicitaciji.size() == 0) {
            for (int i = 0; i < klijenti.length; i++) {
                if (klijenti[i] != null) {
                    if (indeksPoslednjegOdgovora < i && korisniciULicitaciji.contains(klijenti[i].username)) {
                        klijenti[i].izlazniTokKaKlijentu.println("Nema zainteresovanih za proizvod!");
                        indeksPoslednjegOdgovora++;
                    }
                }
            }
        }else{
            String pobednik = lideriULicitaciji.getFirst();
            for (int i = 0; i < klijenti.length; i++) {
                if (klijenti[i] != null) {
                    if (indeksPoslednjegOdgovora < i && korisniciULicitaciji.contains(klijenti[i].username)) {
                        klijenti[i].izlazniTokKaKlijentu.println("Pobedio je: "+pobednik+" i kupio prizvod: "+trenutnoLicitiraniProizvod.toString()+" po ceni od: "+trenutnaCena);
                        indeksPoslednjegOdgovora++;
                        prodatProizvod = true;
                    }
                }
            }
        }
        
        lideriULicitaciji = null;
        lideriULicitaciji = new LinkedList<String>();
        indeksPoslednjegOdgovora = -1;
        pocelaLicitacija = false;
        trenutnaCena = 0;
        for(String users:korisniciNaCekanju){
            if(!korisniciULicitaciji.contains(users)){
                korisniciULicitaciji.add(users);
            }
        }
        korisniciNaCekanju = null;
        korisniciNaCekanju = new LinkedList<String>();
        //ovde pitati hoce li jos da se igraju
    }
    
    public static void klasicnaLicitacija(ServerNitClass[] niti){
        trenutnaCena = trenutnoLicitiraniProizvod.getCena();
        
        nudjenjeProizvoda(niti);
        
        indeksPoslednjegOdgovora = -1;
        do {
            if(indeksPoslednjegOdgovora != -1){
                trenutnaCena = povecanaCena(trenutnaCena);
            }
            indeksPoslednjegOdgovora = -1;
            nadmetanje(niti, trenutnaCena);
        } while (lideriULicitaciji.size() > 1);
        indeksPoslednjegOdgovora = -1;
        
        
        if (lideriULicitaciji.size() == 0) {
            for (int i = 0; i < niti.length; i++) {
                if (niti[i] != null) {
                    if (indeksPoslednjegOdgovora < i && korisniciULicitaciji.contains(niti[i].username)) {
                        niti[i].izlazniTokKaKlijentu.println("Nema zainteresovanih za proizvod!");
                        indeksPoslednjegOdgovora++;
                    }
                }
            }
        }else{
            String pobednik = lideriULicitaciji.getFirst();
            for (int i = 0; i < niti.length; i++) {
                if (niti[i] != null) {
                    if (indeksPoslednjegOdgovora < i && korisniciULicitaciji.contains(niti[i].username)) {
                        niti[i].izlazniTokKaKlijentu.println("Pobedio je: "+pobednik+" i kupio prizvod: "+trenutnoLicitiraniProizvod.toString()+" po ceni od: "+trenutnaCena);
                        indeksPoslednjegOdgovora++;
                        prodatProizvod = true;
                    }
                }
            }
        }
        
        lideriULicitaciji = null;
        lideriULicitaciji = new LinkedList<String>();
        indeksPoslednjegOdgovora = -1;
        pocelaLicitacija = false;
        trenutnaCena = 0;
        for(String users:korisniciNaCekanju){
            if(!korisniciULicitaciji.contains(users)){
                korisniciULicitaciji.add(users);
            }
        }
        korisniciNaCekanju = null;
        korisniciNaCekanju = new LinkedList<String>();
        //ovde pitati hoce li jos da se igraju
        
    }
    
    
    
    public static void drzanjeNaCekanju(ServerNitClass nit) {
        nit.izlazniTokKaKlijentu.println("Cekajte dok se ne zavrsi zapoceta licitacija!");
        korisniciNaCekanju.add(nit.username);
        while (pocelaLicitacija) {
        }
    }
    
    public static double povecanaCena(double iznos){
        return 1.2*iznos;
    }
    
    public static void nadmetanje(ServerNitClass[] klijenti,double trenutnaCena){
        String izbor="";
        for (int j = 0; j < klijenti.length; j++) {
            if (klijenti[j] != null) {
                if (indeksPoslednjegOdgovora < j && korisniciULicitaciji.contains(klijenti[j].username)) {
                    klijenti[j].izlazniTokKaKlijentu.println("Zelite li da prihvatite cenu od (da):" + trenutnaCena);
                    try {
                        izbor = klijenti[j].ulazniTokOdKlijenta.readLine();
                        pocelaLicitacija = true;
                    } catch (IOException ex) {
                        Logger.getLogger(LicitacijaClass.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (izbor.toLowerCase().equals("da")) {
                        if (!lideriULicitaciji.contains(klijenti[j].username)) {
                            lideriULicitaciji.add(klijenti[j].username);
                        }
                    } else {
                        if (lideriULicitaciji.contains(klijenti[j].username)) {
                            lideriULicitaciji.remove(klijenti[j].username);
                        }
                    }
                    indeksPoslednjegOdgovora++;
                }
            }
        }
    }
    
    
    public static void nudjenjeProizvoda(ServerNitClass[] klijenti){
         for (int i = 0; i < klijenti.length; i++) {
            if (klijenti[i] != null) {
                if (indeksPoslednjegOdgovora < i && korisniciULicitaciji.contains(klijenti[i].username)) {
                    klijenti[i].izlazniTokKaKlijentu.println(trenutnoLicitiraniProizvod.toString());
                    klijenti[i].izlazniTokKaKlijentu.println("Pocetna cena proizvoda iznosi: " + trenutnoLicitiraniProizvod.getCena());
                    indeksPoslednjegOdgovora++;
                }
            }
        }
         LicitacijaClass.pocelaLicitacija = true;
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
