package net.thornydev

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileReader
import java.util.*

/**
 * Generates Hive schemas for use with the JSON SerDe from org.openx.data.jsonserde.JsonSerDe.
 * GitHub link: https://github.com/rcongiu/Hive-JSON-Serde
 *
 *
 * Pass in a valid JSON document string to [JsonHiveSchema.createHiveSchema] and it will
 * return a Hive schema for the JSON document.
 *
 *
 * It supports embedded JSON objects, arrays and the standard JSON scalar types: strings, numbers,
 * booleans and null.  You probably don't want null in the JSON document you provide as Hive can't
 * use that.  For numbers - if the example value has a decimal, it will be typed as "double".  If
 * the number has no decimal, it will be typed as "int".
 *
 *
 * This program uses the JSON parsing code from json.org and that code is included in this library,
 * since it has not been packaged and made available for maven/ivy/gradle dependency resolution.
 *
 * **Use of main method:** <br></br>
 * JsonHiveSchema has a main method that takes a file path to a JSON doc - this file should have
 * only one JSON file in it.  An optional second argument can be provided to name the Hive table
 * that is generated.
 */
class JsonHiveSchema {
    private var tableName = "x"

    constructor() {}
    constructor(tableName: String) {
        this.tableName = tableName
    }

    /**
     * Pass in any valid JSON object and a Hive schema will be returned for it. You should avoid
     * having null values in the JSON document, however.
     *
     *
     * The Hive schema columns will be printed in alphabetical order - overall and within
     * subsections.
     *
     * @param json
     * @return string Hive schema
     * @throws JSONException if the JSON does not parse correctly
     */
    @Throws(JSONException::class)
    fun createHiveSchema(json: String?): String {
        val jo = JSONObject(json)
        var keys = jo.keys()
        keys = OrderedIterator(keys)
        val sb = StringBuilder("CREATE TABLE ").append(tableName).append(" (\n")
        while (keys.hasNext()) {
            val k = keys.next()
            sb.append("  ")
            sb.append(k)
            sb.append(' ')
            sb.append(valueToHiveSchema(jo.opt(k)))
            sb.append(',').append("\n")
        }
        sb.replace(sb.length - 2, sb.length, ")\n") // remove last comma
        return sb.append("ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe';").toString()
    }

    @Throws(JSONException::class)
    private fun toHiveSchema(o: JSONObject): String {
        var keys = o.keys()
        keys = OrderedIterator(keys)
        val sb = StringBuilder("struct<")
        while (keys.hasNext()) {
            val k = keys.next()
            sb.append(k)
            sb.append(':')
            sb.append(valueToHiveSchema(o.opt(k)))
            sb.append(", ")
        }
        sb.replace(sb.length - 2, sb.length, ">") // remove last comma
        return sb.toString()
    }

    @Throws(JSONException::class)
    private fun toHiveSchema(a: JSONArray): String {
        return "array<" + arrayJoin(a, ",") + '>'
    }

    @Throws(JSONException::class)
    private fun arrayJoin(a: JSONArray, separator: String): String {
        val sb = StringBuilder()
        check(a.length() != 0) { "Array is empty: $a" }
        val entry0 = a[0]
        if (isScalar(entry0)) {
            sb.append(scalarType(entry0))
        } else if (entry0 is JSONObject) {
            sb.append(toHiveSchema(entry0))
        } else if (entry0 is JSONArray) {
            sb.append(toHiveSchema(entry0))
        }
        return sb.toString()
    }

    private fun scalarType(o: Any): String? {
        if (o is String) {
            return "string"
        }
        if (o is Number) {
            return scalarNumericType(o)
        }
        return if (o is Boolean) {
            "boolean"
        } else null
    }

    private fun scalarNumericType(o: Any): String {
        val s = o.toString()
        return if (s.indexOf('.') > 0) {
            "double"
        } else {
            "int"
        }
    }

    private fun isScalar(o: Any): Boolean {
        return o is String ||
                o is Number ||
                o is Boolean || o === JSONObject.NULL
    }

    @Throws(JSONException::class)
    private fun valueToHiveSchema(o: Any): String? {
        return if (isScalar(o)) {
            scalarType(o)
        } else if (o is JSONObject) {
            toHiveSchema(o)
        } else if (o is JSONArray) {
            toHiveSchema(o)
        } else {
            throw IllegalArgumentException("unknown type: " + o.javaClass)
        }
    }

    internal class OrderedIterator(iter: Iterator<String?>) : MutableIterator<String?> {
        var it: MutableIterator<String?>
        override fun hasNext(): Boolean {
            return it.hasNext()
        }

        override fun next(): String? {
            return it.next()
        }

        override fun remove() {
            it.remove()
        }

        init {
            val keys: SortedSet<String?> = TreeSet()
            while (iter.hasNext()) {
                keys.add(iter.next())
            }
            it = keys.iterator()
        }
    }

    companion object {
        fun help() {
            println("Usage: Two arguments possible. First is required. Second is optional")
            println("  1st arg: path to JSON file to parse into Hive schema")
            println("  2nd arg (optional): tablename.  Defaults to 'x'")
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            require(args.size != 0) { "ERROR: No file specified" }
            if (args[0] == "-h") {
                help()
                System.exit(0)
            }
            val sb = StringBuilder()
            val br = BufferedReader(FileReader(args[0]))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            br.close()
            var tableName = "x"
            if (args.size == 2) {
                tableName = args[1]
            }
            val schemaWriter = JsonHiveSchema(tableName)
            println(schemaWriter.createHiveSchema(sb.toString()))
        }
    }
}