/*
 * Copyright 2025 McXross
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.mcxross.agg.cetus

fun completionCoin(value: String): String {
  val index = value.indexOf("::")
  if (index == -1) {
    return value
  }

  val prefix = value.substring(0, index)
  val rest = value.substring(index)

  if (!prefix.startsWith("0x")) {
    return value
  }

  val hexStr = prefix.removePrefix("0x")
  if (hexStr.length > 64) {
    return value
  }

  val padded = hexStr.padStart(64, '0')
  return "0x$padded$rest"
}

fun processFlattenRoutes(routerData: RouterDataV3): ProcessedRouterData {
  val paths = routerData.paths
  val fromCoinType = paths.first().from
  val targetCoinType = paths.last().target

  val flattenedPaths =
    paths
      .map { path -> FlattenedPath(path = path, isLastUseOfIntermediateToken = false) }
      .toMutableList()

  val seenTokens = mutableSetOf<String>()
  for (index in flattenedPaths.indices.reversed()) {
    val from = flattenedPaths[index].path.from
    if (seenTokens.add(from)) {
      flattenedPaths[index] = flattenedPaths[index].copy(isLastUseOfIntermediateToken = true)
    }
  }

  return ProcessedRouterData(
    quoteID = routerData.quoteID.orEmpty(),
    amountIn = routerData.amountIn,
    amountOut = routerData.amountOut,
    byAmountIn = routerData.byAmountIn,
    flattenedPaths = flattenedPaths,
    fromCoinType = fromCoinType,
    targetCoinType = targetCoinType,
    packages = routerData.packages,
    totalDeepFee = routerData.totalDeepFee,
    error = routerData.error,
    overlayFee = routerData.overlayFee,
  )
}
