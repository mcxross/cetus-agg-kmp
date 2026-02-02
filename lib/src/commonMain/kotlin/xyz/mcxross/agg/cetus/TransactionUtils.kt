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

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import xyz.mcxross.ksui.ptb.Argument
import xyz.mcxross.ksui.ptb.PtbDsl
import xyz.mcxross.ksui.util.toTypeTag

private const val MAX_AMOUNT_IN = "18446744073709551615"
private const val SUI_SYSTEM_STATE = "0x5"
private const val DEEPBOOK_V3_DEEP_FEE_TYPE =
  "0xdeeb7a4662eec9f2f3def03fb937a663dddaa2e215b8078a284d026b7946c270::deep::DEEP"
private const val ORACLE_PACKAGE_ID =
  "0xe84b649199654d18c38e727212f5d8dacfc3cf78d60d0a7fc85fd589f280eb2b"
private const val METASTABLE_SUPER_SUI_TYPE =
  "0x790f258062909e3a0ffc78b3c53ac2f62d7084c3bab95644bdeb05add7250001::super_sui::SUPER_SUI"
private const val METASTABLE_MUSD_TYPE =
  "0xe44df51c0b21a27ab915fa1fe2ca610cd3eaa6d9666fe5e62b988bf7f0bd8722::musd::MUSD"
private const val METASTABLE_METH_TYPE =
  "0xccd628c2334c5ed33e6c47d6c21bb664f8b6307b2ac32c2462a61f69a31ebcee::meth::METH"

internal fun PtbDsl.newSwapContext(
  quoteID: String,
  fromCoinType: String,
  targetCoinType: String,
  expectAmountOut: String,
  amountOutLimit: String,
  inputCoin: Argument,
  feeRate: Long,
  feeRecipient: String,
  aggregatorPublishedAt: String? = null,
  packages: Map<String, String>? = null,
): Argument {
  val publishedAt = getAggregatorPublishedAt(packages, aggregatorPublishedAt)
  return moveCall {
    target = "$publishedAt::router::new_swap_context"
    typeArguments = listOf(fromCoinType, targetCoinType).map { it.toTypeTag() }
    arguments =
      listOf(
        pure(quoteID),
        pure(expectAmountOut.toULong()),
        pure(amountOutLimit.toULong()),
        inputCoin,
        pure(feeRate.toUInt()), // feeRate is u32 in Move
        address(feeRecipient),
      )
  }
}

internal fun PtbDsl.confirmSwap(
  swapContext: Argument,
  targetCoinType: String,
  aggregatorPublishedAt: String? = null,
  packages: Map<String, String>? = null,
): Argument {
  val publishedAt = getAggregatorPublishedAt(packages, aggregatorPublishedAt)
  return moveCall {
    target = "$publishedAt::router::confirm_swap"
    typeArguments = listOf(targetCoinType).map { it.toTypeTag() }
    arguments = listOf(swapContext)
  }
}

internal fun PtbDsl.takeBalance(
  coinType: String,
  amount: String,
  swapCtx: Argument,
  aggregatorPublishedAt: String? = null,
  packages: Map<String, String>? = null,
): Argument {
  val publishedAt = getAggregatorPublishedAt(packages, aggregatorPublishedAt)
  return moveCall {
    target = "$publishedAt::router::take_balance"
    typeArguments = listOf(coinType).map { it.toTypeTag() }
    arguments = listOf(swapCtx, pure(amount.toULong()))
  }
}

internal fun PtbDsl.transferBalance(
  balance: Argument,
  coinType: String,
  recipient: String,
  aggregatorPublishedAt: String? = null,
  packages: Map<String, String>? = null,
) {
  val publishedAt = getAggregatorPublishedAt(packages, aggregatorPublishedAt)
  moveCall {
    target = "$publishedAt::router::transfer_balance"
    typeArguments = listOf(coinType).map { it.toTypeTag() }
    arguments = listOf(balance, address(recipient))
  }
}

internal fun PtbDsl.transferOrDestroyCoin(
  coin: Argument,
  coinType: String,
  aggregatorPublishedAt: String? = null,
  packages: Map<String, String>? = null,
) {
  val publishedAt = getAggregatorPublishedAt(packages, aggregatorPublishedAt)
  moveCall {
    target = "$publishedAt::router::transfer_or_destroy_coin"
    typeArguments = listOf(coinType).map { it.toTypeTag() }
    arguments = listOf(coin)
  }
}

