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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import xyz.mcxross.agg.cetus.dsl.Aggregator
import xyz.mcxross.agg.cetus.dsl.Tokens
import xyz.mcxross.agg.cetus.dsl.percent
import xyz.mcxross.agg.cetus.dsl.sui
import xyz.mcxross.ksui.Sui
import xyz.mcxross.ksui.account.Account
import xyz.mcxross.ksui.model.Network
import xyz.mcxross.ksui.model.SuiConfig
import xyz.mcxross.ksui.model.SuiSettings

class AggregatorIntegrationTest :
  StringSpec({
    "finds routes" {
      val client = AggregatorClient()
      val params =
        FindRouterParams(
          from = "0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI",
          target = "0xdeeb7a4662eec9f2f3def03fb937a663dddaa2e215b8078a284d026b7946c270::deep::DEEP",
          amount = "200000000",
          byAmountIn = true,
        )

      val result = client.findRoutes(params)

      result.shouldNotBeNull()
    }

    "builds swap tx with DSL" {
      val sui = Sui(SuiConfig(SuiSettings(Network.MAINNET)))

      val signer =
        Account.import("suiprivkey1...")

      val aggregator = Aggregator()

      val tx =
        aggregator.swap {
          from = Tokens.SUI
          to = Tokens.CETUS
          amount = 0.2.sui
          slippage = 0.5.percent
          recipient = signer.address.toString()
        }

      val res = sui.signAndExecuteTransactionBlock(signer, tx)

      res.shouldNotBeNull()
    }
  })
