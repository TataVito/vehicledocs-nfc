package com.rvc.vehicledocsnfc

import org.json.JSONObject

data class VehicleData(
    var patente: String = "",
    var marca: String = "",
    var modelo: String = "",
    var anio: String = "",
    var color: String = "",
    var vin: String = "",
    var permisoNumero: String = "",
    var permisoMunicipio: String = "",
    var permisoVenc: String = "",
    var soapCompania: String = "",
    var soapPoliza: String = "",
    var soapVenc: String = "",
    var rtNumero: String = "",
    var rtPlanta: String = "",
    var rtVenc: String = ""
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("v", JSONObject().apply {
            put("p", patente)
            put("m", marca)
            put("mo", modelo)
            put("a", anio)
            put("c", color)
            put("vin", vin)
        })
        json.put("pc", JSONObject().apply {
            put("n", permisoNumero)
            put("mu", permisoMunicipio)
            put("v", permisoVenc)
        })
        json.put("s", JSONObject().apply {
            put("c", soapCompania)
            put("p", soapPoliza)
            put("v", soapVenc)
        })
        json.put("rt", JSONObject().apply {
            put("n", rtNumero)
            put("pl", rtPlanta)
            put("v", rtVenc)
        })
        return json.toString()
    }

    fun toReadable(): String = buildString {
        appendLine("=== VEHÍCULO ===")
        appendLine("Patente   : $patente")
        appendLine("Marca     : $marca")
        appendLine("Modelo    : $modelo")
        appendLine("Año       : $anio")
        appendLine("Color     : $color")
        appendLine("VIN       : $vin")
        appendLine()
        appendLine("=== PERMISO DE CIRCULACIÓN ===")
        appendLine("Número    : $permisoNumero")
        appendLine("Municipio : $permisoMunicipio")
        appendLine("Vence     : $permisoVenc")
        appendLine()
        appendLine("=== SOAP ===")
        appendLine("Compañía  : $soapCompania")
        appendLine("Póliza    : $soapPoliza")
        appendLine("Vence     : $soapVenc")
        appendLine()
        appendLine("=== REVISIÓN TÉCNICA ===")
        appendLine("Número    : $rtNumero")
        appendLine("Planta    : $rtPlanta")
        appendLine("Vence     : $rtVenc")
    }

    companion object {
        fun fromJson(jsonStr: String): VehicleData {
            val json = JSONObject(jsonStr)
            val v = json.optJSONObject("v") ?: JSONObject()
            val pc = json.optJSONObject("pc") ?: JSONObject()
            val s = json.optJSONObject("s") ?: JSONObject()
            val rt = json.optJSONObject("rt") ?: JSONObject()
            return VehicleData(
                patente = v.optString("p"),
                marca = v.optString("m"),
                modelo = v.optString("mo"),
                anio = v.optString("a"),
                color = v.optString("c"),
                vin = v.optString("vin"),
                permisoNumero = pc.optString("n"),
                permisoMunicipio = pc.optString("mu"),
                permisoVenc = pc.optString("v"),
                soapCompania = s.optString("c"),
                soapPoliza = s.optString("p"),
                soapVenc = s.optString("v"),
                rtNumero = rt.optString("n"),
                rtPlanta = rt.optString("pl"),
                rtVenc = rt.optString("v")
            )
        }
    }
}