internal fun getAggregatorPublishedAt(
  packages: Map<String, String>?,
  aggregatorPublishedAt: String?,
): String {
  if (packages != null && packages.containsKey("aggregator_v3")) {
    return packages["aggregator_v3"]!!
  }
  return aggregatorPublishedAt ?: DEFAULT_AGGREGATOR_V3_PACKAGE
}

internal class CetusRouter(private val partner: String, private val globalConfig: String) {

  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    if (path.publishedAt == null) {
      throw IllegalStateException("Cetus path must have published_at")
    }

    val coinAType = if (path.direction) path.from else path.target
    val coinBType = if (path.direction) path.target else path.from

    val amountIn =
      if (flattenedPath.isLastUseOfIntermediateToken) {
        "18446744073709551615"
      } else {
        path.amountIn
      }

    with(dsl) {
      moveCall {
        target = "${path.publishedAt}::cetus::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(globalConfig),
            `object`(path.id),
            `object`(partner),
            pure(path.direction),
            pure(amountIn.toULong()),
            clock(),
          )
      }
    }
  }
}

internal class KriyaV2Router {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Kriya V2 not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::kriya_amm::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(swapContext, `object`(path.id), pure(path.direction), pure(amountIn.toULong()))
      }
    }
  }
}

internal class FlowxV2Router(private val container: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("FlowX V2 not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::flowx_amm::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(swapContext, `object`(container), pure(path.direction), pure(amountIn.toULong()))
      }
    }
  }
}

internal class FlowxV3Router(private val poolRegistry: String, private val versioned: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("FlowX V3 not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()
    val feeRate = (path.feeRate * 1_000_000).toLong()

    with(dsl) {
      moveCall {
        target = "$publishedAt::flowx_clmm::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(poolRegistry),
            `object`(versioned),
            pure(feeRate.toULong()),
            pure(amountIn.toULong()),
            pure(path.direction),
            clock(),
          )
      }
    }
  }
}

internal class KriyaV3Router(private val version: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Kriya V3 not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::kriya_clmm::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(version),
            pure(path.direction),
            pure(amountIn.toULong()),
            clock(),
          )
      }
    }
  }
}

internal class TurbosRouter(private val versioned: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Turbos not set publishedAt")
    val extendedDetails = path.extendedDetails ?: error("Extended details not supported")
    val feeType =
      extendedDetails.stringOrNull("turbos_fee_type") ?: error("Turbos fee type not supported")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::turbos::swap"
        typeArguments = listOf(coinAType, coinBType, feeType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(versioned),
            pure(path.direction),
            pure(amountIn.toULong()),
            clock(),
          )
      }
    }
  }
}

internal class AftermathRouter(
  private val poolRegistry: String,
  private val protocolFeeVault: String,
  private val treasury: String,
  private val insuranceFund: String,
  private val referralVault: String,
) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Aftermath not set publishedAt")
    val extendedDetails = path.extendedDetails
    val feeType =
      extendedDetails?.stringOrNull("aftermath_lp_supply_type", "aftermathLpSupplyType")
        ?: "0x2::sui::SUI"
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::aftermath::swap"
        typeArguments = listOf(coinAType, coinBType, feeType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(poolRegistry),
            `object`(protocolFeeVault),
            `object`(treasury),
            `object`(insuranceFund),
            `object`(referralVault),
            pure(path.direction),
            pure(amountIn.toULong()),
            pure(path.amountOut.toULong()),
          )
      }
    }
  }
}

internal class HaedalRouter {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Haedal not set publishedAt")
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::hasui::swap"
        typeArguments = emptyList()
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(SUI_SYSTEM_STATE),
            pure(path.direction),
            pure(amountIn.toULong()),
          )
      }
    }
  }
}

internal class VoloRouter(private val stakePool: String, private val metadata: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Volo not set publishedAt")
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::volo::swap"
        typeArguments = emptyList()
        arguments =
          listOf(
            swapContext,
            `object`(stakePool),
            `object`(metadata),
            `object`(SUI_SYSTEM_STATE),
            pure(path.direction),
            pure(amountIn.toULong()),
          )
      }
    }
  }
}

