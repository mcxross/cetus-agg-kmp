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

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.mcxross.ksui.client.ClientConfig
import xyz.mcxross.ksui.client.getClient
import xyz.mcxross.ksui.ptb.Argument
import xyz.mcxross.ksui.ptb.PtbDsl

private const val SDK_VERSION = 1010106
private const val ZERO_AMOUNT = "0"
private const val CETUS_GLOBAL_CONFIG_MAINNET =
  "0xdaa46292632c3c4d8f31f23ea0f9b36a28ff3677e9684980e4438403a67a3d8f"
private const val CETUS_PARTNER_MAINNET =
  "0x639b5e433da31739e800cd085f356e64cae222966d0f1b11bd9dc76b322ff58b"
private const val FLOWX_V2_CONTAINER_MAINNET =
  "0xb65dcbf63fd3ad5d0ebfbf334780dc9f785eff38a4459e37ab08fa79576ee511"
private const val FLOWX_V3_POOL_REGISTRY_MAINNET =
  "0x27565d24a4cd51127ac90e4074a841bbe356cca7bf5759ddc14a975be1632abc"
private const val FLOWX_V3_VERSIONED_MAINNET =
  "0x67624a1533b5aff5d0dfcf5e598684350efd38134d2d245f475524c03a64e656"
private const val KRIYA_V3_VERSION_MAINNET =
  "0xf5145a7ac345ca8736cf8c76047d00d6d378f30e81be6f6eb557184d9de93c78"
private const val TURBOS_VERSIONED_MAINNET =
  "0xf1cf0e81048df168ebeb1b8030fad24b3e0b53ae827c25053fff0779c1445b6f"
private const val AFTERMATH_POOL_REGISTRY_MAINNET =
  "0xfcc774493db2c45c79f688f88d28023a3e7d98e4ee9f48bbf5c7990f651577ae"
private const val AFTERMATH_PROTOCOL_FEE_VAULT_MAINNET =
  "0xf194d9b1bcad972e45a7dd67dd49b3ee1e3357a00a50850c52cd51bb450e13b4"
private const val AFTERMATH_TREASURY_MAINNET =
  "0x28e499dff5e864a2eafe476269a4f5035f1c16f338da7be18b103499abf271ce"
private const val AFTERMATH_INSURANCE_FUND_MAINNET =
  "0xf0c40d67b078000e18032334c3325c47b9ec9f3d9ae4128be820d54663d14e3b"
private const val AFTERMATH_REFERRAL_VAULT_MAINNET =
  "0x35d35b0e5b177593d8c3a801462485572fc30861e6ce96a55af6dc4730709278"
private const val VOLO_STAKE_POOL_MAINNET =
  "0x2d914e23d82fedef1b5f56a32d5c64bdcc3087ccfea2b4d6ea51a71f587840e5"
private const val VOLO_METADATA_MAINNET =
  "0x680cd26af32b2bde8d3361e804c53ec1d1cfe24c7f039eb7f549e8dfde389a60"
private const val AFSUI_STAKED_SUI_VAULT_MAINNET =
  "0x2f8f6d5da7f13ea37daa397724280483ed062769813b6f31e9788e59cc88994d"
private const val AFSUI_SAFE_MAINNET =
  "0xeb685899830dd5837b47007809c76d91a098d52aabbf61e8ac467c59e5cc4610"
private const val AFSUI_REFER_VAULT_MAINNET =
  "0x4ce9a19b594599536c53edb25d22532f82f18038dc8ef618afd00fbbfb9845ef"
private const val AFSUI_VALIDATOR_MAINNET =
  "0xd30018ec3f5ff1a3c75656abf927a87d7f0529e6dc89c7ddd1bd27ecb05e3db2"
private const val BLUEMOVE_DEX_INFO_MAINNET =
  "0x3f2d9f724f4a1ce5e71676448dc452be9a6243dac9c5b975a588c8c867066e92"
private const val DEEPBOOK_V3_GLOBAL_CONFIG_MAINNET =
  "0x699d455ab8c5e02075b4345ea1f91be55bf46064ae6026cc2528e701ce3ac135"
private const val SCALLOP_VERSION_MAINNET =
  "0x07871c4b3c847a0f674510d4978d5cf6f960452795e8ff6f189fd2088a3f6ac7"
