package com.retameur.dartsconnect

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.writeByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.markNow

object DartboardDartsConnect {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val selectorManager = SelectorManager(Dispatchers.IO)
    private var connectionRetryJob: Job? = null

    private var socket: Socket? = null
    private var readChannel: ByteReadChannel? = null
    private var writeChannel: ByteWriteChannel? = null
    private var heartBeatJob: Job? = null
    private var lastRx: TimeSource.Monotonic.ValueTimeMark? = null

    private val MAGIC_CODE = listOf<UByte>(0x23u, 0x08u, 0x00u, 0x00u)
    private val TYPE_INFO = listOf<UByte>(0x01u, 0x00u, 0x00u, 0x00u)
    private val TYPE_HEARTBEAT = listOf<UByte>(0xFFu, 0x00u, 0x00u, 0x00u)
    private val TYPE_ACK = listOf<UByte>(0xFEu, 0x00u, 0x00u, 0x00u)
    private val SUBTYPE_IMAGE = listOf<UByte>(0x02u, 0x01u, 0x00u, 0x00u)

    val onHit = MutableSharedFlow<BoardCell>()
    val onRedButton = MutableSharedFlow<Unit>()

    private var _isConnected = MutableStateFlow(false)
    var isConnected = _isConnected.asStateFlow()

    private var _ip = MutableStateFlow("")
    var ip = _ip.asStateFlow()

    private var _directConnection = MutableStateFlow(true)
    var directConnection = _directConnection.asStateFlow()

    init {
        ioScope.launch {
            connect()
        }

        ioScope.launch {
            _ip.value = Settings.getIp()
            _directConnection.value = Settings.getDirectConnection()
        }

        ioScope.launch {
            _ip.collect { Settings.setIp(it) }
        }

        ioScope.launch {
            _directConnection.collect { Settings.setDirectConnection(it) }
        }
    }

    fun setDirectConnection() {
        _directConnection.value = true
        _ip.value = "192.168.0.1"
        disconnect()
    }