internal class AfsuiRouter(
  private val stakedSuiVault: String,
  private val safe: String,
  private val referVault: String,
  private val validator: String,
) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("AFSUI not set publishedAt")
    if (!path.direction) {
      error("AFSUI not support b2a swap")
    }
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::afsui::swap"
        typeArguments = emptyList()
        arguments =
          listOf(
            swapContext,
            `object`(stakedSuiVault),
            `object`(safe),
            `object`(SUI_SYSTEM_STATE),
            `object`(referVault),
            `object`(validator),
            pure(path.direction),
            pure(amountIn.toULong()),
          )
      }
    }
  }
}

internal class BluemoveRouter(private val dexInfo: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("BlueMove not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::bluemove::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(swapContext, `object`(dexInfo), pure(path.direction), pure(amountIn.toULong()))
      }
    }
  }
}

internal class DeepbookV3Router(private val globalConfig: String) {
  fun swap(
    dsl: PtbDsl,
    flattenedPath: FlattenedPath,
    swapContext: Argument,
    deepFeeCoin: Argument? = null,
  ) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("DeepBook V3 not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      val deepCoin =
        deepFeeCoin
          ?: moveCall {
            target = "0x2::coin::zero"
            typeArguments = listOf(DEEPBOOK_V3_DEEP_FEE_TYPE.toTypeTag())
            arguments = emptyList()
          }

      moveCall {
        target = "$publishedAt::deepbookv3::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(globalConfig),
            `object`(path.id),
            pure(amountIn.toULong()),
            pure(path.direction),
            deepCoin,
            clock(),
          )
      }
    }
  }
}

internal class ScallopRouter(private val version: String, private val marketData: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Scallop not set publishedAt")
    val extendedDetails = path.extendedDetails ?: error("Extended details not supported")
    val scallopScoinTreasury =
      extendedDetails.stringOrNull("scallopScoinTreasury", "scallop_scoin_treasury")
        ?: error("Scallop scoin treasury not supported")
    val amountIn = flattenedPath.amountInForPath()
    val (coinAType, coinBType) = path.from to path.target
    val func = if (path.direction) "swap_a2b" else "swap_b2a"

    with(dsl) {
      moveCall {
        target = "$publishedAt::scallop::$func"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(version),
            `object`(marketData),
            `object`(scallopScoinTreasury),
            pure(amountIn.toULong()),
            clock(),
          )
      }
    }
  }
}

internal class BluefinRouter(private val globalConfig: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Bluefin not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::bluefin::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(globalConfig),
            `object`(path.id),
            pure(path.direction),
            pure(amountIn.toULong()),
            clock(),
          )
      }
    }
  }
}

internal class HaedalPmmRouter(private val pythPriceIds: Map<String, String>) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Haedal PMM not set publishedAt")
    val extendedDetails = path.extendedDetails ?: error("Extended details not found for Haedal PMM")
    val basePriceId =
      resolvePriceInfoId(
        extendedDetails = extendedDetails,
        pythPriceIds = pythPriceIds,
        idKeys = arrayOf("haedal_pmm_base_price_id", "haedal_pmm_base_price_info_id"),
        seedKeys = arrayOf("haedal_pmm_base_price_seed"),
        missingSeedMessage = "Haedal PMM base price seed not supported",
        missingIdMessage = "Haedal HMM requires oracle price IDs for both coins",
      )
    val quotePriceId =
      resolvePriceInfoId(
        extendedDetails = extendedDetails,
        pythPriceIds = pythPriceIds,
        idKeys = arrayOf("haedal_pmm_quote_price_id", "haedal_pmm_quote_price_info_id"),
        seedKeys = arrayOf("haedal_pmm_quote_price_seed"),
        missingSeedMessage = "Haedal PMM quote price seed not supported",
        missingIdMessage = "Haedal HMM requires oracle price IDs for both coins",
      )
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::haedal_hmm::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(basePriceId),
            `object`(quotePriceId),
            pure(amountIn.toULong()),
            pure(path.direction),
            clock(),
          )
      }
    }
  }
}

internal class AlphafiRouter(private val suiSystemState: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Alphafi not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::alphafi::swap"
        typeArguments = listOf(coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(suiSystemState),
            pure(path.direction),
            pure(amountIn.toULong()),
          )
      }
    }
  }
}