private const val SCALLOP_MARKET_DATA_MAINNET =
  "0xa757975255146dc9686aa823b7838b507f315d704f428cbadad2f4ea061939d9"
private const val BLUEFIN_GLOBAL_CONFIG_MAINNET =
  "0x03db251ba509a8d5d8777b6338836082335d93eecbdd09a11e190a1cff51c352"
private const val ALPHAFI_SUI_SYSTEM_STATE_MAINNET =
  "0x0000000000000000000000000000000000000000000000000000000000000005"
private const val MOMENTUM_VERSION_MAINNET =
  "0x2375a0b1ec12010aaea3b2545acfa2ad34cfbba03ce4b59f4c39e1e25eed1b2a"
private const val MAGMA_GLOBAL_CONFIG_MAINNET =
  "0x4c4e1402401f72c7d8533d0ed8d5f8949da363c7a3319ccef261ffe153d32f8a"
private const val HAWAL_STAKING_MAINNET =
  "0x10b9d30c28448939ce6c4d6c6e0ffce4a7f8a4ada8248bdad09ef8b70e4a3904"
private const val HAWAL_VALIDATOR_MAINNET =
  "0x7b3ba6de2ae58283f60d5b8dc04bb9e90e4796b3b2e0dea75569f491275242e7"
private const val OBRIC_PYTH_STATE_MAINNET =
  "0x1f9310238ee9298fb703c3419030b35b22bb1cc37113e3bb5007c99aec79e5b8"
private const val METASTABLE_VERSION_MAINNET =
  "0x4696559327b35ff2ab26904e7426a1646312e9c836d5c6cff6709a5ccc30915c"
private const val SEVENK_ORACLE_PUBLISHED_AT_MAINNET =
  "0x8c36ea167c5e6da8c3d60b4fc897416105dcb986471bd81cfbfd38720a4487c0"
private const val FULLSAIL_GLOBAL_CONFIG_MAINNET =
  "0xe93baa80cb570b3a494cbf0621b2ba96bc993926d34dc92508c9446f9a05d615"
private const val FULLSAIL_VAULT_MAINNET =
  "0xfb971d3a2fb98bde74e1c30ba15a3d8bef60a02789e59ae0b91660aeed3e64e1"
private const val FULLSAIL_STATS_MAINNET =
  "0x6822a33d1d971e040c32f7cc74507010d1fe786f7d06ab89135083ddb07d2dc2"
private const val FULLSAIL_PRICE_PROVIDER_MAINNET =
  "0x854b2d2c0381bb656ec962f8b443eb082654384cf97885359d1956c7d76e33c9"

