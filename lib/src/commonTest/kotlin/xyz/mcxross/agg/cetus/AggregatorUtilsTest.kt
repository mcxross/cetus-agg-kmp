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
import io.kotest.matchers.shouldBe

class AggregatorUtilsTest :
  StringSpec({
    "completionCoin pads hex prefixes" {
      val input = "0x2::sui::SUI"
      val expected = "0x${"2".padStart(64, '0')}::sui::SUI"
      completionCoin(input) shouldBe expected
    }

    "processFlattenRoutes marks last use" {
      val paths =
        listOf(
          Path(
            id = "1",
            direction = true,
            provider = "CETUS",
            from = "A",
            target = "B",
            feeRate = 0.3,
            amountIn = "100",
            amountOut = "90",
          ),
          Path(
            id = "2",
            direction = true,
            provider = "CETUS",
            from = "B",
            target = "C",
            feeRate = 0.3,
            amountIn = "90",
            amountOut = "80",
          ),
          Path(
            id = "3",
            direction = true,
            provider = "CETUS",
            from = "B",
            target = "D",
            feeRate = 0.3,
            amountIn = "80",
            amountOut = "70",
          ),
          Path(
            id = "4",
            direction = true,
            provider = "CETUS",
            from = "C",
            target = "E",
            feeRate = 0.3,
            amountIn = "70",
            amountOut = "60",
          ),
        )
      val routerData =
        RouterDataV3(
          quoteID = "quote",
          amountIn = "100",
          amountOut = "60",
          byAmountIn = true,
          paths = paths,
          insufficientLiquidity = false,
        )

      val processed = processFlattenRoutes(routerData)
      val flags = processed.flattenedPaths.map { it.isLastUseOfIntermediateToken }

      flags shouldBe listOf(true, false, true, true)
    }
  })