internal class SpringsuiRouter {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Springsui not set publishedAt")
    val springSuiType = if (path.direction) path.target else path.from
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::springsui::swap"
        typeArguments = listOf(springSuiType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(SUI_SYSTEM_STATE),
            pure(amountIn.toULong()),
            pure(path.direction),
          )
      }
    }
  }
}

internal class SteammCpmmRouter {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Steamm CPMM not set publishedAt")
    val extendedDetails =
      path.extendedDetails ?: error("Extended details not found for Steamm CPMM")
    val bankA =
      extendedDetails.stringOrNull("steamm_bank_a")
        ?: error("Steamm CPMM steamm_bank_a not supported")
    val bankB =
      extendedDetails.stringOrNull("steamm_bank_b")
        ?: error("Steamm CPMM steamm_bank_b not supported")
    val lendingMarket =
      extendedDetails.stringOrNull("steamm_lending_market")
        ?: error("Steamm CPMM steamm_lending_market not supported")
    val lendingMarketType =
      extendedDetails.stringOrNull("steamm_lending_market_type")
        ?: error("Steamm CPMM steamm_lending_market_type not supported")
    val bTokenAType =
      extendedDetails.stringOrNull("steamm_btoken_a_type")
        ?: error("Steamm CPMM steamm_btoken_a_type not supported")
    val bTokenBType =
      extendedDetails.stringOrNull("steamm_btoken_b_type")
        ?: error("Steamm CPMM steamm_btoken_b_type not supported")
    val lpTokenType =
      extendedDetails.stringOrNull("steamm_lp_token_type")
        ?: error("Steamm CPMM steamm_lp_token_type not supported")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::steamm_cpmm::swap"
        typeArguments =
          listOf(lendingMarketType, coinAType, coinBType, bTokenAType, bTokenBType, lpTokenType)
            .map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(bankA),
            `object`(bankB),
            `object`(lendingMarket),
            pure(path.direction),
            pure(amountIn.toULong()),
            clock(),
          )
      }
    }
  }
}

internal class SteammOmmRouter(
  private val pythPriceIds: Map<String, String>,
  private val oraclePackageId: String = ORACLE_PACKAGE_ID,
) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Steamm OMM not set publishedAt")
    val extendedDetails = path.extendedDetails ?: error("Extended details not found for Steamm OMM")

    val bankA =
      extendedDetails.stringOrNull("steamm_bank_a") ?: error("Steamm steamm_bank_a not supported")
    val bankB =
      extendedDetails.stringOrNull("steamm_bank_b") ?: error("Steamm steamm_bank_b not supported")
    val lendingMarket =
      extendedDetails.stringOrNull("steamm_lending_market")
        ?: error("Steamm steamm_lending_market not supported")
    val lendingMarketType =
      extendedDetails.stringOrNull("steamm_lending_market_type")
        ?: error("Steamm steamm_lending_market_type not supported")
    val bTokenAType =
      extendedDetails.stringOrNull("steamm_btoken_a_type")
        ?: error("Steamm steamm_btoken_a_type not supported")
    val bTokenBType =
      extendedDetails.stringOrNull("steamm_btoken_b_type")
        ?: error("Steamm steamm_btoken_b_type not supported")
    val lpTokenType =
      extendedDetails.stringOrNull("steamm_lp_token_type")
        ?: error("Steamm steamm_lp_token_type not supported")
    val oracleRegistryId =
      extendedDetails.stringOrNull("steammOracleRegistryId", "steamm_oracle_registry_id")
        ?: error("Steamm steammOracleRegistryId not supported")
    val oracleIndexA =
      extendedDetails.longOrNull("steammOracleIndexA", "steamm_oracle_index_a")
        ?: error("Steamm steammOracleIndexA not supported")
    val oracleIndexB =
      extendedDetails.longOrNull("steammOracleIndexB", "steamm_oracle_index_b")
        ?: error("Steamm steammOracleIndexB not supported")
    val priceSeedA =
      extendedDetails.stringOrNull("steammOraclePythPriceSeedA", "steamm_oracle_pyth_price_seed_a")
        ?: error("Steamm oracle price seeds not found")
    val priceSeedB =
      extendedDetails.stringOrNull("steammOraclePythPriceSeedB", "steamm_oracle_pyth_price_seed_b")
        ?: error("Steamm oracle price seeds not found")
    val priceInfoObjectIdA =
      pythPriceIds[priceSeedA]
        ?: error("Base price info object id or quote price info object id not found")
    val priceInfoObjectIdB =
      pythPriceIds[priceSeedB]
        ?: error("Base price info object id or quote price info object id not found")
    val (coinAType, coinBType) = path.coinTypes()
    val func = if (path.direction) "swap_a2b_v2" else "swap_b2a_v2"
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      val oraclePriceUpdateA = moveCall {
        target = "$oraclePackageId::oracles::get_pyth_price"
        typeArguments = emptyList()
        arguments =
          listOf(
            `object`(oracleRegistryId),
            `object`(priceInfoObjectIdA),
            pure(oracleIndexA.toULong()),
            clock(),
          )
      }

      val oraclePriceUpdateB = moveCall {
        target = "$oraclePackageId::oracles::get_pyth_price"
        typeArguments = emptyList()
        arguments =
          listOf(
            `object`(oracleRegistryId),
            `object`(priceInfoObjectIdB),
            pure(oracleIndexB.toULong()),
            clock(),
          )
      }

      moveCall {
        target = "$publishedAt::steamm_omm::$func"
        typeArguments =
          listOf(lendingMarketType, coinAType, coinBType, bTokenAType, bTokenBType, lpTokenType)
            .map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(bankA),
            `object`(bankB),
            `object`(lendingMarket),
            oraclePriceUpdateA,
            oraclePriceUpdateB,
            pure(amountIn.toULong()),
            clock(),
          )
      }
    }
  }
}