    fun setLocalNetworkConnection(ip: String) {
        _directConnection.value = false
        _ip.value = ip
        disconnect()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun connect() {
        println("DartboardDartsConnect - Connect to ${ip.value}")
        try {
            socket = aSocket(selectorManager).tcp().connect(ip.value, 11080) {
                keepAlive = true
                socketTimeout = Long.MAX_VALUE
            }

            readChannel = socket?.openReadChannel()
            writeChannel = socket?.openWriteChannel(autoFlush = true)
            _isConnected.value = true

            heartBeatJob?.cancel()
            heartBeatJob = ioScope.launch {
                while(isActive) {
                    delay(10000)

                    val lRx = lastRx
                    if (lRx != null && lRx.elapsedNow() > 15.seconds) {
                        println("DartboardDartsConnect - heartbeat timeout")
                        disconnect()
                    }

                    val arr = ubyteArrayOf(
                        0x23u, 0x08u, 0x00u, 0x00u,
                        0xFFu, 0x00u, 0x00u, 0x00u,
                        0x00u, 0x00u, 0x00u, 0x00u,
                        0x00u, 0x00u, 0x00u, 0x00u,
                        0x00u, 0x00u, 0x00u, 0x00u,
                    )

                    try {
                        println("DartboardDartsConnect - send heartbeat")
                        writeChannel?.writeByteArray(arr.toByteArray())
                        writeChannel?.flush()
                    } catch (e: Exception) {
                        println("DartboardDartsConnect - send heartbeat failed: ${e.message}")
                        disconnect()
                    }
                }
            }

            read()
        } catch (e: Exception) {
            println(e.message)
            disconnect()
        }
    }

    fun disconnect() {
        _isConnected.value = false
        heartBeatJob?.cancel()
        heartBeatJob = null
        lastRx = null
        connectionRetryJob?.cancel()
        connectionRetryJob = ioScope.launch {
            println("DartboardDartsConnect - Retry connection")
            delay(5000)
            if (isActive) {
                connect()
            }
        }
    }



    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun read() {
        val channel = readChannel ?: return

        try {
            while(channel.awaitContent(min = 25)) {

                println("DartboardDartsConnect - RX: availableForRead: ${channel.availableForRead}")
                val packet = channel.readPacket(channel.availableForRead)

                val bytes = packet.readByteArray(25).asUByteArray()

                // Check if the message starts with the magic code
                if(bytes.slice(0..< 4) != MAGIC_CODE) {
                    println("DartboardDartsConnect - RX: magic code missing")
                    continue
                }

                lastRx = markNow()
                val type = bytes.slice(4..<8)
                val subType = bytes.slice(8..<12)

                when(type) {
                    TYPE_HEARTBEAT -> {
                        println("DartboardDartsConnect - RX: Type Heartbeat")
                    }
                    TYPE_ACK -> {
                        println("DartboardDartsConnect - RX: Type Ack")
                    }
                    TYPE_INFO -> {
                        println("DartboardDartsConnect - RX: Type Info")
                        val key = bytes[24]
                        if (key.toInt() == 0) {
                            onRedButton.emit(Unit)
                            println("DartboardDartsConnect - RX: Red button")
                        } else {
                            val cell = try {
                                BoardCell.fromKey(key.toInt())
                            } catch (ex: Exception) {
                                null
                            }

                            if (cell != null) {
                                onHit.emit(cell)
                            }
                        }
                    }
                    else -> {
                        println("DartboardDartsConnect - RX: Unknown type: $type, $subType")
                    }
                }
            }
        } catch (e: Exception) {
            println("DartboardDartsConnect - RX: exception: ${e.message}")
            disconnect()
        }
    }

    suspend fun configWifi(ssid: String, password: String) {
        val head =
            2083.toByteArray() + // magic code
            2.toByteArray() + // type
            259.toByteArray() + // sub-type
            136.toByteArray() + // size
            0.toByteArray() // reserved

        val ssidBytes = ByteArray(64)
        ssid.toByteArray().copyInto(ssidBytes)
        val passwordBytes = ByteArray(64)
        password.toByteArray().copyInto(passwordBytes)

        val config =
            head +
            0.toByteArray() + // reserved
            0.toByteArray() + // result
            ssidBytes +
            passwordBytes

        writeChannel?.writeByteArray(config)
        disconnect()
    }
}

data class BoardCell(val number: Int, val multiplier: Int) {

    override fun toString(): String {
        val mult = when (multiplier) {
            1 -> ""
            2 -> "D"
            3 -> "T"
            else -> throw Exception("BoardCell - Unknown multiplier")
        }
        return "${mult}${number}"
    }

