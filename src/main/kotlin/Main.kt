import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

fun main() {
    val mapper = jacksonObjectMapper()

    val string = """
        [{"points":7,"subCodes":["1", "1a", "1b"],"code":"1","d":0.7,"e":0.7},{"points":7,"subCodes":["2", "2a", "2b"],"code":"2","d":0.9,"e":0.9},{"points":1,"subCodes":["20294212"],"code":"20294212","d":1.0,"e":6.0},{"points":1,"subCodes":["25843651"],"code":"25843651","d":0.1,"e":0.1},{"points":1,"subCodes":["25959109","27704608"],"code":"27704608","d":0.1,"e":0.1}]        
    """.trimIndent()

    val item1 = Item("1", 1.0, null)
    val item2 = Item("2", 1.0, "test")
    val item3 = Item("1", 1.0, null)
    val item4 = Item("4", 1.0, null)
    val items = listOf(item1, item2, item3, item4)

    val recommendations = mapper.readValue<List<Recommendation>>(string)


    val itemsWithOffer = items.map { item ->
        item to recommendations.find { it.code == item.code }
    }.toMutableList()

    val usedRecommendations = mutableSetOf<Recommendation>()
//    val usedRecommendations = mutableListOf<Recommendation>()

    items.map { item ->
        val (offer, quantity) = itemsWithOffer.find { it.first == item }
            ?.let { pair ->
                itemsWithOffer.remove(pair)
                pair.second to if (pair.first.measurement == null) {
                    pair.first.quantity
                } else {
                    itemsWithOffer
                        .filter { it.second == pair.second }
                        .sumOf { it.first.quantity }
                        .also { itemsWithOffer.removeAll { it.second == pair.second } }
                }
            } ?: (null to 0.0)

        ItemWithPoints(
            item,
            Logic.countPoints(offer, quantity).also {
                if (it > 0 && offer != null) {
                    usedRecommendations.add(offer).also { res -> println(res) }
                }
            }
        )
    }

    println(usedRecommendations.size)
    println(usedRecommendations)
}

data class Recommendation(
    override val points: Int,
    override val code: String,
    override val subCodes: List<String>,
    override val d: Double,
    override var e: Double,
    override var f: State = State.AVAILABLE
) : IRecommendation

interface IRecommendation {
    val points: Int
    val code: String
    val subCodes: List<String>
    val d: Double
    var e: Double
    var f: State
}

enum class State {
    AVAILABLE,
    NOT_AVAILABLE
}

data class Item(val code: String, val quantity: Double, val measurement: String?)
data class ItemWithPoints(val item: Item, val points: Int?)

object Logic {
    fun countPoints(recommendation: Recommendation?, quantity: Double): Int {
        return (recommendation?.points?.times(quantity.toInt())) ?: 0
    }
}