internal class SteammOmmV2Router(
  private val pythPriceIds: Map<String, String>,
  private val oraclePackageId: String = ORACLE_PACKAGE_ID,
) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Steamm OMM V2 not set publishedAt")
    val extendedDetails =
      path.extendedDetails ?: error("Extended details not found for Steamm OMM V2")
    val bankA =
      extendedDetails.stringOrNull("steamm_bank_a")
        ?: error("Steamm OMM V2 steamm_bank_a not supported")
    val bankB =
      extendedDetails.stringOrNull("steamm_bank_b")
        ?: error("Steamm OMM V2 steamm_bank_b not supported")
    val lendingMarket =
      extendedDetails.stringOrNull("steamm_lending_market")
        ?: error("Steamm OMM V2 steamm_lending_market not supported")
    val lendingMarketType =
      extendedDetails.stringOrNull("steamm_lending_market_type")
        ?: error("Steamm OMM V2 steamm_lending_market_type not supported")
    val bTokenAType =
      extendedDetails.stringOrNull("steamm_btoken_a_type")
        ?: error("Steamm OMM V2 steamm_btoken_a_type not supported")
    val bTokenBType =
      extendedDetails.stringOrNull("steamm_btoken_b_type")
        ?: error("Steamm OMM V2 steamm_btoken_b_type not supported")
    val lpTokenType =
      extendedDetails.stringOrNull("steamm_lp_token_type")
        ?: error("Steamm OMM V2 steamm_lp_token_type not supported")
    val oracleRegistryId =
      extendedDetails.stringOrNull("steamm_oracle_registry_id")
        ?: error("Steamm OMM V2 steamm_oracle_registry_id not supported")
    val oracleIndexA =
      extendedDetails.longOrNull("steamm_oracle_index_a")
        ?: error("Steamm OMM V2 steamm_oracle_index_a not supported")
    val oracleIndexB =
      extendedDetails.longOrNull("steamm_oracle_index_b")
        ?: error("Steamm OMM V2 steamm_oracle_index_b not supported")
    val priceSeedA =
      extendedDetails.stringOrNull("steamm_oracle_pyth_price_seed_a")
        ?: error("Steamm OMM V2 oracle price seeds not found")
    val priceSeedB =
      extendedDetails.stringOrNull("steamm_oracle_pyth_price_seed_b")
        ?: error("Steamm OMM V2 oracle price seeds not found")
    val priceInfoObjectIdA =
      pythPriceIds[priceSeedA]
        ?: error("Base price info object id or quote price info object id not found")
    val priceInfoObjectIdB =
      pythPriceIds[priceSeedB]
        ?: error("Base price info object id or quote price info object id not found")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      val oraclePriceUpdateA = moveCall {
        target = "$oraclePackageId::oracles::get_pyth_price"
        typeArguments = emptyList()
        arguments =
          listOf(
            `object`(oracleRegistryId),
            `object`(priceInfoObjectIdA),
            pure(oracleIndexA.toULong()),
            clock(),
          )
      }

      val oraclePriceUpdateB = moveCall {
        target = "$oraclePackageId::oracles::get_pyth_price"
        typeArguments = emptyList()
        arguments =
          listOf(
            `object`(oracleRegistryId),
            `object`(priceInfoObjectIdB),
            pure(oracleIndexB.toULong()),
            clock(),
          )
      }

      moveCall {
        target = "$publishedAt::steamm_omm_v2::swap"
        typeArguments =
          listOf(lendingMarketType, coinAType, coinBType, bTokenAType, bTokenBType, lpTokenType)
            .map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(bankA),
            `object`(bankB),
            `object`(lendingMarket),
            oraclePriceUpdateA,
            oraclePriceUpdateB,
            pure(path.direction),
            pure(amountIn.toULong()),
            clock(),
          )
      }
    }
  }
}

