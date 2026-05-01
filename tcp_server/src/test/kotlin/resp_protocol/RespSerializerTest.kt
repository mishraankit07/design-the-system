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

    @Test
    fun `given an error, when deserialized, then return the correct output`() {
        val input = "-1\r\n".byteInputStream()
        val result = RespSerializer.deserialize(input)
        assert(result.result is DataType.NullType)
    }

    @Test
    fun `given an array of bulk strings, when deserialized, then return the correct output`() {
        val input = ("*2\r\n" +
                "$3\r\nram\r\n" +
                "$6\r\nfoobar\r\n").byteInputStream()
        val result = RespSerializer.deserialize(input)

        assert(result.result is DataType.ArrayType)
        val array = result.result as DataType.ArrayType
        assert(array.elements.size == 2)
        assert(array.elements[0] is DataType.BulkStringType)
        assert((array.elements[0] as DataType.BulkStringType).value == "ram")
        assert(array.elements[1] is DataType.BulkStringType)
        assert((array.elements[1] as DataType.BulkStringType).value == "foobar")
    }

    @Test
    fun `given an array within an array of bulk strings, when deserialized, then return the correct output`() {
        val input = ("*2\r\n" +
                "*2\r\n" +
                "$3\r\nfoo\r\n" +
                "$3\r\nbar\r\n" +
                "*3\r\n" +
                "$3\r\nbaz\r\n" +
                "$3\r\nqux\r\n" +
                "$4\r\nquux\r\n").byteInputStream()
        val result = RespSerializer.deserialize(input)

        assert(result.result is DataType.ArrayType)
        val array = result.result as DataType.ArrayType
        assert(array.elements.size == 2)

        // First element is an array of two bulk strings
        assert(array.elements[0] is DataType.ArrayType)
        val firstArray = array.elements[0] as DataType.ArrayType
        assert(firstArray.elements.size == 2)
        assert(firstArray.elements[0] is DataType.BulkStringType)
        assert((firstArray.elements[0] as DataType.BulkStringType).value == "foo")
        assert(firstArray.elements[1] is DataType.BulkStringType)
        assert((firstArray.elements[1] as DataType.BulkStringType).value == "bar")

        // Second element is an array of three bulk strings
        assert(array.elements[1] is DataType.ArrayType)
        val secondArray = array.elements[1] as DataType.ArrayType
        assert(secondArray.elements.size == 3)
        assert(secondArray.elements[0] is DataType.BulkStringType)
        assert((secondArray.elements[0] as DataType.BulkStringType).value == "baz")
        assert(secondArray.elements[1] is DataType.BulkStringType)
        assert((secondArray.elements[1] as DataType.BulkStringType).value == "qux")
        assert(secondArray.elements[2] is DataType.BulkStringType)
        assert((secondArray.elements[2] as DataType.BulkStringType).value == "quux")
    }
}