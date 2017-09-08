/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Sockets;

import Interfaces.CtrlInterface;
import Interfaces.VolInterface;
import Objets.Avion;
import Objets.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;

/**
 *
 * @author Firass Semaan
 */
public class Controleur extends Thread {

    int PORT = 9876;
    String HOST = "PC";
    boolean _keeprunning = true;
    private String _controllerName;
    private CtrlInterface _controllerFrame;
    private ObjectInputStream _inStream;
    private ObjectOutputStream _outStream;
    private Socket _clientSocket;
    public static List<Avion> _detectedFlights;

    public  void UpdateFlightCap(int volId, int cap) throws InterruptedException {
        if (_detectedFlights == null) {
            return;
        }
        Optional<Avion> optionalFlight = _detectedFlights.stream().filter(f -> f.getFlightId() == volId).findFirst();

        if (optionalFlight == null) {
            return;
        }
        Avion selectedFlight = optionalFlight.get();

        selectedFlight.Lock();
       
        selectedFlight.getDeplacement().setCap(cap);
        Message msg = new Message("changer_cap", "controller", _controllerName, selectedFlight, selectedFlight.getFlightName());
        SendObject(msg);
    }

    public void UpdateFlightAltitude(int volId, int altitude) throws InterruptedException {
        if (_detectedFlights == null) {
            return;
        }
        Optional<Avion> optionalFlight = _detectedFlights.stream().filter(f -> f.getFlightId() == volId).findFirst();

        if (optionalFlight == null) {
            return;
        }

        Avion selectedFlight = optionalFlight.get();

        selectedFlight.Lock();
        
        selectedFlight.getPosition().setAltitude(altitude);
        Message msg = new Message("changer_altitude", "controller", _controllerName, selectedFlight, selectedFlight.getFlightName());
        SendObject(msg);
    }

    public void UpdateFlightVitesse(int volId, int vitesse) throws InterruptedException {
        if (_detectedFlights == null) {
            return;
        }
        Optional<Avion> optionalFlight = _detectedFlights.stream().filter(f -> f.getFlightId() == volId).findFirst();

        if (optionalFlight == null) {
            return;
        }
        Avion selectedFlight = optionalFlight.get();

        selectedFlight.Lock();
        
        selectedFlight.getDeplacement().setVitesse(vitesse);
        Message msg = new Message("changer_vitesse", "controller", _controllerName, selectedFlight, selectedFlight.getFlightName());
        SendObject(msg);
    }

    public Controleur(CtrlInterface controllerFrame) {
        _controllerFrame = controllerFrame;
      
        try {
            if (!ouvrir_communication()) {
                _controllerFrame.setTitle("Erreur lors de la connexion au SACA..");
                return;
            }
            
            _outStream = new ObjectOutputStream(_clientSocket.getOutputStream());
            _outStream.flush();
            _inStream = new ObjectInputStream(_clientSocket.getInputStream());
            _controllerName = "Controller " + _clientSocket.getLocalPort();
           
            _controllerFrame.setTitle("\nControleur est connecte' au port " +_clientSocket.getLocalPort());
            List<Avion> dd = _detectedFlights;
            Message msg = new Message("Info", "Controller", _controllerName, null, "SACA");
            SendObject(msg);
            this.start();

        } catch (IOException ex) {
            Logger.getLogger(Controleur.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

  

    public void SendObject(Message message) {
        try {
            _outStream.reset();
            _outStream.writeObject(message);
            _outStream.flush();
        } catch (IOException ex) {
            Logger.getLogger(Controleur.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void ReceiveObject() {
        try {
            if (_clientSocket == null || _clientSocket.isClosed()) {
                return;
            }
            Object obj = _inStream.readObject();
          
            handleMessage(obj);
        } catch (IOException ex) {
           
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Controleur.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleMessage(Object message) {
        if (message == null) {
            return;
        }
        if (message instanceof Message) {
            Message msg = Message.class.cast(message);
            if (msg.command.contains("Info")) {
                UpdateFlight(msg);

            } else if (msg.command.contains("Updated")) {
                Avion updatedAvion = UpdateFlight(msg);
                if (updatedAvion != null) {
                    updatedAvion.Unlock();
                    
                }
            } else {
                // avion ecraser ou sur sol
                RemoveFlight(msg);
            }
        }
        
    }

    private void RemoveFlight(Message msg) {

        if (_detectedFlights == null) {
            _detectedFlights = new ArrayList();
        }
        Optional<Avion> optionalFlight = _detectedFlights.stream().filter(f -> f.getFlightId() == msg.content.getFlightId()).findFirst();
        if (optionalFlight != null && optionalFlight.isPresent()) {
            _detectedFlights.remove(optionalFlight.get());

        }
        DefaultListModel listModel = new DefaultListModel();

        for (int i = 0; i < _detectedFlights.size(); i++) {
            listModel.addElement(_detectedFlights.get(i).getInfo());
        }
        _controllerFrame.lstVols.setModel(listModel);
    }

    private Avion UpdateFlight(Message msg) {

        if (_detectedFlights == null) {
            _detectedFlights = new ArrayList();
        }
        Avion updatedAv = null;
        Optional<Avion> optionalFlight = _detectedFlights.stream().filter(f -> f.getFlightId() == msg.content.getFlightId()).findFirst();
        if (optionalFlight != null && optionalFlight.isPresent()) {
            updatedAv = optionalFlight.get();
            updatedAv.setDeplacement(msg.content.getDeplacement());
            updatedAv.setPosition(msg.content.getPosition());

        } else {
            _detectedFlights.add(msg.content);
            updatedAv = msg.content;
        }
        DefaultListModel listModel = new DefaultListModel();

        for (int i = 0; i < _detectedFlights.size(); i++) {
            listModel.addElement(_detectedFlights.get(i).getInfo());
        }
        _controllerFrame.lstVols.setModel(listModel);
        return updatedAv;
    }

    private Boolean ouvrir_communication() {

        try {
            _clientSocket = new Socket(HOST, PORT);
            return true;

        } catch (IOException ex) {
            Logger.getLogger(VolInterface.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public void fermer_communication() {
        try {
            // fonction qui permet de fermer la communication
            // avec le gestionnaire de vols
            _controllerFrame.setTitle("\nController closed");
            _keeprunning = false;
            sendClosingObject();
            if (_clientSocket != null) {
                _clientSocket.close();
            }
            if (_inStream != null) {
                _inStream.close();
            }
            if (_outStream != null) {
                _outStream.close();
            }
            this.interrupt();

        } catch (IOException ex) {
            Logger.getLogger(VolInterface.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendClosingObject() {
        try {
            _outStream.reset();
            Object objid = _clientSocket.getLocalPort();
            Message msg = new Message("Connection closed..", "controller", _controllerName, null, objid.toString());
            _outStream.writeObject(msg);
            _outStream.flush();

        } catch (IOException ex) {
            Logger.getLogger(Vol.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void run() {
        while (_keeprunning) {
            ReceiveObject();
        }
    }
}
