package br.com.raevan.familialm.dominio.modelos

enum class TipoDispositivo(val rotulo: String) {
    LUZ("Iluminação"),
    CLIMATIZACAO("Ar-Condicionado"),
    PORTAO("Portão / Fechadura"),
    CAMERA("Câmera NVR / Frigate"),
    SENSOR("Sensor de Presença")
}

data class DispositivoAutomacao(
    val id: String,
    val nome: String,
    val comodo: String,
    val tipo: TipoDispositivo,
    val estaLigado: Boolean = false,
    val valorStatus: String = "Normal"
)

data class CameraFrigate(
    val id: String,
    val nome: String,
    val localizacao: String,
    val statusOnline: Boolean = true,
    val ultimaDeteccao: String? = null
)
