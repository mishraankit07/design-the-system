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

class Result(val result: DataType, val error: Exception? = null)


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
        var numberOfElements = 0

        while(true){
            val element = input.read()
            if(element == -1) throw RuntimeException("Unexpected end of stream")
            val value = element.toChar()
            if(value == '\r') {
                val next = input.read()
                if(next != '\n'.code) {
                    throw RuntimeException("Expected LF after CR, but got: ${next.toChar()}")
                }

                break
            }

            numberOfElements = numberOfElements * 10 + (value - '0')
        }


        val elements = mutableListOf<DataType>()
        repeat(numberOfElements) {
            val elementResult = deserialize(input)

            if (elementResult.error != null) {
                return Result(result = DataType.ErrorType(elementResult.error.message ?: "Unknown error"), error = elementResult.error)
            } else {
                elements.add(elementResult.result)
            }
        }

        return Result(result = DataType.ArrayType(elements))
    }

    // example: +OK\r\n -> expected output: OK
    // first char is already read in deserialize function, so we start from O
    private fun readSimpleString(input: InputStream): Result {
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

                break
            }

            result.append(value)
        }

        return Result(result = DataType.StringType(result.toString()))
    }

    // example: :100\r\n -> expected output: 100
    private fun readInteger(input: InputStream): Result {
        var result: Int = 0

        while(true) {
            val inputByte = input.read()
            if(inputByte == -1) throw RuntimeException("Unexpected end of stream")
            val value = inputByte.toChar()
            if(value == '\r') {
                val next = input.read()
                if(next != '\n'.code) {
                    throw RuntimeException("Expected LF after CR, but got: ${next.toChar()}")
                }

                break
            }

            result = result * 10 + (value - '0')
        }

        return Result(result = DataType.IntegerType(result))
    }

    // example: $6\r\nfoobar\r\n -> expected output: foobar
    private fun readBulkString(input: InputStream): Result {
        val lengthObject = readInteger(input)
        val length = (lengthObject.result as DataType.IntegerType).value

        if (length == -1) {
            return Result(result = DataType.BulkStringType(null))
        }

        val result = StringBuilder()

        while(result.length < length) {
            val b = input.read()
            if (b == -1) throw RuntimeException("Unexpected end of stream")
            result.append(b.toChar())
        }

        val cr = input.read()
        val lf = input.read()
        if (cr != '\r'.code || lf != '\n'.code) {
            throw RuntimeException("Expected CRLF after bulk string payload")
        }
        return Result(result = DataType.BulkStringType(result.toString()))
    }

    // example: $-1\r\n -> expected output: null
    private fun readError(input: InputStream): Result {
        while(true) {
            val char = input.read().toChar()
            if(char == '\r') {
                val next = input.read()
                if(next != '\n'.code) {
                    throw RuntimeException("Expected LF after CR, but got: ${next.toChar()}")
                }

                break
            }
        }

        return Result(result = DataType.NullType)
    }
}