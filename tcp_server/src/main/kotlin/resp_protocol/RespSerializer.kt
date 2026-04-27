package resp_protocol

import java.io.InputStream
import java.lang.RuntimeException

interface DataType {
    class StringType(val value: String) : DataType

    class IntegerType(val value: Int) : DataType

    class ErrorType(val message: String) : DataType

    class BulkStringType(val value: String?) : DataType

    class ArrayType(val elements: List<DataType>) : DataType

    object NullType : DataType
}

class Result(val result: DataType, val position: Int, val error: Exception? = null)


object RespSerializer {
    fun serialize(command: String): String = TODO()

    fun deserialize(respData: InputStream): Result {
        val result = when (val type = respData.read().toChar()) {
            '+' -> readSimpleString(respData)
            '-' -> readError(respData)
            ':' -> readInteger(respData)
            '$' -> readBulkString(respData)
            '*' -> readArray(respData)
            else -> throw IllegalArgumentException("Unknown RESP type: $type")
        }

        return result
    }

    /**
        *2\r\n
        $3\r\nGET\r\n
        $3\r\nkey\r\n
    **/
    // output: ["GET", "key"]
    private fun readArray(input: InputStream): Result {
        val firstChar = input.read().toChar()
        val numberOfElements = readInteger(input)

        val elements = mutableListOf<DataType>()
        var position = numberOfElements.position
        for (i in 0..<(numberOfElements.result as DataType.IntegerType).value) {
            val elementResult = deserialize(input)

            if (elementResult.error != null) {
                return Result(result = DataType.ErrorType(elementResult.error.message ?: "Unknown error"), position = position, error = elementResult.error)
            } else {
                elements.add(elementResult.result)
                position += elementResult.position
            }
        }

        return Result(result = DataType.ArrayType(elements), position = position)
    }

    // example: +OK\r\n -> expected output: OK
    // first char is already read in deserialize function, so we start from O
    private fun readSimpleString(input: InputStream): Result {
        var position = 0
        val result = StringBuilder()

        while(true) {
            val char = input.read()
            if(char == -1) throw RuntimeException("Unexpected end of stream")

            val value = char.toChar()

            if(value == '\r') {
                val next = input.read()
                if(next != '\n'.code) {
                    throw RuntimeException("Expected LF after CR, but got: ${next.toChar()}")
                }

                position++
                break
            }

            result.append(value)
            position++
        }

        return Result(result = DataType.StringType(result.toString()), position = position + 2)
    }

    // example: :100\r\n -> expected output: 100
    private fun readInteger(input: InputStream): Result {
        var position = 0
        var result: Int = 0

        while(true) {
            val char = input.read()
            if(char == -1) throw RuntimeException("Unexpected end of stream")
            val value = char.toChar()
            if(value == '\r') {
                val next = input.read()
                if(next != '\n'.code) {
                    throw RuntimeException("Expected LF after CR, but got: ${next.toChar()}")
                }

                position++
                break
            }
            result = result * 10 + (input.read().toChar() - '0')
            position++
        }

        return Result(result = DataType.IntegerType(result), position = position + 2)
    }

    // example: $6\r\nfoobar\r\n -> expected output: foobar
    private fun readBulkString(input: InputStream): Result {
        var position = 0
        val lengthObject = readInteger(input)
        val length = (lengthObject.result as DataType.IntegerType).value
        position = lengthObject.position

        if (length == -1) {
            return Result(result = DataType.BulkStringType(null), position = position)
        }

        position += 2 // skip \r\n
        val result = StringBuilder()

        while(result.length < length) {
            result.append(input.read().toChar())
            position++
        }

        return Result(result = DataType.BulkStringType(result.toString()), position = position + 2)
    }

    // example: $-1\r\n -> expected output: null
    private fun readError(input: InputStream): Result {
        val firstChar = input.read().toChar()
        var position = 1
        val result = StringBuilder()

        while(input.read().toChar() != '\r') {
            result.append(input.read().toChar())
            position++
        }

        return Result(result = DataType.ErrorType(result.toString()), position = position + 2)
    }
}