/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import proizvodServer.StavkaProizvodaClass;

/**
 *
 * @author Luka
 */
public class TransakcijaClass {
    public String prodavacUsername;
    public String kupacUsername;
    public String nazivProzivoda;
    public double prodajnaCena;

    @Override
    public String toString() {
        return "TransakcijaClass{" + "prodavacUsername=" + prodavacUsername + ", kupacUsername=" + kupacUsername + ", nazivProzivoda=" + nazivProzivoda + ", prodajnaCena=" + prodajnaCena + ", vremeTransakcije=" + vremeTransakcije + '}';
    }
     public String vremeTransakcije;

    public void setNazivProzivoda(String nazivProzivoda) {
        this.nazivProzivoda = nazivProzivoda;
    }

    public String getNazivProzivoda() {
        return nazivProzivoda;
    }

 

    public void setProdavacUsername(String prodavacUsername) {
        this.prodavacUsername = prodavacUsername;
    }

    public void setKupacUsername(String kupacUsername) {
        this.kupacUsername = kupacUsername;
    }



    public void setProdajnaCena(double prodajnaCena) {
        this.prodajnaCena = prodajnaCena;
    }

    public void setVremeTransakcije(String vremeTransakcije) {
        this.vremeTransakcije = vremeTransakcije;
    }

    public String getProdavacUsername() {
        return prodavacUsername;
    }

    public String getKupacUsername() {
        return kupacUsername;
    }


    public double getProdajnaCena() {
        return prodajnaCena;
    }

    public String getVremeTransakcije() {
        return vremeTransakcije;
    }

    public TransakcijaClass(String prodavacUsername, String kupacUsername, String nazivProizvoda, double prodajnaCena, String vremeTransakcije) {
        this.prodavacUsername = prodavacUsername;
        this.kupacUsername = kupacUsername;
       this.nazivProzivoda = nazivProizvoda;
        this.prodajnaCena = prodajnaCena;
        this.vremeTransakcije = vremeTransakcije;
    }
   
}
