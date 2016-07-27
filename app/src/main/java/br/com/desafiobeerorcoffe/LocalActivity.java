package br.com.desafiobeerorcoffe;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import br.com.desafiobeerorcoffe.domain.Constantes;
import br.com.desafiobeerorcoffe.domain.EnderecoEB;
import br.com.desafiobeerorcoffe.service.HttpService;
import br.com.desafiobeerorcoffe.service.LocationIntentService;
import de.greenrobot.event.EventBus;
import me.drakeet.materialdialog.MaterialDialog;

public class LocalActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private HttpService mHttpService;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    protected Location mLastLocation;

    private ProgressDialog progressDialog;
    private MaterialDialog mMaterialDialog;

    private EditText edLogradouro;
    private EditText edNumero;
    private EditText edBairro;
    private EditText edCidade;
    private EditText edUf;
    private EditText edPais;
    private EditText edLatitude;
    private EditText edLongitude;
    private EditText edLocal;
    private RadioGroup radioBebidas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local);

        EventBus.getDefault().register(this);
        mHttpService = new HttpService();

        // ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        floatingButtonSalvar();

        bindingView();

        callConnection();
    }

    /**
     * Realiza o binding dos componentes da View com a Activity
     */
    private void bindingView() {
        edLocal = (EditText) findViewById(R.id.editLocal);
        edLogradouro = (EditText) findViewById(R.id.editRua);
        edNumero = (EditText) findViewById(R.id.editNumero);
        edBairro = (EditText) findViewById(R.id.editBairro);
        edCidade = (EditText) findViewById(R.id.editCidade);
        edUf = (EditText) findViewById(R.id.editUf);
        edPais = (EditText) findViewById(R.id.editPais);
        edLatitude = (EditText) findViewById(R.id.editLatitude);
        edLongitude = (EditText) findViewById(R.id.editLongitude);
        radioBebidas = (RadioGroup) findViewById(R.id.radioBebidas);

        addEvent();
    }

    /**
     * Adiciona evento aos EditText da view para atualizar a localizaçao
     */
    private void addEvent() {
        eventOnFocusChange(edLogradouro);
        eventOnFocusChange(edNumero);
        eventOnFocusChange(edBairro);
        eventOnFocusChange(edCidade);
        eventOnFocusChange(edUf);
        eventOnFocusChange(edPais);
    }

    /**
     * Evento onFocusChange no EditText
     * @param ed : EditText
     */
    private void eventOnFocusChange(EditText ed) {
        ed.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    startLocationUpdate(obterEnderecoPreenchido(), false);
                }
            }
        });
    }

    /**
     * Botao salvar
     */
    private void floatingButtonSalvar() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validarCamposObrigatorios()) {
                    int tipoBebida = 0;
                    if (radioBebidas.getCheckedRadioButtonId() == R.id.radioButtonCerveja) {
                        tipoBebida = 1;
                    } else if (radioBebidas.getCheckedRadioButtonId() == R.id.radioButtonCafe) {
                        tipoBebida = 2;
                    } else if (radioBebidas.getCheckedRadioButtonId() == R.id.radioButtonAmbos) {
                        tipoBebida = 3;
                    }

                    new TarefaEnviarLocalizacaoToBeerOrCoffee().execute(new String[]{ edLocal.getText().toString(), obterEnderecoPreenchido(),
                            edLatitude.getText().toString(), edLongitude.getText().toString(), String.valueOf(tipoBebida) });
                }
            }
        });
    }

    /**
     * Valida os campos obrigatorios para salvar um novo Local
     * @return boolean
     */
    private boolean validarCamposObrigatorios() {
        boolean valido = true;

        if (isNullOrEmpty(edLocal.getText().toString())) {
            edLocal.setError("Nome do local é obrigatório.");
            valido = false;
        }

        if (isNullOrEmpty(edLogradouro.getText().toString())) {
            edLogradouro.setError("Rua é obrigatório.");
            valido = false;
        }

        if (isNullOrEmpty(edNumero.getText().toString())) {
            edNumero.setError("Nº é obrigatório.");
            valido = false;
        }

        if (isNullOrEmpty(edBairro.getText().toString())) {
            edBairro.setError("Bairro é obrigatório.");
            valido = false;
        }

        if (isNullOrEmpty(edCidade.getText().toString())) {
            edCidade.setError("Cidade é obrigatório.");
            valido = false;
        }

        if (isNullOrEmpty(edUf.getText().toString())) {
            edUf.setError("UF é obrigatório.");
            valido = false;
        }

        if (isNullOrEmpty(edPais.getText().toString())) {
            edPais.setError("País é obrigatório.");
            valido = false;
        }

        if (isNullOrEmpty(edLatitude.getText().toString())) {
            edLatitude.setError("Latitude é obrigatório.");
            valido = false;
        }

        if (isNullOrEmpty(edLongitude.getText().toString())) {
            edLongitude.setError("Longitude é obrigatório.");
            valido = false;
        }

        return true;
    }

    /**
     * Obtem uma String com o Endereco preenchido
     * @return String
     */
    private String obterEnderecoPreenchido() {
        StringBuilder endereco = new StringBuilder();
        endereco.append(edLogradouro.getText());
        endereco.append(", ");
        endereco.append(edNumero.getText());
        endereco.append(" - ");
        endereco.append(edBairro.getText());
        endereco.append(" - ");
        endereco.append(edCidade.getText());
        endereco.append(", ");
        endereco.append(edUf.getText());
        endereco.append(" - ");
        endereco.append(edPais.getText());

        return endereco.toString();
    }

    /**
     * Realiza a conexao com o a API do Goolge Maps
     */
    private synchronized void callConnection() {
        Log.i(Constantes.TAG, "LocalActivity.callConnection()");

        if (verificarLocalizacao()) {
            if (mGoogleApiClient == null) {
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                        .addConnectionCallbacks(this) //Be aware of state of the connection
                        .addOnConnectionFailedListener(this) //Be aware of failures
                        .addApi(LocationServices.API)
                        .build();
            }

            mGoogleApiClient.connect();
        }
    }

    /**
     * Chama a thread responsavel por manipular e obter a localizacao atual ou informada
     * @param address : String
     * @param obterEnderecoCompleto : boolean
     */
    public void callLocationIntentService(String address, boolean obterEnderecoCompleto) {
        Intent it = new Intent(this, LocationIntentService.class);
        it.putExtra(Constantes.ADDRESS, address);
        it.putExtra(Constantes.LOCATION, address == null ? mLastLocation : null);
        it.putExtra(Constantes.OBTER_ENDERECO_COMPLETO, obterEnderecoCompleto);
        startService(it);
    }

    @Override
    protected void onStop() {
        super.onStop();
        pararConexaoComGoogleApi();
    }

    public void pararConexaoComGoogleApi() {
        //Verificando se está conectado para então cancelar a conexão!
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Depois que o método connect() for chamado, esse método será chamado de forma assíncrona caso a conexão seja bem sucedida.
     * @param bundle : Bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.i(Constantes.TAG, "LocalActivity.onConnected(" + bundle + ")");

        //Conexão com o serviços do Google Service API foi estabelecida!
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (lastLocation != null) {
            mLastLocation = lastLocation;
        }

        startLocationUpdate(null, true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(Constantes.TAG, "LocalActivity.onConnectionSuspended(" + i + ")");
        // Aguardando o GoogleApiClient reestabelecer a conexão.
    }

    /**
     * Método chamado quando um erro de conexão acontece e não é possível acessar os serviços da Google Service.
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(Constantes.TAG, "LocalActivity.onConnectionFailed(" + connectionResult + ")");

        //A conexão com o Google API falhou!
        pararConexaoComGoogleApi();
    }

    /**
     * Metodo responsavel por receber a Resposta da tread de localizacao
     * @param end : EnderecoEB
     */
    public void onEvent(final EnderecoEB end) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (end.getMensagemErro() == null) {
                    if (end.isObterEnderecoCompleto()) {
                        edLogradouro.setText(end.getLogradouro());
                        edNumero.setText(end.getNumero());
                        edBairro.setText(end.getBairro());
                        edCidade.setText(end.getCidade());
                        edUf.setText(end.getUf());
                        edPais.setText(end.getPais());
                    }

                    edLatitude.setText(end.getLatitude());
                    edLongitude.setText(end.getLongitude());
                } else {
                    callDialogErro(end.getMensagemErro(), "Tente novamente");
                }
            }
        });
    }

    /**
     * Limpar o formulario para gerar um novo local
     */
    private void limparFormulario() {
        edLocal.setText("");
        edLocal.setError(null);
        edLogradouro.setText("");
        edLogradouro.setError(null);
        edNumero.setText("");
        edNumero.setError(null);
        edBairro.setText("");
        edBairro.setError(null);
        edCidade.setText("");
        edCidade.setError(null);
        edUf.setText("");
        edUf.setError(null);
        edPais.setText("");
        edPais.setError(null);
        edLatitude.setText("");
        edLatitude.setError(null);
        edLongitude.setText("");
        edLongitude.setError(null);
        radioBebidas.check(R.id.radioButtonAmbos);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    /**
     * Atualiza a localizacao atual
     * @param address : String
     * @param obterEnderecoCompleto : boolean
     */
    private void startLocationUpdate(String address, boolean obterEnderecoCompleto) {
        if (verificarLocalizacao()) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, LocalActivity.this);

            callLocationIntentService(address, obterEnderecoCompleto);
        }
    }

    /**
     * Verifica se o dispositivo possui GPS ou Internet ativo
     * @return boolean
     */
    private boolean verificarLocalizacao() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled && !isNetworkEnabled) {
            Log.i(Constantes.TAG, "Nenhum recurso de localização disponivel!");

            callDialogErro("Nenhum recurso de localização disponivel, favor habilitar para continuar.","GPS!");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Valida se o valor informado é Null ou Vazio
     * @param str : String
     * @return boolean
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_local, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_localizacao: {
                // recarrega a localizacao atual do dispositivo
                startLocationUpdate(null, true);
                return true;
            }

            case R.id.menu_limpar: {
                limparFormulario();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private class TarefaEnviarLocalizacaoToBeerOrCoffee extends AsyncTask<String, Void, Integer> {
        @Override
        protected void onPreExecute() {
            Log.i("AsyncTask", "Exibindo ProgressDialog na tela Thread: " + Thread.currentThread().getName());
            progressDialog = ProgressDialog.show(LocalActivity.this, "Aguarde ...", "Salvando localização ...");
        }

        @Override
        protected Integer doInBackground(String... params) {
            Log.i("AsyncTask", "Salvando ...: " + Thread.currentThread().getName());

            JSONObject json = new JSONObject();
            try {
                json.put("name", params[0]);
                json.put("address", params[1]);
                json.put("latitude", Double.valueOf(params[2]));
                json.put("longitude", Double.valueOf(params[3]));
                json.put("beverage", Integer.valueOf(params[4]));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONObject jObjResult;
            try {
                jObjResult = mHttpService.convertJSONString2Obj(mHttpService.sendPOST(Constantes.WS_HOST + Constantes.POST_PLACE, json.toString(), Constantes.API_KEY));

                if (jObjResult != null) {
                    return Constantes.RESULT_SUCCESS;
                } else {
                    return Constantes.RESULT_ERROR;
                }
            } catch (Exception e) {
                return Constantes.RESULT_ERROR;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            progressDialog.dismiss();

            if(Constantes.RESULT_SUCCESS == result) {
                mMaterialDialog = new MaterialDialog(LocalActivity.this)
                        .setTitle("Sucesso!")
                        .setMessage("Local cadastrado com sucesso!")
                        .setPositiveButton("Ok", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                limparFormulario();
                                mMaterialDialog.dismiss();
                            }
                        });
                mMaterialDialog.show();
            } else {
                callDialogErro("Tente novamente dentro de alguns minutos.", "Ocorreu um erro!");
            }
        }
    }

    private void callDialogErro(String message, String title) {
        mMaterialDialog = new MaterialDialog(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMaterialDialog.dismiss();
                    }
                });
        mMaterialDialog.show();
    }
}