internal class MetastableRouter(
  private val versionId: String,
  private val pythPriceIds: Map<String, String>,
) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Metastable not set publishedAt")
    val extendedDetails = path.extendedDetails ?: error("Extended details not found for Metastable")

    val createCapPkgId =
      extendedDetails.stringOrNull("metastable_create_cap_pkg_id")
        ?: error("CreateCapPkgId not found in extended details")
    val createCapModule =
      extendedDetails.stringOrNull("metastable_create_cap_module")
        ?: error("CreateCapModule not found in extended details")
    val whitelistedAppId =
      extendedDetails.stringOrNull("metastable_whitelisted_app_id")
        ?: error("WhitelistedAppId not found in extended details")
    val createCapAllTypeParams =
      extendedDetails.booleanOrNull("metastable_create_cap_all_type_params") ?: false

    val (coinType, metaCoinType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()
    val func = if (path.direction) "swap_a2b" else "swap_b2a"
    val createCapFunc = if (path.direction) "create_deposit_cap" else "create_withdraw_cap"

    with(dsl) {
      val createCapTypeArgs = mutableListOf(metaCoinType)
      if (createCapAllTypeParams) {
        createCapTypeArgs.add(coinType)
      }

      val depositArgs = mutableListOf<Argument>(`object`(whitelistedAppId), `object`(path.id))
      when (metaCoinType) {
        METASTABLE_SUPER_SUI_TYPE -> {
          val registryId =
            extendedDetails.stringOrNull("metastable_registry_id")
              ?: error("Not found registry id for super sui")
          depositArgs.add(`object`(registryId))
        }
        METASTABLE_MUSD_TYPE,
        METASTABLE_METH_TYPE -> {
          extendedDetails.stringOrNull("metastable_price_seed")?.let { seed ->
            val priceId =
              resolvePriceInfoIdWithFallback(
                seed = seed,
                pythPriceIds = pythPriceIds,
                missingIdMessage = "Invalid Pyth price feed: $seed",
              )
            depositArgs.add(`object`(priceId))
          }
          extendedDetails.stringOrNull("metastable_eth_price_seed")?.let { seed ->
            val priceId =
              resolvePriceInfoIdWithFallback(
                seed = seed,
                pythPriceIds = pythPriceIds,
                missingIdMessage = "Invalid Pyth price feed: $seed",
              )
            depositArgs.add(`object`(priceId))
          }
          depositArgs.add(clock())
        }
        else -> error("Invalid Metacoin: $metaCoinType")
      }

      val depositCap = moveCall {
        target = "$createCapPkgId::$createCapModule::$createCapFunc"
        typeArguments = createCapTypeArgs.map { it.toTypeTag() }
        arguments = depositArgs
      }

      moveCall {
        target = "$publishedAt::metastable::$func"
        typeArguments = listOf(coinType, metaCoinType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(versionId),
            depositCap,
            pure(amountIn.toULong()),
          )
      }
    }
  }
}

