package ru.yuksanbo.common.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import de.undercouch.actson.JsonEvent
import de.undercouch.actson.JsonParser
import javaslang.control.Either
import ru.yuksanbo.common.misc.ControlException
import java.util.LinkedList

class JsonParserIntoJackson(private val jsonParser: JsonParser) {

    companion object {
        val om = com.fasterxml.jackson.databind.ObjectMapper()
    }

    private var stack: LinkedList<JsonNode> = LinkedList()
    private var root: JsonNode? = null

    private var fieldName: String? = null

    private var result0: Either<ParserException, JsonNode>? = null

    private inline fun firstObj(): ObjectNode = (stack.first as ObjectNode)

    fun result(): Either<ParserException, JsonNode> {
        assert(result0 != null)
        return result0!!
    }

    fun continueParsing() {
        if (result0 != null) {
            throw IllegalStateException("Illegal continueParsing(), the state is finished with ${result0}")
        }
        nextEvent@ while (true) {
            val event = jsonParser.nextEvent()
            when (event) {
                JsonEvent.ERROR -> {
                    result0 = Either.left(ParserException(
                            "Json parsing error at position ${jsonParser.parsedCharacterCount}"
                    ))
                    return
                }
                JsonEvent.START_OBJECT -> {
                    if (root == null) {
                        val newObject = om.createObjectNode()
                        root = newObject
                        stack.addFirst(newObject)
                        continue@nextEvent
                    }

                    if (fieldName != null) {
                        stack.addFirst(firstObj().putObject(fieldName))
                        continue@nextEvent
                    }

                    stack.addFirst((stack.first as ArrayNode).addObject())
                }

                JsonEvent.END_OBJECT -> stack.removeFirst()

                JsonEvent.START_ARRAY -> {
                    val newArray = om.createArrayNode()
                    if (root == null) {
                        root = newArray
                        stack.addFirst(newArray)
                        continue@nextEvent
                    }

                    firstObj().putArray(fieldName)
                }

                JsonEvent.END_ARRAY -> stack.removeFirst()

                JsonEvent.FIELD_NAME -> fieldName = jsonParser.currentString

                JsonEvent.VALUE_DOUBLE, JsonEvent.VALUE_INT, JsonEvent.VALUE_STRING, JsonEvent.VALUE_TRUE, JsonEvent.VALUE_FALSE,
                JsonEvent.VALUE_NULL -> {
                    val obj = firstObj()
                    when (event) {
                        JsonEvent.VALUE_DOUBLE -> obj.put(fieldName, jsonParser.currentDouble)
                        JsonEvent.VALUE_INT -> obj.put(fieldName, jsonParser.currentInt)
                        JsonEvent.VALUE_STRING -> obj.put(fieldName, jsonParser.currentString)
                        JsonEvent.VALUE_TRUE -> obj.put(fieldName, true)
                        JsonEvent.VALUE_FALSE -> obj.put(fieldName, false)
                        JsonEvent.VALUE_NULL -> obj.putNull(fieldName)
                    }
                    fieldName = null
                }

                JsonEvent.NEED_MORE_INPUT -> return

                JsonEvent.EOF -> {
                    result0 = Either.right(root)
                    return
                }
            }
        }
    }

    class ParserException : ControlException {
        constructor() : super()
        constructor(message: String?) : super(message)
        constructor(message: String?, cause: Throwable?) : super(message, cause)
        constructor(cause: Throwable?) : super(cause)
    }
}