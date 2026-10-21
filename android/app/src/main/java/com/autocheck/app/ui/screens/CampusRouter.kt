package com.autocheck.app.ui.screens

import android.content.Context
import androidx.compose.ui.geometry.Offset
import org.json.JSONObject
import java.util.PriorityQueue
import kotlin.math.hypot

data class RouteNode(
    val floor: Int,
    val x: Float,
    val y: Float,
)

data class RouteStep(
    val floor: Int,
    val description: String,
    val points: List<Offset>,
)

class CampusRouter(
    val nodes: List<RouteNode>,
    val adjacency: List<List<Pair<Int, Float>>>,
    val roomTargets: Map<String, Int>,
    val entranceIdx: Int,
) {
    fun findPath(
        startFloor: Int,
        startName: String?,
        startPoint: Offset?,
        destFloor: Int,
        destName: String?,
        destPoint: Offset?
    ): List<RouteNode> {
        val startIdx = resolveNodeIndex(startFloor, startName, startPoint) ?: entranceIdx
        val destIdx = resolveNodeIndex(destFloor, destName, destPoint) ?: return emptyList()

        if (startIdx == destIdx) return listOf(nodes[startIdx])
        if (nodes.isEmpty() || startIdx !in nodes.indices || destIdx !in nodes.indices) return emptyList()

        val dist = FloatArray(nodes.size) { Float.MAX_VALUE }
        val prev = IntArray(nodes.size) { -1 }
        val pq = PriorityQueue<Pair<Int, Float>>(compareBy { it.second })

        dist[startIdx] = 0f
        pq.add(startIdx to 0f)

        while (pq.isNotEmpty()) {
            val (u, d) = pq.poll()
            if (u == destIdx) break
            if (d > dist[u]) continue

            for ((v, weight) in adjacency[u]) {
                val newDist = d + weight
                if (newDist < dist[v]) {
                    dist[v] = newDist
                    prev[v] = u
                    pq.add(v to newDist)
                }
            }
        }

        if (dist[destIdx] == Float.MAX_VALUE) return emptyList()

        val path = mutableListOf<RouteNode>()
        var curr = destIdx
        while (curr != -1) {
            path.add(nodes[curr])
            if (curr == startIdx) break
            curr = prev[curr]
        }
        path.reverse()
        return path
    }

    private fun resolveNodeIndex(floor: Int, name: String?, point: Offset?): Int? {
        if (name == "Главный вход" || name == "start") return entranceIdx
        if (name != null) {
            roomTargets["name:$name"]?.let { return it }
            roomTargets["$floor:$name"]?.let { return it }
            roomTargets[name]?.let { return it }
            val cleanName = name.replace("ауд.", "").replace("каб.", "").trim()
            roomTargets["name:$cleanName"]?.let { return it }
        }
        // Fallback to closest node on this floor
        if (point != null) {
            var bestIdx: Int? = null
            var bestDist = Float.MAX_VALUE
            for (i in nodes.indices) {
                val n = nodes[i]
                if (n.floor == floor) {
                    val d = hypot(n.x - point.x, n.y - point.y)
                    if (d < bestDist) {
                        bestDist = d
                        bestIdx = i
                    }
                }
            }
            return bestIdx
        }
        return null
    }

    /**
     * Разбивает сплошной маршрут на шаги по этажам (для отображения плашек этапов и фильтрации отрисовки)
     */
    fun splitIntoSteps(fullRoute: List<RouteNode>, targetName: String): List<RouteStep> {
        if (fullRoute.isEmpty()) return emptyList()

        val steps = mutableListOf<RouteStep>()
        var currentFloor = fullRoute[0].floor
        var currentPoints = mutableListOf<Offset>()

        for (node in fullRoute) {
            if (node.floor != currentFloor) {
                if (currentPoints.isNotEmpty()) {
                    steps.add(
                        RouteStep(
                            floor = currentFloor,
                            description = "${currentFloor} этаж (переход к лестнице/корпусу)",
                            points = currentPoints.toList()
                        )
                    )
                    currentPoints = mutableListOf()
                }
                currentFloor = node.floor
            }
            currentPoints.add(Offset(node.x, node.y))
        }

        if (currentPoints.isNotEmpty()) {
            steps.add(
                RouteStep(
                    floor = currentFloor,
                    description = "${currentFloor} этаж (к $targetName)",
                    points = currentPoints.toList()
                )
            )
        }

        return steps
    }

    companion object {
        fun load(context: Context): CampusRouter {
            return try {
                val jsonStr = context.assets.open("maps/campus_graph.json").bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)

                val nodesArr = obj.getJSONArray("nodes")
                val nodesList = ArrayList<RouteNode>(nodesArr.length())
                for (i in 0 until nodesArr.length()) {
                    val item = nodesArr.getJSONArray(i)
                    nodesList.add(
                        RouteNode(
                            floor = item.getInt(0),
                            x = item.getDouble(1).toFloat(),
                            y = item.getDouble(2).toFloat(),
                        )
                    )
                }

                val adjList = List(nodesList.size) { mutableListOf<Pair<Int, Float>>() }
                val edgesArr = obj.getJSONArray("edges")
                for (i in 0 until edgesArr.length()) {
                    val edge = edgesArr.getJSONArray(i)
                    val u = edge.getInt(0)
                    val v = edge.getInt(1)
                    val w = edge.getDouble(2).toFloat()
                    if (u in adjList.indices && v in adjList.indices) {
                        adjList[u].add(v to w)
                        adjList[v].add(u to w)
                    }
                }

                val roomTargets = mutableMapOf<String, Int>()
                val targetsObj = obj.getJSONObject("room_targets")
                val keys = targetsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    roomTargets[k] = targetsObj.getInt(k)
                }

                val entranceIdx = obj.optInt("entrance_idx", 0)
                CampusRouter(nodesList, adjList, roomTargets, entranceIdx)
            } catch (e: Exception) {
                CampusRouter(emptyList(), emptyList(), emptyMap(), 0)
            }
        }
    }
}
