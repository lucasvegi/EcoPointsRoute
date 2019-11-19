package lucas.vegi.coletapp.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class Routes {
    public Routes(ArrayList<LatLng> p, double d){
        this.points = p;
        this.distance = d;
    }
    public ArrayList<LatLng> points;
    public double distance;
}
