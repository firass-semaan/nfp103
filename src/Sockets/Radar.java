/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Sockets;

import Interfaces.RadarInterface;
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
public class Radar extends Thread {

    int PORT = 9876;
    String HOST = "PC";
    boolean _keepRunning = true;
    private String _radarName;
    private RadarInterface _radarFrame;
    private ObjectInputStream _inStream;
    private ObjectOutputStream _outStream;
    private Socket _clientSocket;
    public List<Avion> _detectedFlights;

    public Radar(RadarInterface radarFrame) {
        _radarFrame = radarFrame;
        
        try {
            if (!ouvrir_communication()) {
                _radarFrame.setTitle("Erreur lors de la connexion au serveur..");
                return;
            }
            
            _outStream = new ObjectOutputStream(_clientSocket.getOutputStream());
            _outStream.flush();
            _inStream = new ObjectInputStream(_clientSocket.getInputStream());
            _radarFrame.setTitle("Radar est connecte' au port : "+_clientSocket.getLocalPort());
            _radarName = "Radar " + _clientSocket.getLocalPort();
            
            Message msg = new Message("Info", "radar", _radarName, null, "SACA");
            SendObject(msg);
            this.start();

        } catch (IOException ex) {
            Logger.getLogger(Radar.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void FillFlightsList(Object lstClients) {

        if (lstClients != null) {
            _detectedFlights = (ArrayList) lstClients;
            DefaultListModel listModel = new DefaultListModel();
            for (int i = 0; i < _detectedFlights.size(); i++) {
                listModel.addElement(_detectedFlights.get(i).getInfo());

            }
            _radarFrame.lstVols.setModel(listModel);
        }
    }

    private void SendObject(Message message) {
        try {
            _outStream.reset();
            _outStream.writeObject(message);
            _outStream.flush();
        } catch (IOException ex) {
            Logger.getLogger(Radar.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Object ReceiveFlightsList() {
        try {
            return _inStream.readObject();
        } catch (IOException ex) {
            Logger.getLogger(Radar.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Radar.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void ReceiveObject() {
        try {
            if (_clientSocket == null || _clientSocket.isClosed() || !_keepRunning) {
                return;
            }
            Object obj = _inStream.readObject();
            
            handleMessage(obj);
        } catch (IOException ex) {
           
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Radar.class.getName()).log(Level.SEVERE, null, ex);
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
            } else {
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
        _radarFrame.lstVols.setModel(listModel);
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
        _radarFrame.lstVols.setModel(listModel);
        return updatedAv;
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

    private void sendClosingObject() {
        try {
            _outStream.reset();
            Object objid = _clientSocket.getLocalPort();
            Message msg = new Message("Connection closed..", "controller", _radarName, null, objid.toString());
            _outStream.writeObject(msg);
            _outStream.flush();

        } catch (IOException ex) {
            Logger.getLogger(Vol.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void fermer_communication() {
        try {
            // fonction qui permet de fermer la communication
            // avec le gestionnaire de vols
            _radarFrame.setTitle("Radar is closed");
            _keepRunning = false;
            sendClosingObject();
            _inStream.close();
            _clientSocket.close();
            this.interrupt();
        } catch (IOException ex) {
            Logger.getLogger(VolInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void run() {
        while (_keepRunning) {

            ReceiveObject();
        }
    }
}
