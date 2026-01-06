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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class FindRouterParams(
  val from: String,
  val target: String,
  val amount: String,
  val byAmountIn: Boolean,
  val depth: Int? = null,
  val splitAlgorithm: String? = null,
  val splitFactor: Int? = null,
  val splitCount: Int? = null,
  val providers: List<String>? = null,
  val liquidityChanges: List<PreSwapLpChangeParams>? = null,
)

@Serializable
data class PreSwapLpChangeParams(
  val poolID: String,
  val tickLower: Int,
  val tickUpper: Int,
  val deltaLiquidity: Int,
)

@Serializable data class RouterError(val code: Int, val msg: String)

@Serializable
data class Path(
  val id: String,
  val direction: Boolean,
  val provider: String,
  val from: String,
  val target: String,
  val feeRate: Double,
  val amountIn: String,
  val amountOut: String,
  val version: String? = null,
  val publishedAt: String? = null,
  val extendedDetails: JsonObject? = null,
)

@Serializable
data class RouterDataV3(
  val quoteID: String? = null,
  val amountIn: String,
  val amountOut: String,
  val byAmountIn: Boolean,
  val paths: List<Path>,
  val insufficientLiquidity: Boolean,
  val deviationRatio: Double = 0.0,
  val packages: Map<String, String>? = null,
  val totalDeepFee: Long? = null,
  val error: RouterError? = null,
  val overlayFee: String? = null,
)

@Serializable data class FlattenedPath(val path: Path, val isLastUseOfIntermediateToken: Boolean)

@Serializable
data class ProcessedRouterData(
  val quoteID: String,
  val amountIn: String,
  val amountOut: String,
  val byAmountIn: Boolean,
  val flattenedPaths: List<FlattenedPath>,
  val fromCoinType: String,
  val targetCoinType: String,
  val packages: Map<String, String>? = null,
  val totalDeepFee: Long? = null,
  val error: RouterError? = null,
  val overlayFee: String? = null,
)

@Serializable
data class DeepbookV3Config(
  val id: String,
  @SerialName("is_alternative_payment") val isAlternativePayment: Boolean,
  @SerialName("alternative_payment_amount") val alternativePaymentAmount: Long,
  @SerialName("trade_cap") val tradeCap: String,
  @SerialName("balance_manager") val balanceManager: String,
  @SerialName("deep_fee_vault") val deepFeeVault: Long,
  val whitelist: Int,
  @SerialName("package_version") val packageVersion: Int,
  @SerialName("last_updated_time") val lastUpdatedTime: Long,
  @SerialName("whitelist_pools") val whitelistPools: List<String>,
)

@Serializable
data class DeepbookV3ConfigResponse(val code: Int, val msg: String, val data: DeepbookV3Config)

enum class AggregatorServerErrorCode(val code: Int) {
  NumberTooLarge(1000),
  RateLimitExceeded(1001),
  InsufficientLiquidity(1002),
  HoneyPot(1003),
}

fun getAggregatorServerErrorMessage(code: AggregatorServerErrorCode): String {
  return when (code) {
    AggregatorServerErrorCode.NumberTooLarge -> "Number too large"
    AggregatorServerErrorCode.RateLimitExceeded -> "Rate limit exceeded"
    AggregatorServerErrorCode.InsufficientLiquidity -> "Insufficient liquidity"
    AggregatorServerErrorCode.HoneyPot -> "HoneyPot scam detected"
  }
}

const val DEFAULT_ENDPOINT = "https://api-sui.cetus.zone/router_v3"
const val DEFAULT_AGGREGATOR_V3_PACKAGE =
  "0x3864c7c59a4889fec05d1aae4bc9dba5a0e0940594b424fbed44cb3f6ac4c032"
