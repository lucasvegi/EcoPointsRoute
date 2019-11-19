package lucas.vegi.coletapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.VoiceInteractor;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lucas.vegi.coletapp.models.Routes;
import lucas.vegi.coletapp.utils.BancoDados;
import lucas.vegi.coletapp.utils.MapDirectionsParser;
import lucas.vegi.coletapp.utils.MyApplication;

import static com.google.android.gms.internal.zzagz.runOnUiThread;

public class MapaFragment extends Fragment implements OnMapReadyCallback {
    private static View rootView;
    private GoogleMap mapa;
    public BancoDados bd;
    private MapFragment mapFragment;
    private LatLng POSICAO_ATUAL;

    private Marker myMarker = null;
    private Polyline mPolyline = null;
    private ArrayList<LatLng> traceOfMe = null;
    public Context context;
    private ArrayList<LatLng> mDestinationLatLng = null;

    public ArrayList<LatLng> points = null;
    public ArrayList<Routes> rotas = new ArrayList<Routes>();
    public int indiceMenor = 0;
    public double menorDistancia = 0;


    public void plotarMarcadoresCheckIn() {
        try {
            bd = BancoDados.getINSTANCE(getActivity());
            Cursor c = bd.buscar("Ponto", new String[]{"nome", "latitude", "longitude", "dt_coleta", "hr_coleta"}, "", "");

            if (c.getCount() > 0) {
                LatLng posicao;
                while (c.moveToNext()) {
                    int indexLocal = c.getColumnIndex("nome");
                    int indexLatitude = c.getColumnIndex("latitude");
                    int indexLongitude = c.getColumnIndex("longitude");
                    int indexDtColeta = c.getColumnIndex("dt_coleta");
                    int indexHrColeta = c.getColumnIndex("hr_coleta");

                    double latitude = Double.parseDouble(c.getString(indexLatitude));
                    double longitude = Double.parseDouble(c.getString(indexLongitude));
                    String titulo = c.getString(indexLocal);
                    String dtColeta = c.getString(indexDtColeta);
                    String hrColeta = c.getString(indexHrColeta);

                    posicao = new LatLng(latitude, longitude);

                    mapa.addMarker(new MarkerOptions().position(posicao).title(titulo).snippet("Data: " + dtColeta + " Hora: " + hrColeta));
                }
            }
            c.close();
        } catch (Exception e) {
            Log.i("ERRO", "Erro ao tentar plotar os pontos no mapa " + e.getMessage());
        }
    }

	@SuppressLint("ValidFragment")
    public MapaFragment(Context ctx){
        this.context = ctx;

        //TODO: definir local de destino
        mDestinationLatLng = new ArrayList<LatLng>();
        mDestinationLatLng.add(new LatLng(-20.757360, -42.875076)); // quatro pilastras
        mDestinationLatLng.add(new LatLng(-20.762948, -42.867008)); // no bugs
        mDestinationLatLng.add(new LatLng(-20.761222, -42.867780)); // bbt central
        mDestinationLatLng.add(new LatLng(-20.753472, -42.881833)); // paróquia santa rita
    }

