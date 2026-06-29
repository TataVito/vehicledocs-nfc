package com.rvc.vehicledocsnfc

import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.rvc.vehicledocsnfc.databinding.ActivityMainBinding
import com.rvc.vehicledocsnfc.databinding.DialogLeerTagBinding

enum class NfcMode { IDLE, WRITING, READING }

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DocsPagerAdapter

    private var nfcAdapter: NfcAdapter? = null
    private var nfcMode = NfcMode.IDLE
    private var pendingReadDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no tiene NFC", Toast.LENGTH_LONG).show()
        }

        adapter = DocsPagerAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = adapter.tabs[pos]
        }.attach()

        binding.btnEscribir.setOnClickListener { iniciarModo(NfcMode.WRITING) }
        binding.btnLeer.setOnClickListener { iniciarModo(NfcMode.READING) }
        binding.btnCancelar.setOnClickListener { cancelarModo() }
    }

    private fun iniciarModo(mode: NfcMode) {
        if (nfcAdapter == null) {
            toast("Este dispositivo no tiene NFC")
            return
        }
        if (!nfcAdapter!!.isEnabled) {
            toast("Activa el NFC en Ajustes del teléfono")
            return
        }
        nfcMode = mode
        val msg = if (mode == NfcMode.WRITING)
            "Acerca el tag NFC para ESCRIBIR los datos..."
        else
            "Acerca el tag NFC para LEER los datos..."
        binding.bannerStatus.isVisible = true
        binding.tvStatus.text = msg
    }

    private fun cancelarModo() {
        nfcMode = NfcMode.IDLE
        binding.bannerStatus.isVisible = false
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pending = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            it.enableForegroundDispatch(this, pending, null, null)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (nfcMode == NfcMode.IDLE) return

        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

        when (nfcMode) {
            NfcMode.WRITING -> escribirTag(tag)
            NfcMode.READING -> leerTag(tag)
            NfcMode.IDLE -> {}
        }
    }

    // ─── ESCRITURA ───────────────────────────────────────────────────────────

    private fun escribirTag(tag: Tag) {
        val data = leerFormulario()
        val json = data.toJson()
        val record = NdefRecord.createMime("application/vnd.rvc.vehicledocs", json.toByteArray(Charsets.UTF_8))
        val message = NdefMessage(arrayOf(record))

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    toast("El tag está protegido contra escritura")
                    ndef.close()
                    return
                }
                val maxSize = ndef.maxSize
                if (message.toByteArray().size > maxSize) {
                    toast("Datos demasiado grandes para este tag (máx $maxSize bytes). Reduce el texto.")
                    ndef.close()
                    return
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                cancelarModo()
                toast("Tag escrito correctamente")
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(message)
                    formatable.close()
                    cancelarModo()
                    toast("Tag formateado y escrito correctamente")
                } else {
                    toast("Este tag no es compatible con NDEF")
                }
            }
        } catch (e: Exception) {
            toast("Error al escribir: ${e.message}")
        }
    }

    private fun leerFormulario(): VehicleData {
        fun tab(pos: Int) = binding.viewPager.let {
            // Accede al fragment actualmente adjunto al pager
            supportFragmentManager.findFragmentByTag("f$pos")
        }

        fun et(fragment: androidx.fragment.app.Fragment?, id: Int): String =
            (fragment?.view?.findViewById<TextInputEditText>(id))?.str() ?: ""

        val fVeh = tab(0)
        val fPer = tab(1)
        val fSoap = tab(2)
        val fRt = tab(3)

        return VehicleData(
            patente = et(fVeh, R.id.etPatente),
            marca = et(fVeh, R.id.etMarca),
            modelo = et(fVeh, R.id.etModelo),
            anio = et(fVeh, R.id.etAnio),
            color = et(fVeh, R.id.etColor),
            vin = et(fVeh, R.id.etVin),
            permisoNumero = et(fPer, R.id.etPermisoNumero),
            permisoMunicipio = et(fPer, R.id.etPermisoMunicipio),
            permisoVenc = et(fPer, R.id.etPermisoVenc),
            soapCompania = et(fSoap, R.id.etSoapCompania),
            soapPoliza = et(fSoap, R.id.etSoapPoliza),
            soapVenc = et(fSoap, R.id.etSoapVenc),
            rtNumero = et(fRt, R.id.etRtNumero),
            rtPlanta = et(fRt, R.id.etRtPlanta),
            rtVenc = et(fRt, R.id.etRtVenc)
        )
    }

    // ─── LECTURA ─────────────────────────────────────────────────────────────

    private fun leerTag(tag: Tag) {
        try {
            val ndef = Ndef.get(tag) ?: run {
                toast("El tag no contiene datos NDEF")
                return
            }
            ndef.connect()
            val ndefMessage = ndef.ndefMessage ?: run {
                toast("El tag está vacío")
                ndef.close()
                return
            }
            ndef.close()

            val payload = ndefMessage.records.firstOrNull()?.payload ?: run {
                toast("No se pudo leer el contenido")
                return
            }
            val json = String(payload, Charsets.UTF_8)
            val data = VehicleData.fromJson(json)

            cancelarModo()
            mostrarDialogoLectura(data)
        } catch (e: Exception) {
            toast("Error al leer: ${e.message}")
        }
    }

    private fun mostrarDialogoLectura(data: VehicleData) {
        val dialog = Dialog(this)
        val dBinding = DialogLeerTagBinding.inflate(layoutInflater)
        dialog.setContentView(dBinding.root)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dBinding.tvDatosLeidos.text = data.toReadable()

        dBinding.btnCargarDatos.setOnClickListener {
            cargarEnFormulario(data)
            dialog.dismiss()
            toast("Datos cargados en el formulario")
        }
        dBinding.btnCerrarDialog.setOnClickListener { dialog.dismiss() }

        pendingReadDialog = dialog
        dialog.show()
    }

    private fun cargarEnFormulario(data: VehicleData) {
        fun tab(pos: Int) = supportFragmentManager.findFragmentByTag("f$pos")
        fun set(fragment: androidx.fragment.app.Fragment?, id: Int, value: String) {
            fragment?.view?.findViewById<TextInputEditText>(id)?.setText(value)
        }

        // Fuerza que todos los tabs estén creados navegando por ellos
        binding.viewPager.setCurrentItem(3, false)
        binding.viewPager.setCurrentItem(0, false)

        val fVeh = tab(0); val fPer = tab(1); val fSoap = tab(2); val fRt = tab(3)
        set(fVeh, R.id.etPatente, data.patente)
        set(fVeh, R.id.etMarca, data.marca)
        set(fVeh, R.id.etModelo, data.modelo)
        set(fVeh, R.id.etAnio, data.anio)
        set(fVeh, R.id.etColor, data.color)
        set(fVeh, R.id.etVin, data.vin)
        set(fPer, R.id.etPermisoNumero, data.permisoNumero)
        set(fPer, R.id.etPermisoMunicipio, data.permisoMunicipio)
        set(fPer, R.id.etPermisoVenc, data.permisoVenc)
        set(fSoap, R.id.etSoapCompania, data.soapCompania)
        set(fSoap, R.id.etSoapPoliza, data.soapPoliza)
        set(fSoap, R.id.etSoapVenc, data.soapVenc)
        set(fRt, R.id.etRtNumero, data.rtNumero)
        set(fRt, R.id.etRtPlanta, data.rtPlanta)
        set(fRt, R.id.etRtVenc, data.rtVenc)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
