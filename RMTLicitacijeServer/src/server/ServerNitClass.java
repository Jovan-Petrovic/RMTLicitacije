/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import com.google.gson.*;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import korisnikServer.KarticaClass;
import korisnikServer.KorisnikClass;
import proizvodServer.KnjigaClass;
import proizvodServer.KozmetikaClass;
import proizvodServer.KozmetikaClass.Pol;
import proizvodServer.KucniAparatiClass;
import proizvodServer.MuzickaOpremaClass;
import proizvodServer.MuzickaOpremaClass.KategorijaMuzickeOpreme;
import proizvodServer.ProizvodClass;
import proizvodServer.SportskaOpremaClass;
import proizvodServer.StavkaProizvodaClass;
import static server.LicitacijaClass.indeksPoslednjegOdgovora;
import static server.LicitacijaClass.korisniciULicitaciji;

/**
 *
 * @author Luka
 */
public class ServerNitClass extends Thread {

    BufferedReader ulazniTokOdKlijenta = null;
    PrintStream izlazniTokKaKlijentu = null;
    Socket soketZaKomunikaciju = null;
    static LinkedList<KorisnikClass> registrovaniKorisnici = new LinkedList<KorisnikClass>();
    LinkedList<KorisnikClass> onlineKorisnici = new LinkedList<KorisnikClass>();
    static LinkedList<StavkaProizvodaClass> proizvodiUBazi = new LinkedList<StavkaProizvodaClass>();
    // username-ovi korisnika koji su u licitaciji!
    static LinkedList<String> usernameULicitaciji = new LinkedList<String>();
    String username = null;
    int brojKorisnika = 0;
    int brojProizvoda = 0;
    ServerNitClass[] klijentiNiti = new ServerNitClass[20];
    int pocetniZaIspisProizvoda = 0;
    static LinkedList<String> lideri = new LinkedList<String>();
    static LinkedList<String> vecPitan = new LinkedList<String>();
    static LicitacijaClass licitacija;
    public double stanjeNaRacunu;
    static LinkedList<TransakcijaClass> transakcije = new LinkedList<TransakcijaClass>();
          

    ServerNitClass(Socket klijentSoket, LinkedList<KorisnikClass> korisnici, LinkedList<StavkaProizvodaClass> poizvodi, ServerNitClass[] klijenti,int i,LinkedList<TransakcijaClass> transakcije) {
        soketZaKomunikaciju = klijentSoket;
        registrovaniKorisnici = korisnici;
        brojKorisnika = korisnici.size() + 1;
        proizvodiUBazi = poizvodi;
        if (proizvodiUBazi == null) {
            brojProizvoda = 0;
            proizvodiUBazi = new LinkedList<StavkaProizvodaClass>();
        } else {
            brojProizvoda = poizvodi.size() + 1;
        }
        klijentiNiti = klijenti;
        pocetniZaIspisProizvoda = i;
        if (transakcije != null) {
            ServerNitClass.transakcije = transakcije;
        }
        if (klijenti[1] == null) {
            LicitacijaClass.trenutnoLicitiraniProizvod = proizvodiUBazi.getFirst();
        }
    }
    
    private void Licitacija() {
        if (LicitacijaClass.korisniciULicitaciji.size() == 0) {
            izlazniTokKaKlijentu.println(LicitacijaClass.licitacijaPrvog(username));

            while (LicitacijaClass.korisniciULicitaciji.size() < 2) {
                //ovde mozemo malko da usporimo sa nekim timeout-om
            }
        }
                
        if (!LicitacijaClass.pocelaLicitacija) {
            LicitacijaClass.Licitiranje(klijentiNiti, username);
        } else {               //moze malo da se uspori
            LicitacijaClass.drzanjeNaCekanju(this);
        }
   
        umanjivanjeIznosaNaRacunu();
  
        

        while (proizvodiUBazi != null) {
            LicitacijaClass.osvezenRazunPobednika = false;
            LicitacijaClass.dodatNovacVlasniku = false;
            
            LicitacijaClass.trenutnoLicitiraniProizvod = sledeciProizvodZaLicitaciju();
            LicitacijaClass.klasicnaLicitacija(klijentiNiti);
             LicitacijaClass.uspesnaTransakcija = false;
            umanjivanjeIznosaNaRacunu();
         
        }
    }
    
    public boolean trebaDaBudeUklonjen(){
        if(LicitacijaClass.korisniciZaUklanjanje.contains(username)){
            LicitacijaClass.korisniciULicitaciji.remove(username);
            LicitacijaClass.korisniciZaUklanjanje.remove(username);
            return true;
        }
        return false;
    }
    
    
    public void upisivanjeTransakcije(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        TransakcijaClass novaTransakcija = new TransakcijaClass(LicitacijaClass.trenutnoLicitiraniProizvod.getVlasnik(),
                LicitacijaClass.pobednik, LicitacijaClass.trenutnoLicitiraniProizvod.getProizvod().getNazivProizvoda(),
                LicitacijaClass.trenutnaCena, timeStamp);
        transakcije.add(novaTransakcija);
        upisivanjeTransakcijeUJson();
    }
    
