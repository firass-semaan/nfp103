/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Sockets;

import Interfaces.VolInterface;
import Objets.Avion;
import Objets.Coordonnees;
import Objets.Deplacement;
import Objets.Message;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;

/**
 *
 * @author Firass Semaan
 */
public class Vol extends Thread {

    int ALTMAX = 20000;
    int ALTMIN = 0;
    int VITMAX = 1000;
    int VITMIN = 200;
    int PAUSE = 20000;//20 seconds 
    int PORT = 9876;
    String HOST = "PC";

    private Avion _avion = null;
    private Coordonnees _coord = null;
    private Deplacement _deplacement = null;
    private String _volName;

    private VolInterface _volFrame;
    private ObjectInputStream _inStream;
    private ObjectOutputStream _outStream;
    private Socket _clientSocket;

      Timer flightTimer;
    public Vol(VolInterface volFrame) {
        _volFrame = volFrame;
       
        try {
            if (!ouvrir_communication()) {
                _volFrame.txtInfo.append("Erreur lors de la connexion du serveur..");
                return;
            }
            _outStream = new ObjectOutputStream(_clientSocket.getOutputStream());
            _outStream.flush();
            _inStream = new ObjectInputStream(_clientSocket.getInputStream());
            initialiser_avion();
            _avion.setFlightId(_clientSocket.getLocalPort());

            Message msg = new Message("Info", "flight", _volName, _avion, "SACA");
            afficher_donnees();
            sendObject(msg);

            this.start();

            flightTimer = new Timer(PAUSE, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    se_deplacer();
                }
            });
            flightTimer.start();

        } catch (IOException ex) {
            Logger.getLogger(Vol.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendObject(Message message) {
        try {
            _outStream.reset();
            _outStream.writeObject(message);
            _outStream.flush();

        } catch (IOException ex) {
            Logger.getLogger(Vol.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendClosingObject() {
        try {
            _outStream.reset();
            Message msg = new Message("Connection closed..", "flight", _volName, _avion, "SACA");
            _outStream.writeObject(msg);
            _outStream.flush();
            if ( flightTimer != null)
                flightTimer.stop();

        } catch (IOException ex) {
            Logger.getLogger(Vol.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void receiveObject() {
        try {
            
            Object obj = _inStream.readObject();
            _volFrame.txtInfo.append("\n[SACA]>>>>[Moi]: Nouveau Message Recu..");
            handleMessage(obj);

        }
        catch(EOFException ex)
        {
            
        }
        catch (IOException ex) {
            Logger.getLogger(Vol.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Vol.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Private methods">
    private String getRandomFlightName() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String numbers = "0123456789";
        Random rand = new Random();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            buf.append(chars.charAt(rand.nextInt(chars.length())));
        }
        buf.append(numbers.charAt(rand.nextInt(numbers.length())));
        buf.append(numbers.charAt(rand.nextInt(numbers.length())));

        return buf.toString();
    }

    private Boolean ouvrir_communication() {

        try {
            _clientSocket = new Socket(HOST, PORT);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(VolInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public void fermer_communication() {
        try {
            // fonction qui permet de fermer la communication
            // avec le gestionnaire de vols
            sendClosingObject();
            _clientSocket.close();
            
            this.interrupt();
        } catch (IOException ex) {
            Logger.getLogger(VolInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void envoyer_caracteristiques() {
        // fonction qui envoie l'ensemble des caractéristiques
        // courantes de l'avion au gestionnaire de vols
        Message msg = new Message("VolInfo", "flight", _volName, _avion, "Radar");
        sendObject(msg);
    }

    private void handleMessage(Object message) {
        if (message == null) {
            _volFrame.txtInfo.append("\nMessage n'est pas valide ..");
            return;
        }

        Message msg = Message.class.cast(message);
        if (msg.type.equals("controller")) {
            if (msg.command.equals("changer_vitesse")) {
                _volFrame.txtInfo.append("\n[" + msg.sender + "] >> [ME]: Changement de vitesse.. ");
                changer_vitesse(msg.content.getDeplacement().getVitesse());
                Message newMessage = new Message("VitesseUpdated", "flight", _volName, msg.content, msg.sender);
                sendObject(newMessage);
                _volFrame.txtInfo.append("\n[ME]>>[" + msg.sender + "]: La vitesse est modifiée.. ");
                afficher_donnees();
                return;
            }
            if (msg.command.equals("changer_cap")) {
                _volFrame.txtInfo.append("\n[" + msg.sender + "] >> [ME]: Changement du cap.. ");
                changer_cap(msg.content.getDeplacement().getCap());
                Message newMessage = new Message("CapUpdated", "flight", _volName, msg.content, msg.sender);
                sendObject(newMessage);
                _volFrame.txtInfo.append("\n[ME]>>[" + msg.sender + "]: Cap est modifiée.. ");
                afficher_donnees();
                return;
            }
            if (msg.command.equals("changer_altitude")) {
                _volFrame.txtInfo.append("\n[" + msg.sender + "] >> [ME]: Changement d'altitude.. ");
                changer_altitude(msg.content.getPosition().getAltitude());
                Message newMessage = new Message("AltitudeUpdated", "flight", _volName, msg.content, msg.sender);
                sendObject(newMessage);
                _volFrame.txtInfo.append("\n[ME]>>[" + msg.sender + "]: Altitude est modifiée.. ");
                afficher_donnees();
                return;
            }
        }

    }

    /**
     * ******************************
     *** Fonctions gérant le déplacement de l'avion : ne pas modifier
     * ******************************
     */
    // initialise aléatoirement les paramétres initiaux de l'avion
    private void initialiser_avion() {
        // initialisation al�atoire du compteur aléatoire
        // intialisation des paramétres de l'avion
        _coord = new Coordonnees();
        _deplacement = new Deplacement();
        Random numberGenerator = new Random();
        _coord.setX((int) (1000 + numberGenerator.nextInt(100) % 1000));
        _coord.setY((int) (1000 + numberGenerator.nextInt(100) % 1000));
        _coord.setAltitude((int) (900 + numberGenerator.nextInt(100) % 100));
        _deplacement.setCap((int) (numberGenerator.nextInt(100) % 360));
        _deplacement.setVitesse((int) (600 + numberGenerator.nextInt(100) % 200));
        _volName = getRandomFlightName();
        _avion = new Avion(_coord, _deplacement, _volName);

        _volFrame.setTitle("Vol " + _volName);
    }

// modifie la valeur de l'avion avec la valeur passée en param�tre
    private void changer_vitesse(int vitesse) {
        if (vitesse < 0) {
            _deplacement.setVitesse(0);
        } else if (vitesse > VITMAX) {
            _deplacement.setVitesse(VITMAX);
        } else {
            _deplacement.setVitesse(vitesse);
        }
    }

// modifie le cap de l'avion avec la valeur passée en paramètre
    private void changer_cap(int cap) {
        if ((cap >= 0) && (cap < 360)) {
            _deplacement.setCap(cap);
        }
    }

// modifie l'altitude de l'avion avec la valeur passée en paramètre
    private void changer_altitude(int alt) {

        if (alt < 0) {
            _coord.setAltitude(0);
        } else if (alt > ALTMAX) {
            _coord.setAltitude(ALTMAX);
        } else {
            _coord.setAltitude(alt);
        }
    }

// affiche les caractéristiques courantes de l'avion
    private void afficher_donnees() {
        _volFrame.txtInfo.append("\n[ME]>> " + _avion.getInfo());
    }

// recalcule la localisation de l'avion en fonction de sa vitesse et de son cap
    private Boolean calcul_deplacement() {
        double cosinus, sinus;
        double dep_x, dep_y;

        if (_deplacement.getVitesse() < VITMIN) {
            _volFrame.txtInfo.append("\nVitesse trop faible : crash de l'avion\n");
            Message msg = new Message("Alert: crash de l'avion", "flight", _volName, _avion, "Radar");
            sendObject(msg);
            fermer_communication();
            return false;
        }
        if (_coord.getAltitude() == 0) {
            _volFrame.txtInfo.append("\nL'avion s'est ecrase au sol\n");
            Message msg = new Message("Alert: L'avion s'est ecrase au sol", "flight", _volName, _avion, "Radar");
            sendObject(msg);
            fermer_communication();
            return false;
        }
        //cos et sin ont un paramétre en radian, dep.cap en degré nos habitudes francophone
        /* Angle en radian = pi * (angle en degré) / 180 
       Angle en radian = pi * (angle en grade) / 200 
       Angle en grade = 200 * (angle en degré) / 180 
       Angle en grade = 200 * (angle en radian) / pi 
       Angle en degré = 180 * (angle en radian) / pi 
       Angle en degré = 180 * (angle en grade) / 200 
         */

        cosinus = cos(_deplacement.getCap() * 2 * Math.PI / 360);
        sinus = sin(_deplacement.getCap() * 2 * Math.PI / 360);

        //newPOS = oldPOS + Vt
        dep_x = cosinus * _deplacement.getVitesse() * 100 / VITMIN;
        dep_y = sinus * _deplacement.getVitesse() * 100 / VITMIN;

        // on se d�place d'au moins une case quels que soient le cap et la vitesse
        // sauf si cap est un des angles droit
        if ((dep_x > 0) && (dep_x < 1)) {
            dep_x = 1;
        }
        if ((dep_x < 0) && (dep_x > -1)) {
            dep_x = -1;
        }

        if ((dep_y > 0) && (dep_y < 1)) {
            dep_y = 1;
        }
        if ((dep_y < 0) && (dep_y > -1)) {
            dep_y = -1;
        }

        //printf(" x : %f y : %f\n", dep_x, dep_y);
        _coord.setX(_coord.getX() + (int) dep_x);
        _coord.setY(_coord.getY() + (int) dep_y);
        _avion.setPosition(_coord);
        afficher_donnees();
        return true;
    }

// fonction principale : gère l'exécution de l'avion au fil du temps
    private void se_deplacer() {
        if (calcul_deplacement()) {
            envoyer_caracteristiques();
        }
    }
    //</editor-fold>

    public void run() {
        while (true) {
    
            receiveObject();
        }
    }
}
