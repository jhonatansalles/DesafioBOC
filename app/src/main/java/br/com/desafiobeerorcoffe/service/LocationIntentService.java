package br.com.desafiobeerorcoffe.service;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.desafiobeerorcoffe.LocalActivity;
import br.com.desafiobeerorcoffe.R;
import br.com.desafiobeerorcoffe.domain.Constantes;
import br.com.desafiobeerorcoffe.domain.EnderecoEB;
import de.greenrobot.event.EventBus;

/**
 * @author Jhonatan
 */
public class LocationIntentService extends IntentService {

    public LocationIntentService() {
        super("worker_thread");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String mensagemErro = "";

        // obtem a localizacao passada para o servico via parametro
        Location localizacaoDispositivo = intent.getParcelableExtra(Constantes.LOCATION);
        String enderecoPreenchido = intent.getStringExtra(Constantes.ADDRESS);
        boolean obterEnderecoCompleto = intent.getBooleanExtra(Constantes.OBTER_ENDERECO_COMPLETO, true);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> resultEndereco = null;

        try {
            // Obtem o endereço de acordo com a localidade atual do dispositivo
            if (localizacaoDispositivo != null) {
                resultEndereco = geocoder.getFromLocation(localizacaoDispositivo.getLatitude(), localizacaoDispositivo.getLongitude(), 1);
            } else if (enderecoPreenchido != null && !enderecoPreenchido.isEmpty()) {
                // Obtem as coordenadas de acordo com o Endereço informado no formulario
                resultEndereco = geocoder.getFromLocationName(enderecoPreenchido, 1);
            }
        } catch (IOException ioException) {
            // Dispositivo sem conexão com Internet ou GPS
            mensagemErro = getString(R.string.servico_nao_disponivel);
            Log.e(Constantes.TAG, mensagemErro, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // latitude ou longitude com valores invalidos.
            mensagemErro = getString(R.string.lat_long_invalido);
            Log.e(Constantes.TAG, mensagemErro + ". " + "Latitude = " + localizacaoDispositivo.getLatitude() +
                    ", Longitude = " + localizacaoDispositivo.getLongitude(), illegalArgumentException);
        }

        EnderecoEB end = null;

        // verifica se obteve algum endereco ou se houve error
        if (resultEndereco == null || resultEndereco.size() == 0) {
            if (mensagemErro.isEmpty()) {
                mensagemErro = getString(R.string.endereco_nao_encontrado);
                Log.e(Constantes.TAG, mensagemErro);
            }

            end = new EnderecoEB();
            end.setMensagemErro(mensagemErro);
        } else {
            Log.i(Constantes.TAG, getString(R.string.endereco_encontrado));

            Address enderecoObtido = resultEndereco.get(0);

            end = new EnderecoEB();

            if (obterEnderecoCompleto) {
                end.setLogradouro(enderecoObtido.getThoroughfare());
                end.setNumero(enderecoObtido.getSubThoroughfare());
                end.setBairro(enderecoObtido.getSubLocality());
                end.setCidade(enderecoObtido.getLocality());
                end.setUf(enderecoObtido.getAdminArea());
                end.setPais(enderecoObtido.getCountryName());

                end.setObterEnderecoCompleto(obterEnderecoCompleto);
            }

            end.setLatitude(String.valueOf(enderecoObtido.getLatitude()));
            end.setLongitude(String.valueOf(enderecoObtido.getLongitude()));
        }

        // retorna a resposta com o objeto para Activity
        EventBus.getDefault().post(end);
    }
}
