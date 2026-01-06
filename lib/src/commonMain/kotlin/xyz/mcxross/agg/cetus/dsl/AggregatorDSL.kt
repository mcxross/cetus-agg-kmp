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
package xyz.mcxross.agg.cetus.dsl

import xyz.mcxross.agg.cetus.AggregatorClient
import xyz.mcxross.agg.cetus.FindRouterParams
import xyz.mcxross.ksui.ptb.Argument
import xyz.mcxross.ksui.ptb.ProgrammableTransaction
import xyz.mcxross.ksui.ptb.ptb

object Tokens {
  const val SUI = "0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI"
  const val CETUS =
    "0x06864a6f921804860930db6ddbe2e16acdf8504495ea7481637a1c8b9a8fe54b::cetus::CETUS"
  const val DEEP = "0xdeeb7a4662eec9f2f3def03fb937a663dddaa2e215b8078a284d026b7946c270::deep::DEEP"
  const val USDC = "0x5d4b302506645c37ff133b98c4b50a5ae14841659738d6d733d59d0d217a93bf::coin::COIN"
}

val Number.sui: ULong
  get() = (this.toDouble() * 1_000_000_000).toULong()

val Number.percent: Double
  get() = this.toDouble() / 100.0

class Aggregator(private val client: AggregatorClient = AggregatorClient()) {

  suspend fun swap(block: SwapScope.() -> Unit): ProgrammableTransaction {
    val scope = SwapScope().apply(block)
    scope.validate()

    val params =
      FindRouterParams(
        from = scope.from,
        target = scope.to,
        amount = scope.amount.toString(),
        byAmountIn = true,
        providers = scope.providers.ifEmpty { null },
      )

    val route =
      client.findRoutes(params)
        ?: throw IllegalStateException("No route found for ${scope.from} -> ${scope.to}")

    return ptb {
      // Automatic Gas Handling:
      // If the input is SUI, we MUST split from gas to avoid "Invalid taking of GasCoin".
      // If the input is another coin, we assume the user passed a specific Coin object ID string,
      // or we (in a future version) fetch it. For this simple DSL, if it's not SUI,
      // we expect the user to have provided an input object ID via `inputCoin`.

      val inputCoinArg: Argument =
        if (scope.from == Tokens.SUI) {
          val split = splitCoins {
            coin = Argument.GasCoin
            into = +pure(scope.amount)
          }
          split.first()
        } else {
          // For non-SUI tokens, if `inputObject` is provided, use it.
          // Otherwise, this simple version can't automagically find your object ID without a query.
          if (scope.inputObject == null) {
            throw IllegalArgumentException(
              "For non-SUI tokens, you must specify 'inputObject = \"0x...\"'"
            )
          }
          `object`(scope.inputObject!!)
        }

      val outputCoin =
        client.swap(
          dsl = this,
          routerData = route,
          inputCoin = inputCoinArg,
          slippage = scope.slippage,
          partner = scope.partner,
          pythPriceIds = scope.pythPriceIds,
          deepbookV3DeepFee = scope.deepbookV3DeepFee,
        )

      transferObjects {
        objects = +outputCoin
        to = address(scope.recipient)
      }
    }
  }
}

class SwapScope {
  var from: String = Tokens.SUI
  var to: String = Tokens.USDC
  var amount: ULong = 0uL
  var slippage: Double = 0.5.percent
  var recipient: String = ""
  var partner: String? = null
  var providers: List<String> = emptyList()
  var inputObject: String? = null
  var pythPriceIds: Map<String, String> = emptyMap()
  var deepbookV3DeepFee: Argument? = null

  fun validate() {
    require(amount > 0uL) { "Amount must be greater than 0" }
    require(recipient.isNotBlank()) { "Recipient address is required" }
  }
}
