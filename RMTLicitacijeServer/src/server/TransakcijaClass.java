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
    public StavkaProizvodaClass proizvodUTransakciji;
    public double prodajnaCena;
     public String vremeTransakcije;

    @Override
    public String toString() {
        return "TransakcijaClass{" + "prodavacUsername=" + prodavacUsername + ", kupacUsername=" + kupacUsername + ", proizvodUTransakciji=" + proizvodUTransakciji + ", prodajnaCena=" + prodajnaCena + ", vremeTransakcije=" + vremeTransakcije + '}';
    }

    public void setProdavacUsername(String prodavacUsername) {
        this.prodavacUsername = prodavacUsername;
    }

    public void setKupacUsername(String kupacUsername) {
        this.kupacUsername = kupacUsername;
    }

    public void setProizvodUTransakciji(StavkaProizvodaClass proizvodUTransakciji) {
        this.proizvodUTransakciji = proizvodUTransakciji;
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

    public StavkaProizvodaClass getProizvodUTransakciji() {
        return proizvodUTransakciji;
    }

    public double getProdajnaCena() {
        return prodajnaCena;
    }

    public String getVremeTransakcije() {
        return vremeTransakcije;
    }

    public TransakcijaClass(String prodavacUsername, String kupacUsername, StavkaProizvodaClass proizvodUTransakciji, double prodajnaCena, String vremeTransakcije) {
        this.prodavacUsername = prodavacUsername;
        this.kupacUsername = kupacUsername;
        this.proizvodUTransakciji = proizvodUTransakciji;
        this.prodajnaCena = prodajnaCena;
        this.vremeTransakcije = vremeTransakcije;
    }
   
}