    public void upisivanjeTransakcijeUJson(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            FileWriter upisivac = new FileWriter("files/transakcije.json");
            String transakcijaUString = gson.toJson(transakcije);

            upisivac.write(transakcijaUString);

            upisivac.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    public void osvezavanjeBazeProizvoda() {
        RuntimeTypeAdapterFactory<ProizvodClass> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                .of(ProizvodClass.class, "type")
                .registerSubtype(KnjigaClass.class, "knjigaClass")
                .registerSubtype(SportskaOpremaClass.class, "sportskaOpremaClass")
                .registerSubtype(KozmetikaClass.class, "kozmetikaClass")
                .registerSubtype(MuzickaOpremaClass.class, "muzickaOpremaClass")
                .registerSubtype(KucniAparatiClass.class, "kucniAparatiClass");

        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

        try {
            FileWriter upisivac = new FileWriter("files/proizvodi.json");
            String proizvodUString = gson.toJson(proizvodiUBazi);

            upisivac.write(proizvodUString);

            upisivac.close();
            return;
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    public void umanjivanjeIznosaNaRacunu() {
        for (KorisnikClass korisnik : registrovaniKorisnici) {
            if (korisnik.getUsername().equals(LicitacijaClass.pobednik) && !LicitacijaClass.osvezenRazunPobednika) {
                double trenutniIznos = korisnik.getKarticeKorisnika().getIznos();
                if (korisnik.getKarticeKorisnika().getTipKartice() == KarticaClass.TipKartice.Kreditna) {
                    korisnik.getKarticeKorisnika().setIznos(trenutniIznos - LicitacijaClass.trenutnaCena);
                    osvezavanjeBazeKorisnika();
                    LicitacijaClass.osvezenRazunPobednika = true;
                    LicitacijaClass.uspesnaTransakcija = true;
                    dodavanjeNovcaNaRacunVlasnika();
                } else {
                    if (trenutniIznos < LicitacijaClass.trenutnaCena) {
                        LicitacijaClass.uspesnaTransakcija = false;
                        for (int i = 0; i < klijentiNiti.length; i++) {
                            if (klijentiNiti[i] != null) {
                                if (indeksPoslednjegOdgovora < i && korisniciULicitaciji.contains(klijentiNiti[i].username)) {
                                    klijentiNiti[i].izlazniTokKaKlijentu.println("Transakcija neuspela zbog nedovoljno novca na racunu. Proizvod ce naknadno biti ponovo na licitaciji.");
                                    indeksPoslednjegOdgovora++;
                                }
                            }
                        }
                    } else {
                        korisnik.getKarticeKorisnika().setIznos(trenutniIznos - LicitacijaClass.trenutnaCena);
                        osvezavanjeBazeKorisnika();
                        LicitacijaClass.osvezenRazunPobednika = true;
                        LicitacijaClass.uspesnaTransakcija = true;
                        dodavanjeNovcaNaRacunVlasnika();
                    }
                }
            }
        }
    }
    
    public void dodavanjeNovcaNaRacunVlasnika(){
        for (KorisnikClass korisnik:registrovaniKorisnici) {
            if(korisnik.getUsername().equals(LicitacijaClass.trenutnoLicitiraniProizvod.getVlasnik()) && !LicitacijaClass.dodatNovacVlasniku){
                double trenutanIznos =korisnik.getKarticeKorisnika().getIznos();
                korisnik.getKarticeKorisnika().setIznos(trenutanIznos+(LicitacijaClass.trenutnaCena*0.95));
               KorisnikClass admin = adminObjekat();
                if(admin != null){
                     double trenutniIznos = admin.getKarticeKorisnika().getIznos();
                     admin.getKarticeKorisnika().setIznos(trenutniIznos+(LicitacijaClass.trenutnaCena*0.05));
                }
               upisivanjeTransakcije();
                LicitacijaClass.dodatNovacVlasniku = true;
                osvezavanjeBazeKorisnika();
            }
        }
        
    }
    public KorisnikClass adminObjekat(){
        for(KorisnikClass korisnik:registrovaniKorisnici){
            if(korisnik.getUsername().equals("admin")){
                return korisnik;
            }
        }
        return null;
    }

    public void osvezavanjeBazeKorisnika() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            FileWriter upisivac = new FileWriter("files/korisnici.json");
            String korisnikUString = gson.toJson(registrovaniKorisnici);

            upisivac.write(korisnikUString);

            upisivac.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public StavkaProizvodaClass sledeciProizvodZaLicitaciju() {
        StavkaProizvodaClass prodatProizvod = null;
        StavkaProizvodaClass elementZaPrebacivanje = null;
        if (LicitacijaClass.prodatProizvod && LicitacijaClass.uspesnaTransakcija) {
            prodatProizvod = proizvodiUBazi.pop();

            //napraviti metode za pamcenje prozivoda za korisnika
        } else {
            elementZaPrebacivanje = proizvodiUBazi.pop();
            proizvodiUBazi.addLast(elementZaPrebacivanje);
            //pomeri na kraj prvi i predji na sledeci
        }
        osvezavanjeBazeProizvoda();
        LicitacijaClass.prodatProizvod = false;
        return proizvodiUBazi.getFirst();
    }

    public void LicitacijaC() {
        StavkaProizvodaClass trenutnoLicitirani = null;

        usernameULicitaciji.add(username);
        for (int i = 0; i < klijentiNiti.length; i++) {
            if (klijentiNiti[i] != null && usernameULicitaciji.contains(klijentiNiti[i].username)) {
                klijentiNiti[i].izlazniTokKaKlijentu.println("Klijent " + username + " je tu!");
            }
        }
        trenutnoLicitirani = proizvodiUBazi.getFirst();
        
        while (proizvodiUBazi != null) {
            double trenutnaCena = trenutnoLicitirani.getCena();
            
            for (int i = pocetniZaIspisProizvoda; i < klijentiNiti.length; i++) {
                if (klijentiNiti[i] != null && usernameULicitaciji.contains(klijentiNiti[i].username)) {
                    klijentiNiti[i].izlazniTokKaKlijentu.println(trenutnoLicitirani.toString());
                    klijentiNiti[i].izlazniTokKaKlijentu.println("Pocetna cena proizvoda iznosi: " + trenutnoLicitirani.getCena());
                }
            }
            //
            // do ovde radi!!!
  
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            do {
 
            for(int i = 0;i < klijentiNiti.length;i++){
                 if (klijentiNiti[i] != null && usernameULicitaciji.contains(klijentiNiti[i].username) && !vecPitan.contains(klijentiNiti[i].username)) {
                    vecPitan.add(klijentiNiti[i].username);
                    klijentiNiti[i].izlazniTokKaKlijentu.println("zelite li da prihvatite " + trenutnaCena + "?");
                    String izbor = "";
                    try {
                        izbor = ulazniTokOdKlijenta.readLine();
                    } catch (IOException ex) {
                        Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (izbor.toLowerCase().equals("da")) {
                        for (int j = 0; j < klijentiNiti.length; j++) {
                            if (klijentiNiti[j] != null && usernameULicitaciji.contains(klijentiNiti[j].username)) {
                                klijentiNiti[j].izlazniTokKaKlijentu.println("Klijent " + username + " je licitirao!"+trenutnoLicitirani.proizvod.getIDProizvoda());
                                lideri.add(klijentiNiti[i].username);
                            }
                        }
                    } else {
                        if (lideri.contains(klijentiNiti[i].username)) {
                            lideri.remove(klijentiNiti[i].username);
                        }
                    }
                }
            }
            trenutnaCena = povecavanjeCene(trenutnaCena);
            vecPitan = new LinkedList<String>();
           }while (lideri.size() > 1);
            
            for (int i = 0; i < lideri.size(); i++) {
                System.out.println(lideri.get(i));
            }

            //doradicemo sta i kako
            //proizvodiUBazi.removeFirst();
            //trenutnoLicitirani = proizvodiUBazi.getFirst();
            
            // konflikt resen manuelno, proba commitovanja
        }
    }

    private boolean validacijaUsername(String usernameTemp) {
        if (usernameTemp.length() < 4) {
            return false;
        }
        return true;

    }

    private boolean validacijaPassword(String passwordTemp) {
        if (passwordTemp.length() < 8 || passwordTemp.toLowerCase().equals(passwordTemp)) {
            return false;
        }

        for (int i = 0; i < passwordTemp.length(); i++) {
            if (passwordTemp.charAt(i) >= 48 && passwordTemp.charAt(i) <= 57) {
                return true;
            }
        }

        return false;
    }

    private void dodavanjeNovogKorisnikaUFile() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            FileWriter upisivac = new FileWriter("files/korisnici.json");
            String korisnikUString = gson.toJson(registrovaniKorisnici);

            upisivac.write(korisnikUString);

            upisivac.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void prikaziSveArtikle() {
        for (StavkaProizvodaClass stavka : proizvodiUBazi) {
            izlazniTokKaKlijentu.println(stavka.toString());
        }
    }

    private void prikaziKnjige() {
        for (StavkaProizvodaClass stavka : proizvodiUBazi) {
            if (stavka.proizvod instanceof KnjigaClass) {
                izlazniTokKaKlijentu.println(stavka.toString());
            }
        }
    }

    private void prikaziKozmetiku() {
        for (StavkaProizvodaClass stavka : proizvodiUBazi) {
            if (stavka.proizvod instanceof KozmetikaClass) {
                izlazniTokKaKlijentu.println(stavka.toString());
            }
        }
    }

    private void prikaziKucneAparate() {
        for (StavkaProizvodaClass stavka : proizvodiUBazi) {
            if (stavka.proizvod instanceof KucniAparatiClass) {
                izlazniTokKaKlijentu.println(stavka.toString());
            }
        }
    }

    private void prikaziMuzickuOpremu() {
        for (StavkaProizvodaClass stavka : proizvodiUBazi) {
            if (stavka.proizvod instanceof MuzickaOpremaClass) {
                izlazniTokKaKlijentu.println(stavka.toString());
            }
        }
    }

    private void prikaziSportskuOpremu() {
        for (StavkaProizvodaClass stavka : proizvodiUBazi) {
            if (stavka.proizvod instanceof SportskaOpremaClass) {
                izlazniTokKaKlijentu.println(stavka.toString());
            }
        }
    }

    private void razgledanjeArtikala() {
        boolean razgledanje = true;
        izborRazgledanjeMeni izbor = izborRazgledanjeMeni.Nedefinisano;
        while (razgledanje) {
            zaglavljeRazgledanja();
            String izborTemp = "";
            try {
                izborTemp = ulazniTokOdKlijenta.readLine();
            } catch (IOException ex) {
                Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                izbor = izborRazgledanjeMeni.valueOf(izborTemp);
            } catch (Exception e) {
                System.out.println("Verovatno je primljen broj");
            }
            if (izbor == izborRazgledanjeMeni.Sve || izborTemp.equals("1")) {
                izlazniTokKaKlijentu.println("SVE");
                prikaziSveArtikle();
                izbor = izborRazgledanjeMeni.Nedefinisano;
            } else if (izbor == izborRazgledanjeMeni.Knjige || izborTemp.equals("2")) {
                izlazniTokKaKlijentu.println("Knjige");
                prikaziKnjige();
                izbor = izborRazgledanjeMeni.Nedefinisano;
            } else if (izbor == izborRazgledanjeMeni.Kozmetika || izborTemp.equals("3")) {
                izlazniTokKaKlijentu.println("Kozmetika");
                prikaziKozmetiku();
                izbor = izborRazgledanjeMeni.Nedefinisano;
            } else if (izbor == izborRazgledanjeMeni.KucniAparati || izborTemp.equals("4") || izborTemp.equals("Kucni aparati")) {
                izlazniTokKaKlijentu.println("Kucni aparati");
                prikaziKucneAparate();
                izbor = izborRazgledanjeMeni.Nedefinisano;
            } else if (izbor == izborRazgledanjeMeni.MuzickaOprema || izborTemp.equals("5") || izborTemp.equals("Muzicka oprema")) {
                izlazniTokKaKlijentu.println("Muzicka oprema");
                prikaziMuzickuOpremu();
                izbor = izborRazgledanjeMeni.Nedefinisano;
            } else if (izbor == izborRazgledanjeMeni.SportskaOprema || izborTemp.equals("6") || izborTemp.equals("Sportska oprema")) {
                izlazniTokKaKlijentu.println("Sportska oprema");
                prikaziSportskuOpremu();
                izbor = izborRazgledanjeMeni.Nedefinisano;
            } else if (izbor == izborRazgledanjeMeni.Odjava || izborTemp.equals("7")) {
                razgledanje = false;
                izbor = izborRazgledanjeMeni.Nedefinisano;
                return;
            } else {
                System.out.println("Neispravan unos!");
                izbor = izborRazgledanjeMeni.Nedefinisano;
            }
        }

    }

    public void zaglavljeRazgledanja() {
        izlazniTokKaKlijentu.println("Razgledajte:"
                + "\n1.Sve"
                + "\n"
                + "\n***Razgledajte i po kategorijama sa sledecim izborima:***"
                + "\n2.Knjige"
                + "\n3.Kozmetika"
                + "\n4.Kucni aparati"
                + "\n5.Muzicka oprema"
                + "\n6.Sportska oprema"
                + "\n"
                + "\n7.Odjava"
                + "\nUnesite Vas izbor:");
    }

    public void zaglavljeZaDodavanjeProizvoda() {
        izlazniTokKaKlijentu.println("Unesite broj za odgovarajucu kategoriju proizvoda:"
                + "\n1. Knjige"
                + "\n2. Kozmetika"
                + "\n3. Kucni aparati"
                + "\n4. Muzicka oprema"
                + "\n5. Sportska oprema"
                + "\n6. Ostalo"
                + "\n7. Odjava"
                + "\nUnesite izbor:");

    }

    private void dodavanjeNovogProizvoda() {
        int izbor = -1;
        String izborS = "";
        while (izbor != 7) {
            zaglavljeZaDodavanjeProizvoda();
            try {
                izborS = ulazniTokOdKlijenta.readLine();
            } catch (IOException ex) {
                Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                izbor = Integer.parseInt(izborS);
            } catch (Exception e) {
                izlazniTokKaKlijentu.println("Neispravan format unosa zelje!");
            }
            if (izbor == 1) {
                dodavanjeKnjige();
                return;
            } else if (izbor == 2) {
                dodavanjeKozmetike();
                return;
            } else if (izbor == 3) {
                dodavanjeKucnihAparata();
                return;
            } else if (izbor == 4) {
                dodavanjeMuzickeOpreme();
                return;
            } else if (izbor == 5) {
                dodavanjeSportskeOpreme();
                return;
            } else if (izbor == 6) {
                dodavanjeOstalihProizvoda();
                return;
            } else {
                return;
            }
        }
    }

    private void dodavanjeKnjige() {
        String nazivKnjige = "";
        String autorKnjige = "";
        String izdavacKnjige = "";
        String godinaIzdanja = "nepoznato";
        int godinaIzdanjaInt = 0;
        double pocetnaCena = 1000;
        String pocetnaCenaString = "";
        izlazniTokKaKlijentu.println("Unosenje informacija o knjizi"
                + "\nUnesite naziv knjige: ");
        try {
            nazivKnjige = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite autora knjige: ");
        try {
            autorKnjige = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite izdavaca knjige: ");
        try {
            izdavacKnjige = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite godinu izdanja ako je poznata, ako ne napisite nepoznato: ");
        try {
            godinaIzdanja = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            godinaIzdanjaInt = Integer.parseInt(godinaIzdanja);
        } catch (Exception e) {
            godinaIzdanja = "nepoznata";
        }
        izlazniTokKaKlijentu.println("Unesite pocetnu cenu (NAPOMENA: Ukoliko je cena uneta nepravilo automatski se nudi po ceni od 1000 dinara):");
        try {
            pocetnaCenaString = ulazniTokOdKlijenta.readLine();
            pocetnaCena = Double.parseDouble(pocetnaCenaString);
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
            pocetnaCena = 1000;
        }
        KnjigaClass novaKnjiga = null;
        for(StavkaProizvodaClass stavka:proizvodiUBazi){
            if(stavka.getProizvod().getIDProizvoda() > brojProizvoda){
                brojProizvoda = stavka.getProizvod().getIDProizvoda();
            }
        }
        brojProizvoda++;
        if (godinaIzdanja.equals("nepoznata")) {
            novaKnjiga = new KnjigaClass(autorKnjige, izdavacKnjige, -1, brojProizvoda, nazivKnjige);
        } else {
            novaKnjiga = new KnjigaClass(autorKnjige, izdavacKnjige, godinaIzdanjaInt, brojProizvoda, nazivKnjige);
        }
        StavkaProizvodaClass noviProizvod = new StavkaProizvodaClass(novaKnjiga, username, pocetnaCena, null);
        proizvodiUBazi.add(noviProizvod);

        dodavanjeProizvodaUBazu();
    }

    public void dodavanjeProizvodaUBazu() {

        RuntimeTypeAdapterFactory<ProizvodClass> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                .of(ProizvodClass.class, "type")
                .registerSubtype(KnjigaClass.class, "knjigaClass")
                .registerSubtype(SportskaOpremaClass.class, "sportskaOpremaClass")
                .registerSubtype(KozmetikaClass.class, "kozmetikaClass")
                .registerSubtype(MuzickaOpremaClass.class, "muzickaOpremaClass")
                .registerSubtype(KucniAparatiClass.class, "kucniAparatiClass");

        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

        try {
            FileWriter upisivac = new FileWriter("files/proizvodi.json");
            String proizvodUString = gson.toJson(proizvodiUBazi);

            upisivac.write(proizvodUString);
            izlazniTokKaKlijentu.println("Uspesan unos proizvoda za licitaciju!");
            upisivac.close();
            return;
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Neuspesno unosenje proizvoda za licitaciju!");
    }

    private void dodavanjeKozmetike() {
        String nazivProizvoda = "";
        String proizvodjac = "";
        String namena = "";
        String pocetnaCenaString = "";
        double pocetnaCena = 1000;
        Pol namenjenZaPol = Pol.Zenski;
        izlazniTokKaKlijentu.println("Unosenje informacija o kozmetici"
                + "\nUnesite naziv proizvoda: ");
        try {
            nazivProizvoda = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite naziv proizvodjaca:");
        try {
            proizvodjac = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Da li je prozivod namenjen zenskom polu (DA-Da-dA-da ukoliko jeste):");
        try {
            namena = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (namena.toLowerCase().equals("da")) {

        } else {
            namenjenZaPol = Pol.Muski;
        }
        for (StavkaProizvodaClass stavka : proizvodiUBazi) {
            if (stavka.getProizvod().getIDProizvoda() > brojProizvoda) {
                brojProizvoda = stavka.getProizvod().getIDProizvoda();
            }
        }
        brojProizvoda++;
        KozmetikaClass noviProizvod = new KozmetikaClass(proizvodjac, namenjenZaPol, brojProizvoda, nazivProizvoda);
        izlazniTokKaKlijentu.println("Unesite pocetnu cenu (NAPOMENA: Ukoliko je cena uneta nepravilo automatski se nudi po ceni od 1000 dinara):");
        try {
            pocetnaCenaString = ulazniTokOdKlijenta.readLine();
            pocetnaCena = Double.parseDouble(pocetnaCenaString);
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
            pocetnaCena = 1000;
        }
        StavkaProizvodaClass novaStavka = new StavkaProizvodaClass(noviProizvod, username, pocetnaCena, null);

        proizvodiUBazi.add(novaStavka);

        dodavanjeProizvodaUBazu();
    }

    private void dodavanjeKucnihAparata() {
        String nazivProizvoda = "";
        String proizvodjac = "";
        String godinaProizvodnjeString = "nepoznata";
        int godinaProizvodnje = 0;
        String pocetnaCenaString = "";
        double pocetnaCena = 1000;
        KucniAparatiClass noviProizvod = null;
        izlazniTokKaKlijentu.println("Unosenje informacija o kucnom aparatu"
                + "\nUnesite naziv proizvoda: ");
        try {
            nazivProizvoda = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite naziv proizvodjaca:");
        try {
            proizvodjac = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite godinu proizvodnje ako je poznata, ako ne napisite nepoznato: ");
        try {
            godinaProizvodnjeString = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            godinaProizvodnje = Integer.parseInt(godinaProizvodnjeString);
        } catch (Exception e) {
            godinaProizvodnjeString = "nepoznata";
        }
                for(StavkaProizvodaClass stavka:proizvodiUBazi){
            if(stavka.getProizvod().getIDProizvoda() > brojProizvoda){
                brojProizvoda = stavka.getProizvod().getIDProizvoda();
            }
        }
        brojProizvoda++;
        if (godinaProizvodnjeString.equals("nepoznata")) {
            noviProizvod = new KucniAparatiClass(proizvodjac, -1, brojProizvoda, nazivProizvoda);
        } else {
            noviProizvod = new KucniAparatiClass(proizvodjac, godinaProizvodnje, brojProizvoda, nazivProizvoda);
        }
        izlazniTokKaKlijentu.println("Unesite pocetnu cenu (NAPOMENA: Ukoliko je cena uneta nepravilo automatski se nudi po ceni od 1000 dinara):");
        try {
            pocetnaCenaString = ulazniTokOdKlijenta.readLine();
            pocetnaCena = Double.parseDouble(pocetnaCenaString);
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
            pocetnaCena = 1000;
        }

        StavkaProizvodaClass novaStavka = new StavkaProizvodaClass(noviProizvod, username, pocetnaCena, null);

        proizvodiUBazi.add(novaStavka);

        dodavanjeProizvodaUBazu();
    }

    private void dodavanjeMuzickeOpreme() {
        String nazivProizvoda = "";
        String proizvodjac = "";
        String godinaProizvodnjeString = "nepoznata";
        int godinaProizvodnje = 0;
        String pocetnaCenaString = "";
        double pocetnaCena = 1000;
        String kategorijaString = "";
        KategorijaMuzickeOpreme kategorija = KategorijaMuzickeOpreme.MuzickaPloca;
        MuzickaOpremaClass noviProizvod;

        izlazniTokKaKlijentu.println("Unosenje informacija o muzickoj opremi"
                + "\nUnesite naziv proizvoda: ");
        try {
            nazivProizvoda = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite naziv proizvodjaca:");
        try {
            proizvodjac = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite godinu proizvodnje ako je poznata, ako ne napisite nepoznato: ");
        try {
            godinaProizvodnjeString = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            godinaProizvodnje = Integer.parseInt(godinaProizvodnjeString);
        } catch (Exception e) {
            godinaProizvodnjeString = "nepoznata";
        }
        izlazniTokKaKlijentu.println("Unesite kategoriju proizvoda (MuzickaPloca,MuzickiCD,MuzickiInstrument,MuzickiAlbum)");
        try {
            kategorijaString = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            kategorija = KategorijaMuzickeOpreme.valueOf(kategorijaString);
        } catch (Exception e) {
            izlazniTokKaKlijentu.println("Pogresan unos kategorije!");
            return;
        }
        for (StavkaProizvodaClass stavka : proizvodiUBazi) {
            if (stavka.getProizvod().getIDProizvoda() > brojProizvoda) {
                brojProizvoda = stavka.getProizvod().getIDProizvoda();
            }
        }
        brojProizvoda++;
        noviProizvod = new MuzickaOpremaClass(proizvodjac, kategorija, godinaProizvodnje, brojProizvoda, nazivProizvoda);

        izlazniTokKaKlijentu.println("Unesite pocetnu cenu (NAPOMENA: Ukoliko je cena uneta nepravilo automatski se nudi po ceni od 1000 dinara):");
        try {
            pocetnaCenaString = ulazniTokOdKlijenta.readLine();
            pocetnaCena = Double.parseDouble(pocetnaCenaString);
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
            pocetnaCena = 1000;
        }

        StavkaProizvodaClass novaStavka = new StavkaProizvodaClass(noviProizvod, username, pocetnaCena, null);

        proizvodiUBazi.add(novaStavka);

        dodavanjeProizvodaUBazu();
    }

    private void dodavanjeSportskeOpreme() {
        String nazivProizvoda = "";
        String marka = "";
        String model = "";
        String pocetnaCenaString = "";
        double pocetnaCena = 1000;
        SportskaOpremaClass noviProizvod = null;

        izlazniTokKaKlijentu.println("Unosenje informacija o sportskoj opremi"
                + "\nUnesite naziv proizvoda: ");
        try {
            nazivProizvoda = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite marku proizvodjaca:");
        try {
            marka = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite model proizvoda:");
        try {
            model = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (StavkaProizvodaClass stavka : proizvodiUBazi) {
            if (stavka.getProizvod().getIDProizvoda() > brojProizvoda) {
                brojProizvoda = stavka.getProizvod().getIDProizvoda();
            }
        }
        brojProizvoda++;

        noviProizvod = new SportskaOpremaClass(marka, model, brojProizvoda, nazivProizvoda);

        izlazniTokKaKlijentu.println("Unesite pocetnu cenu (NAPOMENA: Ukoliko je cena uneta nepravilo automatski se nudi po ceni od 1000 dinara):");
        try {
            pocetnaCenaString = ulazniTokOdKlijenta.readLine();
            pocetnaCena = Double.parseDouble(pocetnaCenaString);
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
            pocetnaCena = 1000;
        }

        StavkaProizvodaClass novaStavka = new StavkaProizvodaClass(noviProizvod, username, pocetnaCena, null);

        proizvodiUBazi.add(novaStavka);

        dodavanjeProizvodaUBazu();
    }

    private void dodavanjeOstalihProizvoda() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private double povecavanjeCene(double trenutnaCena) {
        return 1.20 * trenutnaCena;
    }


    public enum izborLogMeni {
        Prijava,
        Registracija,
        Izlaz
    }

    public enum izborPrijavljenMeni {
        Razgledanje,
        Licitacija,
        Dopuna,
        Stanje,
        Istorija,
        DodavanjeNovogProizovda,
        Odjava,
        Nedefinisano
    }

    public enum izborRazgledanjeMeni {
        Sve,
        Knjige,
        Kozmetika,
        KucniAparati,
        MuzickaOprema,
        SportskaOprema,
        Odjava,
        Nedefinisano
    }

    @Override
    public void run() {
        try {
            ulazniTokOdKlijenta = new BufferedReader(new InputStreamReader(soketZaKomunikaciju.getInputStream()));
            izlazniTokKaKlijentu = new PrintStream(soketZaKomunikaciju.getOutputStream());
            //obezbedi da moze meni sa brojem
            izborLogMeni izbor;
            boolean prijavljen = false;
            do {
                izbor = izborLogMeni.Izlaz;
                prijavljivanjeKorisnikaMeni();
                String izborTemp = ulazniTokOdKlijenta.readLine();
                try {
                    izbor = izborLogMeni.valueOf(izborTemp);
                } catch (Exception e) {
                    System.out.println("Unet broj kao izbor");
                }
                if (izbor == izborLogMeni.Registracija || izborTemp.equals("2")) {
                    registracijaKorisnika();
                    izbor = izborLogMeni.Registracija;
                }
                if (izbor == izborLogMeni.Prijava || izborTemp.equals("1")) {
                    prijavljen = prijavaKorisnika();
                    izbor = izborLogMeni.Prijava;
                }
                if (izbor == izborLogMeni.Izlaz || izborTemp.equals("3")) {
                    izlazniTokKaKlijentu.println("Dovidjenja!");
                    return;
                }
            } while (!prijavljen);
            // izlazniTokKaKlijentu.println(izbor);
            izborPrijavljenMeni izborPrijavljen;
            boolean odjava = true;
            do {
                izborPrijavljen = izborPrijavljenMeni.Nedefinisano;
                prijavljenKorisnikMeni();
                String izborTemp = ulazniTokOdKlijenta.readLine();
                //try?catch,treba srediti da moze i sa brojem da se bira
                //u sustini, parsirati u int i onda ubaciti dodatni uslov u ifu da moze i broj
                try {
                    izborPrijavljen = izborPrijavljenMeni.valueOf(izborTemp);
                } catch (Exception e) {
                    System.out.println("Unet je broj kao izbor");
                }

                if (izborPrijavljen == izborPrijavljenMeni.Stanje || izborTemp.equals("4")) {
                    proveraStanja();
                    izborPrijavljen = izborPrijavljenMeni.Stanje;
                } else if (izborPrijavljen == izborPrijavljenMeni.Dopuna || izborTemp.equals("3")) {
                    dopunaRacuna();
                    izborPrijavljen = izborPrijavljenMeni.Dopuna;
                } else if (izborPrijavljen == izborPrijavljenMeni.Razgledanje || izborTemp.equals("1")) {
                    razgledanjeArtikala();
                    izborPrijavljen = izborPrijavljenMeni.Razgledanje;
                } else if (izborPrijavljen == izborPrijavljenMeni.Licitacija || izborTemp.equals("2")) {
                    Licitacija();
                    izborPrijavljen = izborPrijavljenMeni.Licitacija;
                } else if (izborPrijavljen == izborPrijavljenMeni.Istorija || izborTemp.equals("5")) {
                    istorija();
                    izborPrijavljen = izborPrijavljenMeni.Istorija;
                } else if (izborPrijavljen == izborPrijavljenMeni.DodavanjeNovogProizovda || izborTemp.equals("6")) {
                    dodavanjeNovogProizvoda();
                    izborPrijavljen = izborPrijavljenMeni.DodavanjeNovogProizovda;
                } else if (izborPrijavljen == izborPrijavljenMeni.Odjava || izborTemp.equals("7")) {
                    izlazniTokKaKlijentu.println("Dovidjenja!");
                    System.out.println(username + " se diskonektovao!");
                    return;
                } else {
                    izlazniTokKaKlijentu.println("Greska u unosu");
                }
            } while (odjava);
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void prijavljivanjeKorisnikaMeni() {
        izlazniTokKaKlijentu.println("Unesite:"
                + "\n1.Prijava"
                + "\n2.Registracija"
                + "\n3.Izlaz"
                + "\nUnesite Vas izbor:");
    }

    public void prijavljenKorisnikMeni() {
        izlazniTokKaKlijentu.println("Unesite:"
                + "\n1.Razgledanje"
                + "\n2.Licitacija"
                + "\n3.Dopuna"
                + "\n4.Stanje"
                + "\n5.Istorija"
                + "\n6.Dodaj novi proizvod"
                + "\n7.Odjava"
                + "\nUnesite Vas izbor:");
    }
    
    public void istorija(){
        for(TransakcijaClass transakcija:transakcije){
          if(transakcija.getKupacUsername().equals(username) || transakcija.getProdavacUsername().equals(username)){
              izlazniTokKaKlijentu.println(transakcija.toString());
          }  
        }
    }

    public void registracijaKorisnika() {
        String imePrezime = "prazan";
        String username = "prazan";
        String password = "prazan";
        izlazniTokKaKlijentu.println("Unesite ime i prezime: ");
        try {
            imePrezime = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite korisnicko ime koje zelite da koristite: ");
        try {
            //omoguceno da unosi username, sve dok ne unese od 4+ karaktera
            username = ulazniTokOdKlijenta.readLine();
            boolean validanUsername = validacijaUsername(username);
            while (!validanUsername) {
                izlazniTokKaKlijentu.println("Username mora imati minimum 4 karaktera. Pokusajte ponovo!");
                username = ulazniTokOdKlijenta.readLine();
                validanUsername = validacijaUsername(username);
            }
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        izlazniTokKaKlijentu.println("Unesite lozinku koju zelite da koristite: ");
        try {
            //omoguceno da unosi sifru sve dok ne ispuni ogranicenja
            password = ulazniTokOdKlijenta.readLine();
            boolean validanPassword = validacijaPassword(password);
            while (!validanPassword) {
                izlazniTokKaKlijentu.println("Password mora imati minimum 8 karaktera, barem jedno veliko slovo i barem jednu cifru. Pokusajte ponovo!");
                password = ulazniTokOdKlijenta.readLine();
                validanPassword = validacijaPassword(password);
            }
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!validacija(imePrezime, username, password)) {
            KorisnikClass noviKorisnik = new KorisnikClass(brojKorisnika++, imePrezime, username, password);
            registrovaniKorisnici.add(noviKorisnik);
            String brojKartice = null;
            String tipKartice = null;

            izlazniTokKaKlijentu.println("Unesite broj kartice koju zelite da koristite: ");
            try {
                brojKartice = ulazniTokOdKlijenta.readLine();
            } catch (IOException ex) {
                Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
            }
            izlazniTokKaKlijentu.println("Unesite tip kartice koju zelite da koristite (1-Debitna,2-Kreditna):");
            try {
                tipKartice = ulazniTokOdKlijenta.readLine();
            } catch (IOException ex) {
                Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
            }
            //try?catch
            int IDKartice = Integer.parseInt(brojKartice);
            KarticaClass novaKartica = null;
            if (tipKartice.equals("1")) {
                novaKartica = new KarticaClass(IDKartice, KarticaClass.TipKartice.Debitna, 2000);
            } else {
                novaKartica = new KarticaClass(IDKartice, KarticaClass.TipKartice.Kreditna, 2000);
            }
            for (KorisnikClass k : registrovaniKorisnici) {
                if (noviKorisnik.IDKorisnika == k.IDKorisnika) {
                    k.setKarticeKorisnika(novaKartica);
                }
            }

            System.out.println(noviKorisnik.getUsername() + " se registrovao!");
            dodavanjeNovogKorisnikaUFile();
            izlazniTokKaKlijentu.println("Uspesno ste se registrovali!");
        } else {
            izlazniTokKaKlijentu.println("Neuspesna registracija!");
        }
    }

    public boolean validacija(String str1, String str2, String str3) {
        return str1.equals("prazan") || str2.equals("prazan") || str3.equals("prazan");
    }

    private boolean prijavaKorisnika() {
        String usernameTemp = null;
        String password = null;
        String passwordTemp = null;
        boolean kontrolaPrijave = true;
        izlazniTokKaKlijentu.println("Unesite korisnicko ime koje koristite: ");
        try {
            usernameTemp = ulazniTokOdKlijenta.readLine();

            //minimum 4 karaktera
            //validacijaUsername(usernameTemp);
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        kontrolaPrijave = kontrolaPrijave && proveraUsername(usernameTemp);
        if (!kontrolaPrijave) {
            return false;
        }
        password = nadjiPassword(usernameTemp);
        izlazniTokKaKlijentu.println("Unesite lozniku koju koristite: ");
        try {
            passwordTemp = ulazniTokOdKlijenta.readLine();

            //minimum 8 karaktera, jedno veliko slovo i cifra
            //validacijaPassword(passwordTemp);
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        kontrolaPrijave = kontrolaPrijave && password.equals(passwordTemp);
        if (!kontrolaPrijave) {
            izlazniTokKaKlijentu.println("Uneli ste pogresu lozinku. Pokusajte ponovo:  ");
            passwordTemp = null;
            kontrolaPrijave = true;
            try {
                passwordTemp = ulazniTokOdKlijenta.readLine();
            } catch (IOException ex) {
                Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        kontrolaPrijave = kontrolaPrijave && password.equals(passwordTemp);
        if (!kontrolaPrijave) {
            izlazniTokKaKlijentu.println("Neuspesna prijava!");
            return false;
        }
        KorisnikClass k = null;
        for (KorisnikClass korisnik : registrovaniKorisnici) {
            if (korisnik.getUsername().equals(usernameTemp)) {
                k = korisnik;
                break;
            }
        }
        onlineKorisnici.add(k);
        System.out.println(k.getImePrezimeKorisnika() + " se konektovao!");
        username = k.getUsername();
        stanjeNaRacunu = k.getKarticeKorisnika().getIznos();
        izlazniTokKaKlijentu.println("Uspesna prijava!");
        return true;
    }

    private String nadjiPassword(String username) {
        for (KorisnikClass k : registrovaniKorisnici) {
            if (k.getUsername().equals(username)) {
                return k.getPassword();
            }
        }
        return null;
    }

    private boolean proveraUsername(String username) {
        for (KorisnikClass k : registrovaniKorisnici) {
            if (k.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    private void proveraStanja() {
        izlazniTokKaKlijentu.println("IDKartice" + "\t" + "Iznos");
        for (KorisnikClass k : registrovaniKorisnici) {
            if (k.username.equals(username)) {
                    izlazniTokKaKlijentu.println(k.getKarticeKorisnika().getIDKartice() + "\t" + k.getKarticeKorisnika().getIznos());
            }
        }
    }

    private void dopunaRacuna() {
        String tempBrojKartice = null;
        String tempIznos = null;
        int IDKartice;
        double iznos;
        izlazniTokKaKlijentu.println("Unesite broj kartice: ");
        try {
            tempBrojKartice = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        //try?catch
        IDKartice = Integer.parseInt(tempBrojKartice);
        izlazniTokKaKlijentu.println("Unesite iznos za dopunu: ");
        try {
            tempIznos = ulazniTokOdKlijenta.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ServerNitClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        //try?catch
        iznos = Double.parseDouble(tempIznos);
        for (KorisnikClass korisnik : registrovaniKorisnici) {
            if (korisnik.getUsername().equals(username)) {
                double trenutnoStanje = korisnik.getKarticeKorisnika().getIznos();
                korisnik.getKarticeKorisnika().setIznos(trenutnoStanje+iznos);
            }
        }
        izlazniTokKaKlijentu.println("Neuspesna dopuna!");
    }
}