internal class ObricRouter(
  private val pythStateObjectId: String,
  private val pythPriceIds: Map<String, String>,
) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Obric not set publishedAt")
    val extendedDetails = path.extendedDetails ?: error("Extended details not supported in obric")

    val coinAPriceSeed = extendedDetails.stringOrNull("obric_coin_a_price_seed")
    val coinBPriceSeed = extendedDetails.stringOrNull("obric_coin_b_price_seed")
    val coinAPriceId =
      extendedDetails.stringOrNull("obric_coin_a_price_id")
        ?: coinAPriceSeed?.let { pythPriceIds[it] }
    val coinBPriceId =
      extendedDetails.stringOrNull("obric_coin_b_price_id")
        ?: coinBPriceSeed?.let { pythPriceIds[it] }

    if (coinAPriceId == null || coinBPriceId == null) {
      error("Base price id or quote price id not supported")
    }

    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::obric::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            pure(amountIn.toULong()),
            pure(path.direction),
            `object`(pythStateObjectId),
            `object`(coinAPriceId),
            `object`(coinBPriceId),
            clock(),
          )
      }
    }
  }
}

internal class HawalRouter(private val staking: String, private val validator: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Hawal not set publishedAt")
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::hawal::swap"
        typeArguments = emptyList()
        arguments =
          listOf(
            swapContext,
            `object`(staking),
            `object`(path.id),
            `object`(validator),
            pure(path.direction),
            pure(amountIn.toULong()),
          )
      }
    }
  }
}

internal class MomentumRouter(private val version: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Momentum not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::momentum::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            pure(path.direction),
            pure(amountIn.toULong()),
            `object`(version),
            clock(),
          )
      }
    }
  }
}

internal class MagmaRouter(private val globalConfig: String) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Magma not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::magma::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(globalConfig),
            `object`(path.id),
            pure(path.direction),
            pure(amountIn.toULong()),
            clock(),
          )
      }
    }
  }
}

internal class SevenkRouter(
  private val oraclePublishedAt: String,
  private val pythPriceIds: Map<String, String>,
) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Sevenk not set publishedAt")
    val extendedDetails = path.extendedDetails ?: error("Extended details not found for Sevenk")
    val coinAPriceSeed =
      extendedDetails.stringOrNull("sevenk_coin_a_price_seed")
        ?: error("Required Sevenk extended details not found")
    val coinBPriceSeed =
      extendedDetails.stringOrNull("sevenk_coin_b_price_seed")
        ?: error("Required Sevenk extended details not found")
    val coinAOracleId =
      extendedDetails.stringOrNull("sevenk_oracle_config_a")
        ?: error("Required Sevenk extended details not found")
    val coinBOracleId =
      extendedDetails.stringOrNull("sevenk_oracle_config_b")
        ?: error("Required Sevenk extended details not found")
    val lpCapType =
      extendedDetails.stringOrNull("sevenk_lp_cap_type")
        ?: error("Required Sevenk extended details not found")

    val coinAPriceId =
      pythPriceIds[coinAPriceSeed] ?: error("Sevenk price info object IDs not found")
    val coinBPriceId =
      pythPriceIds[coinBPriceSeed] ?: error("Sevenk price info object IDs not found")

    val amountIn = flattenedPath.amountInForPath()
    val (coinAType, coinBType) = path.coinTypes()
    val func = if (path.direction) "swap_a2b" else "swap_b2a"

    with(dsl) {
      val holder = moveCall {
        target = "$oraclePublishedAt::oracle::new_holder"
        typeArguments = emptyList()
        arguments = emptyList()
      }

      moveCall {
        target = "$oraclePublishedAt::pyth::get_price"
        typeArguments = emptyList()
        arguments = listOf(`object`(coinAOracleId), holder, `object`(coinAPriceId), clock())
      }

      moveCall {
        target = "$oraclePublishedAt::pyth::get_price"
        typeArguments = emptyList()
        arguments = listOf(`object`(coinBOracleId), holder, `object`(coinBPriceId), clock())
      }

      moveCall {
        target = "$publishedAt::sevenk::$func"
        typeArguments = listOf(coinAType, coinBType, lpCapType).map { it.toTypeTag() }
        arguments = listOf(swapContext, `object`(path.id), holder, pure(amountIn.toULong()))
      }
    }
  }
}