    companion object {
        fun fromKey(key: Int): BoardCell {
            return when (key) {
                133 -> BoardCell(number = 1, multiplier = 1)
                135 -> BoardCell(number = 2, multiplier = 1)
                131 -> BoardCell(number = 3, multiplier = 1)
                137 -> BoardCell(number = 4, multiplier = 1)
                129 -> BoardCell(number = 5, multiplier = 1)
                139 -> BoardCell(number = 6, multiplier = 1)
                130 -> BoardCell(number = 7, multiplier = 1)
                138 -> BoardCell(number = 8, multiplier = 1)
                132 -> BoardCell(number = 9, multiplier = 1)
                136 -> BoardCell(number = 10, multiplier = 1)
                134 -> BoardCell(number = 11, multiplier = 1)
                146 -> BoardCell(number = 12, multiplier = 1)
                142 -> BoardCell(number = 13, multiplier = 1)
                141 -> BoardCell(number = 14, multiplier = 1)
                144 -> BoardCell(number = 15, multiplier = 1)
                143 -> BoardCell(number = 16, multiplier = 1)
                147 -> BoardCell(number = 17, multiplier = 1)
                140 -> BoardCell(number = 18, multiplier = 1)
                145 -> BoardCell(number = 19, multiplier = 1)
                148 -> BoardCell(number = 20, multiplier = 1)
                153 -> BoardCell(number = 25, multiplier = 1)

                5 -> BoardCell(number = 1, multiplier = 1)
                7 -> BoardCell(number = 2, multiplier = 1)
                3 -> BoardCell(number = 3, multiplier = 1)
                9 -> BoardCell(number = 4, multiplier = 1)
                1 -> BoardCell(number = 5, multiplier = 1)
                11 -> BoardCell(number = 6, multiplier = 1)
                2 -> BoardCell(number = 7, multiplier = 1)
                10 -> BoardCell(number = 8, multiplier = 1)
                4 -> BoardCell(number = 9, multiplier = 1)
                8 -> BoardCell(number = 10, multiplier = 1)
                6 -> BoardCell(number = 11, multiplier = 1)
                18 -> BoardCell(number = 12, multiplier = 1)
                14 -> BoardCell(number = 13, multiplier = 1)
                13 -> BoardCell(number = 14, multiplier = 1)
                16 -> BoardCell(number = 15, multiplier = 1)
                15 -> BoardCell(number = 16, multiplier = 1)
                19 -> BoardCell(number = 17, multiplier = 1)
                12 -> BoardCell(number = 18, multiplier = 1)
                17 -> BoardCell(number = 19, multiplier = 1)
                20 -> BoardCell(number = 20, multiplier = 1)
                25 -> BoardCell(number = 25, multiplier = 1)

                37 -> BoardCell(number = 1, multiplier = 2)
                39 -> BoardCell(number = 2, multiplier = 2)
                35 -> BoardCell(number = 3, multiplier = 2)
                41 -> BoardCell(number = 4, multiplier = 2)
                33 -> BoardCell(number = 5, multiplier = 2)
                43 -> BoardCell(number = 6, multiplier = 2)
                34 -> BoardCell(number = 7, multiplier = 2)
                42 -> BoardCell(number = 8, multiplier = 2)
                36 -> BoardCell(number = 9, multiplier = 2)
                40 -> BoardCell(number = 10, multiplier = 2)
                38 -> BoardCell(number = 11, multiplier = 2)
                50 -> BoardCell(number = 12, multiplier = 2)
                46 -> BoardCell(number = 13, multiplier = 2)
                45 -> BoardCell(number = 14, multiplier = 2)
                48 -> BoardCell(number = 15, multiplier = 2)
                47 -> BoardCell(number = 16, multiplier = 2)
                51 -> BoardCell(number = 17, multiplier = 2)
                44 -> BoardCell(number = 18, multiplier = 2)
                49 -> BoardCell(number = 19, multiplier = 2)
                52 -> BoardCell(number = 20, multiplier = 2)
                57 -> BoardCell(number = 25, multiplier = 2)

                69 -> BoardCell(number = 1, multiplier = 3)
                71 -> BoardCell(number = 2, multiplier = 3)
                67 -> BoardCell(number = 3, multiplier = 3)
                73 -> BoardCell(number = 4, multiplier = 3)
                65 -> BoardCell(number = 5, multiplier = 3)
                75 -> BoardCell(number = 6, multiplier = 3)
                66 -> BoardCell(number = 7, multiplier = 3)
                74 -> BoardCell(number = 8, multiplier = 3)
                68 -> BoardCell(number = 9, multiplier = 3)
                72 -> BoardCell(number = 10, multiplier = 3)
                70 -> BoardCell(number = 11, multiplier = 3)
                82 -> BoardCell(number = 12, multiplier = 3)
                78 -> BoardCell(number = 13, multiplier = 3)
                77 -> BoardCell(number = 14, multiplier = 3)
                80 -> BoardCell(number = 15, multiplier = 3)
                79 -> BoardCell(number = 16, multiplier = 3)
                83 -> BoardCell(number = 17, multiplier = 3)
                76 -> BoardCell(number = 18, multiplier = 3)
                81 -> BoardCell(number = 19, multiplier = 3)
                84 -> BoardCell(number = 20, multiplier = 3)
                89 -> BoardCell(number = 25, multiplier = 3)
                else -> throw Exception("BoardCell - Unknown key")
            }
        }
    }
}

fun Int.toByteArray(): ByteArray {
   return ByteArray(4) { i -> (this.toLong() shr (i*8)).toByte() }
}