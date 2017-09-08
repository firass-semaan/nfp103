/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objets;

import java.io.Serializable;

/**
 *
 * @author Firass Semaan
 */
public class Coordonnees implements Serializable{
    int _x;
    private int _y;
    private int _altitude;
    
    public Coordonnees(int x, int y, int altitude)
    {
        _x = x;
        _y = y;
        _altitude = altitude;
    }
    public Coordonnees()
    {
    }
    public int getX()
    {
        return _x;
    }

    public int getY() {
        return _y;
    }

    public int getAltitude() {
        return _altitude;
    }

    public void setX(int _x) {
        this._x = _x;
    }

    public void setY(int _y) {
        this._y = _y;
    }

    public void setAltitude(int _altitude) {
        this._altitude = _altitude;
    }
    
    
}


