package resp_protocol

import org.junit.jupiter.api.Test

class RespSerializerTest {
    @Test
    fun `given a simple string, when deserialized, then return the correct result`() {
        val input = "+OK\r\n".byteInputStream()
        val result = RespSerializer.deserialize(input)

        assert(result.result is DataType.StringType)
        assert((result.result as DataType.StringType).value == "OK")
    }

    @Test
    fun `given an integer string, when deserialized, then return the correct result`() {
        val input = ":1000\r\n".byteInputStream()
        val result = RespSerializer.deserialize(input)

        assert(result.result is DataType.IntegerType)
        assert((result.result as DataType.IntegerType).value == 1000)
    }

    @Test
    fun `given a bulk string, when deserialized, then return the correct result`() {
        val input = "$6\r\nfoobar\r\n".byteInputStream()
        val result = RespSerializer.deserialize(input)

        assert(result.result is DataType.BulkStringType)
        assert((result.result as DataType.BulkStringType).value == "foobar")
    }
}