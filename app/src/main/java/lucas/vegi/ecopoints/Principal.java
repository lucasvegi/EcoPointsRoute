package lucas.vegi.ecopoints;

import lucas.vegi.ecopoints.adapters.*;
import lucas.vegi.ecopoints.models.*;
import lucas.vegi.ecopoints.utils.BancoDados;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class Principal extends FragmentActivity implements LocationListener {

    public final int OP_HOME = 1;
    public final int OP_MAPA = 2;
    public final int OP_SOBRE = 4;
    public int OP_ATUAL;

    public String latitude;
    public String longitude;
	public float precisao;
    public static Location loc = null;

    public BancoDados bd;

    public LocationManager lm;
    public Criteria criteria;
    public static String provider;
    public int TEMPO_REQUISICAO_LATLONG = 2000;
    public int DISTANCIA_MIN_METROS = 0;


	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	// nav drawer title
	private CharSequence mDrawerTitle;

	// used to store app title
	private CharSequence mTitle;

	// slide menu items
	private String[] navMenuTitles;
	private TypedArray navMenuIcons;

	private ArrayList<NavDrawerItem> navDrawerItems;
	private NavDrawerListAdapter adapter;

    private ProgressDialog PD;

    public final int LOCATION_PERMISSION = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        OP_ATUAL = OP_HOME;

        //Prepara a aplicação
		bd = BancoDados.getINSTANCE(this);
        configuraCriterioLocationProvider();

		mTitle = mDrawerTitle = getTitle();

		// load slide menu items
		navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);

		// nav drawer icons from resources
		navMenuIcons = getResources()
				.obtainTypedArray(R.array.nav_drawer_icons);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.list_slidermenu);

		navDrawerItems = new ArrayList<NavDrawerItem>();

        // adding nav drawer items to array
		//Monta opções do Navigation Drawer buscando no Banco de dados local
        // Tela Inicial
        try {
            navDrawerItems.add(new NavDrawerItem("Início", navMenuIcons.getResourceId(0, -1)));

            Cursor c = BancoDados.getINSTANCE(this).buscar("Tipo", new String[]{"idTipo, nome"}, "", "");
            while (c.moveToNext()) {
                int indexIdTipo = c.getColumnIndex("idTipo");
                int indexNome = c.getColumnIndex("nome");

                if (c.getString(indexIdTipo).equals("1")) {
                    navDrawerItems.add(new NavDrawerItem(c.getString(indexNome), c.getInt(indexIdTipo), navMenuIcons.getResourceId(7, -1)));
                } else if (c.getString(indexIdTipo).equals("2")) {
                    navDrawerItems.add(new NavDrawerItem(c.getString(indexNome), c.getInt(indexIdTipo), navMenuIcons.getResourceId(8, -1)));
                } else if (c.getString(indexIdTipo).equals("3")) {
                    navDrawerItems.add(new NavDrawerItem(c.getString(indexNome), c.getInt(indexIdTipo), navMenuIcons.getResourceId(5, -1)));
                } else if (c.getString(indexIdTipo).equals("4")) {
                    navDrawerItems.add(new NavDrawerItem(c.getString(indexNome), c.getInt(indexIdTipo), navMenuIcons.getResourceId(6, -1)));
                } else if (c.getString(indexIdTipo).equals("5")) {
                    navDrawerItems.add(new NavDrawerItem(c.getString(indexNome), c.getInt(indexIdTipo), navMenuIcons.getResourceId(9, -1)));
                }
            }
            c.close();

            // SOBRE
            navDrawerItems.add(new NavDrawerItem("Sobre o App", navMenuIcons.getResourceId(4, -1)));
        }catch (Exception e){
            Log.e("MENU", "Erro ao montar opções do navigation drawer!\n " + e.getMessage());
        }


		/*
		// What's hot, We  will add a counter here
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[5], navMenuIcons.getResourceId(5, -1), true, "50+"));*/

		// Recycle the typed array
		navMenuIcons.recycle();

		mDrawerList.setOnItemClickListener(new SlideMenuClickListener());

		// setting the nav drawer list adapter
		adapter = new NavDrawerListAdapter(getApplicationContext(),
				navDrawerItems);
		mDrawerList.setAdapter(adapter);

		// enabling action bar app icon and behaving it as toggle button
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, //nav menu toggle icon
				R.string.app_name, // nav drawer open - description for accessibility
				R.string.app_name // nav drawer close - description for accessibility
		) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				// calling onPrepareOptionsMenu() to show action bar icons
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
				// calling onPrepareOptionsMenu() to hide action bar icons
				invalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null) {
			// on first time display view for first nav item
			try {
                displayView(0,0);
            }catch (Exception e){
                Log.e("ERRO", "Problema escolher opção no MENU lateral - " + e.getMessage());
            }
            //configuraTela();
		}
	}

    @Override
    protected void onStart() {
        super.onStart();

        requestLocationPermission();
        iniciaLocation();
    }

    @Override
    protected void onStop() {
        //interrompe o Location Manager
        lm.removeUpdates(this);

        Log.w("LOCATION", "Provedor " + provider + " parado!");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //fecha conexão com o Banco de Dados
        super.onDestroy();
        bd.fechar();
    }

    public void requestLocationPermission(){
        // verifica a necessidade de pedir a permissão
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // verifica se precisa explicar para o usuário a necessidade da permissão
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

                //explica para o usuário a necessidade da permissão caso ele já tenha negado pelo menos uma vez
                Toast.makeText(this,"Permita o uso da geolocalização para obter as rotas!",Toast.LENGTH_LONG).show();

                //pede permissão de localização
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION);

                Log.i("Permission","Devo dar explicação");

            } else {

                // Pede a permissão direto a primeira vez que o usuário tentar usar o recurso.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION);

                Log.i("Permission","Pede a permissão");

                // LOCATION_PERMISSION é uma constante declarada para ser usada no callback da resposta da permissão
            }
        }
    }

    //trata resposta do usuário para permissão
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION: {
                // Se a requisição é cancelada, um array vazio é retornado
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    // permissão foi concedida. Esse ponto deve conter a ação a ser feita neste momento
                    iniciaLocation();

                    Log.i("Permission","Deu a permissão");

                } else {

                    // permissão não foi concedida pelo usuário. Desabilitar recursos que dependem dela
                    Log.i("Permission","Não permitiu");
                }
                return;
            }

            // tratar outros "case" referentes a eventuais novas requisições de permissão de recursos.
        }
    }

    @SuppressLint("MissingPermission")
    public void iniciaLocation(){
        //Obtem melhor provedor habilitado com o critério estabelecido
        provider = lm.getBestProvider( criteria, true );

        if ( provider == null ){
            Log.e("PROVEDOR", "Nenhum provedor encontrado!");
        }else{
            Log.i( "PROVEDOR", "Está sendo utilizado o provedor: " + provider );

            try {
                //Obtem atualizações de posição
                lm.requestLocationUpdates(provider, TEMPO_REQUISICAO_LATLONG, DISTANCIA_MIN_METROS, this);
            }catch (Exception e){
                Log.e("LOCATION", e.getMessage());
            }
        }
    }

    private void configuraCriterioLocationProvider(){
        //Location Manager
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();

        //Testa se o aparelho tem GPS
        PackageManager packageManager = getPackageManager();
        boolean hasGPS = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);

        //Estabelece critério de precisão
        if(hasGPS){
            criteria.setAccuracy( Criteria.ACCURACY_FINE );
            Log.i("LOCATION", "Usando GPS");
        }else{
            Log.i("LOCATION", "Usando WI-FI ou dados");
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        }
    }

    /**
	 * Slide menu item click listener
	 * */
	private class SlideMenuClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// display view for selected nav drawer item
            try {
                displayView(position,getNavItemIdOnSelect(parent,position));
                Log.d("Titulo", "ID: " + getNavItemIdOnSelect(parent,position));
            }catch (Exception e){
                Log.e("ERRO", "Problema escolher opção no MENU lateral - " + e.getMessage());
            }
		}
	}

	//obter a chave primária da opçao selecionada no Menu
	private int getNavItemIdOnSelect(AdapterView<?> parent, int position){
	    return ((NavDrawerListAdapter)parent.getAdapter()).getNavDrawerItems(position).getId();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// toggle nav drawer on selecting action bar app icon/title
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		// Handle action bar actions click
		switch (item.getItemId()) {
		case R.id.action_settings:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/* *
	 * Called when invalidateOptionsMenu() is triggered
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// if nav drawer is opened, hide the action items
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Diplaying fragment view for selected nav drawer list item
	 * */
	private void displayView(int position, int idTipo) {
		// update the main content by replacing fragments
		Fragment fragment = null;

		if(position == 0){
            OP_ATUAL = OP_HOME;
            fragment = new HomeFragment();
        }else if(position == navDrawerItems.size()-1){
            OP_ATUAL = OP_SOBRE;
            fragment = new SobreFragment();
        }else{
		    fragment = new MapaFragment(getBaseContext(),idTipo);
		    OP_ATUAL = OP_MAPA;
        }


        if (fragment != null) {
            //só não entra aqui se o Dispositivo não tiver pelo menos API 17
            if(!(position == 1 && OP_ATUAL != OP_MAPA)) {
                if (loc != null || OP_ATUAL == OP_HOME || OP_ATUAL == OP_SOBRE){
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_container, fragment).commit();

                    // update selected item and title, then close the drawer
                    mDrawerList.setItemChecked(position, true);
                    mDrawerList.setSelection(position);
                    setTitle(navDrawerItems.get(position).getTitle());

                    mDrawerLayout.closeDrawer(mDrawerList);

                }else{
                    Toast.makeText(this,"Aguarde a obtenção da sua localização!",Toast.LENGTH_SHORT).show();
                }
            }
        }else {
            // error in creating fragment
            Log.e("MainActivity", "Error in creating fragment");
        }
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

    @Override
    public void onLocationChanged(Location location) {
         if(location != null){
            try {
                loc = location;  //localização global
                DecimalFormat df = new DecimalFormat("0.00");

                //obtem atributos na localização atual
                latitude = location.getLatitude() + "";
                longitude = location.getLongitude() + "";
                precisao = location.getAccuracy();

                // DecimalFormat df = new DecimalFormat("0.##");
                Log.d("LOCATION", "Localização atual: " + location);
            }catch (Exception e){
                Log.d("LOCATION", "ERRO NA Localização atual: " + location);
            }
        }
    }

    public void onProviderDisabled(String provider) {
        Log.d("LOCATION", "Desabilitou o provedor");
    }

    public void onProviderEnabled(String provider) {
        Log.d("LOCATION", "Habilitou o provedor");
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("LOCATION", "Provedor mudou de estado");
    }

    public void sobreLabd2m(View v){
        //geraDadosAnalitics("ui","open","Site");

        //Representa o endereço que desejamos abrir
        Uri uri = Uri.parse("http://www.labd2m.ufv.br");

        //Cria a Intent com o endereço
        Intent it = new Intent(Intent.ACTION_VIEW, uri);

        //Envia a mensagem para o sistema operacional
        startActivity(it);
    }

}