class AggregatorClient(
  private val endpoint: String = DEFAULT_ENDPOINT,
  private val apiKey: String = "",
  private val overlayFeeBps: Int = 0,
  private val overlayFeeReceiver: String = "0x0",
  clientConfig: ClientConfig = ClientConfig.default,
  private val json: Json = Json { ignoreUnknownKeys = true },
  private val httpClient: HttpClient = getClient(clientConfig),
) {

  fun swap(
    dsl: PtbDsl,
    routerData: RouterDataV3,
    inputCoin: Argument,
    slippage: Double = 0.005,
    partner: String? = null,
    pythPriceIds: Map<String, String> = emptyMap(),
    deepbookV3DeepFee: Argument? = null,
  ): Argument {
    if (!routerData.byAmountIn) {
      throw UnsupportedOperationException("Only swap by amount in is supported currently")
    }

    val quoteID = routerData.quoteID ?: throw IllegalArgumentException("Quote ID is required")

    val processedData = processFlattenRoutes(routerData)

    val amountOut = processedData.amountOut.toULongOrNull() ?: 0uL
    val amountOutLimit = (amountOut.toLong() * (1.0 - slippage)).toLong()

    with(dsl) {
      val swapCtx =
        newSwapContext(
          quoteID = quoteID,
          fromCoinType = processedData.fromCoinType,
          targetCoinType = processedData.targetCoinType,
          expectAmountOut = processedData.amountOut,
          amountOutLimit = amountOutLimit.toString(),
          inputCoin = inputCoin,
          feeRate = overlayFeeBps.toLong(),
          feeRecipient = overlayFeeReceiver,
          packages = processedData.packages,
        )

      val cetusRouter =
        CetusRouter(
          partner = partner ?: CETUS_PARTNER_MAINNET,
          globalConfig = CETUS_GLOBAL_CONFIG_MAINNET,
        )
      val flowxV2Router = FlowxV2Router(FLOWX_V2_CONTAINER_MAINNET)
      val flowxV3Router = FlowxV3Router(FLOWX_V3_POOL_REGISTRY_MAINNET, FLOWX_V3_VERSIONED_MAINNET)
      val kriyaV2Router = KriyaV2Router()
      val kriyaV3Router = KriyaV3Router(KRIYA_V3_VERSION_MAINNET)
      val turbosRouter = TurbosRouter(TURBOS_VERSIONED_MAINNET)
      val aftermathRouter =
        AftermathRouter(
          poolRegistry = AFTERMATH_POOL_REGISTRY_MAINNET,
          protocolFeeVault = AFTERMATH_PROTOCOL_FEE_VAULT_MAINNET,
          treasury = AFTERMATH_TREASURY_MAINNET,
          insuranceFund = AFTERMATH_INSURANCE_FUND_MAINNET,
          referralVault = AFTERMATH_REFERRAL_VAULT_MAINNET,
        )
      val haedalRouter = HaedalRouter()
      val voloRouter = VoloRouter(VOLO_STAKE_POOL_MAINNET, VOLO_METADATA_MAINNET)
      val afsuiRouter =
        AfsuiRouter(
          stakedSuiVault = AFSUI_STAKED_SUI_VAULT_MAINNET,
          safe = AFSUI_SAFE_MAINNET,
          referVault = AFSUI_REFER_VAULT_MAINNET,
          validator = AFSUI_VALIDATOR_MAINNET,
        )
      val bluemoveRouter = BluemoveRouter(BLUEMOVE_DEX_INFO_MAINNET)
      val deepbookV3Router = DeepbookV3Router(DEEPBOOK_V3_GLOBAL_CONFIG_MAINNET)
      val scallopRouter = ScallopRouter(SCALLOP_VERSION_MAINNET, SCALLOP_MARKET_DATA_MAINNET)
      val bluefinRouter = BluefinRouter(BLUEFIN_GLOBAL_CONFIG_MAINNET)
      val haedalPmmRouter = HaedalPmmRouter(pythPriceIds)
      val alphafiRouter = AlphafiRouter(ALPHAFI_SUI_SYSTEM_STATE_MAINNET)
      val springsuiRouter = SpringsuiRouter()
      val steammCpmmRouter = SteammCpmmRouter()
      val steammOmmRouter = SteammOmmRouter(pythPriceIds)
      val steammOmmV2Router = SteammOmmV2Router(pythPriceIds)
      val metastableRouter = MetastableRouter(METASTABLE_VERSION_MAINNET, pythPriceIds)
      val obricRouter = ObricRouter(OBRIC_PYTH_STATE_MAINNET, pythPriceIds)
      val hawalRouter = HawalRouter(HAWAL_STAKING_MAINNET, HAWAL_VALIDATOR_MAINNET)
      val momentumRouter = MomentumRouter(MOMENTUM_VERSION_MAINNET)
      val magmaRouter = MagmaRouter(MAGMA_GLOBAL_CONFIG_MAINNET)
      val sevenkRouter = SevenkRouter(SEVENK_ORACLE_PUBLISHED_AT_MAINNET, pythPriceIds)
      val haedalHmmV2Router = HaedalHmmV2Router(pythPriceIds)
      val fullsailRouter =
        FullsailRouter(
          globalConfig = FULLSAIL_GLOBAL_CONFIG_MAINNET,
          vault = FULLSAIL_VAULT_MAINNET,
          stats = FULLSAIL_STATS_MAINNET,
          priceProvider = FULLSAIL_PRICE_PROVIDER_MAINNET,
        )

      for (flattenedPath in processedData.flattenedPaths) {
        val path = flattenedPath.path
        val provider = normalizedProvider(path)
        when (provider) {
          "CETUS" -> cetusRouter.swap(dsl, flattenedPath, swapCtx)
          "KRIYA" -> kriyaV2Router.swap(dsl, flattenedPath, swapCtx)
          "FLOWXV2" -> flowxV2Router.swap(dsl, flattenedPath, swapCtx)
          "FLOWXV3" -> flowxV3Router.swap(dsl, flattenedPath, swapCtx)
          "KRIYAV3" -> kriyaV3Router.swap(dsl, flattenedPath, swapCtx)
          "TURBOS" -> turbosRouter.swap(dsl, flattenedPath, swapCtx)
          "AFTERMATH" -> aftermathRouter.swap(dsl, flattenedPath, swapCtx)
          "HAEDAL" -> haedalRouter.swap(dsl, flattenedPath, swapCtx)
          "VOLO" -> voloRouter.swap(dsl, flattenedPath, swapCtx)
          "AFSUI" -> afsuiRouter.swap(dsl, flattenedPath, swapCtx)
          "BLUEMOVE" -> bluemoveRouter.swap(dsl, flattenedPath, swapCtx)
          "DEEPBOOKV3" -> deepbookV3Router.swap(dsl, flattenedPath, swapCtx, deepbookV3DeepFee)
          "SCALLOP" -> scallopRouter.swap(dsl, flattenedPath, swapCtx)
          "SUILEND" -> springsuiRouter.swap(dsl, flattenedPath, swapCtx)
          "BLUEFIN" -> bluefinRouter.swap(dsl, flattenedPath, swapCtx)
          "HAEDALPMM" -> haedalPmmRouter.swap(dsl, flattenedPath, swapCtx)
          "ALPHAFI" -> alphafiRouter.swap(dsl, flattenedPath, swapCtx)
          "SPRINGSUI" -> springsuiRouter.swap(dsl, flattenedPath, swapCtx)
          "STEAMM" -> steammCpmmRouter.swap(dsl, flattenedPath, swapCtx)
          "METASTABLE" -> metastableRouter.swap(dsl, flattenedPath, swapCtx)
          "OBRIC" -> obricRouter.swap(dsl, flattenedPath, swapCtx)
          "HAWAL" -> hawalRouter.swap(dsl, flattenedPath, swapCtx)
          "MOMENTUM" -> momentumRouter.swap(dsl, flattenedPath, swapCtx)
          "STEAMM_OMM" -> steammOmmRouter.swap(dsl, flattenedPath, swapCtx)
          "STEAMM_OMM_V2" -> steammOmmV2Router.swap(dsl, flattenedPath, swapCtx)
          "MAGMA" -> magmaRouter.swap(dsl, flattenedPath, swapCtx)
          "SEVENK" -> sevenkRouter.swap(dsl, flattenedPath, swapCtx)
          "HAEDALHMMV2" -> haedalHmmV2Router.swap(dsl, flattenedPath, swapCtx)
          "FULLSAIL" -> fullsailRouter.swap(dsl, flattenedPath, swapCtx)
          else ->
            throw UnsupportedOperationException("Provider ${path.provider} not supported currently")
        }
      }

      return confirmSwap(
        swapContext = swapCtx,
        targetCoinType = processedData.targetCoinType,
        packages = processedData.packages,
      )
    }
  }

  suspend fun findRoutes(params: FindRouterParams): RouterDataV3? {
    val response =
      if (params.liquidityChanges.isNullOrEmpty()) {
        getRouter(params)
      } else {
        postRouterWithLiquidityChanges(params)
      } ?: return null

    if (!response.status.isSuccess()) {
      val errorCode =
        if (response.status == HttpStatusCode.TooManyRequests) {
          AggregatorServerErrorCode.RateLimitExceeded
        } else {
          AggregatorServerErrorCode.NumberTooLarge
        }
      return emptyResultWithError(params.byAmountIn, errorCode)
    }

    val body = response.bodyAsText()
    val payload = json.parseToJsonElement(body).jsonObject
    val msg = payload["msg"]?.jsonPrimitive?.contentOrNull
    val insufficientLiquidity = msg == "liquidity is not enough"

    if (msg != null && msg.contains("HoneyPot scam")) {
      return emptyResultWithError(params.byAmountIn, AggregatorServerErrorCode.HoneyPot)
    }

    val data = payload["data"] as? JsonObject
    if (data != null) {
      val parsed = parseRouterResponse(data, params.byAmountIn)
      return applyOverlayFee(parsed)
    }

    return emptyResultWithError(params.byAmountIn, AggregatorServerErrorCode.InsufficientLiquidity)
      .copy(insufficientLiquidity = insufficientLiquidity)
  }

  suspend fun getDeepbookV3Config(): DeepbookV3ConfigResponse? {
    val url = "${endpoint.trimEnd('/')}/deepbookv3_config"
    return try {
      val response = httpClient.get(url)
      if (!response.status.isSuccess()) {
        null
      } else {
        json.decodeFromString(DeepbookV3ConfigResponse.serializer(), response.bodyAsText())
      }
    } catch (_: Exception) {
      null
    }
  }

  fun close() {
    httpClient.close()
  }

  private suspend fun getRouter(params: FindRouterParams) =
    try {
      httpClient.get(buildFindRoutesUrl(params))
    } catch (_: Exception) {
      null
    }

  private suspend fun postRouterWithLiquidityChanges(params: FindRouterParams) =
    try {
      val fromCoin = completionCoin(params.from)
      val targetCoin = completionCoin(params.target)
      val providersStr = params.providers?.joinToString(",")
      val request = buildJsonObject {
        put("from", JsonPrimitive(fromCoin))
        put("target", JsonPrimitive(targetCoin))
        put("amount", params.amount.toJsonAmount())
        put("by_amount_in", JsonPrimitive(params.byAmountIn))
        params.depth?.let { put("depth", JsonPrimitive(it)) }
        params.splitAlgorithm?.let { put("split_algorithm", JsonPrimitive(it)) }
        params.splitFactor?.let { put("split_factor", JsonPrimitive(it)) }
        params.splitCount?.let { put("split_count", JsonPrimitive(it)) }
        providersStr?.let { put("providers", JsonPrimitive(it)) }
        put(
          "liquidity_changes",
          JsonArray(
            params.liquidityChanges.orEmpty().map { change ->
              buildJsonObject {
                put("pool", JsonPrimitive(change.poolID))
                put("tick_lower", JsonPrimitive(change.tickLower))
                put("tick_upper", JsonPrimitive(change.tickUpper))
                put("delta_liquidity", JsonPrimitive(change.deltaLiquidity))
              }
            }
          ),
        )
        put("v", JsonPrimitive(SDK_VERSION))
      }

      httpClient.post("${endpoint.trimEnd('/')}/find_routes") {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(JsonObject.serializer(), request))
      }
    } catch (_: Exception) {
      null
    }

  private fun buildFindRoutesUrl(params: FindRouterParams): String {
    val fromCoin = completionCoin(params.from)
    val targetCoin = completionCoin(params.target)
    val builder = StringBuilder()
    builder.append(endpoint.trimEnd('/'))
    builder.append("/find_routes")
    builder.append("?from=").append(fromCoin.encodeURLParameter())
    builder.append("&target=").append(targetCoin.encodeURLParameter())
    builder.append("&amount=").append(params.amount.encodeURLParameter())
    builder.append("&by_amount_in=").append(params.byAmountIn)
    params.depth?.let { builder.append("&depth=").append(it) }
    params.splitAlgorithm?.let {
      builder.append("&split_algorithm=").append(it.encodeURLParameter())
    }
    params.splitFactor?.let { builder.append("&split_factor=").append(it) }
    params.splitCount?.let { builder.append("&split_count=").append(it) }
    params.providers
      ?.takeIf { it.isNotEmpty() }
      ?.let { builder.append("&providers=").append(it.joinToString(",").encodeURLParameter()) }
    if (apiKey.isNotBlank()) {
      builder.append("&apiKey=").append(apiKey.encodeURLParameter())
    }
    builder.append("&v=").append(SDK_VERSION)
    return builder.toString()
  }

  private fun parseRouterResponse(data: JsonObject, byAmountIn: Boolean): RouterDataV3 {
    val packages =
      data["packages"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }?.toMutableMap()
        ?: mutableMapOf()
    if (!packages.containsKey("aggregator_v3")) {
      packages["aggregator_v3"] = DEFAULT_AGGREGATOR_V3_PACKAGE
    }

    val paths = data["paths"]?.jsonArray?.mapNotNull { parsePath(it) }.orEmpty()

    return RouterDataV3(
      quoteID = data.stringOrNull("request_id").orEmpty(),
      amountIn = data.stringOrNull("amount_in") ?: ZERO_AMOUNT,
      amountOut = data.stringOrNull("amount_out") ?: ZERO_AMOUNT,
      byAmountIn = byAmountIn,
      paths = paths,
      insufficientLiquidity = false,
      deviationRatio = data.doubleOrNull("deviation_ratio") ?: 0.0,
      packages = packages,
    )
  }

  private fun parsePath(element: JsonElement): Path? {
    val obj = element as? JsonObject ?: return null
    return Path(
      id = obj.stringOrNull("id").orEmpty(),
      direction = obj.booleanOrNull("direction") ?: false,
      provider = obj.stringOrNull("provider").orEmpty(),
      from = obj.stringOrNull("from").orEmpty(),
      target = obj.stringOrNull("target").orEmpty(),
      feeRate = obj.doubleOrNull("fee_rate") ?: 0.0,
      amountIn = obj.stringOrNull("amount_in") ?: ZERO_AMOUNT,
      amountOut = obj.stringOrNull("amount_out") ?: ZERO_AMOUNT,
      version = obj.stringOrNull("version"),
      publishedAt = obj.stringOrNull("published_at"),
      extendedDetails = obj["extended_details"] as? JsonObject,
    )
  }

  private fun normalizedProvider(path: Path): String {
    val raw = path.provider.trim()
    if (raw.isEmpty()) return raw
    val upper = raw.uppercase()
    return when (upper) {
      "FLOWX" -> resolveFlowxVersion(path.version)
      "FLOWX_V2", "FLOWX-V2" -> "FLOWXV2"
      "FLOWX_V3", "FLOWX-V3" -> "FLOWXV3"
      else -> upper
    }
  }

  private fun resolveFlowxVersion(version: String?): String {
    val normalized = version?.trim()?.lowercase()
    return when {
      normalized == null -> "FLOWXV2"
      normalized.contains("3") -> "FLOWXV3"
      normalized.contains("2") -> "FLOWXV2"
      else -> "FLOWXV2"
    }
  }

  private fun applyOverlayFee(routerData: RouterDataV3): RouterDataV3 {
    if (overlayFeeBps <= 0 || overlayFeeReceiver == "0x0") {
      return routerData
    }

    val amountIn = routerData.amountIn.toULongOrNull()
    val amountOut = routerData.amountOut.toULongOrNull()
    if (amountIn == null || amountOut == null) {
      return routerData
    }

    val overlayFeeAmount =
      if (routerData.byAmountIn) {
        amountOut * overlayFeeBps.toULong() / 1_000_000u
      } else {
        amountIn * overlayFeeBps.toULong() / 1_000_000u
      }

    return if (routerData.byAmountIn) {
      routerData.copy(
        amountOut = (amountOut - overlayFeeAmount).toString(),
        overlayFee = overlayFeeAmount.toString(),
      )
    } else {
      routerData.copy(
        amountIn = (amountIn + overlayFeeAmount).toString(),
        overlayFee = overlayFeeAmount.toString(),
      )
    }
  }

  private fun emptyResultWithError(
    byAmountIn: Boolean,
    errorCode: AggregatorServerErrorCode,
  ): RouterDataV3 {
    return RouterDataV3(
      quoteID = "",
      amountIn = ZERO_AMOUNT,
      amountOut = ZERO_AMOUNT,
      byAmountIn = byAmountIn,
      paths = emptyList(),
      insufficientLiquidity = false,
      deviationRatio = 0.0,
      error = RouterError(code = errorCode.code, msg = getAggregatorServerErrorMessage(errorCode)),
    )
  }
}

private fun JsonObject.stringOrNull(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

private fun JsonObject.doubleOrNull(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull

private fun JsonObject.booleanOrNull(key: String): Boolean? =
  this[key]?.jsonPrimitive?.booleanOrNull

private fun String.toJsonAmount(): JsonPrimitive {
  return toLongOrNull()?.let { JsonPrimitive(it) } ?: JsonPrimitive(this)
}