    public MapaFragment(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView != null) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null)
                parent.removeView(rootView);
        }
        try {
            rootView = inflater.inflate(R.layout.fragment_mapa, container, false);

        } catch (InflateException e) {
        /* map is already there, just return view as it is */
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //tentar obter o mapa de forma assíncrona
        mapFragment = getMapFragment();
        mapFragment.getMapAsync(this);

    }

    //Decide a forma de Recuperar o mapFragment baseado na versão do android
    private MapFragment getMapFragment() {
        FragmentManager fm = null;

        Log.d("MAPA", "sdk: " + Build.VERSION.SDK_INT);
        Log.d("MAPA", "release: " + Build.VERSION.RELEASE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d("MAPA", "using getFragmentManager");
            fm = getFragmentManager();
        } else {
            Log.d("MAPA", "using getChildFragmentManager");
            fm = getChildFragmentManager();
        }

        return (MapFragment) fm.findFragmentById(R.id.mapa);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;
        Log.i("ERRO", "PASSEI AQUI 4");
        if (mapa != null) {
            Log.i("MAPA", "Obteve o mapa");

            try {
                mapa.setMyLocationEnabled(true);
            }catch (Exception e){
                Log.e("LOCATION", e.getMessage());
            }

            mapa.setBuildingsEnabled(true);

            if(Principal.loc != null){
                //usa variavel global
                POSICAO_ATUAL = new LatLng(Principal.loc.getLatitude(),Principal.loc.getLongitude());

                //centraliza camera na posição atual
                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(POSICAO_ATUAL, 16);
                mapa.animateCamera(update);
            }

            //TODO: retirar esse metodo daqui
            plotarMarcadoresCheckIn();

            //renicia list de rotas
            if (rotas != null)
                rotas.clear();

            //gera todas as possíveis rotas
            for (LatLng destination : mDestinationLatLng){
                traceMe(POSICAO_ATUAL, destination);
            }

            //fica paralemente esperando todos os retornos do Google para desenhar a rota menor
            new Thread() {
                @Override
                public void run() {
                    //fica esperando os retornos da APIs
                    while (rotas.size() != mDestinationLatLng.size()){
                        Log.i("Rotas", "Diferente: " + rotas.size());
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    Log.i("Tamanho", "Tamanho: " + rotas.size());

                    //busca a menor rota entre todas
                    indiceMenor = 0;
                    menorDistancia = 0;
                    for(int i = 0; i < rotas.size(); i++){
                        if(i == 0){
                            indiceMenor = 0;
                            menorDistancia = rotas.get(i).distance;
                        }else if(rotas.get(i).distance < menorDistancia){
                            menorDistancia = rotas.get(i).distance;
                            indiceMenor = i;
                        }
                    }

                    //desenha a menor rota
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                drawPoints(rotas.get(indiceMenor).points,mapa);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        }
        Log.i("ERRO", "PASSEI AQUI 5");
    }

    private void traceMe(LatLng srcLatLng, LatLng destLatLng) {

        String srcParam = srcLatLng.latitude + "," + srcLatLng.longitude;
        String destParam = destLatLng.latitude + "," + destLatLng.longitude;

        //Mesmo padrão de requisição do iOS
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="+srcParam+"&destination="
                + destParam + "&sensor=false&mode=driving&key=AIzaSyC2vzuwOgPqc-bKKZZ_OykqsTYx6qRTTe8";


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Rota", "Obtive rota");
                        MapDirectionsParser parser = new MapDirectionsParser();
                        List<List<HashMap<String, String>>> routes = parser.parse(response);
                        //ArrayList<LatLng> points = null;

                        Log.d("Rota", "Tamanho rota: " + routes.size());

                        if(routes.size() > 0) {
                            for (int i = 0; i < routes.size(); i++) {
                                points = new ArrayList<LatLng>();

                                // Fetching i-th route
                                List<HashMap<String, String>> path = routes.get(i);

                                //Limpa mapa
                                //mapa.clear();

                                //Adiciona marcador à posição final
                                HashMap<String, String> pointAux = path.get(path.size() - 1);
                                Double latAux = Double.parseDouble(pointAux.get("lat"));
                                Double lngAux = Double.parseDouble(pointAux.get("lng"));

                                //Marcador do local do evento
                                addMarker(latAux, lngAux, "EcoPoint");


                                //TODO: posição inicial da pessoa. Eliminar futuramente
                                if (myMarker != null)
                                    myMarker.remove();

                                myMarker = addMarker(Principal.loc.getLatitude(),
                                        Principal.loc.getLongitude(),
                                        "Minha Localização",
                                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                                Log.d("Rota", "Pontos na rota: " + path.size());

                                // Fetching all the points in i-th route
                                for (int j = 0; j < path.size(); j++) {
                                    HashMap<String, String> point = path.get(j);

                                    double lat = Double.parseDouble(point.get("lat"));
                                    double lng = Double.parseDouble(point.get("lng"));
                                    LatLng position = new LatLng(lat, lng);

                                    points.add(position);
                                }

                            }

                            //cria uma lista com todas as rotas para os pontos e suas respectivas distancias
                            rotas.add(new Routes(points, calcRouteDistance(points)));

                        }else
                            Log.d("Rota", "Não foi possível obter rota");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Rota", "Erro na rota");
                    }
                });

        if(jsonObjectRequest != null) {
            MyApplication.getInstance().addToReqQueue(jsonObjectRequest);
        }else{
            Log.e("VolleyLog", "Valor nulo para JSON");
        }
    }

    private double calcRouteDistance(ArrayList<LatLng> points){
        if (points.size() > 0) {
            double distance = 0;

            Location inicio = new Location(Principal.provider);
            Location fim = new Location(Principal.provider);

            for (int i = 1; i < points.size(); i++){
                inicio.setLatitude(points.get(i-1).latitude);
                inicio.setLongitude(points.get(i-1).longitude);

                fim.setLatitude(points.get(i).latitude);
                fim.setLongitude(points.get(i).longitude);

                distance += inicio.distanceTo(fim);
            }

            Log.i("Distancia", "KM: " + distance/1000 );
            return distance; //metros
        }
        return 0;
    }


    private void drawPoints(ArrayList<LatLng> points, GoogleMap mMaps) {
        if (points == null) {
            return;
        }
        traceOfMe = points;
        PolylineOptions polylineOpt = new PolylineOptions();
        for (LatLng latlng : traceOfMe) {
            polylineOpt.add(latlng);
        }
        polylineOpt.color(Color.BLUE);
        if (mPolyline != null) {
            mPolyline.remove();
            mPolyline = null;
        }
        if (mapa != null) {
            mPolyline = mapa.addPolyline(polylineOpt);

        } else {

        }
        if (mPolyline != null)
            mPolyline.setWidth(10);
    }

    private Marker addMarker(double lat, double lng, String text) {
        return mapa.addMarker(new MarkerOptions()
                .position(new LatLng(lat,lng))
                .title(text)
                .flat(false));
    }

    private Marker addMarker(double lat, double lng, String text, BitmapDescriptor cor ) {
        return mapa.addMarker(new MarkerOptions()
                .position(new LatLng(lat,lng))
                .title(text)
                .icon(cor)
                .flat(false));
    }

}