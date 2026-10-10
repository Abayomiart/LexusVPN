package com.novavpn.app.vpn.transport

import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Roteador bidirecional de pacotes IP de alto desempenho:
 * - Lê pacotes brutos da interface TUN (FileInputStream) e encaminha ao túnel TLS/SSH.
 * - Lê pacotes de resposta do túnel TLS/SSH e escreve de volta na interface TUN (FileOutputStream).
 * - Suporta IPv4 e IPv6 com validação de cabeçalhos de pacote.
 */
class TunPacketRouter(
    private val tunInterface: ParcelFileDescriptor,
    private val packetChannel: VpnPacketChannel,
    private val mtu: Int = 1420,
    private val onTrafficUpdate: (bytesIn: Long, bytesOut: Long) -> Unit,
    private val onError: (Throwable) -> Unit
) {

    companion object {
        private const val TAG = "TunPacketRouter"
    }

    private val isRunning = AtomicBoolean(false)
    private val bytesIn = AtomicLong(0)
    private val bytesOut = AtomicLong(0)

    private var tunToTunnelJob: Job? = null
    private var tunnelToTunJob: Job? = null

    private var tunInputStream: FileInputStream? = null
    private var tunOutputStream: FileOutputStream? = null
    private var tunnelIn: DataInputStream? = null
    private var tunnelOut: DataOutputStream? = null

    /**
     * Inicia o roteamento concorrente de pacotes IP.
     */
    fun start(scope: CoroutineScope) {
        if (!isRunning.compareAndSet(false, true)) {
            Log.w(TAG, "Roteador já está em execução.")
            return
        }

        tunInputStream = FileInputStream(tunInterface.fileDescriptor)
        tunOutputStream = FileOutputStream(tunInterface.fileDescriptor)
        tunnelIn = DataInputStream(packetChannel.inputStream)
        tunnelOut = DataOutputStream(packetChannel.outputStream)

        Log.i(TAG, "Iniciando workers bidirecionais TUN <-> TÚNEL (MTU: $mtu)...")

        // 1. Worker de Saída: Interface TUN -> Túnel Remoto
        tunToTunnelJob = scope.launch(Dispatchers.IO) {
            runTunToTunnelWorker()
        }

        // 2. Worker de Entrada: Túnel Remoto -> Interface TUN
        tunnelToTunJob = scope.launch(Dispatchers.IO) {
            runTunnelToTunWorker()
        }
    }

    /**
     * Loop de leitura da interface TUN: captura pacotes IP emitidos pelos aplicativos
     * e os envia através do socket protegido para o servidor remoto.
     */
    private fun runTunToTunnelWorker() {
        val buffer = ByteArray(mtu + 100)
        val inStream = tunInputStream ?: return
        val outStream = tunnelOut ?: return

        try {
            while (isRunning.get()) {
                val length = inStream.read(buffer)
                if (length > 0) {
                    // Valida versão IP no primeiro nibble (IPv4 = 4, IPv6 = 6)
                    val ipVersion = (buffer[0].toInt() shr 4) and 0x0F
                    if (ipVersion == 4 || ipVersion == 6) {
                        // Escreve comprimento do pacote em formato Big-Endian (2 bytes) + Payload
                        synchronized(outStream) {
                            outStream.writeShort(length)
                            outStream.write(buffer, 0, length)
                            outStream.flush()
                        }

                        val totalOut = bytesOut.addAndGet(length.toLong())
                        onTrafficUpdate(bytesIn.get(), totalOut)
                    } else {
                        Log.w(TAG, "Pacote com versão IP inválida descartado: $ipVersion")
                    }
                } else if (length < 0) {
                    Log.d(TAG, "Fim do stream da interface TUN detectado.")
                    break
                }
            }
        } catch (e: Exception) {
            if (isRunning.get() && e !is CancellationException) {
                Log.e(TAG, "Erro no worker TUN -> Túnel: ${e.message}", e)
                onError(e)
            }
        }
    }

    /**
     * Loop de leitura do túnel remoto: recebe pacotes IP de resposta do servidor
     * e injeta diretamente na interface TUN para consumo pelos apps locais.
     */
    private fun runTunnelToTunWorker() {
        val inStream = tunnelIn ?: return
        val outStream = tunOutputStream ?: return
        val packetBuffer = ByteArray(65535)

        try {
            while (isRunning.get()) {
                // Lê tamanho do pacote IP delimitado pelo túnel
                val packetLength = inStream.readUnsignedShort()
                if (packetLength > packetBuffer.size) {
                    throw IOException("Pacote recebido excede tamanho máximo de buffer: $packetLength")
                }

                // Lê exatamente a quantidade de bytes do pacote IP completo
                inStream.readFully(packetBuffer, 0, packetLength)

                // Escreve pacote de volta na interface TUN do Android
                outStream.write(packetBuffer, 0, packetLength)
                outStream.flush()

                val totalIn = bytesIn.addAndGet(packetLength.toLong())
                onTrafficUpdate(totalIn, bytesOut.get())
            }
        } catch (e: Exception) {
            if (isRunning.get() && e !is CancellationException) {
                Log.e(TAG, "Erro no worker Túnel -> TUN: ${e.message}", e)
                onError(e)
            }
        }
    }

    /**
     * Encerra com segurança ambos os fluxos e fecha streams associados.
     */
    fun stop() {
        if (!isRunning.compareAndSet(true, false)) return

        Log.i(TAG, "Interrompendo TunPacketRouter e liberando recursos...")
        tunToTunnelJob?.cancel()
        tunnelToTunJob?.cancel()

        try {
            tunInputStream?.close()
        } catch (_: Exception) {}
        try {
            tunOutputStream?.close()
        } catch (_: Exception) {}

        tunInputStream = null
        tunOutputStream = null
        tunnelIn = null
        tunnelOut = null

        Log.i(TAG, "TunPacketRouter encerrado.")
    }

    fun getStats(): Pair<Long, Long> = Pair(bytesIn.get(), bytesOut.get())
}