internal class HaedalHmmV2Router(private val pythPriceIds: Map<String, String>) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Haedal HMM V2 not set publishedAt")
    val extendedDetails =
      path.extendedDetails ?: error("Extended details not found for Haedal HMM V2")
    val basePriceId =
      resolvePriceInfoId(
        extendedDetails = extendedDetails,
        pythPriceIds = pythPriceIds,
        idKeys = arrayOf("haedalhmmv2_base_price_id", "haedal_hmm_v2_base_price_id"),
        seedKeys = arrayOf("haedalhmmv2_base_price_seed"),
        missingSeedMessage = "Haedal HMM V2 base price seed not supported",
        missingIdMessage = "Haedal HMM V2 requires oracle price IDs for base coin",
      )
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::haedal_hmm_v2::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(path.id),
            `object`(basePriceId),
            pure(amountIn.toULong()),
            pure(path.direction),
            clock(),
          )
      }
    }
  }
}

internal class FullsailRouter(
  private val globalConfig: String,
  private val vault: String,
  private val stats: String,
  private val priceProvider: String,
) {
  fun swap(dsl: PtbDsl, flattenedPath: FlattenedPath, swapContext: Argument) {
    val path = flattenedPath.path
    val publishedAt = path.publishedAt ?: error("Fullsail not set publishedAt")
    val (coinAType, coinBType) = path.coinTypes()
    val amountIn = flattenedPath.amountInForPath()

    with(dsl) {
      moveCall {
        target = "$publishedAt::fullsail::swap"
        typeArguments = listOf(coinAType, coinBType).map { it.toTypeTag() }
        arguments =
          listOf(
            swapContext,
            `object`(globalConfig),
            `object`(vault),
            `object`(stats),
            `object`(priceProvider),
            `object`(path.id),
            pure(path.direction),
            pure(amountIn.toULong()),
            clock(),
          )
      }
    }
  }
}

private fun FlattenedPath.amountInForPath(): String {
  return if (isLastUseOfIntermediateToken) {
    MAX_AMOUNT_IN
  } else {
    path.amountIn
  }
}

private fun Path.coinTypes(): Pair<String, String> {
  return if (direction) {
    from to target
  } else {
    target to from
  }
}

private fun JsonObject.stringOrNull(vararg keys: String): String? {
  for (key in keys) {
    val value = this[key]?.jsonPrimitive?.contentOrNull
    if (!value.isNullOrBlank()) {
      return value
    }
  }
  return null
}

private fun JsonObject.longOrNull(vararg keys: String): Long? {
  for (key in keys) {
    val element = this[key]?.jsonPrimitive ?: continue
    element.longOrNull?.let {
      return it
    }
    element.contentOrNull?.toLongOrNull()?.let {
      return it
    }
  }
  return null
}

private fun JsonObject.booleanOrNull(vararg keys: String): Boolean? {
  for (key in keys) {
    val element = this[key]?.jsonPrimitive ?: continue
    element.booleanOrNull?.let {
      return it
    }
    when (element.contentOrNull?.lowercase()) {
      "true" -> return true
      "false" -> return false
    }
  }
  return null
}

private fun resolvePriceInfoId(
  extendedDetails: JsonObject,
  pythPriceIds: Map<String, String>,
  idKeys: Array<String>,
  seedKeys: Array<String>,
  missingSeedMessage: String,
  missingIdMessage: String,
): String {
  extendedDetails.stringOrNull(*idKeys)?.let { return it }
  val seed = extendedDetails.stringOrNull(*seedKeys) ?: error(missingSeedMessage)
  return pythPriceIds[seed] ?: error(missingIdMessage)
}

private fun resolvePriceInfoIdWithFallback(
  seed: String,
  pythPriceIds: Map<String, String>,
  missingIdMessage: String,
): String {
  pythPriceIds[seed]?.let { return it }
  if (seed.looksLikeObjectId()) {
    return seed
  }
  error(missingIdMessage)
}

private fun String.looksLikeObjectId(): Boolean {
  if (!startsWith("0x") || length != 66) return false
  for (i in 2 until length) {
    val c = this[i]
    val isHex = (c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F')
    if (!isHex) return false
  }
  return true
}
