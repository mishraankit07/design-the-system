import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket

fun main() {
    val port = 9999
    val serverSocket = ServerSocket(port)

    println("Echo server started on port $port...")

    while (true) {
        println("Waiting for client...")

        val clientSocket = serverSocket.accept()  // BLOCKING
        println("Client connected: ${clientSocket.inetAddress}")

        val reader = BufferedReader(
            InputStreamReader(clientSocket.getInputStream())
        )
        val writer = PrintWriter(clientSocket.getOutputStream(), true)

        var line: String?

        while (true) {
            line = reader.readLine()  // BLOCKING

            if (line == null) {
                println("Client disconnected")
                break
            }

            println("Received: $line")

            // Echo back
            writer.println(line)
        }

        clientSocket.close()
    }
}