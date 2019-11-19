package lucas.vegi.coletapp;

import lucas.vegi.coletapp.adapters.*;
import lucas.vegi.coletapp.models.*;
import lucas.vegi.coletapp.utils.BancoDados;
import lucas.vegi.coletapp.utils.ExportExcel;
import lucas.vegi.coletapp.utils.GoogleAnalyticsConfig;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Build;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;

public class Principal extends FragmentActivity implements LocationListener {

    public final int OP_COLETA = 1;
    public final int OP_MAPA = 2;
    public final int OP_EXPORTACAO = 3;
    public final int OP_SOBRE = 4;
    public int OP_ATUAL;

    public int ID_DESFAZER = 0;

    public String latitude;
    public String longitude;
	public float precisao;
    public static Location loc;

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

        OP_ATUAL = OP_COLETA;

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
		// COLETA DE DADOS
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1)));
		// MAPA
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1)));
		// EXPORTAR DADOS
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));
		// EXCLUIR DADOS
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));
        // SOBRE
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[4], navMenuIcons.getResourceId(4, -1)));

		/*
		// Pages
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[4], navMenuIcons.getResourceId(4, -1)));
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
                displayView(0);
            }catch (Exception e){
                Log.e("ERRO", "Problema escolher opção no MENU lateral - " + e.getMessage());
            }
            //configuraTela();
		}
	}

    @Override
    protected void onStart() {
        super.onStart();

        //iniciaLocation();
        requestLocationPermission();
    }

    @Override
    protected void onStop() {
        //interrompe o Location Manager
        lm.removeUpdates(this);

        Log.w("PROVEDOR", "Provedor " + provider + " parado!");
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

    public void configuraTela(){
        try {
            Cursor c = bd.buscar("Ponto", new String[]{"idPonto"}, "", "idPonto DESC");
            ImageButton imagemDesfazer = (ImageButton) findViewById(R.id.btDesfazer);

            //Toast.makeText(this, "Total:"+c.getCount(),Toast.LENGTH_SHORT).show();

            if (c.getCount() > 0) {
                c.moveToNext();

                int indexIdPonto = c.getColumnIndex("idPonto");
                ID_DESFAZER = c.getInt(indexIdPonto);

                imagemDesfazer.setVisibility(View.VISIBLE);
                imagemDesfazer.setClickable(true);
            }else{
                ID_DESFAZER = 0;

                imagemDesfazer.setVisibility(View.INVISIBLE);
                imagemDesfazer.setClickable(false);
            }

            c.close();
        }catch (Exception e){
            Log.i("ERRO","Erro ao tentar habilitar botão de desfazer "+e.getMessage());
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
                displayView(position);
            }catch (Exception e){
                Log.e("ERRO", "Problema escolher opção no MENU lateral - " + e.getMessage());
            }
		}
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
	private void displayView(int position) {
		// update the main content by replacing fragments
		Fragment fragment = null;

		switch (position) {
		case 0:
            OP_ATUAL = OP_COLETA;
            fragment = new HomeFragment();
            //geraDadosAnalitics("ui","open","Coleta");
			break;
		case 1:
			//Só habilita essa opção para API level 17 ou superior
			if(Build.VERSION.SDK_INT >= 17) {
				fragment = new MapaFragment(getBaseContext());
                OP_ATUAL = OP_MAPA;
            }else{
                Toast.makeText(this,"Opção indisponível para esse aparelho.", Toast.LENGTH_LONG).show();
            }
            //geraDadosAnalitics("ui","open","Mapas");
			break;
		case 2:
            OP_ATUAL = OP_EXPORTACAO;

            PD = ProgressDialog.show(Principal.this, "Exportando dados", "Aguarde um momento...");

            new Thread() {
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ExportExcel.exportar(Principal.this);
                            PD.dismiss();
                        }
                    });
                }
            }.start();

            //geraDadosAnalitics("ui", "open", "Exportar Dados");
			break;
		case 3:
            //limpar base
            AlertDialog.Builder confirmacao = new AlertDialog.Builder(this);
            confirmacao.setTitle("Exclusão da Base de Dados");
            confirmacao.setMessage("Tem certeza que deseja excluir TODOS os dados coletados?");

            confirmacao.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        int qtd = bd.apagarBase();
                        if (qtd == 0)
                            Toast.makeText(getBaseContext(), "Base de dados já estava vazia", Toast.LENGTH_LONG).show();
                        else if (qtd == 1)
                            Toast.makeText(getBaseContext(), qtd + " ponto foi deletado", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getBaseContext(), qtd + " pontos foram deletados", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.i("ERRO", "Erro ao tentar limpar a base de dados" + e.getMessage());
                    }
                }
            });

            confirmacao.setNegativeButton("Não", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });

            confirmacao.show();

            //geraDadosAnalitics("ui", "open", "Excluir Base de Dados");
			break;
		case 4:
            OP_ATUAL = OP_SOBRE;
            fragment = new SobreFragment();

            //geraDadosAnalitics("ui","open","Sobre");
            break;
		/*case 5:
			fragment = new WhatsHotFragment();
			break;*/

		default:
			break;
		}

        if (fragment != null) {
            //só não entra aqui se o Dispositivo não tiver pelo menos API 17
            if(!(position == 1 && OP_ATUAL != OP_MAPA)) {
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.frame_container, fragment).commit();

                // update selected item and title, then close the drawer
                mDrawerList.setItemChecked(position, true);
                mDrawerList.setSelection(position);
                setTitle(navMenuTitles[position]);
                mDrawerLayout.closeDrawer(mDrawerList);

                if (position == 0) {
                    requestLocationPermission();
                    iniciaLocation();
                } else if(position != 3 && position != 2) //quando limpa base de dados ou exporta dados, não precisa parar GPS
                    lm.removeUpdates(this);
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
        //TODO: atributo nessa classe que controle em qual opção estou. se não tiver na 0 nada será feito também!
        //TODO: além disso, abraçar tudo isso com try..catch para evitar erros quando estiver no mapa
        if(location != null && OP_ATUAL == OP_COLETA){
            try {
                loc = location;  //localização global
                DecimalFormat df = new DecimalFormat("0.00");

                //obtem atributos na localização atual
                latitude = location.getLatitude() + "";
                longitude = location.getLongitude() + "";
                precisao = location.getAccuracy();

                TextView txtLat = (TextView) findViewById(R.id.txtLatitude);
                TextView txtLong = (TextView) findViewById(R.id.txtLongitude);
                TextView txtPrecisao = (TextView) findViewById(R.id.txtPrecisao);

                //atualiza valores na interface
                txtLat.setText(latitude + "");
                txtLong.setText(longitude + "");
                txtPrecisao.setText(df.format(precisao) + " metros");

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

    public void marcarLocal(View v){
        try {
            EditText editNomeLocal = (EditText) findViewById(R.id.edtNomeLocal);
            String local = editNomeLocal.getText().toString();

            //Toast.makeText(this,local + "\n - " + categoria + "\n - " + latitude + "\n - " + longitude,Toast.LENGTH_LONG).show();

            if ((!local.equals("")) && latitude != null && longitude != null) {

                String dataColeta = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
                String horaColeta = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());

                ContentValues valores = new ContentValues();
                //cria novo registro de checkin
                valores.put("nome", local);
                valores.put("latitude", latitude);
                valores.put("longitude", longitude);
                valores.put("precisao", precisao + "");
                valores.put("dt_coleta", dataColeta);
                valores.put("hr_coleta", horaColeta);

                bd.inserir("Ponto", valores);
                Toast.makeText(this, "Local marcado!", Toast.LENGTH_LONG).show();

                editNomeLocal.setText("");
                configuraTela();

            } else {
                Toast.makeText(this, "Informe o nome do local ou aguarde o GPS obter a sua posição!", Toast.LENGTH_LONG).show();
            }
        }catch (Exception e){
            Log.i("ERRO", "Erro ao tentar marcar um local " + e.getMessage());
        }
    }

    public void desfazerColeta(View v){
        try {
            if(ID_DESFAZER != 0){

                //geraDadosAnalitics("ui","open","Desfazer");

                AlertDialog.Builder confDesfazer = new AlertDialog.Builder(this);
                confDesfazer.setTitle("Desfazer marcação");
                confDesfazer.setMessage("Tem certeza que deseja desfazer UMA marcação?");

                confDesfazer.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int cont = bd.deletar("Ponto","idPonto = "+ID_DESFAZER);
                        if(cont == 1) {
                            Toast.makeText(getBaseContext(), "Uma marcação foi desfeita", Toast.LENGTH_LONG).show();
                            configuraTela();
                        }
                    }
                });

                confDesfazer.setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                confDesfazer.show();
            }
        }catch (Exception e){
            Log.i("ERRO", "Erro ao tentar desfazer última marcação " + e.getMessage());
        }
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

    public void geraDadosAnalitics(String category, String action, String labelOpcao){
        // Log setting open event with category="ui", action="open", and label="settings"
        GoogleAnalyticsConfig.tracker().send(new HitBuilders.EventBuilder(category, action)
                .setLabel(labelOpcao)
                .build());
    }

}
